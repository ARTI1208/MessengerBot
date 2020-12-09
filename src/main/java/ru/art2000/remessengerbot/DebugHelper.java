package ru.art2000.remessengerbot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.request.SendMessage;

class DebugHelper {

    private final String chatId;
    private final TelegramBot bot;

    DebugHelper(TelegramBot telegramBot) {
        bot = telegramBot;
        chatId = System.getenv("ISSUE_CHAT");
    }

    void sendIssueMessage(String text) {
        if (isDebuggingEnabled()) {
            bot.execute(new SendMessage(chatId, "ISSUE: " + text));
        }
        System.out.println("ISSUE: " + text);
    }

    void sendInfoMessage(String text) {
        if (isDebuggingEnabled()) {
            bot.execute(new SendMessage(chatId, "INFO: " + text));
        }
        System.out.println("INFO: " + text);
    }

    void sendMessageSenderInfo(User user) {
        if (String.valueOf(user.id()).equals(chatId)) return;

        sendInfoMessage("Received message from: " + user.toString());
    }

    private boolean isDebuggingEnabled() {
        String debug = System.getenv("DEBUG");
        if (debug == null) {
            return false;
        }
        try {
            int val = Integer.parseInt(debug);
            return val != 0;
        } catch (Exception ignored) {

        }
        return false;
    }
}
