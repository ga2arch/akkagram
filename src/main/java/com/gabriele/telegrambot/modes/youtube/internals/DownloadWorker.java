package com.gabriele.telegrambot.modes.youtube.internals;

import akka.actor.UntypedActor;
import com.gabriele.telegrambot.Bot;
import com.gabriele.telegrambot.modes.youtube.messages.DownloadCompleted;
import com.gabriele.telegrambot.modes.youtube.messages.DownloadMessage;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.request.InputFile;
import okio.Okio;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;

public class DownloadWorker extends UntypedActor {

    @Override
    public void onReceive(Object in) throws Exception {
        if (in instanceof DownloadMessage) {
            startDownload(((DownloadMessage) in).getChatId(),
                    ((DownloadMessage) in).getLink(),
                    ((DownloadMessage) in).getJobId());
        } else
            unhandled(in);
    }

    private void startDownload(String chatId, String link, String jobId) {
        try {
            Process process = new ProcessBuilder(
                    "youtube-dl",
                    "--get-filename",
                    link
            ).start();

            String stdin = Okio.buffer(Okio.source(process.getInputStream())).readUtf8();
            String stderr = Okio.buffer(Okio.source(process.getErrorStream())).readUtf8();

            if (!stderr.isEmpty()) {
                Bot.getInstance().sendMessage(chatId, stderr);
                
            } else {
                String filename = stdin.substring(0, stdin.lastIndexOf(".")) + ".mp3";
                Bot.getInstance().sendMessage(chatId, "Downloading:\n" + filename);

                ProcessBuilder dlBuilder = new ProcessBuilder(
                        "youtube-dl",
                        "-x",
                        "--audio-format",
                        "mp3",
                        link);

                dlBuilder.directory(new File("static"));
                dlBuilder.start().waitFor();

                File file = Paths.get("static", filename).toFile();
                Bot.getInstance().sendAudio(chatId,
                        InputFile.audio(file),
                        null, null, null, null, null);

                file.delete();
            }

            getSender().tell(new DownloadCompleted(jobId), getSelf());

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}

