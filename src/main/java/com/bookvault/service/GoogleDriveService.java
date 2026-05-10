package com.bookvault.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
public class GoogleDriveService {

    @Value("classpath:serviceAccountKey.json")
    private Resource credentialsResource;

    @Value("${google.drive.folder.id:}")
    private String defaultFolderId;

    private Drive driveService;

    @PostConstruct
    public void init() throws IOException, GeneralSecurityException {
        GoogleCredentials credentials = null;
        String envJson = System.getenv("GOOGLE_CREDENTIALS_JSON");
        String envPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");

        if (envJson != null && !envJson.isEmpty()) {
            credentials = GoogleCredentials.fromStream(new java.io.ByteArrayInputStream(envJson.getBytes(StandardCharsets.UTF_8)))
                    .createScoped(Collections.singleton("https://www.googleapis.com/auth/drive"));
        } else if (envPath != null && !envPath.isEmpty()) {
            credentials = GoogleCredentials.getApplicationDefault()
                    .createScoped(Collections.singleton("https://www.googleapis.com/auth/drive"));
        } else if (credentialsResource.exists()) {
            try (InputStream is = credentialsResource.getInputStream()) {
                credentials = GoogleCredentials.fromStream(is)
                        .createScoped(Collections.singleton("https://www.googleapis.com/auth/drive"));
            }
        }

        if (credentials != null) {
            this.driveService = new Drive.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("BookVault")
                    .build();
            System.out.println("Google Drive Service initialized successfully.");
        } else {
            System.err.println("Google Drive credentials not found via GOOGLE_APPLICATION_CREDENTIALS or classpath:serviceAccountKey.json");
        }
    }

    public String uploadJsonToDrive(String fileName, String jsonContent) throws IOException {
        if (this.driveService == null) {
            throw new RuntimeException("Google Drive Service not initialized.");
        }

        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        fileMetadata.setMimeType("application/json");
        if (defaultFolderId != null && !defaultFolderId.isEmpty()) {
            fileMetadata.setParents(Collections.singletonList(defaultFolderId));
        }

        java.io.File tempFile = java.io.File.createTempFile("book-", ".json");
        java.nio.file.Files.write(tempFile.toPath(), jsonContent.getBytes(StandardCharsets.UTF_8));

        FileContent mediaContent = new FileContent("application/json", tempFile);
        File file = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id, webViewLink")
                .execute();

        tempFile.delete();
        return file.getId();
    }
}
