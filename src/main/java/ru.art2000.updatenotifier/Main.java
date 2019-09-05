package ru.art2000.updatenotifier;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import static spark.Spark.port;
import static spark.Spark.post;

@SpringBootApplication

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