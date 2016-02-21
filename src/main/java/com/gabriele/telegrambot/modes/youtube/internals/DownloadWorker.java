package com.gabriele.telegrambot.modes.youtube.internals;

import akka.actor.UntypedActor;
import com.gabriele.telegrambot.Bot;
import com.gabriele.telegrambot.modes.youtube.messages.DownloadMessage;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.request.InputFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;

public class DownloadWorker extends UntypedActor {

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
            String line;
            while ( (line = reader.readLine()) != null) {
                builder.append(line);
                builder.append(System.getProperty("line.separator"));
            }
            String filename = builder.toString();
            filename = filename.substring(0, filename.lastIndexOf(".")) + ".mp3";

            Bot.getInstance().sendMessage(chat.id(), "Downloading:\n" + filename);

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

