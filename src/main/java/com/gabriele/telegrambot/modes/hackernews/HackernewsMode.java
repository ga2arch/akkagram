package com.gabriele.telegrambot.modes.hackernews;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.gabriele.telegrambot.modes.hackernews.commands.ThresholdCommand;
import com.gabriele.telegrambot.modes.hackernews.internal.HNWorker;
import com.gabriele.telegrambot.modes.hackernews.messages.CheckHNMessage;
import com.gabriele.telegrambot.modes.Mode;
import com.gabriele.telegrambot.modes.hackernews.messages.SetThresholdMessage;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.pengrad.telegrambot.model.Message;
import scala.concurrent.duration.Duration;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class HackernewsMode extends Mode {

    HashMap<String, ActorRef> commands = new HashMap<>();

    RedisClient redisClient = RedisClient.create("redis://localhost:6379");
    RedisCommands<String, String> mDb = redisClient.connect().sync();

    ActorRef mHNWorker = getContext().actorOf(
            Props.create(HNWorker.class)
            .withDispatcher("commands-dispatcher"));

    @Override
    public void preStart() throws Exception {
        super.preStart();
        getContext()
                .system()
                .scheduler()
                .schedule(
                        Duration.Zero(),
                        Duration.create(2, TimeUnit.MINUTES),
                        mHNWorker,
                        new CheckHNMessage(),
                        getContext().system().dispatcher(),
                        null);

        commands.put("threshold", getContext().actorOf(
                Props.create(ThresholdCommand.class)
                        .withDispatcher("commands-dispatcher")));
    }

    @Override
    public void onReceive(Object in) throws Exception {
        if (in instanceof SetThresholdMessage) {
            SetThresholdMessage m = (SetThresholdMessage) in;
            mDb.hset(m.getUserid(), "hn:threshold", m.getThreshold());
        } else {
            super.onReceive(in);
        }
    }

    @Override
    protected String getName() {
        return "hackernews";
    }

    @Override
    protected HashMap<String, ActorRef> getCommands() {
        return commands;
    }

    @Override
    protected void run(Message message) {

    }

    @Override
    protected boolean isActive(Message message) {
        return mDb.sismember("hn:users", String.valueOf(message.chat().id()));
    }

    @Override
    protected void enable(Message message) {
        mDb.sadd("hn:users", String.valueOf(message.chat().id()));
        System.out.println("hn on");

    }

    @Override
    protected void disable(Message message) {
        mDb.srem("hn:users", String.valueOf(message.chat().id()));
        System.out.println("hn off");
    }
}
