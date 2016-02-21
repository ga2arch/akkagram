package com.gabriele.telegrambot;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.TelegramBotAdapter;
import com.pengrad.telegrambot.impl.BotApi;
import com.pengrad.telegrambot.impl.FileApi;
import retrofit.RestAdapter;

public class Bot extends TelegramBot {
    private static Bot instance;

    final ActorSystem system = ActorSystem.create();
    final ActorRef botActor = system.actorOf(Props.create(BotActor.class), "bot");

    public static Bot getInstance() {
        if (instance == null) {
            String token = System.getenv("TOKEN");
            RestAdapter restAdapter = TelegramBotAdapter.prepare(token).build();
            BotApi botApi = restAdapter.create(BotApi.class);
            FileApi fileApi = new FileApi(token);
            instance = new Bot(botApi, fileApi);
        }
        return instance;
    }

    public Bot(BotApi botApi, FileApi fileApi) {
        super(botApi, fileApi);
    }

    public void start() {
        botActor.tell(new InitBotMessage(), null);
    }

    public void stop() {
        system.terminate();
    }

    public static class InitBotMessage {}
}
