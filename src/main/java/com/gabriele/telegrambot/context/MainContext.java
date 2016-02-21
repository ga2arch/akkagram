package com.gabriele.telegrambot.context;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.gabriele.telegrambot.commands.PingCommand;
import com.gabriele.telegrambot.modes.hackernews.HackernewsMode;
import com.gabriele.telegrambot.modes.youtube.YoutubeMode;
import com.pengrad.telegrambot.model.Message;

import java.util.HashMap;

public class MainContext extends Context {

    HashMap<String, ActorRef> commands = new HashMap<>();
    HashMap<String, ActorRef> modes = new HashMap<>();

    @Override
    public void preStart() throws Exception {
        super.preStart();
        commands.put("ping", getContext().
                actorOf(Props.create(PingCommand.class)
                        .withDispatcher("commands-dispatcher")));

        modes.put("youtube", getContext().
                actorOf(Props.create(YoutubeMode.class)
                        .withDispatcher("commands-dispatcher"), "youtube"));

        modes.put("hackernews", getContext()
                .actorOf(Props.create(HackernewsMode.class)
                        .withDispatcher("commands-dispatcher")));
    }

    @Override
    protected String getName() {
        return "main";
    }

    @Override
    protected HashMap<String, ActorRef> getCommands() {
        return commands;
    }

    @Override
    protected HashMap<String, ActorRef> getModes() {
        return modes;
    }

    @Override
    protected void enable(Message message) {
        System.out.println("Main: Enabled");
    }

    @Override
    protected void disable(Message message) {
        System.out.println("Main: Disable");
    }

}
