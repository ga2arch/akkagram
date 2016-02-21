package com.gabriele.telegrambot.modes.youtube.messages;

import com.pengrad.telegrambot.model.Chat;

public class DownloadMessage {
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
