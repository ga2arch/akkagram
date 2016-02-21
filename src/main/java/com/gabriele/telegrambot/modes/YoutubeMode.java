package com.gabriele.telegrambot.modes;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.routing.*;
import com.gabriele.telegrambot.Bot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.InputFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.HashMap;

public class YoutubeMode extends Mode {

    private ActorRef downloader = getContext()
            .actorOf(FromConfig.getInstance()
                    .props(Props.create(DownloadWorker.class)), "downloader");

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
        System.out.println("Received link: " + message.text());
        downloader.tell(new DownloadMessage(message.chat(), message.text()), getSelf());
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

    public static class DownloadWorker extends UntypedActor {

        @Override
        public void onReceive(Object in) throws Exception {
            if (in instanceof DownloadMessage) {
                startDownload(((DownloadMessage) in).getChat(),
                        ((DownloadMessage) in).getLink());
            } else
                unhandled(in);
        }

        private void startDownload(Chat chat, String link) {
            try {
                Process process = new ProcessBuilder(
                        "youtube-dl",
                        "--get-filename",
                        link
                ).start();

                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder builder = new StringBuilder();
                String line = null;
                while ( (line = reader.readLine()) != null) {
                    builder.append(line);
                    builder.append(System.getProperty("line.separator"));
                }
                String filename = builder.toString();
                filename = filename.substring(0, filename.lastIndexOf(".")) + ".mp3";

                ProcessBuilder dlBuilder = new ProcessBuilder(
                        "youtube-dl",
                        "-x",
                        "--audio-format",
                        "mp3",
                        link);

                dlBuilder.directory(new File("static"));
                dlBuilder.start().waitFor();

                File file = Paths.get("static", filename).toFile();
                Bot.getInstance().sendAudio(chat.id(),
                        InputFile.audio(file),
                        null, null, null, null, null);

                file.delete();

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    public static class DownloadMessage {
        Chat chat;
        String link;

        public DownloadMessage(Chat chat, String link) {
            this.chat = chat;
            this.link = link;
        }

        public Chat getChat() {
            return chat;
        }

        public String getLink() {
            return link;
        }
    }
}
