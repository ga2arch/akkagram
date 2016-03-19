package com.gabriele.telegrambot.modes.youtube.internals;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.gabriele.telegrambot.Bot;
import com.gabriele.telegrambot.modes.youtube.messages.DownloadCompleted;
import com.gabriele.telegrambot.modes.youtube.messages.DownloadMessage;
import com.pengrad.telegrambot.model.request.InputFile;
import com.pengrad.telegrambot.response.SendResponse;
import okio.Okio;
import org.apache.commons.io.FileUtils;
import retrofit.RetrofitError;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class DownloadWorker extends UntypedActor {

    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

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
            log.debug(jobId + ") Fetching title for: " + url);
            Process process = new ProcessBuilder(
                    "youtube-dl",
                    "-o",
                    "%(title)s.%(ext)s",
                    "--get-filename",
                    url
            ).start();

            String stdin = Okio.buffer(Okio.source(process.getInputStream())).readUtf8();
            String stderr = Okio.buffer(Okio.source(process.getErrorStream())).readUtf8();

            if (!stderr.isEmpty()) {
                log.error(jobId + ") " + stderr);
                Bot.getInstance().sendMessage(chatId, stderr);
                getSender().tell(new DownloadError(jobId, url, stderr), getSelf());

            } else {
                String title = stdin.substring(0, stdin.lastIndexOf("."));
                String filename = title + ".mp3";
                Bot.getInstance().sendMessage(chatId, "Downloading:\n" + filename);

                log.info(jobId + ") Found title: " + title);
                log.info(jobId + ") Starting download");

                ProcessBuilder dlBuilder = new ProcessBuilder(
                        "youtube-dl",
                        "-o",
                        stdin,
                        "-x",
                        "--audio-format",
                        "mp3",
                        "-f",
                        "'best[filesize<50M]'",
                        url);

                log.info(jobId + ") Download finished");

                Path dlFolder = Files.createDirectories(Paths.get("static", UUID.randomUUID().toString()));
                try {
                    dlBuilder.directory(dlFolder.toFile());
                    dlBuilder.start().waitFor();

                    Path file = dlFolder.resolve(filename);
                    SendResponse resp = Bot.getInstance().sendAudio(chatId,
                            InputFile.audio(file.toFile()),
                            null, null, title, null, null);

                    String fileId = resp.message().audio().fileId();
                    getSender().tell(new DownloadCompleted(jobId, fileId, url), getSelf());

                } catch (RetrofitError e) {
                    getSender().tell(new DownloadError(jobId, url, e.getMessage()), getSelf());
                    e.printStackTrace();

                } finally {
                    FileUtils.deleteDirectory(dlFolder.toFile());
                }
            }

        } catch (IOException | InterruptedException e) {
            getSender().tell(new DownloadError(jobId, url, e.getMessage()), getSelf());
            e.printStackTrace();
        }
    }
}

