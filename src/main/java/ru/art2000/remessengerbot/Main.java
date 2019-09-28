package ru.art2000.remessengerbot;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.pengrad.telegrambot.request.SetWebhook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import static spark.Spark.port;
import static spark.Spark.post;

public class Main {

    public static void main(String[] args) {

        //Setup Firebase
        try {
            File firebaseDir = new File("firebase");
            if (!firebaseDir.exists() && !firebaseDir.mkdir()) {
                System.out.println("Cannot initialize folder for firebase");
                return;
            }
            try (FileWriter writer = new FileWriter("firebase/serviceKey.json")) {
                writer.write(System.getenv("FIREBASE_CONFIG"));
            }
            String firebaseDatabaseUrl = System.getenv("FIRESTORE_DB");
            try (FileInputStream serviceAccount = new FileInputStream("firebase/serviceKey.json")) {
                FirebaseOptions.Builder optionsBuilder = new FirebaseOptions.Builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount));
                if (firebaseDatabaseUrl != null) {
                    optionsBuilder.setDatabaseUrl(firebaseDatabaseUrl);
                }
                FirebaseApp.initializeApp(optionsBuilder.build());
            }
        } catch (IOException e) {
            System.out.println("Cannot start Firebase");
            e.printStackTrace();
        }

        //Setup working port
        final String portNumber = System.getenv("PORT");
        if (portNumber != null) {
            port(Integer.parseInt(portNumber));
        }

        //Create bot
        MessengerBot bot = new MessengerBot();
        post(bot.getBotToken(), bot);

        //Setup webhook
        String appUrl = System.getenv("APP_URL");
        if (appUrl != null) {
            if (!appUrl.endsWith("/")) {
                appUrl += "/";
            }
            bot.execute(new SetWebhook().url(appUrl + bot.getBotToken()));
        }
    }
}