package com.gabriele.telegrambot.context;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import com.gabriele.telegrambot.commands.Command;
import com.gabriele.telegrambot.modes.Mode;
import com.pengrad.telegrambot.model.Message;

import java.util.HashMap;

public abstract class Context extends UntypedActor {

    protected abstract String getName();
    protected abstract HashMap<String, ActorRef> getModes();
    protected abstract HashMap<String, ActorRef> getCommands();

    protected abstract void enable(Message message);
    protected abstract void disable(Message message);

    @Override
    public void onReceive(Object in) throws Exception {
        if (in instanceof Message) {
            String cmd = Command.findCommand((Message) in);

            if (cmd != null) {
                if (cmd.equals("enter")) {
                    enable((Message) in);

                } else if (cmd.equals("end")) {
                    disable((Message) in);

                } else if (getCommands().containsKey(cmd)) {
                    getCommands().get(cmd).tell(in, getSelf());

                } else if (getModes().containsKey(cmd)) {
                    getModes().get(cmd).tell(in, getSelf());

                } else {
                    for (ActorRef mode: getModes().values()) {
                        mode.tell(in, getSelf());
                    }
                }

            } else {
                for (ActorRef mode: getModes().values()) {
                    mode.tell(in, getSelf());
                }
            }
        }
    }
}
