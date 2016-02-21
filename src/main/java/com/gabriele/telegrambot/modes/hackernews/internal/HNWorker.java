package com.gabriele.telegrambot.modes.hackernews.internal;

import akka.actor.UntypedActor;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import com.gabriele.telegrambot.Bot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.pengrad.telegrambot.model.request.ParseMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static akka.dispatch.Futures.future;

public class HNWorker extends UntypedActor {
    final Materializer mat = ActorMaterializer.create(getContext().system());
    final RedisClient redisClient = RedisClient.create("redis://localhost:6379");
    final RedisCommands<String, String> mDb = redisClient.connect().sync();

    final String baseUrl = "https://hacker-news.firebaseio.com/v0/";
    final String topUrl  = baseUrl + "topstories.json";
    final String itemUrl = baseUrl + "item/";

    @Override
    public void onReceive(Object in) throws Exception {
        System.out.println("Checking hn");
        checkHN();
    }

    private CompletableFuture<List<Long>> fetchTopStories() {
        final CompletionStage<HttpResponse> resp = Http.get(getContext().system())
                .singleRequest(HttpRequest.create(topUrl), mat);

        return resp.thenCompose((ctx) -> ctx.entity()
                .getDataBytes()
                .runFold(new StringBuilder(), (acc, elem) -> acc.append(elem.utf8String()), mat)
                .thenApply((content) -> {
                    Gson gson = new Gson();
                    List<Long> ids
                            = gson.fromJson(content.toString(),
                            new TypeToken<ArrayList<Long>>(){}.getType());

                    return ids;
                }).toCompletableFuture()).toCompletableFuture();
    }

    private CompletionStage<HNStory> fetchStory(String storyId) {
        final CompletionStage<HttpResponse> resp = Http.get(getContext().system())
                .singleRequest(HttpRequest.create(itemUrl + storyId + ".json"), mat);

        return resp.thenCompose((ctx) -> ctx.entity()
                .getDataBytes()
                .runFold(new StringBuilder(), (acc, elem) -> acc.append(elem.utf8String()), mat)
                .thenApply((content) -> {
                    Gson gson = new Gson();
                    return gson.fromJson(content.toString(), HNStory.class);
                }).toCompletableFuture())
                .toCompletableFuture();
    }

    private Set<String> getUsers() {
        return mDb.smembers("hn:users");
    }

    private boolean hasBeenSent(String userid, String storyId) {
        return mDb.sismember(userid + ":hn:sent", storyId);
    }

    private void setHasSent(String userid, String storyId) {
        mDb.sadd(userid + ":hn:sent", storyId);
    }

    private String getFromCache(String storyId) {
        return mDb.get(storyId + ":hn:cache");
    }

    private void saveInCache(HNStory story) {
        mDb.set(story.id + ":hn:cache", story.toString());
    }

    private void checkHN() {
        fetchTopStories()
                .thenApply((ids) -> {
                    for (int i=0; i < 20; i++) {
                        long storyId = ids.get(i);

                        for (String userid: getUsers()) {
                            future((Callable<Void>) () -> {
                                sendStory(userid, String.valueOf(storyId));
                                return null;
                            }, getContext().dispatcher());
                        }
                    }
                    return null;
                });
    }

    private void sendStory(String userid, String storyId) {
        if (!hasBeenSent(userid, storyId)) {
            String story = getFromCache(storyId);
            if (story != null) {
                Bot.getInstance().sendMessage(
                        userid,
                        story,
                        null,
                        true, null, null);

            } else {
                fetchStory(storyId)
                        .thenApply((hnStory -> {
                            saveInCache(hnStory);

                            Bot.getInstance().sendMessage(
                                    userid,
                                    hnStory.toString(),
                                    null,
                                    true, null, null);
                            return null;
                        }));
            }

            setHasSent(userid, storyId);
        }

    }
}