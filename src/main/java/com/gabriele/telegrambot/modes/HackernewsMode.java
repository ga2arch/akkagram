package com.gabriele.telegrambot.modes;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import com.gabriele.telegrambot.Bot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.ParseMode;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static akka.dispatch.Futures.future;

public class HackernewsMode extends Mode {

    HashMap<Long, ArrayList<Long>> mUsers = new HashMap<>();
    ActorRef mHNWorker = getContext().actorOf(
            Props.create(HackernewsWorker.class));

    @Override
    public void preStart() throws Exception {
        super.preStart();
        getContext()
                .system()
                .scheduler()
                .schedule(Duration.Zero(),
                    Duration.create(10,
                            TimeUnit.SECONDS),
                        mHNWorker,
                        mUsers,
                        getContext()
                                .system()
                                .dispatcher(),
                        null);
    }

    @Override
    protected String getName() {
        return "hackernews";
    }

    @Override
    protected HashMap<String, ActorRef> getCommands() {
        return new HashMap<>();
    }

    @Override
    protected void run(Message message) {

    }

    @Override
    protected boolean isActive(Message message) {
        return mUsers.containsKey(message.chat().id());
    }

    @Override
    protected void enable(Message message) {
        mUsers.put(message.chat().id(), new ArrayList<>());
        System.out.println("hn on");

    }

    @Override
    protected void disable(Message message) {
        mUsers.remove(message.chat().id());
        System.out.println("hn off");
    }

    public static class HackernewsWorker extends UntypedActor {
        final Materializer mat = ActorMaterializer.create(getContext().system());

        String baseUrl = "https://hacker-news.firebaseio.com/v0/";
        String topUrl  = baseUrl + "topstories.json";
        String itemUrl = baseUrl + "item/";

        HashMap<Long, ArrayList<Long>> mUsers = new HashMap<>();
        ArrayList<Long> history      = new ArrayList<>();
        HashMap<Long, HNStory> cache = new HashMap<>();

        @Override
        public void onReceive(Object in) throws Exception {
            mUsers = (HashMap<Long, ArrayList<Long>>) in;
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

        private CompletionStage<HNStory> fetchStory(long id) {
            final CompletionStage<HttpResponse> resp = Http.get(getContext().system())
                    .singleRequest(HttpRequest.create(itemUrl + id + ".json"), mat);

            return resp.thenCompose((ctx) -> ctx.entity()
                    .getDataBytes()
                    .runFold(new StringBuilder(), (acc, elem) -> acc.append(elem.utf8String()), mat)
                    .thenApply((content) -> {
                        Gson gson = new Gson();
                        return gson.fromJson(content.toString(), HNStory.class);
                    }).toCompletableFuture())
                    .toCompletableFuture();
        }

        private void checkHN() {
            fetchTopStories()
                    .thenApply((ids) -> {
                        for (int i=0; i < 5; i++) {
                            long storyId = ids.get(i);

                            for (long userid: mUsers.keySet()) {
                                future((Callable<Void>) () -> {
                                        sendStory(userid, storyId);
                                        return null;
                                }, getContext().dispatcher());
                            }
                        }
                        return null;
                    });
        }

        private void sendStory(long userid, long storyId) {
            ArrayList<Long> sent = mUsers.get(userid);

            if (!sent.contains(storyId)) {
                if (cache.containsKey(storyId)) {
                    Bot.getInstance().sendMessage(
                            userid,
                            cache.get(storyId).toString(),
                            ParseMode.Markdown,
                            false, null, null);

                } else {
                    fetchStory(storyId)
                            .thenApply((hnStory -> {
                                cache.put(storyId, hnStory);
                                Bot.getInstance().sendMessage(
                                        userid,
                                        hnStory.toString(),
                                        ParseMode.Markdown,
                                        false, null, null);
                                return null;
                            }));
                }

                sent.add(storyId);
            }

        }
    }

    public class HNStory {
        public long id;
        public String title;
        public String url;

        private String getCommentUrl() {
            return "https://news.ycombinator.com/item?id=" + id;
        }

        @Override
        public String toString() {
            return String.format("[%s](%s)   \n%s  \n   \n[comments](%s)",
                    title, url, "", getCommentUrl());
        }
    }
}
