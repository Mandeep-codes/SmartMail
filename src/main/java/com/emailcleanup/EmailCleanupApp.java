package com.emailcleanup;

import com.emailcleanup.ui.EnhancedMainWindow;
import com.emailcleanup.service.DatabaseService;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailCleanupApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(EmailCleanupApp.class);

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Starting Smart Email Cleanup Assistant - Enhanced Edition");
            
            DatabaseService.getInstance().initialize();
            
            EnhancedMainWindow mainWindow = new EnhancedMainWindow();
            mainWindow.show(primaryStage);
            
            logger.info("Application started successfully");
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            throw new RuntimeException("Failed to start application", e);
        }
    }

    @Override
    public void stop() {
        try {
            logger.info("Shutting down application");
            DatabaseService.getInstance().shutdown();
        } catch (Exception e) {
            logger.error("Error during shutdown", e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
