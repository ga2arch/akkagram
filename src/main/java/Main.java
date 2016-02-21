import com.gabriele.telegrambot.Bot;

import java.io.IOException;

public class Main  {

    public static void main(String[] args) throws IOException, InterruptedException {
        Bot.getInstance().start();
        System.out.println("Starting up ...");
        Thread.currentThread().join();
        Bot.getInstance().stop();
    }
}
