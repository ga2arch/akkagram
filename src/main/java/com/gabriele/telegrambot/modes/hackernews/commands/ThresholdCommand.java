package com.gabriele.telegrambot.modes.hackernews.commands;

import akka.actor.ActorRef;
import com.gabriele.telegrambot.commands.Command;
import com.gabriele.telegrambot.modes.hackernews.messages.SetThresholdMessage;
import com.pengrad.telegrambot.model.Message;

import java.util.regex.Matcher;

public class ThresholdCommand extends Command {

    @Override
    protected String getName() {
        return "threshold";
    }

    @Override
    protected String getArgsRegex() {
        return "([0-9]+)";
    }

    @Override
    protected void run(Message message, Matcher matcher) {
        getSender().tell(new SetThresholdMessage(message.chat().id(),
                Integer.valueOf(matcher.group(1))), getSelf());
    }
}
