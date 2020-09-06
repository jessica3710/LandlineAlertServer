import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class LandLineAlertServer {
    private static final List<String> registrationTokens = new ArrayList<>();

    private static LandLineAlertServer landLineAlertServer = null;

    public static LandLineAlertServer getInstance() throws IOException {
        if (landLineAlertServer == null) {
            landLineAlertServer = new LandLineAlertServer();
        }
        return landLineAlertServer;
    }

    private LandLineAlertServer() throws IOException {
        Properties prop = new Properties();
        String fileName = "config.properties";

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);

        if (inputStream != null) {
            prop.load(inputStream);
        } else {
            throw new FileNotFoundException("property file '" + fileName + "' not found in the classpath");
        }
        String tokens = prop.getProperty("tokens");
        String[] split = tokens.split(",");
        for(String s : split) {
            registrationTokens.add(s.trim());
        }

        // Replace this with the path to your google private key json file
        FileInputStream serviceAccount =
                new FileInputStream("google_application_credentials.json");

        // set to your own firebase project url
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setDatabaseUrl("https://home-phone-notifications.firebaseio.com")
                .build();

        FirebaseApp.initializeApp(options);
    }

    public void sendMessage(String title, String body) throws FirebaseMessagingException {
        MulticastMessage message = MulticastMessage.builder()
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).build())
                .addAllTokens(registrationTokens)
                .build();

        BatchResponse response = FirebaseMessaging.getInstance().sendMulticast(message);
        if (response.getFailureCount() > 0) {
            List<SendResponse> responses = response.getResponses();
            List<String> failedTokens = new ArrayList<>();
            for (int i = 0; i < responses.size(); i++) {
                if (!responses.get(i).isSuccessful()) {
                    // The order of responses corresponds to the order of the registration tokens.
                    failedTokens.add(registrationTokens.get(i));
                }
            }
            System.out.println("List of tokens that caused failures: " + failedTokens);
        }
    }
}
