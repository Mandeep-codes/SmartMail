package com.emailcleanup.ui;

import com.emailcleanup.model.Email;
import com.emailcleanup.model.SenderStats;
import com.emailcleanup.service.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MainWindow {
    private static final Logger logger = LoggerFactory.getLogger(MainWindow.class);
    
    private Stage stage;
    private TableView<SenderStats> senderTable;
    private TableView<Email> emailTable;
    private Label totalEmailsLabel;
    private Label totalStorageLabel;
    private Label statusLabel;
    private ProgressBar progressBar;
    private TextField searchField;
    
    private ObservableList<SenderStats> senderData = FXCollections.observableArrayList();
    private ObservableList<Email> emailData = FXCollections.observableArrayList();

    public void show(Stage primaryStage) {
        this.stage = primaryStage;
        primaryStage.setTitle("Smart Email Cleanup Assistant");
        
        BorderPane root = new BorderPane();
        root.setTop(createMenuBar());
        root.setCenter(createMainContent());
        root.setBottom(createStatusBar());
        
        Scene scene = new Scene(root, 1200, 700);
        primaryStage.setScene(scene);
        primaryStage.show();
        
        checkAuthentication();
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        Menu fileMenu = new Menu("File");
        MenuItem scanItem = new MenuItem("Scan Emails");
        scanItem.setOnAction(e -> showScanDialog());
        MenuItem refreshItem = new MenuItem("Refresh");
        refreshItem.setOnAction(e -> refreshData());
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> Platform.exit());
        fileMenu.getItems().addAll(scanItem, refreshItem, new SeparatorMenuItem(), exitItem);
        
        Menu accountMenu = new Menu("Account");
        MenuItem loginItem = new MenuItem("Login");
        loginItem.setOnAction(e -> authenticate());
        MenuItem logoutItem = new MenuItem("Logout");
        logoutItem.setOnAction(e -> logout());
        accountMenu.getItems().addAll(loginItem, logoutItem);
        
        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAboutDialog());
        helpMenu.getItems().add(aboutItem);
        
        menuBar.getMenus().addAll(fileMenu, accountMenu, helpMenu);
        return menuBar;
    }

    private VBox createMainContent() {
        VBox mainContent = new VBox(10);
        mainContent.setPadding(new Insets(15));
        
        Label title = new Label("Email Analysis Dashboard");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        
        HBox statsBox = createStatsBox();
        
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        Label searchLabel = new Label("Search:");
        searchField = new TextField();
        searchField.setPromptText("Search by sender email or name...");
        searchField.setPrefWidth(300);
        searchField.textProperty().addListener((obs, old, newVal) -> filterSenders(newVal));
        searchBox.getChildren().addAll(searchLabel, searchField);
        
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.5);
        
        VBox senderBox = createSenderTable();
        VBox emailBox = createEmailTable();
        
        splitPane.getItems().addAll(senderBox, emailBox);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        
        mainContent.getChildren().addAll(title, statsBox, searchBox, splitPane);
        return mainContent;
    }

    private HBox createStatsBox() {
        HBox statsBox = new HBox(30);
        statsBox.setPadding(new Insets(10));
        statsBox.setStyle("-fx-background-color: #f0f0f0; -fx-border-radius: 5; -fx-background-radius: 5;");
        
        totalEmailsLabel = new Label("Total Emails: 0");
        totalEmailsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        totalStorageLabel = new Label("Total Storage: 0 MB");
        totalStorageLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        statsBox.getChildren().addAll(totalEmailsLabel, totalStorageLabel);
        return statsBox;
    }

    private VBox createSenderTable() {
        VBox box = new VBox(10);
        
        Label label = new Label("Senders by Storage Usage");
        label.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        senderTable = new TableView<>();
        senderTable.setItems(senderData);
        
        TableColumn<SenderStats, String> nameCol = new TableColumn<>("Sender");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("displayName"));
        nameCol.setPrefWidth(200);
        
        TableColumn<SenderStats, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("senderEmail"));
        emailCol.setPrefWidth(150);
        
        TableColumn<SenderStats, Integer> countCol = new TableColumn<>("Count");
        countCol.setCellValueFactory(new PropertyValueFactory<>("emailCount"));
        countCol.setPrefWidth(70);
        
        TableColumn<SenderStats, String> sizeCol = new TableColumn<>("Storage");
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("sizeFormatted"));
        sizeCol.setPrefWidth(100);
        
        TableColumn<SenderStats, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryCol.setPrefWidth(100);
        
        senderTable.getColumns().addAll(nameCol, emailCol, countCol, sizeCol, categoryCol);
        
        senderTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadEmailsForSender(newVal.getSenderEmail());
            }
        });
        
        Button deleteAllBtn = new Button("Delete All from Selected Sender");
        deleteAllBtn.setOnAction(e -> deleteSenderEmails());
        
        box.getChildren().addAll(label, senderTable, deleteAllBtn);
        VBox.setVgrow(senderTable, Priority.ALWAYS);
        return box;
    }

    private VBox createEmailTable() {
        VBox box = new VBox(10);
        
        Label label = new Label("Emails from Selected Sender");
        label.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        emailTable = new TableView<>();
        emailTable.setItems(emailData);
        emailTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        TableColumn<Email, String> subjectCol = new TableColumn<>("Subject");
        subjectCol.setCellValueFactory(new PropertyValueFactory<>("subject"));
        subjectCol.setPrefWidth(250);
        
        TableColumn<Email, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        dateCol.setPrefWidth(150);
        
        TableColumn<Email, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("sizeFormatted"));
        sizeCol.setPrefWidth(80);
        
        TableColumn<Email, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryCol.setPrefWidth(100);
        
        emailTable.getColumns().addAll(subjectCol, dateCol, sizeCol, categoryCol);
        
        HBox actionBox = new HBox(10);
        Button deleteSelectedBtn = new Button("Delete Selected");
        deleteSelectedBtn.setOnAction(e -> deleteSelectedEmails());
        
        Button unsubscribeBtn = new Button("Unsubscribe");
        unsubscribeBtn.setOnAction(e -> unsubscribeFromSender());
        
        actionBox.getChildren().addAll(deleteSelectedBtn, unsubscribeBtn);
        
        box.getChildren().addAll(label, emailTable, actionBox);
        VBox.setVgrow(emailTable, Priority.ALWAYS);
        return box;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(5, 10, 5, 10));
        statusBar.setStyle("-fx-background-color: #e0e0e0;");
        statusBar.setAlignment(Pos.CENTER_LEFT);
        
        statusLabel = new Label("Ready");
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(200);
        progressBar.setVisible(false);
        
        statusBar.getChildren().addAll(statusLabel, progressBar);
        return statusBar;
    }

    private void checkAuthentication() {
        if (!GmailAuthService.getInstance().isAuthenticated()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Authentication Required");
            alert.setHeaderText("Gmail Authentication");
            alert.setContentText("Please authenticate with your Gmail account to start scanning emails.");
            alert.showAndWait();
        }
    }

    private void authenticate() {
        new Thread(() -> {
            try {
                Platform.runLater(() -> updateStatus("Authenticating..."));
                GmailAuthService.getInstance().getGmailService();
                Platform.runLater(() -> {
                    updateStatus("Authentication successful");
                    showInfo("Success", "Successfully authenticated with Gmail!");
                });
            } catch (Exception e) {
                logger.error("Authentication failed", e);
                Platform.runLater(() -> {
                    updateStatus("Authentication failed");
                    showError("Authentication Error", e.getMessage());
                });
            }
        }).start();
    }

    private void logout() {
        GmailAuthService.getInstance().logout();
        senderData.clear();
        emailData.clear();
        updateStats(0, 0);
        updateStatus("Logged out");
        showInfo("Logged Out", "Successfully logged out from Gmail");
    }

    private void showScanDialog() {
        TextInputDialog dialog = new TextInputDialog("500");
        dialog.setTitle("Scan Emails");
        dialog.setHeaderText("Email Scan Configuration");
        dialog.setContentText("Number of emails to scan:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(value -> {
            try {
                int maxResults = Integer.parseInt(value);
                scanEmails(maxResults);
            } catch (NumberFormatException e) {
                showError("Invalid Input", "Please enter a valid number");
            }
        });
    }

    private void scanEmails(int maxResults) {
        new Thread(() -> {
            try {
                Platform.runLater(() -> {
                    progressBar.setVisible(true);
                    progressBar.setProgress(-1);
                });
                
                EmailScannerService.getInstance().scanEmails(maxResults, message -> {
                    Platform.runLater(() -> updateStatus(message));
                });
                
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    refreshData();
                    showInfo("Scan Complete", "Email scan completed successfully!");
                });
            } catch (Exception e) {
                logger.error("Email scan failed", e);
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    showError("Scan Error", "Failed to scan emails: " + e.getMessage());
                });
            }
        }).start();
    }

    private void refreshData() {
        new Thread(() -> {
            try {
                List<SenderStats> stats = DatabaseService.getInstance().getSenderStats();
                int totalEmails = DatabaseService.getInstance().getTotalEmailCount();
                long totalStorage = DatabaseService.getInstance().getTotalStorageUsed();
                
                Platform.runLater(() -> {
                    senderData.setAll(stats);
                    updateStats(totalEmails, totalStorage);
                    updateStatus("Data refreshed");
                });
            } catch (Exception e) {
                logger.error("Failed to refresh data", e);
                Platform.runLater(() -> showError("Refresh Error", "Failed to refresh data"));
            }
        }).start();
    }

    private void loadEmailsForSender(String senderEmail) {
        new Thread(() -> {
            try {
                List<Email> emails = DatabaseService.getInstance().getEmailsBySender(senderEmail);
                Platform.runLater(() -> emailData.setAll(emails));
            } catch (Exception e) {
                logger.error("Failed to load emails", e);
            }
        }).start();
    }

    private void filterSenders(String query) {
        if (query == null || query.isEmpty()) {
            refreshData();
            return;
        }
        
        String lowerQuery = query.toLowerCase();
        List<SenderStats> filtered = senderData.stream()
            .filter(s -> s.getSenderEmail().toLowerCase().contains(lowerQuery) ||
                        (s.getSenderName() != null && s.getSenderName().toLowerCase().contains(lowerQuery)))
            .collect(Collectors.toList());
        senderData.setAll(filtered);
    }

    private void deleteSenderEmails() {
        SenderStats selected = senderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("No Selection", "Please select a sender first");
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete all emails from " + selected.getDisplayName() + "?");
        confirm.setContentText(String.format("This will delete %d emails (%s)", 
            selected.getEmailCount(), selected.getSizeFormatted()));
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                deleteEmailsFromSender(selected.getSenderEmail());
            }
        });
    }

    private void deleteEmailsFromSender(String senderEmail) {
        new Thread(() -> {
            try {
                List<Email> emails = DatabaseService.getInstance().getEmailsBySender(senderEmail);
                List<String> emailIds = emails.stream().map(Email::getId).collect(Collectors.toList());
                
                Platform.runLater(() -> updateStatus("Deleting emails..."));
                EmailActionService.getInstance().deleteEmails(emailIds);
                
                Platform.runLater(() -> {
                    refreshData();
                    emailData.clear();
                    updateStatus("Emails deleted successfully");
                    showInfo("Success", "Emails deleted successfully!");
                });
            } catch (Exception e) {
                logger.error("Failed to delete emails", e);
                Platform.runLater(() -> showError("Delete Error", "Failed to delete emails: " + e.getMessage()));
            }
        }).start();
    }

    private void deleteSelectedEmails() {
        ObservableList<Email> selected = emailTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            showWarning("No Selection", "Please select emails to delete");
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete selected emails?");
        confirm.setContentText("This will delete " + selected.size() + " email(s)");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                List<String> emailIds = new ArrayList<>();
                for (Email email : selected) {
                    emailIds.add(email.getId());
                }
                deleteEmails(emailIds);
            }
        });
    }

    private void deleteEmails(List<String> emailIds) {
        new Thread(() -> {
            try {
                EmailActionService.getInstance().deleteEmails(emailIds);
                Platform.runLater(() -> {
                    SenderStats selected = senderTable.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        loadEmailsForSender(selected.getSenderEmail());
                    }
                    refreshData();
                    showInfo("Success", "Emails deleted successfully!");
                });
            } catch (Exception e) {
                logger.error("Failed to delete emails", e);
                Platform.runLater(() -> showError("Delete Error", "Failed to delete emails"));
            }
        }).start();
    }

    private void unsubscribeFromSender() {
        Email selected = emailTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("No Selection", "Please select an email first");
            return;
        }
        
        if (!selected.isHasUnsubscribeLink()) {
            showWarning("No Unsubscribe Link", "This email doesn't have an unsubscribe link");
            return;
        }
        
        EmailActionService.getInstance().openUnsubscribeLink(selected.getUnsubscribeUrl());
    }

    private void updateStats(int totalEmails, long totalStorage) {
        totalEmailsLabel.setText("Total Emails: " + totalEmails);
        double storageMB = totalStorage / (1024.0 * 1024.0);
        totalStorageLabel.setText(String.format("Total Storage: %.2f MB", storageMB));
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("Smart Email Cleanup Assistant");
        alert.setContentText("Version 1.0.0\n\n" +
            "A desktop application for analyzing and cleaning up your Gmail inbox.\n\n" +
            "Features:\n" +
            "- Scan and analyze emails\n" +
            "- Group by sender with storage statistics\n" +
            "- Bulk delete emails\n" +
            "- One-click unsubscribe\n" +
            "- Email categorization");
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
