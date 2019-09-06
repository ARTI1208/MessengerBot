package ru.art2000.updatenotifier;

import static spark.Spark.port;
import static spark.Spark.post;

public class Main {


    public static void main(String[] args) {
        final String portNumber = System.getenv("PORT");
        System.out.println(portNumber);
        if (portNumber != null) {
            port(Integer.parseInt(portNumber));
        }
        MessengerBot bot = new MessengerBot();
        post(bot.getBotToken(), bot);
    }
}