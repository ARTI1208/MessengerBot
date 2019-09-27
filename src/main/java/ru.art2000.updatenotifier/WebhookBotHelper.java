package ru.art2000.updatenotifier;

import com.google.gson.Gson;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import spark.Request;
import spark.Response;
import spark.Route;

public abstract class WebhookBotHelper extends TelegramBot implements Route {

    private Gson gson = new Gson();

    public WebhookBotHelper(String botToken) {
        super(botToken);
    }

    protected abstract void onReceiveWebhookUpdate(com.pengrad.telegrambot.model.Update update);

    @Override
    public Object handle(Request request, Response response) {

        System.out.println("Req||" + request.body());
        System.out.println("Resp||" + response.body());
        Update update = gson.fromJson(request.body(), com.pengrad.telegrambot.model.Update.class);
        System.out.println("Updd||" + update);

        onReceiveWebhookUpdate(update);

        return "ok";
    }
}
