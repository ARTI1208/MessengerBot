package ru.art2000.updatenotifier;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import static spark.Spark.port;
import static spark.Spark.post;

public class Main {

    public static void main(String[] args) {

        try {
            File firebaseDir = new File("firebase");
            if (!firebaseDir.exists() && !firebaseDir.mkdir()) {
                System.out.println("Cannot initialize folder for firebase");
                return;
            }
            try (FileWriter writer = new FileWriter("firebase/serviceKey.json")) {
                writer.write(System.getenv("FIREBASE_CONFIG"));
            }
            try (FileInputStream serviceAccount = new FileInputStream("firebase/serviceKey.json")) {
                FirebaseOptions options = new FirebaseOptions.Builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
                FirebaseApp.initializeApp(options);
            }
        } catch (IOException e) {
            System.out.println("Cannot start Firebase");
            e.printStackTrace();
        }

        final String portNumber = System.getenv("PORT");
        if (portNumber != null) {
            port(Integer.parseInt(portNumber));
        }

        MessengerBot bot = new MessengerBot();
        post(bot.getBotToken(), bot);
    }
}