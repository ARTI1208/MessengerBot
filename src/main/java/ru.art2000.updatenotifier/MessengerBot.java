package ru.art2000.updatenotifier;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import com.google.gson.Gson;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.pengrad.telegrambot.request.SendMessage;

import java.util.*;

public class MessengerBot extends WebhookBotHelper {

    private static final String token;
    private static final Firestore firestore = FirestoreClient.getFirestore();
    private static final DocumentReference availableUsersReference =
            firestore.collection("users").document("available");
    private static final HashMap<String, DocumentReference> contactsListReferenceMap =
            new HashMap<>();
    private static final DocumentReference currentContactReference =
            firestore.collection("users").document("current");
    private static HashMap<String, User> availablePartners = new HashMap<>();
    private static HashMap<User, HashMap<String, User>> userPartners = new HashMap<>();
    private static HashMap<User, User> currentContact = new HashMap<>();
    private static Gson gson = new Gson();
    private DebugHelper debugHelper = new DebugHelper(this);

    static {
        System.out.println("Getting token..");
        token = System.getenv("TOKEN");
        System.out.println("Getting values from db");
        System.out.println("Getting available users");
        ApiFuture<DocumentSnapshot> future = availableUsersReference.get();
        try {
            DocumentSnapshot snapshot = future.get();
            Map<String, Object> dataMap = snapshot.getData();
            if (dataMap != null) {
                for (Map.Entry<String, Object> pair : snapshot.getData().entrySet()) {
                    if (pair.getValue() instanceof String) {
                        User user = gson.fromJson((String) pair.getValue(), User.class);
                        availablePartners.put(pair.getKey(), user);
                    }
                }
            } else {
                System.out.println("Cannot retrieve av users data map");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Getting current contact");
        future = currentContactReference.get();
        try {
            DocumentSnapshot snapshot = future.get();
            Map<String, Object> dataMap = snapshot.getData();
            if (dataMap != null) {
                for (Map.Entry<String, Object> pair : snapshot.getData().entrySet()) {
                    if (pair.getValue() instanceof String) {
                        User contact = gson.fromJson((String) pair.getValue(), User.class);
                        User caller = gson.fromJson(pair.getKey(), User.class);
                        currentContact.put(caller, contact);
                    }
                }
            } else {
                System.out.println("Cannot retrieve cur contact data map");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Getting contacts lists");
        Iterable<DocumentReference> docs = firestore.collection("contacts").listDocuments();
        try {
            for (DocumentReference reference : docs) {
                String callerJson = reference.getId();
                User caller = gson.fromJson(callerJson, User.class);
                future = reference.get();
                DocumentSnapshot snapshot = future.get();
                Map<String, Object> dataMap = snapshot.getData();
                HashMap<String, User> realDataMap = userPartners.getOrDefault(caller, null);
                if (realDataMap == null) {
                    realDataMap = new HashMap<>();
                    userPartners.put(caller, realDataMap);
                }
                if (dataMap != null) {
                    for (Map.Entry<String, Object> pair : dataMap.entrySet()) {
                        if (pair.getValue() instanceof String) {
                            User contact = gson.fromJson((String) pair.getValue(), User.class);
                            realDataMap.put(pair.getKey(), contact);
                        }
                    }
                } else {
                    System.out.println("Cannot retrieve contacts lists data map");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    MessengerBot() {
        super(token);
        debugHelper.sendInfoMessage("Bot was recreated " + new Date().toString());
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

    private void addUserToAvailablePartners(User user) {
        availablePartners.put(String.valueOf(user.id()), user);
        availableUsersReference.set(generateMapToSave());
    }

    private void removeUserFromAvailablePartners(User user) {
        availablePartners.remove(String.valueOf(user.id()));
        availableUsersReference.set(generateMapToSave());
    }

    private void addUserToContactList(User caller, User contact) {
        HashMap<String, User> contacts = userPartners.getOrDefault(caller, null);
        if (contacts == null) {
            contacts = new HashMap<>();
        }
        contacts.put(String.valueOf(contact.id()), contact);
        setUserContactsList(caller, contacts);
    }

    private void removeUserFromContactList(User caller, User contactToRemove) {
        HashMap<String, User> contacts = userPartners.getOrDefault(caller, null);
        if (contacts == null) {
            return;
        }
        contacts.remove(String.valueOf(contactToRemove.id()));
        setUserContactsList(caller, contacts);
    }

    private void setUserContactsList(User caller, HashMap<String, User> contacts) {
        userPartners.put(caller, contacts);
        String callerId = String.valueOf(caller.id());
        HashMap<String, String> mapToPut = new HashMap<>();
        for (Map.Entry<String, User> entry : contacts.entrySet()) {
            mapToPut.put(entry.getKey(), gson.toJson(entry.getValue()));
        }
        String callerJson = gson.toJson(caller);
        DocumentReference userContactsReference = contactsListReferenceMap.getOrDefault(callerJson, null);
        if (userContactsReference == null) {
            userContactsReference = firestore.collection("contacts").document(callerJson);
            contactsListReferenceMap.put(callerId, userContactsReference);
        }
        userContactsReference.set(mapToPut);
    }

    private void setCurrentContact(User caller, User contact) {
        if (contact != null) {
            currentContact.put(caller, contact);
            sendMsg(caller.id(), "You're now talking with " + contact.firstName());
        } else if (currentContact.get(caller) != null) {
            currentContact.remove(caller);
        }

        HashMap<String, String> mapToPut = new HashMap<>();
        for (Map.Entry<User, User> pair : currentContact.entrySet()) {
            mapToPut.put(gson.toJson(pair.getKey()), gson.toJson(pair.getValue()));
        }
        currentContactReference.set(mapToPut);
    }


    private HashMap<String, String> generateMapToSave() {
        HashMap<String, String> map = new HashMap<>();
        for (Map.Entry<String, User> pair : availablePartners.entrySet()) {
            map.put(pair.getKey(), gson.toJson(pair.getValue()));
        }
        return map;
    }

    private void parseCommand(Message message) {
        String messageText = message.text().substring(1);
        long chatId = message.chat().id();
        String userId = String.valueOf(message.from().id());
        if (messageText.startsWith("show")) {
            if (availablePartners.containsKey(userId)) {
                sendMsg(chatId, "You're already visible for others!");
            } else {
                addUserToAvailablePartners(message.from());
                sendMsg(chatId, message.from().firstName() + ", now you're visible for others!");
            }
        } else if (messageText.startsWith("hide")) {
            if (availablePartners.containsKey(userId)) {
                removeUserFromAvailablePartners(message.from());
                sendMsg(chatId, "You can't be found now!");
            } else {
                sendMsg(chatId, "You're already hidden for others!");
            }
        } else if (messageText.startsWith("cancel")) {
            User contact = currentContact.getOrDefault(message.from(), null);
            if (contact == null) {
                sendMsg(chatId, "Cannot exit chat because you're not in any");
            } else {
                sendMsg(chatId, "You've exited chat with " + contact.firstName());
                setCurrentContact(message.from(), null);
            }
        } else if (messageText.startsWith("find")) {

            int size = availablePartners.size();

            if (size == 0) {
                sendMsg(chatId, "No available people to chat with");
                return;
            }

            SendMessage sendMessage = new SendMessage(chatId, "Choose your partner:");

            InlineKeyboardButton[][] buttons = new InlineKeyboardButton[size][1];

            Collection<User> users = availablePartners.values();
            Iterator<User> userIterator = users.iterator();

            for (int i = 0; i < size; i++) {

                User user = userIterator.next();
                if (user == null) {
                    continue;
                }

                buttons[i][0] = new InlineKeyboardButton(getUserFullName(user));
                buttons[i][0].callbackData("find:" + user.id());
            }

            Keyboard keyboard = new InlineKeyboardMarkup(buttons);

            sendMessage.replyMarkup(keyboard);
            execute(sendMessage);
        } else if (messageText.startsWith("partners")) {

            HashMap<String, User> contacts = userPartners.get(message.from());
            int size = contacts == null ? 0 : contacts.size();

            if (size == 0) {
                sendMsg(chatId, "You have no contacts. Try to /find them");
                return;
            }

            SendMessage sendMessage = new SendMessage(chatId, "List of your contacts:");

            InlineKeyboardButton[][] buttons = new InlineKeyboardButton[size][1];

            Collection<User> users = contacts.values();
            Iterator<User> userIterator = users.iterator();

            for (int i = 0; i < size; i++) {

                User user = userIterator.next();
                if (user == null) {
                    continue;
                }

                buttons[i][0] = new InlineKeyboardButton(getUserFullName(user));
                buttons[i][0].callbackData("contact:" + user.id());
            }

            Keyboard keyboard = new InlineKeyboardMarkup(buttons);

            sendMessage.replyMarkup(keyboard);
            execute(sendMessage);
        } else if (messageText.startsWith("remove")) {
            HashMap<String, User> contacts = userPartners.get(message.from());
            int size = contacts == null ? 0 : contacts.size();

            if (size == 0) {
                sendMsg(chatId, "You have no contacts. Try to /find them");
                return;
            }

            SendMessage sendMessage = new SendMessage(chatId, "Click contact to remove:");

            InlineKeyboardButton[][] buttons = new InlineKeyboardButton[size][1];
            Collection<User> users = contacts.values();
            Iterator<User> userIterator = users.iterator();

            for (int i = 0; i < size; i++) {

                User user = userIterator.next();
                if (user == null) {
                    continue;
                }

                buttons[i][0] = new InlineKeyboardButton(getUserFullName(user));
                buttons[i][0].callbackData("remove:" + user.id());
            }

            Keyboard keyboard = new InlineKeyboardMarkup(buttons);

            sendMessage.replyMarkup(keyboard);
            execute(sendMessage);
        } else if (messageText.startsWith("start")) {
            sendMsg(chatId, "Ancient evil has awakened!\n\n" +
                    "Commands:\n" +
                    "/show - Makes you available to be found by other users\n" +
                    "/hide - Makes you unavailable to be found by other users\n" +
                    "/find - Lists available users to start chat with\n" +
                    "/partners - Lists users you have chat with\n" +
                    "/remove - Lists partners you can delete chat with");
        } else {
            sendMsg(chatId, "Unknown command");
        }
    }

    private void proceedCallback(CallbackQuery callbackQuery) {
        String data = callbackQuery.data();
        String findPrefix = "find:";
        String contactPrefix = "contact:";
        String removePrefix = "remove:";
        if (data.startsWith(findPrefix)) {
            data = data.substring(findPrefix.length());
            User newPartner = availablePartners.getOrDefault(data, null);
            if (newPartner != null) {
                addUserToContactList(callbackQuery.from(), newPartner);
                setCurrentContact(callbackQuery.from(), newPartner);
            } else {
                sendMsg(callbackQuery.from().id(), "This user is not available now. " +
                        "Rerun /find to get updated list of available partners");
            }
        } else if (data.startsWith(contactPrefix)) {
            data = data.substring(contactPrefix.length());
            HashMap<String, User> contacts = userPartners.getOrDefault(callbackQuery.from(), null);
            if (contacts == null) {
                sendMsg(callbackQuery.from().id(), "You have no contacts. Try to /find them");
                return;
            }
            User newCurrentContact = contacts.getOrDefault(data, null);
            if (newCurrentContact == null) {
                sendMsg(callbackQuery.from().id(), "Cannot select user that is not in your contacts list. " +
                        "Rerun /partners to get updated list of your contacts");
                return;
            }
            setCurrentContact(callbackQuery.from(), newCurrentContact);
        } else if (data.startsWith(removePrefix)) {
            data = data.substring(removePrefix.length());
            HashMap<String, User> contacts = userPartners.getOrDefault(callbackQuery.from(), null);
            if (contacts == null) {
                sendMsg(callbackQuery.from().id(), "You have no contacts. Try to /find them");
                return;
            }
            User contactToRemove = contacts.get(data);
            if (contactToRemove != null) {
                removeUserFromContactList(callbackQuery.from(), contactToRemove);
                if (currentContact.get(callbackQuery.from()) == contactToRemove) {
                    setCurrentContact(callbackQuery.from(), null);
                }
                sendMsg(callbackQuery.from().id(),
                        "You've removed " +
                                contactToRemove.firstName() +
                                " from your contact list");
            } else {
                sendMsg(callbackQuery.from().id(), "Cannot remove user that is not in your contacts list. " +
                        "Rerun /remove to get updated list of your contacts available to remove");
            }
        } else {
            debugHelper.sendIssueMessage("Unknown callback query: " + callbackQuery.toString());
            sendMsg(callbackQuery.from().id(), "Unknown callback query");
        }
    }

    private String getUserFullName(User user) {
        String fullName = user.firstName();
        if (user.lastName() != null && !user.lastName().isEmpty()) {
            fullName += " " + user.lastName();
        }
        return fullName;
    }

    @Override
    protected void onReceiveWebhookUpdate(Update update) {
        Message message = update.message();
        if (message == null && update.callbackQuery() == null) {
            debugHelper.sendIssueMessage("This type of update is not supported: " + update.toString());
            return;
        }

        if (update.callbackQuery() != null) {
            proceedCallback(update.callbackQuery());
        } else if (message != null){
            if (message.text().startsWith("/")) {
                parseCommand(message);
            } else if (currentContact.containsKey(message.from())) {
                User partner = currentContact.get(message.from());
                String messageText = "Message from " + getUserFullName(message.from()) + ":\n" + message.text();
                sendMsg(partner.id(), messageText);
            } else {
                sendMsg(message.from().id(), message.text());
            }
        } else {
            debugHelper.sendIssueMessage("Cannot parse webhook data correctly: " + update.toString());
        }

    }

}
