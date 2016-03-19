package com.gabriele.telegrambot.commands;

import com.gabriele.telegrambot.Bot;
import com.pengrad.telegrambot.model.Message;

import java.util.regex.Matcher;

public class PingCommand extends Command {
    @Override
    public String getName() {
        return "ping";
    }

    @Override
    protected String getArgsRegex() {
        return "";
    }

    @Override
    protected void run(Message message, Matcher matcher) {
        Bot.getInstance().sendMessage(message.chat().id(), "PONG");
    }
}
