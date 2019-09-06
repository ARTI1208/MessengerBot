package ru.art2000.updatenotifier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pengrad.telegrambot.model.*;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.pengrad.telegrambot.model.request.ReplyKeyboardRemove;
import com.pengrad.telegrambot.request.SendMessage;

import java.io.File;
import java.util.*;

public class MessengerBot extends WebhookBotHelper {

    private static final String token;

    private static HashMap<Integer, User> availablePartners = new HashMap<>();
    private static HashMap<User, List<User>> userPartners = new HashMap<>();

    private static HashMap<User, User> currentContact = new HashMap<>();

    static {
        System.out.println("Getting token..");
        token = System.getenv("TOKEN");
    }

    MessengerBot(){
        super(token);
    }

    private synchronized void sendMsg(long chatId, String s) {
        SendMessage sendMessage = new SendMessage(chatId, s);
        try {
            execute(sendMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String getBotToken() {
        return token;
    }

    private void parseCommand(Message message) {
        String messageText = message.text().substring(1);
        long chatId = message.chat().id();
        if (messageText.startsWith("open")) {
            if (availablePartners.containsKey(message.from().id())) {
                sendMsg(chatId, "You're already visible for others!");
            } else {
                availablePartners.put(message.from().id(), message.from());
                sendMsg(chatId, message.from().firstName() + ", now you're visible for others!");
            }
        } else if (messageText.startsWith("close")) {
            if (availablePartners.containsKey(message.from().id())) {
                availablePartners.remove(message.from().id());
                sendMsg(chatId, "You can't be found now!");
            } else {
                sendMsg(chatId, "You're already hidden for others!");
            }
        } else if (messageText.startsWith("exit")) {
            currentContact.remove(message.from());
        } else if (messageText.startsWith("find_partner")) {

            int size = Math.min(5, availablePartners.size());

            if (size == 0){
                sendMsg(chatId, "No available peeople to chat with");
                return;
            }

            SendMessage sendMessage = new SendMessage(chatId, "Choose your partner:");

            InlineKeyboardButton[] buttons = new InlineKeyboardButton[size];

            Collection<User> users = availablePartners.values();
            Iterator<User> userIterator = users.iterator();

            for (int i = 0; i < size; i++) {

                User user = userIterator.next();
                if (user == null) {
                    System.out.println("Null user at " + i);
                    continue;
                }

                System.out.println("User" + message.contact());

                String firstOrFullName = (user.lastName() == null || user.lastName().isEmpty())
                        ? user.firstName()
                        : user.firstName() + " " + user.lastName();

                buttons[i] = new InlineKeyboardButton(firstOrFullName);
                buttons[i].callbackData("user:" + user.id());
            }

            Keyboard keyboard = new InlineKeyboardMarkup(buttons);

            sendMessage.replyMarkup(keyboard);
            execute(sendMessage);
        }
    }

    private void proceedCallback(CallbackQuery callbackQuery) {
        String data = callbackQuery.data();
        String userPrefix = "user:";
        System.out.println("CallbackQuery||" + data);
        if (data.startsWith(userPrefix)) {
            data = data.substring(userPrefix.length());
            User newPartner = availablePartners.getOrDefault(Integer.parseInt(data), null);
            System.out.println("CallbackQueryPartner||" + (newPartner == null));
            if (newPartner != null) {
                List<User> contacts = userPartners.getOrDefault(callbackQuery.from(), null);
                if (contacts == null) {
                    contacts = new ArrayList<>();
                }
                contacts.add(newPartner);
                userPartners.put(callbackQuery.from(), contacts);
                currentContact.put(callbackQuery.from(), newPartner);
                sendMsg(callbackQuery.from().id(), "You're now talking with " + newPartner.firstName());
            } else {
                sendMsg(callbackQuery.from().id(), "Something went wrong");
            }
        }
    }

    private void saveData(Long chatId){
        File personDir = new File(String.valueOf(chatId.longValue()));
        if (!personDir.exists()){
            personDir.mkdirs();
        }
        Gson gson = new Gson();
        System.out.println("AvPartners||" + gson.toJson(availablePartners));
    }

    private void restoreData(Long chatId){

    }

    @Override
    protected void onReceiveWebhookUpdate(Update update) {
        Message message = update.message();
        if (message == null){
            return;
        }
        Chat chat = message.chat();
        restoreData(chat.id());
        if (update.callbackQuery() != null) {
            proceedCallback(update.callbackQuery());
        } else if (message.text().startsWith("/")) {
            parseCommand(message);
        } else if (currentContact.containsKey(message.from())) {
            User partner = currentContact.get(message.from());
            sendMsg(partner.id(), message.text());
        } else {
            sendMsg(message.from().id(), message.text());
        }
        saveData(chat.id());
        System.out.println("Update " + update + " at " + new Date().toString());
        System.out.println("Message " + message + " at " + new Date().toString());
//        System.out.println("Chat " + chat + " at " + new Date().toString());
    }

}