package com.emailcleanup.service;

import com.emailcleanup.model.Email;
import com.emailcleanup.model.SenderStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DatabaseService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);
    private static final String DB_URL = "jdbc:h2:~/.smart-email-cleanup/emaildb";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";
    
    private static DatabaseService instance;
    private Connection connection;

    private DatabaseService() {
    }

    public static synchronized DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }

    public void initialize() throws SQLException {
        logger.info("Initializing database");
        connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        createTables();
        logger.info("Database initialized successfully");
    }

    private void createTables() throws SQLException {
        String createEmailsTable = """
            CREATE TABLE IF NOT EXISTS emails (
                id VARCHAR(255) PRIMARY KEY,
                message_id VARCHAR(255),
                from_email VARCHAR(500),
                from_name VARCHAR(500),
                subject VARCHAR(1000),
                snippet TEXT,
                email_date TIMESTAMP,
                size_bytes BIGINT,
                category VARCHAR(50),
                has_unsubscribe BOOLEAN,
                unsubscribe_url TEXT,
                is_read BOOLEAN,
                label_ids VARCHAR(500),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String createIndexes = """
            CREATE INDEX IF NOT EXISTS idx_from_email ON emails(from_email);
            CREATE INDEX IF NOT EXISTS idx_category ON emails(category);
            CREATE INDEX IF NOT EXISTS idx_email_date ON emails(email_date);
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createEmailsTable);
            stmt.execute(createIndexes);
        }
    }

    public void saveEmail(Email email) throws SQLException {
        String sql = """
            MERGE INTO emails (id, message_id, from_email, from_name, subject, snippet, 
                              email_date, size_bytes, category, has_unsubscribe, 
                              unsubscribe_url, is_read, label_ids)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, email.getId());
            pstmt.setString(2, email.getMessageId());
            pstmt.setString(3, email.getFrom());
            pstmt.setString(4, email.getFromName());
            pstmt.setString(5, email.getSubject());
            pstmt.setString(6, email.getSnippet());
            pstmt.setTimestamp(7, email.getDate() != null ? Timestamp.valueOf(email.getDate()) : null);
            pstmt.setLong(8, email.getSizeBytes());
            pstmt.setString(9, email.getCategory());
            pstmt.setBoolean(10, email.isHasUnsubscribeLink());
            pstmt.setString(11, email.getUnsubscribeUrl());
            pstmt.setBoolean(12, email.isRead());
            pstmt.setString(13, email.getLabelIds());
            pstmt.executeUpdate();
        }
    }

    public void saveEmails(List<Email> emails) throws SQLException {
        connection.setAutoCommit(false);
        try {
            for (Email email : emails) {
                saveEmail(email);
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public List<Email> getAllEmails() throws SQLException {
        String sql = "SELECT * FROM emails ORDER BY email_date DESC";
        List<Email> emails = new ArrayList<>();

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                emails.add(mapResultSetToEmail(rs));
            }
        }
        return emails;
    }

    public List<Email> getEmailsBySender(String senderEmail) throws SQLException {
        String sql = "SELECT * FROM emails WHERE from_email = ? ORDER BY email_date DESC";
        List<Email> emails = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, senderEmail);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    emails.add(mapResultSetToEmail(rs));
                }
            }
        }
        return emails;
    }

    public List<SenderStats> getSenderStats() throws SQLException {
        String sql = """
            SELECT from_email, from_name, COUNT(*) as email_count, SUM(size_bytes) as total_size, category
            FROM emails
            GROUP BY from_email, from_name, category
            ORDER BY total_size DESC
        """;
        
        List<SenderStats> stats = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                SenderStats stat = new SenderStats();
                stat.setSenderEmail(rs.getString("from_email"));
                stat.setSenderName(rs.getString("from_name"));
                stat.setEmailCount(rs.getInt("email_count"));
                stat.setTotalSizeBytes(rs.getLong("total_size"));
                stat.setCategory(rs.getString("category"));
                stats.add(stat);
            }
        }
        return stats;
    }

    public void deleteEmailsByIds(List<String> emailIds) throws SQLException {
        if (emailIds.isEmpty()) return;

        StringBuilder sql = new StringBuilder("DELETE FROM emails WHERE id IN (");
        for (int i = 0; i < emailIds.size(); i++) {
            sql.append("?");
            if (i < emailIds.size() - 1) sql.append(",");
        }
        sql.append(")");

        try (PreparedStatement pstmt = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < emailIds.size(); i++) {
                pstmt.setString(i + 1, emailIds.get(i));
            }
            pstmt.executeUpdate();
        }
    }

    public int getTotalEmailCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM emails";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public long getTotalStorageUsed() throws SQLException {
        String sql = "SELECT SUM(size_bytes) FROM emails";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        return 0;
    }

    private Email mapResultSetToEmail(ResultSet rs) throws SQLException {
        Email email = new Email();
        email.setId(rs.getString("id"));
        email.setMessageId(rs.getString("message_id"));
        email.setFrom(rs.getString("from_email"));
        email.setFromName(rs.getString("from_name"));
        email.setSubject(rs.getString("subject"));
        email.setSnippet(rs.getString("snippet"));
        Timestamp timestamp = rs.getTimestamp("email_date");
        email.setDate(timestamp != null ? timestamp.toLocalDateTime() : null);
        email.setSizeBytes(rs.getLong("size_bytes"));
        email.setCategory(rs.getString("category"));
        email.setHasUnsubscribeLink(rs.getBoolean("has_unsubscribe"));
        email.setUnsubscribeUrl(rs.getString("unsubscribe_url"));
        email.setRead(rs.getBoolean("is_read"));
        email.setLabelIds(rs.getString("label_ids"));
        return email;
    }

    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Database connection closed");
            }
        } catch (SQLException e) {
            logger.error("Error closing database connection", e);
        }
    }
}
