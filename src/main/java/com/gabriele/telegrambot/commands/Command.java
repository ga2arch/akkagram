package com.gabriele.telegrambot.commands;

import akka.actor.UntypedActor;
import com.pengrad.telegrambot.model.Message;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Command extends UntypedActor {

    protected abstract String getName();
    protected abstract String getArgsRegex();

    protected Pattern getPattern() {
        return Pattern.compile(String.format("/%s %s", getName(), getArgsRegex()));
    }

    protected abstract void run(Message message, Matcher matcher);

    @Override
    public void onReceive(Object in) throws Exception {
        if (in instanceof Message) {
            Matcher m = getPattern().matcher(((Message) in).text());
            System.out.println(((Message) in).text());

            if (m.matches()) {
                run((Message) in, m);
            }
        } else
            unhandled(in);
    }

    public static String findCommand(Message message) {
        Pattern pattern = Pattern.compile("/(\\w+)");

        if (message.text().startsWith("/")) {
            Matcher m = pattern.matcher(message.text());
            if (m.find()) {
                return m.group(1);
            }
        }

        return null;
    }
}
