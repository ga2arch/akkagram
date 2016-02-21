import com.gabriele.telegrambot.Bot;

import java.io.IOException;

public class Main  {

    public static void main(String[] args) throws IOException {

        Bot.getInstance().start();
        System.out.println("Type RETURN to exit");
        System.in.read();
        Bot.getInstance().stop();
    }


}
