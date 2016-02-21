package com.gabriele.telegrambot.modes;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import com.gabriele.telegrambot.commands.Command;
import com.pengrad.telegrambot.model.Message;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Mode extends UntypedActor {

    protected abstract String getName();
    protected abstract HashMap<String, ActorRef> getCommands();

    protected abstract void run(Message message);
    protected abstract boolean isActive(Message message);
    protected abstract void enable(Message message);
    protected abstract void disable(Message message);

    public String getEnableRegex() {
        return String.format("/%s on", getName());
    }

    public String getDisableRegex() {
        return String.format("/%s off", getName());
    }

    @Override
    public void onReceive(Object in) throws Exception {
        if (in instanceof Message) {
            Message message = (Message) in;

            String cmd = Command.findCommand(message);

            if (cmd != null && isActive(message) && getCommands().containsKey(cmd)) {
                getCommands().get(cmd).tell(in, getSelf());

            } else {
                Pattern on = Pattern.compile(getEnableRegex());
                Pattern off = Pattern.compile(getDisableRegex());

                Matcher mOn = on.matcher(message.text());
                Matcher mOff = off.matcher(message.text());

                if (isActive(message)) {
                    if (mOff.matches()) {
                        disable(message);
                    } else {
                        run(message);
                    }
                } else if (mOn.matches()) {
                    enable(message);
                }
            }
        } else
            unhandled(in);
    }
}
