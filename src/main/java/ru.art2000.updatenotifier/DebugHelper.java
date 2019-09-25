package ru.art2000.updatenotifier;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;

class DebugHelper {

    private String chatId;
    private TelegramBot bot;

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
