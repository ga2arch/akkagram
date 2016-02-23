package com.gabriele.telegrambot.modes.youtube;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.routing.*;
import com.gabriele.telegrambot.Bot;
import com.gabriele.telegrambot.modes.Mode;
import com.gabriele.telegrambot.modes.youtube.internals.DownloadError;
import com.gabriele.telegrambot.modes.youtube.internals.DownloadWorker;
import com.gabriele.telegrambot.modes.youtube.messages.DownloadCompleted;
import com.gabriele.telegrambot.modes.youtube.messages.DownloadMessage;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.InputFile;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.pengrad.telegrambot.response.SendResponse;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YoutubeMode extends Mode {

    RedisClient redisClient = RedisClient.create("redis://"+ System.getenv("REDIS") + ":6379");
    RedisCommands<String, String> mDb = redisClient.connect().sync();

    private ActorRef downloader = getContext()
            .actorOf(FromConfig.getInstance()
                    .props(Props.create(DownloadWorker.class)), "downloader");

    private Pattern youtubeRegex = Pattern.compile("(?:youtube\\.com\\/\\S*(?:(?:\\/e(?:mbed))?\\/|watch\\?(?:\\S*?&?v\\=))|youtu\\.be\\/)([a-zA-Z0-9_-]{6,11})");

    @Override
    public void preStart() throws Exception {
        super.preStart();

        File dlFolder = new File("static");
        FileUtils.deleteDirectory(dlFolder);

        for (String jobId: mDb.smembers("youtube:jobs")) {
            String url = mDb.hget(jobId, "url");
            String chatId = mDb.hget(jobId, "chatId");
            downloader.tell(new DownloadMessage(chatId, url, jobId), getSelf());
        }
    }

    @Override
    protected String getName() {
        return "youtube";
    }

    @Override
    protected HashMap<String, ActorRef> getCommands() {
        return new HashMap<>();
    }

    @Override
    protected void run(Message message) {
        Matcher m = youtubeRegex.matcher(message.text());
        if (m.find()) {
            System.out.println("Received id: " + m.group(1));
            String url = "https://www.youtube.com/watch?v=" + m.group(1);

            if (!sendFromCache(message.chat().id(), url)) {
                String jobId = saveJob(message.chat().id(), url);
                downloader.tell(new DownloadMessage(String.valueOf(message.chat().id()), url, jobId), getSelf());
            }
        }
    }

    private boolean sendFromCache(long chatId, String url) {
        String fileId = mDb.hget(url, "fileId");
        String error = mDb.hget(url, "error");

        if (fileId == null && error == null) {
            return false;
        }

        System.out.println("Sending from cache");

        if (fileId != null) {
            try {
                Bot.getInstance().sendAudio(chatId,
                        fileId, null, null, " ", null, null);
            } catch (Exception e) {
                return false;
            }
        } else {
            Bot.getInstance().sendMessage(chatId, error);
        }

        return true;
    }

    private String saveJob(long chatId, String url) {
        String jobId = UUID.randomUUID().toString();
        mDb.hset(jobId, "chatId", String.valueOf(chatId));
        mDb.hset(jobId, "url", url);
        mDb.sadd("youtube:jobs", jobId);
        System.out.println("Saved job: " + jobId);
        return jobId;
    }

    @Override
    public void onReceive(Object in) throws Exception {
        if (in instanceof DownloadCompleted) {
            DownloadCompleted m = (DownloadCompleted) in;
            mDb.del(m.getJobId());
            mDb.srem("youtube:jobs", m.getJobId());

            mDb.hset(m.getUrl(), "fileId", m.getFileId());

        } else if (in instanceof DownloadError) {
            DownloadError m = (DownloadError) in;
            mDb.del(m.getJobId());
            mDb.srem("youtube:jobs", m.getJobId());

            mDb.hset(m.getUrl(), "error", m.getError());
        } else
            super.onReceive(in);
    }

    @Override
    protected boolean isActive(Message message) {
        return true;
    }

    @Override
    protected void enable(Message message) {

    }

    @Override
    protected void disable(Message message) {

    }
}
