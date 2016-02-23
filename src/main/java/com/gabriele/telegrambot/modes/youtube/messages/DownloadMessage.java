package com.gabriele.telegrambot.modes.youtube.messages;

import com.pengrad.telegrambot.model.Chat;

public class DownloadMessage {
    String chatId;
    String link;
    String jobId;

    public DownloadMessage(String chatId, String link, String jobId) {
        this.chatId = chatId;
        this.link = link;
        this.jobId = jobId;
    }

    public String getChatId() {
        return chatId;
    }

    public String getLink() {
        return link;
    }

    public String getJobId() {
        return jobId;
    }
}

