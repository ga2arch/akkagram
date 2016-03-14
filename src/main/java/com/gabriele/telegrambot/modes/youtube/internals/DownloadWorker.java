package com.gabriele.telegrambot.modes.youtube.internals;

import akka.actor.UntypedActor;
import com.gabriele.telegrambot.Bot;
import com.gabriele.telegrambot.modes.youtube.messages.DownloadCompleted;
import com.gabriele.telegrambot.modes.youtube.messages.DownloadMessage;
import com.pengrad.telegrambot.model.request.InputFile;
import com.pengrad.telegrambot.response.SendResponse;
import okio.Okio;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

            // TODO retrieve Song name and performer
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

                Path dlFolder = Files.createDirectories(Paths.get("static", UUID.randomUUID().toString()));
                try {
                    dlBuilder.directory(dlFolder.toFile());
                    dlBuilder.start().waitFor();

                    Path file = dlFolder.resolve(filename);
                    SendResponse resp = Bot.getInstance().sendAudio(chatId,
                            InputFile.audio(file.toFile()),
                            null, null, null, null, null);

                    if (resp.isOk()) {
                        String fileId = resp.message().audio().fileId();
                        getSender().tell(new DownloadCompleted(jobId, fileId, url), getSelf());
                    } else {
                        getSender().tell(new DownloadError(jobId, url, "File too big"), getSelf());
                    }
                } finally {
                    FileUtils.deleteDirectory(dlFolder.toFile());
                }
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}

