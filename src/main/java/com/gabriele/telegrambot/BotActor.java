package com.gabriele.telegrambot;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.Handler;
import akka.http.javadsl.server.HttpApp;
import akka.http.javadsl.server.Route;
import akka.http.javadsl.server.RouteResult;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import com.gabriele.telegrambot.context.MainContext;
import com.pengrad.telegrambot.BotUtils;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;

import java.util.HashMap;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BotActor extends UntypedActor {

    final Materializer mat = ActorMaterializer.create(getContext().system());
    HashMap<String, ActorRef> contexts = new HashMap<>();

    ActorRef currentContext;

    @Override
    public void onReceive(Object in) throws Exception {
        if (in instanceof Message) {
            if (((Message) in).text().startsWith("/enter")) {
                changeContex(((Message) in));
            } else {
                currentContext.tell(in, getSelf());
            }
        }
        else if (in instanceof Bot.InitBotMessage) {
            loadContexts();
            startServer();
        }
        else
            unhandled(in);
    }

    private void loadContexts() {
        ActorRef main = getContext()
                .actorOf(Props.create(MainContext.class)
                .withDispatcher("commands-dispatcher"), "main");

        contexts.put("main", main);

        currentContext = main;
    }

    private void changeContex(Message message) {
        Pattern enter = Pattern.compile("/enter (.*)");
        Matcher m = enter.matcher(message.text());
        if (m.matches()) {
            String name = m.group(1);
            if (contexts.containsKey(name)) {
                System.out.println("Changing context: " + name);

                currentContext = contexts.get(name);
                currentContext.tell(message, getSelf());
            }
        }
    }

    private void startServer() {
        HttpApp server = new HttpApp() {
            @Override
            public Route createRoute() {
                Route mainRoute = handleWith((Handler) ctx -> {
                    final CompletionStage<RouteResult> r = ctx.request().entity()
                            .getDataBytes()
                            .runFold(new StringBuilder(), (acc, elem) -> acc.append(elem.utf8String()), mat)
                            .thenApply((resp) -> {
                                Update update = BotUtils.parseUpdate(resp.toString());
                                if (update.message() != null)
                                    getSelf().tell(update.message(), null);

                                return ctx.completeWithStatus(StatusCodes.OK);
                            });

                    return ctx.completeWith(r);
                });

                return route(post(pathSingleSlash().route(mainRoute)));
            }
        };

        server.bindRoute("localhost", 8000, getContext().system());
    }
}
