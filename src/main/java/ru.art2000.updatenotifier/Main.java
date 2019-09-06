package ru.art2000.updatenotifier;

import static spark.Spark.port;
import static spark.Spark.post;

public class Main {


    public static void main(String[] args) {
        final String portNumber = System.getenv("PORT");
        if (portNumber != null) {
            port(Integer.parseInt(portNumber));
        }
        NotifierBot bot = NotifierBot.newBot();
        post(bot.getBotToken(), bot);
    }
}