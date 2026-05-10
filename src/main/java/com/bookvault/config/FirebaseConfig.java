package com.bookvault.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {
            GoogleCredentials credentials;
            
            // Step 1: Try to load JSON string directly from Environment Variable
            String envJson = System.getenv("GOOGLE_CREDENTIALS_JSON");
            String envPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
            
            if (envJson != null && !envJson.isEmpty()) {
                credentials = GoogleCredentials.fromStream(new java.io.ByteArrayInputStream(envJson.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            } else if (envPath != null && !envPath.isEmpty()) {
                // Step 2: Try to load from Render Environment Variable path (Production)
                credentials = GoogleCredentials.getApplicationDefault();
            } else {
                // Step 3: Fall back to local src/main/resources (Development)
                java.io.InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream("serviceAccountKey.json");
                if (serviceAccount == null) return;
                credentials = GoogleCredentials.fromStream(serviceAccount);
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setProjectId("bookvault-c0995")
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("Firebase Admin SDK (Firestore & Auth) initialized successfully!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
