package com.gabriele.telegrambot.modes.youtube.internals;

import akka.actor.UntypedActor;
import com.gabriele.telegrambot.Bot;
import com.gabriele.telegrambot.modes.youtube.messages.DownloadCompleted;
import com.gabriele.telegrambot.modes.youtube.messages.DownloadMessage;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.request.InputFile;
import com.pengrad.telegrambot.response.SendResponse;
import okio.Okio;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.UUID;

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

    private void startDownload(String chatId, String url, String jobId) {
        try {
            Process process = new ProcessBuilder(
                    "youtube-dl",
                    "--get-filename",
                    url
            ).start();

            String stdin = Okio.buffer(Okio.source(process.getInputStream())).readUtf8();
            String stderr = Okio.buffer(Okio.source(process.getErrorStream())).readUtf8();

            if (!stderr.isEmpty()) {
                Bot.getInstance().sendMessage(chatId, stderr);
                getSender().tell(new DownloadError(jobId, url, stderr), getSelf());

            } else {
                String filename = stdin.substring(0, stdin.lastIndexOf(".")) + ".mp3";
                Bot.getInstance().sendMessage(chatId, "Downloading:\n" + filename);

                ProcessBuilder dlBuilder = new ProcessBuilder(
                        "youtube-dl",
                        "-x",
                        "--audio-format",
                        "mp3",
                        url);

                File dlFolder = new File("static/" + UUID.randomUUID().toString());
                try {
                    dlFolder.mkdirs();

                    dlBuilder.directory(dlFolder);
                    dlBuilder.start().waitFor();

                    File file = Paths.get(dlFolder.getAbsolutePath(), filename).toFile();
                    SendResponse resp = Bot.getInstance().sendAudio(chatId,
                            InputFile.audio(file),
                            null, null, null, null, null);

                    if (resp.isOk()) {
                        String fileId = resp.message().audio().fileId();
                        getSender().tell(new DownloadCompleted(jobId, fileId, url), getSelf());
                    } else {
                        getSender().tell(new DownloadError(jobId, url, "File to big"), getSelf());
                    }
                } finally {
                    FileUtils.deleteDirectory(dlFolder);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

