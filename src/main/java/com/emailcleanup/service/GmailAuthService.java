package com.emailcleanup.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.util.Comparator;
import java.util.List;

public class GmailAuthService {
    private static final Logger logger = LoggerFactory.getLogger(GmailAuthService.class);

    private static final String APPLICATION_NAME = "Smart Email Cleanup Assistant";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    
    // 🔥 PERMANENT STORAGE: Saves config to User Home Directory so it survives app updates
    private static final File APP_DIR = new File(System.getProperty("user.home"), ".smart-email-cleanup");
    private static final File CREDENTIALS_FILE = new File(APP_DIR, "credentials.json");
    private static final File TOKENS_DIR = new File(APP_DIR, "tokens");

    /**
     * 🔥 SCOPES — KEPT YOUR CUSTOM FULL ACCESS CONFIG
     * Full scope "https://mail.google.com/" is required for hard deletes.
     */
    private static final List<String> SCOPES = List.of(
            "https://mail.google.com/",   // Full Gmail access (delete + modify + batch)
            GmailScopes.GMAIL_MODIFY,
            GmailScopes.GMAIL_READONLY
    );

    private static GmailAuthService instance;
    private Gmail gmailService;

    private GmailAuthService() {}

    public static synchronized GmailAuthService getInstance() {
        if (instance == null) {
            instance = new GmailAuthService();
        }
        return instance;
    }

    /**
     * Check if the user has already uploaded their keys.
     * Used by the UI to decide whether to show the Setup Wizard.
     */
    public boolean hasCredentials() {
        return CREDENTIALS_FILE.exists();
    }

    /**
     * Copies the user's uploaded JSON file to our permanent app folder.
     */
    public void importCredentials(File jsonFile) throws IOException {
        if (!APP_DIR.exists()) {
            APP_DIR.mkdirs();
        }
        Files.copy(jsonFile.toPath(), CREDENTIALS_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Safety check: If file is missing, the UI Wizard should have caught this earlier.
        if (!hasCredentials()) {
            throw new FileNotFoundException(
                "Credentials file missing. Please restart the app and complete the Setup Wizard."
            );
        }

        // Load credentials.json from the permanent location
        InputStream in = new FileInputStream(CREDENTIALS_FILE);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // OAuth Flow
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT,
                JSON_FACTORY,
                clientSecrets,
                SCOPES
        )
        .setDataStoreFactory(new FileDataStoreFactory(TOKENS_DIR))
        .setAccessType("offline")
        .build();

        // Local OAuth redirect: http://localhost:8888
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();

        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public Gmail getGmailService() throws IOException, GeneralSecurityException {
        if (gmailService == null) {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            Credential credential = getCredentials(HTTP_TRANSPORT);

            gmailService = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            logger.info("Gmail service initialized successfully");
        }
        return gmailService;
    }

    public boolean isAuthenticated() {
        return gmailService != null;
    }

    public void logout() {
        try {
            if (TOKENS_DIR.exists()) {
                Files.walk(TOKENS_DIR.toPath())
                     .sorted(Comparator.reverseOrder())
                     .map(java.nio.file.Path::toFile)
                     .forEach(File::delete);
            }
            gmailService = null;
            logger.info("User logged out successfully");
        } catch (IOException e) {
            logger.error("Error during logout", e);
        }
    }
}

