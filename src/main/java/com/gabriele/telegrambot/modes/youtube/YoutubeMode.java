package com.gabriele.telegrambot.modes.youtube;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.routing.*;
import com.gabriele.telegrambot.Bot;
import com.gabriele.telegrambot.modes.Mode;
import com.gabriele.telegrambot.modes.youtube.internals.DownloadWorker;
import com.gabriele.telegrambot.modes.youtube.messages.DownloadMessage;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.InputFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YoutubeMode extends Mode {

    private ActorRef downloader = getContext()
            .actorOf(FromConfig.getInstance()
                    .props(Props.create(DownloadWorker.class)), "downloader");

    private Pattern youtubeRegex = Pattern.compile("(?:youtube\\.com\\/\\S*(?:(?:\\/e(?:mbed))?\\/|watch\\?(?:\\S*?&?v\\=))|youtu\\.be\\/)([a-zA-Z0-9_-]{6,11})");

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
            System.out.println("Received link: " + m.group(1));
            String url = "https://www.youtube.com/watch?v=" + m.group(1);
            downloader.tell(new DownloadMessage(message.chat(), url), getSelf());
        }
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
