package com.emailcleanup.ui;

import com.emailcleanup.model.Email;
import com.emailcleanup.model.SenderStats;
import com.emailcleanup.service.*;
//import com.emailcleanup.ui.components.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.emailcleanup.service.SmartAnalysisService.SenderDecayScore; // Import the record
import com.emailcleanup.service.SmartAnalysisService.SubjectCluster;
import com.emailcleanup.service.SmartAnalysisService.PrivacyRisk;
import com.emailcleanup.service.SmartAnalysisService.RiskLevel;

public class EnhancedMainWindow {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedMainWindow.class);
    
    private Stage stage;
    private Scene scene;
    private TableView<SenderStats> senderTable;
    private TableView<Email> emailTable;
    private Label totalEmailsLabel;
    private Label totalStorageLabel;
    private Label promotionalLabel;
    private Label newsletterLabel;
    private Label statusLabel;
    private ProgressBar progressBar;
    private TextField searchField;
    private ComboBox<String> categoryFilter;
    private TextArea emailPreview;
    private PieChart categoryChart;
    private BarChart<String, Number> storageChart;
    private boolean isDarkTheme = false;
    
    private ObservableList<SenderStats> senderData = FXCollections.observableArrayList();
    private ObservableList<SenderStats> allSenderData = FXCollections.observableArrayList();
    private ObservableList<Email> emailData = FXCollections.observableArrayList();

    public void show(Stage primaryStage) {
        this.stage = primaryStage;
        primaryStage.setTitle("Smart Email Cleanup Assistant - Enhanced Edition");
        
        BorderPane root = new BorderPane();
        root.setTop(createMenuBar());
        root.setCenter(createMainContent());
        root.setBottom(createStatusBar());
        
        scene = new Scene(root, 1400, 800);
        applyTheme();
        primaryStage.setScene(scene);
        primaryStage.show();
        
        if (!GmailAuthService.getInstance().hasCredentials()) {
            showSetupWizard();
        } else {
            checkAuthentication();
        }
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        Menu fileMenu = new Menu("📧 File");
        MenuItem scanItem = new MenuItem("Scan Emails");
        scanItem.setOnAction(e -> showScanDialog());
        MenuItem refreshItem = new MenuItem("Refresh");
        refreshItem.setOnAction(e -> refreshData());
        MenuItem exportCSVItem = new MenuItem("Export to CSV");
        exportCSVItem.setOnAction(e -> exportToCSV());
        MenuItem exportReportItem = new MenuItem("Export Report");
        exportReportItem.setOnAction(e -> exportReport());
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> Platform.exit());
        fileMenu.getItems().addAll(scanItem, refreshItem, new SeparatorMenuItem(), 
                                   exportCSVItem, exportReportItem, new SeparatorMenuItem(), exitItem);
        
        Menu viewMenu = new Menu("🎨 View");
        MenuItem toggleThemeItem = new MenuItem("Toggle Dark/Light Theme");
        toggleThemeItem.setOnAction(e -> toggleTheme());
        MenuItem showStatsItem = new MenuItem("Show Statistics");
        showStatsItem.setOnAction(e -> showStatistics());
        viewMenu.getItems().addAll(toggleThemeItem, showStatsItem);
        
        Menu accountMenu = new Menu("👤 Account");
        MenuItem loginItem = new MenuItem("Login");
        loginItem.setOnAction(e -> authenticate());
        MenuItem logoutItem = new MenuItem("Logout");
        logoutItem.setOnAction(e -> logout());
        accountMenu.getItems().addAll(loginItem, logoutItem);
        
        Menu toolsMenu = new Menu("🛠️ Tools");
        MenuItem scheduleScanItem = new MenuItem("Schedule Scan");
        scheduleScanItem.setOnAction(e -> showScheduleDialog());
        MenuItem advancedSearchItem = new MenuItem("Advanced Search");
        advancedSearchItem.setOnAction(e -> showAdvancedSearch());
        toolsMenu.getItems().addAll(scheduleScanItem, advancedSearchItem);
        
        Menu helpMenu = new Menu("❓ Help");
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAboutDialog());
        MenuItem userGuideItem = new MenuItem("User Guide");
        userGuideItem.setOnAction(e -> showUserGuide());
        helpMenu.getItems().addAll(aboutItem, userGuideItem);
        
        menuBar.getMenus().addAll(fileMenu, viewMenu, accountMenu, toolsMenu, helpMenu);
        return menuBar;
    }

    private VBox createMainContent() {
        VBox mainContent = new VBox(15);
        mainContent.setPadding(new Insets(20));
        mainContent.getStyleClass().add("main-content");
        
        Label title = new Label("📊 Email Analysis Dashboard");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        title.getStyleClass().add("dashboard-title");
        
        HBox statsBox = createEnhancedStatsBox();
        
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab smartTab = new Tab("✨ Smart Clean");
smartTab.setContent(createSmartCleanPane());

        Tab patternTab = new Tab("🧩 Pattern Clean");
    patternTab.setContent(createPatternCleanPane());

       Tab privacyTab = new Tab("🛡️ Privacy Shield");
       privacyTab.setContent(createPrivacyPane());
        
        Tab analysisTab = new Tab("📧 Email Analysis");
        analysisTab.setContent(createAnalysisPane());
        
        Tab chartsTab = new Tab("📈 Charts & Visualizations");
        chartsTab.setContent(createChartsPane());
        
        Tab previewTab = new Tab("👁️ Email Preview");
        previewTab.setContent(createPreviewPane());
        
        tabPane.getTabs().addAll(privacyTab,patternTab, smartTab, analysisTab, chartsTab, previewTab);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        
        mainContent.getChildren().addAll(title, statsBox, tabPane);
        return mainContent;
    }

    private HBox createEnhancedStatsBox() {
        HBox statsBox = new HBox(20);
        statsBox.setPadding(new Insets(15));
        statsBox.getStyleClass().add("stats-box");
        statsBox.setAlignment(Pos.CENTER);
        
        VBox emailsBox = createStatCard("📧 Total Emails", "0", "#4CAF50");
        totalEmailsLabel = (Label) ((VBox) emailsBox.getChildren().get(1)).getChildren().get(0);
        
        VBox storageBox = createStatCard("💾 Total Storage", "0 MB", "#2196F3");
        totalStorageLabel = (Label) ((VBox) storageBox.getChildren().get(1)).getChildren().get(0);
        
        VBox promoBox = createStatCard("🎁 Promotional", "0", "#FF9800");
        promotionalLabel = (Label) ((VBox) promoBox.getChildren().get(1)).getChildren().get(0);
        
        VBox newsletterBox = createStatCard("📰 Newsletters", "0", "#9C27B0");
        newsletterLabel = (Label) ((VBox) newsletterBox.getChildren().get(1)).getChildren().get(0);
        
        statsBox.getChildren().addAll(emailsBox, storageBox, promoBox, newsletterBox);
        return statsBox;
    }

    private VBox createStatCard(String title, String value, String color) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.getStyleClass().add("stat-card");
        card.setStyle("-fx-border-color: " + color + "; -fx-border-width: 2;");
        card.setPrefWidth(200);
        
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        titleLabel.getStyleClass().add("stat-title");
        
        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        valueLabel.getStyleClass().add("stat-value");
        valueLabel.setStyle("-fx-text-fill: " + color + ";");
        
        VBox valueBox = new VBox(valueLabel);
        valueBox.setAlignment(Pos.CENTER);
        
        card.getChildren().addAll(titleLabel, valueBox);
        return card;
    }

    private VBox createAnalysisPane() {
        VBox analysisPane = new VBox(10);
        analysisPane.setPadding(new Insets(15));
        
        HBox filterBox = new HBox(15);
        filterBox.setAlignment(Pos.CENTER_LEFT);
        
        Label searchLabel = new Label("🔍 Search:");
        searchField = new TextField();
        searchField.setPromptText("Search by sender email or name...");
        searchField.setPrefWidth(300);
        searchField.getStyleClass().add("search-field");
        searchField.textProperty().addListener((obs, old, newVal) -> filterSenders());
        
        Label filterLabel = new Label("📁 Category:");
        categoryFilter = new ComboBox<>();
        categoryFilter.getItems().addAll("All", "PROMOTIONAL", "NEWSLETTER", "SOCIAL", "IMPORTANT", "AUTOMATED", "SPAM");
        categoryFilter.setValue("All");
        categoryFilter.setOnAction(e -> filterSenders());
        
        Button clearFiltersBtn = new Button("Clear Filters");
        clearFiltersBtn.setOnAction(e -> clearFilters());
        
        filterBox.getChildren().addAll(searchLabel, searchField, filterLabel, categoryFilter, clearFiltersBtn);
        
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.5);
        
        VBox senderBox = createEnhancedSenderTable();
        VBox emailBox = createEnhancedEmailTable();
        
        splitPane.getItems().addAll(senderBox, emailBox);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        
        analysisPane.getChildren().addAll(filterBox, splitPane);
        return analysisPane;
    }

    private VBox createChartsPane() {
        VBox chartsPane = new VBox(20);
        chartsPane.setPadding(new Insets(15));
        
        Label title = new Label("📊 Visual Analytics");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        
        HBox chartsBox = new HBox(20);
        
        categoryChart = createCategoryPieChart();
        storageChart = createStorageBarChart();
        
        chartsBox.getChildren().addAll(categoryChart, storageChart);
        HBox.setHgrow(categoryChart, Priority.ALWAYS);
        HBox.setHgrow(storageChart, Priority.ALWAYS);
        
        chartsPane.getChildren().addAll(title, chartsBox);
        VBox.setVgrow(chartsBox, Priority.ALWAYS);
        
        return chartsPane;
    }

    private PieChart createCategoryPieChart() {
        PieChart chart = new PieChart();
        chart.setTitle("Emails by Category");
        chart.setLegendSide(Side.RIGHT);
        return chart;
    }

    private BarChart<String, Number> createStorageBarChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Top Senders");
        yAxis.setLabel("Storage (MB)");
        
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Top 10 Senders by Storage");
        chart.setLegendVisible(false);
        
        return chart;
    }

    private VBox createPreviewPane() {
        VBox previewPane = new VBox(10);
        previewPane.setPadding(new Insets(15));
        
        Label title = new Label("📧 Email Preview");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        
        emailPreview = new TextArea();
        emailPreview.setEditable(false);
        emailPreview.setWrapText(true);
        emailPreview.setPrefHeight(500);
        emailPreview.setPromptText("Select an email to preview...");
        emailPreview.getStyleClass().add("email-preview");
        
        VBox.setVgrow(emailPreview, Priority.ALWAYS);
        previewPane.getChildren().addAll(title, emailPreview);
        return previewPane;
    }

    private VBox createEnhancedSenderTable() {
        VBox box = new VBox(10);
        
        Label label = new Label("📨 Senders by Storage Usage");
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        
        senderTable = new TableView<>();
        senderTable.setItems(senderData);
        senderTable.getStyleClass().add("table-view");
        
        TableColumn<SenderStats, String> nameCol = new TableColumn<>("Sender Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("displayName"));
        nameCol.setPrefWidth(200);
        
        TableColumn<SenderStats, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("senderEmail"));
        emailCol.setPrefWidth(180);
        
        TableColumn<SenderStats, Integer> countCol = new TableColumn<>("Count");
        countCol.setCellValueFactory(new PropertyValueFactory<>("emailCount"));
        countCol.setPrefWidth(80);
        countCol.setStyle("-fx-alignment: CENTER;");
        
        TableColumn<SenderStats, String> sizeCol = new TableColumn<>("Storage");
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("sizeFormatted"));
        sizeCol.setPrefWidth(100);
        sizeCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        
        TableColumn<SenderStats, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryCol.setPrefWidth(120);
        categoryCol.setCellFactory(column -> new CategoryCell());
        
        senderTable.getColumns().addAll(nameCol, emailCol, countCol, sizeCol, categoryCol);
        
        senderTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadEmailsForSender(newVal.getSenderEmail());
            }
        });
        
        HBox actionBox = new HBox(10);
        Button deleteAllBtn = new Button("🗑️ Delete All from Sender");
        deleteAllBtn.getStyleClass().add("danger-button");
        deleteAllBtn.setOnAction(e -> deleteSenderEmails());
        
        Button viewDetailsBtn = new Button("📊 View Details");
        viewDetailsBtn.setOnAction(e -> showSenderDetails());
        
        actionBox.getChildren().addAll(deleteAllBtn, viewDetailsBtn);
        
        box.getChildren().addAll(label, senderTable, actionBox);
        VBox.setVgrow(senderTable, Priority.ALWAYS);
        return box;
    }

    private VBox createEnhancedEmailTable() {
        VBox box = new VBox(10);
        
        Label label = new Label("✉️ Emails from Selected Sender");
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        
        emailTable = new TableView<>();
        emailTable.setItems(emailData);
        emailTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        emailTable.getStyleClass().add("table-view");
        
        TableColumn<Email, String> subjectCol = new TableColumn<>("Subject");
        subjectCol.setCellValueFactory(new PropertyValueFactory<>("subject"));
        subjectCol.setPrefWidth(280);
        
        TableColumn<Email, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        dateCol.setPrefWidth(150);
        
        TableColumn<Email, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("sizeFormatted"));
        sizeCol.setPrefWidth(80);
        sizeCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        
        TableColumn<Email, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryCol.setPrefWidth(120);
        categoryCol.setCellFactory(column -> new EmailCategoryCell());
        
        emailTable.getColumns().addAll(subjectCol, dateCol, sizeCol, categoryCol);
        
        emailTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showEmailPreview(newVal);
            }
        });
        
        HBox actionBox = new HBox(10);
        Button deleteSelectedBtn = new Button("🗑️ Delete Selected");
        deleteSelectedBtn.getStyleClass().add("danger-button");
        deleteSelectedBtn.setOnAction(e -> deleteSelectedEmails());
        
        Button unsubscribeBtn = new Button("📭 Unsubscribe");
        unsubscribeBtn.setOnAction(e -> unsubscribeFromSender());
        
        Button markReadBtn = new Button("✓ Mark Read");
        markReadBtn.setOnAction(e -> showInfo("Info", "Mark as read feature coming soon!"));
        
        actionBox.getChildren().addAll(deleteSelectedBtn, unsubscribeBtn, markReadBtn);
        
        box.getChildren().addAll(label, emailTable, actionBox);
        VBox.setVgrow(emailTable, Priority.ALWAYS);
        return box;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(8, 15, 8, 15));
        statusBar.getStyleClass().add("status-bar");
        statusBar.setAlignment(Pos.CENTER_LEFT);
        
        statusLabel = new Label("Ready");
        statusLabel.getStyleClass().add("status-label");
        
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(250);
        progressBar.setVisible(false);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label versionLabel = new Label("v2.0 Enhanced");
        versionLabel.getStyleClass().add("version-label");
        
        statusBar.getChildren().addAll(statusLabel, progressBar, spacer, versionLabel);
        return statusBar;
    }

    private void applyTheme() {
        scene.getStylesheets().clear();
        if (isDarkTheme) {
            scene.getStylesheets().add(getClass().getResource("/styles/dark-theme.css").toExternalForm());
        } else {
            scene.getStylesheets().add(getClass().getResource("/styles/light-theme.css").toExternalForm());
        }
    }

    private void toggleTheme() {
        isDarkTheme = !isDarkTheme;
        applyTheme();
        updateStatus("Theme changed to " + (isDarkTheme ? "Dark" : "Light"));
    }

    private void clearFilters() {
        searchField.clear();
        categoryFilter.setValue("All");
        filterSenders();
    }

    private void filterSenders() {
        String searchText = searchField.getText().toLowerCase();
        String category = categoryFilter.getValue();
        
        List<SenderStats> filtered = allSenderData.stream()
            .filter(s -> {
                boolean matchesSearch = searchText.isEmpty() || 
                    s.getSenderEmail().toLowerCase().contains(searchText) ||
                    (s.getSenderName() != null && s.getSenderName().toLowerCase().contains(searchText));
                
                boolean matchesCategory = category.equals("All") || 
                    (s.getCategory() != null && s.getCategory().equals(category));
                
                return matchesSearch && matchesCategory;
            })
            .collect(Collectors.toList());
        
        senderData.setAll(filtered);
    }

    private void showEmailPreview(Email email) {
        StringBuilder preview = new StringBuilder();
        preview.append("From: ").append(email.getFromName()).append(" <").append(email.getFrom()).append(">\n");
        preview.append("Subject: ").append(email.getSubject()).append("\n");
        preview.append("Date: ").append(email.getDate()).append("\n");
        preview.append("Size: ").append(email.getSizeFormatted()).append("\n");
        preview.append("Category: ").append(email.getCategory()).append("\n");
        preview.append("\n─────────────────────────────────────\n\n");
        preview.append(email.getSnippet());
        
        emailPreview.setText(preview.toString());
    }

    private void updateCharts() {
        new Thread(() -> {
            try {
                List<Email> allEmails = DatabaseService.getInstance().getAllEmails();
                
                Map<String, Long> categoryCount = allEmails.stream()
                    .collect(Collectors.groupingBy(
                        e -> e.getCategory() != null ? e.getCategory() : "UNKNOWN",
                        Collectors.counting()
                    ));
                
                Platform.runLater(() -> {
                    ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
                    categoryCount.forEach((cat, count) -> 
                        pieData.add(new PieChart.Data(cat + " (" + count + ")", count))
                    );
                    categoryChart.setData(pieData);
                    
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    series.setName("Storage");
                    
                    allSenderData.stream()
                        .sorted((a, b) -> Long.compare(b.getTotalSizeBytes(), a.getTotalSizeBytes()))
                        .limit(10)
                        .forEach(sender -> {
                            String name = sender.getSenderName() != null && !sender.getSenderName().isEmpty() 
                                ? sender.getSenderName() : sender.getSenderEmail();
                            if (name.length() > 20) name = name.substring(0, 17) + "...";
                            double sizeMB = sender.getTotalSizeBytes() / (1024.0 * 1024.0);
                            series.getData().add(new XYChart.Data<>(name, sizeMB));
                        });
                    
                    storageChart.getData().clear();
                    storageChart.getData().add(series);
                });
            } catch (Exception e) {
                logger.error("Error updating charts", e);
            }
        }).start();
    }

    private void exportToCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export to CSV");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );
        fileChooser.setInitialFileName("email_report_" + 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv");
        
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            new Thread(() -> {
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write("Sender Email,Sender Name,Email Count,Total Size (bytes),Category\n");
                    for (SenderStats stats : allSenderData) {
                        writer.write(String.format("%s,%s,%d,%d,%s\n",
                            stats.getSenderEmail(),
                            stats.getSenderName() != null ? stats.getSenderName() : "",
                            stats.getEmailCount(),
                            stats.getTotalSizeBytes(),
                            stats.getCategory() != null ? stats.getCategory() : ""
                        ));
                    }
                    Platform.runLater(() -> showInfo("Success", "CSV exported successfully!"));
                } catch (Exception e) {
                    logger.error("Error exporting CSV", e);
                    Platform.runLater(() -> showError("Error", "Failed to export CSV"));
                }
            }).start();
        }
    }

    private void exportReport() {
        showInfo("Export Report", "PDF report generation will be added in next update!");
    }

    private void showStatistics() {
        new Thread(() -> {
            try {
                int totalEmails = DatabaseService.getInstance().getTotalEmailCount();
                long totalStorage = DatabaseService.getInstance().getTotalStorageUsed();
                List<Email> allEmails = DatabaseService.getInstance().getAllEmails();
                
                Map<String, Long> categoryStats = allEmails.stream()
                    .collect(Collectors.groupingBy(
                        e -> e.getCategory() != null ? e.getCategory() : "UNKNOWN",
                        Collectors.counting()
                    ));
                
                StringBuilder stats = new StringBuilder();
                stats.append("📊 Email Statistics\n");
                stats.append("══════════════════════════════════\n\n");
                stats.append("Total Emails: ").append(totalEmails).append("\n");
                stats.append("Total Storage: ").append(String.format("%.2f MB", totalStorage / (1024.0 * 1024.0))).append("\n\n");
                stats.append("Breakdown by Category:\n");
                stats.append("─────────────────────────\n");
                
                categoryStats.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                    .forEach(entry -> {
                        double percentage = (entry.getValue() * 100.0) / totalEmails;
                        stats.append(String.format("%-15s: %5d (%.1f%%)\n", 
                            entry.getKey(), entry.getValue(), percentage));
                    });
                
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Statistics");
                    alert.setHeaderText("Email Statistics Report");
                    alert.setContentText(stats.toString());
                    alert.showAndWait();
                });
            } catch (Exception e) {
                logger.error("Error showing statistics", e);
            }
        }).start();
    }

private VBox createSmartCleanPane() {
        VBox pane = new VBox(15);
        pane.setPadding(new Insets(15));

        // Header
        Label title = new Label("✨ Smart Suggestions");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        Label subtitle = new Label("These senders are taking up space but you rarely read their emails.");
        subtitle.setStyle("-fx-text-fill: #666;");

        // The Smart Table
        TableView<SenderDecayScore> smartTable = new TableView<>();
        smartTable.setPlaceholder(new Label("Click 'Analyze Inbox' to find clean-up candidates"));
        smartTable.getStyleClass().add("table-view");

        // --- FIXED COLUMNS (Using Lambdas instead of PropertyValueFactory) ---

        // 1. Status Column
        TableColumn<SenderDecayScore, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().status())); 
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) { setText(null); setStyle(""); } 
                else {
                    setText(item);
                    String color = switch(item) {
                        case "SPAMMER" -> "#ffcdd2"; // Red
                        case "GHOST" -> "#e1bee7";   // Purple
                        default -> "#fff9c4";        // Yellow
                    };
                    setStyle("-fx-background-color: " + color + "; -fx-text-fill: #333; -fx-alignment: CENTER; -fx-font-weight: bold;");
                }
            }
        });

        // 2. Sender Name Column (The one that was blank)
        TableColumn<SenderDecayScore, String> senderCol = new TableColumn<>("Sender");
        senderCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().senderName()));
        senderCol.setPrefWidth(250);

        // 3. Open Rate Column
        TableColumn<SenderDecayScore, String> openRateCol = new TableColumn<>("Open Rate");
        openRateCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getFormattedOpenRate()));

        // 4. Wasted Space Column
        TableColumn<SenderDecayScore, String> wasteCol = new TableColumn<>("Wasted Space");
        wasteCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getFormattedWastedSpace()));

        smartTable.getColumns().addAll(statusCol, senderCol, openRateCol, wasteCol);
        VBox.setVgrow(smartTable, Priority.ALWAYS);

        // Action Bar
        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER_LEFT);
        
        Button analyzeBtn = new Button("🧠 Analyze Inbox");
        analyzeBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        analyzeBtn.setOnAction(e -> {
            new Thread(() -> {
                try {
                    SmartAnalysisService service = new SmartAnalysisService();
                    List<Email> allEmails = DatabaseService.getInstance().getAllEmails();
                    List<SenderDecayScore> scores = service.analyzeInboxHealth(allEmails);
                    
                    Platform.runLater(() -> {
                        smartTable.getItems().setAll(scores);
                        if (scores.isEmpty()) {
                            showInfo("Analysis Complete", "Your inbox is clean! No zombie threads found.");
                        } else {
                            showInfo("Analysis Complete", "Found " + scores.size() + " candidates for cleanup.");
                        }
                    });
                } catch (Exception ex) {
                    logger.error("Analysis failed", ex);
                }
            }).start();
        });

        Button quickCleanBtn = new Button("🧹 Clean Selected Sender");
        quickCleanBtn.getStyleClass().add("danger-button");
        quickCleanBtn.setOnAction(e -> {
            SenderDecayScore selected = smartTable.getSelectionModel().getSelectedItem();
            if(selected != null) {
                deleteEmailsFromSender(selected.senderEmail());
            } else {
                showWarning("Select a Sender", "Please select a row to clean.");
            }
        });

        actions.getChildren().addAll(analyzeBtn, quickCleanBtn);
        pane.getChildren().addAll(title, subtitle, actions, smartTable);
        return pane;
    }

    private VBox createPatternCleanPane() {
        VBox pane = new VBox(15);
        pane.setPadding(new Insets(15));

        Label title = new Label("🧩 Pattern Cleanup");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        Label subtitle = new Label("We found these repetitive email patterns. Delete them in bulk.");
        subtitle.setStyle("-fx-text-fill: #666;");

        TableView<SubjectCluster> clusterTable = new TableView<>();
        clusterTable.setPlaceholder(new Label("Click 'Scan Patterns' to find repetitive emails"));
        clusterTable.getStyleClass().add("table-view");

        // 1. Pattern Name
        TableColumn<SubjectCluster, String> patternCol = new TableColumn<>("Subject Pattern");
        patternCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().normalizedSubject()));
        patternCol.setPrefWidth(400);

        // 2. Count
        TableColumn<SubjectCluster, Number> countCol = new TableColumn<>("Count");
        countCol.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().count()));
        countCol.setStyle("-fx-alignment: CENTER;");

        // 3. Size
        TableColumn<SubjectCluster, String> sizeCol = new TableColumn<>("Total Size");
        sizeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getSizeFormatted()));
        sizeCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        clusterTable.getColumns().addAll(patternCol, countCol, sizeCol);
        VBox.setVgrow(clusterTable, Priority.ALWAYS);

        // Buttons
        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button scanBtn = new Button("🔍 Scan Patterns");
        scanBtn.setStyle("-fx-background-color: #673AB7; -fx-text-fill: white; -fx-font-weight: bold;");
        scanBtn.setOnAction(e -> {
            new Thread(() -> {
                SmartAnalysisService service = new SmartAnalysisService();
                List<Email> emails = null;
                try {
                    emails = DatabaseService.getInstance().getAllEmails();
                } catch (SQLException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                List<SubjectCluster> clusters = service.findSimilarSubjects(emails);
                
                Platform.runLater(() -> {
                    clusterTable.getItems().setAll(clusters);
                    showInfo("Scan Complete", "Found " + clusters.size() + " repeating patterns.");
                });
            }).start();
        });

        Button deleteBtn = new Button("🗑️ Delete Pattern Group");
        deleteBtn.getStyleClass().add("danger-button");
        deleteBtn.setOnAction(e -> {
            SubjectCluster selected = clusterTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showWarning("Select Pattern", "Please select a pattern to delete.");
                return;
            }
            
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Bulk Delete");
            confirm.setHeaderText("Delete " + selected.count() + " emails?");
            confirm.setContentText("Pattern: " + selected.normalizedSubject());
            
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    List<String> ids = selected.emails().stream().map(Email::getId).collect(Collectors.toList());
                    deleteEmails(ids); // Reuses your existing delete logic
                    clusterTable.getItems().remove(selected);
                }
            });
        });

        actions.getChildren().addAll(scanBtn, deleteBtn);
        pane.getChildren().addAll(title, subtitle, actions, clusterTable);
        return pane;
    }

    private VBox createPrivacyPane() {
        VBox pane = new VBox(15);
        pane.setPadding(new Insets(15));

        Label title = new Label("🛡️ Privacy Shield");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        Label subtitle = new Label("Scan for sensitive emails (Passwords, Bank Info) that could be dangerous if leaked.");
        subtitle.setStyle("-fx-text-fill: #666;");

        TableView<PrivacyRisk> riskTable = new TableView<>();
        riskTable.setPlaceholder(new Label("Click 'Scan for Risks' to find sensitive data"));
        riskTable.getStyleClass().add("table-view");

        // 1. Risk Level
        TableColumn<PrivacyRisk, String> levelCol = new TableColumn<>("Risk Level");
        levelCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().level().toString()));
        levelCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) { setText(null); setStyle(""); } 
                else {
                    setText(item);
                    String color = item.equals("HIGH") ? "#ffebee" : "#e3f2fd"; // Red for High, Blue for Med
                    String textColor = item.equals("HIGH") ? "#c62828" : "#1565c0";
                    setStyle("-fx-background-color: " + color + "; -fx-text-fill: " + textColor + "; -fx-font-weight: bold; -fx-alignment: CENTER;");
                }
            }
        });

        // 2. Type
        TableColumn<PrivacyRisk, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().riskType()));

        // 3. Subject
        TableColumn<PrivacyRisk, String> subjectCol = new TableColumn<>("Subject");
        subjectCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().email().getSubject()));
        subjectCol.setPrefWidth(350);

        // 4. Trigger
        TableColumn<PrivacyRisk, String> triggerCol = new TableColumn<>("Detected Keyword");
        triggerCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().triggerPhrase()));

        riskTable.getColumns().addAll(levelCol, typeCol, subjectCol, triggerCol);
        VBox.setVgrow(riskTable, Priority.ALWAYS);

        // Buttons
        HBox actions = new HBox(15);
        
        Button scanBtn = new Button("🛡️ Scan for Risks");
        scanBtn.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-weight: bold;");
        scanBtn.setOnAction(e -> {
            new Thread(() -> {
                SmartAnalysisService service = new SmartAnalysisService();
                List<Email> emails = null;
                try {
                    emails = DatabaseService.getInstance().getAllEmails();
                } catch (SQLException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                List<PrivacyRisk> risks = service.scanForPrivacyRisks(emails);
                
                Platform.runLater(() -> {
                    riskTable.getItems().setAll(risks);
                    showInfo("Security Scan", "Found " + risks.size() + " sensitive emails.");
                });
            }).start();
        });

        Button deleteBtn = new Button("🔐 Secure Delete Selected");
        deleteBtn.setOnAction(e -> {
            PrivacyRisk selected = riskTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                deleteEmails(List.of(selected.email().getId()));
                riskTable.getItems().remove(selected);
            }
        });

        actions.getChildren().addAll(scanBtn, deleteBtn);
        pane.getChildren().addAll(title, subtitle, actions, riskTable);
        return pane;
    }

    private void showScheduleDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Schedule Scan");
        alert.setHeaderText("Automated Scanning");
        alert.setContentText("Scheduled scanning feature coming soon!\n\nYou'll be able to:\n- Schedule daily/weekly scans\n- Set cleanup rules\n- Get email notifications");
        alert.showAndWait();
    }

    private void showSetupWizard() {
        // Create a dialog that blocks usage until setup is done
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Welcome Setup");
        dialog.setHeaderText("🔑 Gmail API Setup (5 Minutes)");
        
        // Close app if they try to close the wizard without finishing
        dialog.getDialogPane().getScene().getWindow().setOnCloseRequest(event -> Platform.exit());

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(600);

        // 1. Instructions Text
        String steps = """
            1. Visit https://console.cloud.google.com/
            2. Click "New Project" -> Name it "Email Cleanup App" -> Click "Create"
            
            Enable Gmail API:
            1. Go to "APIs & Services" > "Library"
            2. Search for "Gmail API" -> Click "Enable"
            
            Create OAuth Credentials:
            1. Go to "APIs & Services" > "Credentials"
            2. Click "Create Credentials" > "OAuth client ID"
            3. Configure Consent Screen (User Type: External, add your email as test user)
            4. Application type: Desktop app
            5. Click "Create" -> Download JSON file
            """;

        Label instructionsLabel = new Label("Follow these steps to get your key:");
        instructionsLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));

        TextArea instructionsArea = new TextArea(steps);
        instructionsArea.setEditable(false);
        instructionsArea.setWrapText(true);
        instructionsArea.setPrefHeight(250);
        instructionsArea.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 13px;");

        // 2. Link Button
        Hyperlink link = new Hyperlink("👉 Open Google Cloud Console");
        link.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
       link.setOnAction(e -> {
    try {
        java.awt.Desktop.getDesktop().browse(new java.net.URI("https://console.cloud.google.com/"));
    } catch (Exception ex) {
        ex.printStackTrace(); // Just in case browser fails to open
    }
});
        // 3. Upload Section
        Separator sep = new Separator();
        
        Label uploadLabel = new Label("Upload the downloaded JSON file:");
        uploadLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));

        Button uploadBtn = new Button("📂 Select JSON File");
        uploadBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");
        
        Label statusLabel = new Label("");
        statusLabel.setFont(Font.font(13));

        Button continueBtn = new Button("🚀 Launch App");
        continueBtn.setDisable(true); // Disabled until valid file uploaded
        continueBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");

        // Upload Logic
        uploadBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select credentials.json");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
            File file = fileChooser.showOpenDialog(stage);
            
            if (file != null) {
                try {
                    GmailAuthService.getInstance().importCredentials(file);
                    statusLabel.setText("✅ Success! Credentials saved.");
                    statusLabel.setStyle("-fx-text-fill: green;");
                    continueBtn.setDisable(false); // Enable Launch button
                    uploadBtn.setDisable(true);
                } catch (Exception ex) {
                    statusLabel.setText("❌ Error: " + ex.getMessage());
                    statusLabel.setStyle("-fx-text-fill: red;");
                }
            }
        });
        
        continueBtn.setOnAction(e -> {
            dialog.setResult(true);
            dialog.close();
            checkAuthentication(); // Immediately start login flow
        });

        content.getChildren().addAll(
            instructionsLabel, 
            instructionsArea, 
            link, 
            sep, 
            uploadLabel, 
            uploadBtn, 
            statusLabel,
            continueBtn
        );
        
        dialog.getDialogPane().setContent(content);
        // Add a hidden close button to satisfy Dialog requirements
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);

        dialog.showAndWait();
    }

    private Object getHostServices() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getHostServices'");
    }

    private void showAdvancedSearch() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Advanced Search");
        alert.setHeaderText("Advanced Search Options");
        alert.setContentText("Advanced search coming soon!\n\nFeatures:\n- Date range filtering\n- Size range filtering\n- Custom queries\n- Save search filters");
        alert.showAndWait();
    }

    private void showUserGuide() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("User Guide");
        alert.setHeaderText("How to Use");
        alert.setContentText(
            "1. Login with your Gmail account\n" +
            "2. Scan emails using File > Scan Emails\n" +
            "3. Browse senders and emails in the Analysis tab\n" +
            "4. View charts in the Charts tab\n" +
            "5. Preview emails in the Preview tab\n" +
            "6. Delete unwanted emails\n" +
            "7. Export reports for analysis"
        );
        alert.showAndWait();
    }

    private void showSenderDetails() {
        SenderStats selected = senderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("No Selection", "Please select a sender first");
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Sender Details");
        alert.setHeaderText(selected.getDisplayName());
        alert.setContentText(String.format(
            "Email: %s\n" +
            "Total Emails: %d\n" +
            "Total Storage: %s\n" +
            "Category: %s",
            selected.getSenderEmail(),
            selected.getEmailCount(),
            selected.getSizeFormatted(),
            selected.getCategory() != null ? selected.getCategory() : "N/A"
        ));
        alert.showAndWait();
    }

    private void checkAuthentication() {
        if (!GmailAuthService.getInstance().isAuthenticated()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Welcome!");
            alert.setHeaderText("Gmail Authentication Required");
            alert.setContentText("Please authenticate with your Gmail account to start analyzing your inbox.");
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
        allSenderData.clear();
        emailData.clear();
        updateStats(0, 0, 0, 0);
        updateStatus("Logged out");
        showInfo("Logged Out", "Successfully logged out from Gmail");
    }

    private void showScanDialog() {
        TextInputDialog dialog = new TextInputDialog("1000");
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
                
                List<Email> allEmails = DatabaseService.getInstance().getAllEmails();
                long promotional = allEmails.stream().filter(e -> "PROMOTIONAL".equals(e.getCategory())).count();
                long newsletter = allEmails.stream().filter(e -> "NEWSLETTER".equals(e.getCategory())).count();
                
                Platform.runLater(() -> {
                    allSenderData.setAll(stats);
                    senderData.setAll(stats);
                    updateStats(totalEmails, totalStorage, (int)promotional, (int)newsletter);
                    updateCharts();
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

    private void updateStats(int totalEmails, long totalStorage, int promotional, int newsletter) {
        totalEmailsLabel.setText(String.valueOf(totalEmails));
        double storageMB = totalStorage / (1024.0 * 1024.0);
        totalStorageLabel.setText(String.format("%.2f MB", storageMB));
        promotionalLabel.setText(String.valueOf(promotional));
        newsletterLabel.setText(String.valueOf(newsletter));
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("Smart Email Cleanup Assistant - Enhanced Edition");
        alert.setContentText("Version 2.0.0\n\n" +
            "A powerful desktop application for analyzing and cleaning up your Gmail inbox.\n\n" +
            "Features:\n" +
            "✓ Advanced email scanning and analysis\n" +
            "✓ Interactive charts and visualizations\n" +
            "✓ Email categorization with ML patterns\n" +
            "✓ Bulk delete and unsubscribe operations\n" +
            "✓ Dark/Light theme support\n" +
            "✓ Export to CSV and reports\n" +
            "✓ Email preview and search\n\n" +
            "Built with JavaFX, Gmail API, and H2 Database\n" +
            "© 2025 - All Rights Reserved");
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

    private static class CategoryCell extends TableCell<SenderStats, String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setStyle("");
            } else {
                setText(item);
                String color = getCategoryColor(item);
                setStyle("-fx-background-color: " + color + "; -fx-alignment: CENTER; -fx-font-weight: bold;");
            }
        }
        
        private String getCategoryColor(String category) {
            return switch (category) {
                case "PROMOTIONAL" -> "#FFE0B2";
                case "NEWSLETTER" -> "#E1BEE7";
                case "SOCIAL" -> "#BBDEFB";
                case "IMPORTANT" -> "#C8E6C9";
                case "AUTOMATED" -> "#F0F0F0";
                case "SPAM" -> "#FFCDD2";
                default -> "#FFFFFF";
            };
        }
    }

    private static class EmailCategoryCell extends TableCell<Email, String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setStyle("");
            } else {
                setText(item);
                String color = getCategoryColor(item);
                setStyle("-fx-background-color: " + color + "; -fx-alignment: CENTER; -fx-font-weight: bold;");
            }
        }
        
        private String getCategoryColor(String category) {
            return switch (category) {
                case "PROMOTIONAL" -> "#FFE0B2";
                case "NEWSLETTER" -> "#E1BEE7";
                case "SOCIAL" -> "#BBDEFB";
                case "IMPORTANT" -> "#C8E6C9";
                case "AUTOMATED" -> "#F0F0F0";
                case "SPAM" -> "#FFCDD2";
                default -> "#FFFFFF";
            };
        }
    }
}
