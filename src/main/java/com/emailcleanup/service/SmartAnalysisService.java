package com.emailcleanup.service;

import com.emailcleanup.model.Email;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class SmartAnalysisService {

    // A simple record to hold analysis results (Java 17 feature)
    public record SenderDecayScore(
        String senderEmail,
        String senderName,
        int totalEmails,
        int unreadCount,
        double openRate, // 0.0 to 1.0
        LocalDateTime lastReceived,
        long wastedBytes,
        String status // "ZOMBIE", "SPAMMER", "COLD"
    ) {
        public String getFormattedOpenRate() {
            return String.format("%.1f%%", openRate * 100);
        }
        
        public String getFormattedWastedSpace() {
            if (wastedBytes < 1024 * 1024) return (wastedBytes / 1024) + " KB";
            return String.format("%.2f MB", wastedBytes / (1024.0 * 1024.0));
        }
    }

    public List<SenderDecayScore> analyzeInboxHealth(List<Email> allEmails) {
        Map<String, List<Email>> bySender = allEmails.stream()
            .collect(Collectors.groupingBy(Email::getFrom));

        List<SenderDecayScore> scores = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (var entry : bySender.entrySet()) {
            String sender = entry.getKey();
            List<Email> emails = entry.getValue();
            int total = emails.size();
            
            // Skip insignificant senders
            if (total < 3) continue;

            long unread = emails.stream().filter(e -> !e.isRead()).count();
            double openRate = (double) (total - unread) / total;
            
            LocalDateTime lastDate = emails.stream()
                .map(Email::getDate)
                .max(LocalDateTime::compareTo)
                .orElse(now);
                
            long daysSinceLast = ChronoUnit.DAYS.between(lastDate, now);
            long wastedBytes = emails.stream().mapToLong(Email::getSizeBytes).sum();
            
            String status = determineStatus(openRate, daysSinceLast, total);

            // Only add if it's worth cleaning up
            if (!status.equals("ACTIVE")) {
                String name = emails.get(0).getFromName();
                if (name == null || name.isEmpty()) name = sender;
                
                scores.add(new SenderDecayScore(
                    sender, name, total, (int)unread, openRate, lastDate, wastedBytes, status
                ));
            }
        }

        // Sort: "Zombie" (Oldest dead threads) first, then by size
        scores.sort(Comparator.comparing(SenderDecayScore::wastedBytes).reversed());
        return scores;
    }

    private String determineStatus(double openRate, long daysSinceLast, int total) {
        if (openRate < 0.10 && total > 10) return "SPAMMER"; // High volume, you never read
        if (daysSinceLast > 90) return "GHOST"; // Haven't heard from them in 3 months
        if (openRate < 0.30) return "COLD"; // You rarely read these
        return "ACTIVE";
    }

 // --- NEW: CLUSTERING LOGIC ---

    public record SubjectCluster(
        String normalizedSubject,
        int count,
        long totalSize,
        List<Email> emails
    ) {
        public String getSizeFormatted() {
             if (totalSize < 1024) return totalSize + " B";
             if (totalSize < 1024 * 1024) return String.format("%.2f KB", totalSize / 1024.0);
             return String.format("%.2f MB", totalSize / (1024.0 * 1024.0));
        }
    }

    public List<SubjectCluster> findSimilarSubjects(List<Email> allEmails) {
        Map<String, List<Email>> clusters = new HashMap<>();

        for (Email email : allEmails) {
            String subject = email.getSubject();
            if (subject == null || subject.isBlank()) continue;

            // Normalize: "Order #123 Confirmed" -> "Order #... Confirmed"
            String cleanSubject = subject
                .replaceAll("(?i)^(re|fwd|fw):\\s*", "") // Remove Re:, Fwd:
                .replaceAll("\\d+", "#")                 // Replace numbers with #
                .replaceAll("\\s+", " ")                 // Collapse spaces
                .trim();

            clusters.computeIfAbsent(cleanSubject, k -> new ArrayList<>()).add(email);
        }

        // Filter: Only keep patterns that appear 3+ times
        return clusters.entrySet().stream()
            .filter(entry -> entry.getValue().size() >= 3)
            .map(entry -> {
                long size = entry.getValue().stream().mapToLong(Email::getSizeBytes).sum();
                return new SubjectCluster(entry.getKey(), entry.getValue().size(), size, entry.getValue());
            })
            .sorted(Comparator.comparingLong(SubjectCluster::totalSize).reversed())
            .collect(Collectors.toList());
    }

    // --- NEW: PRIVACY SHIELD LOGIC ---

    public enum RiskLevel {
        HIGH, MEDIUM, LOW
    }

    public record PrivacyRisk(
        Email email,
        String riskType, // "Password", "Financial", "Identity"
        RiskLevel level,
        String triggerPhrase
    ) {}

    public List<PrivacyRisk> scanForPrivacyRisks(List<Email> allEmails) {
        List<PrivacyRisk> risks = new ArrayList<>();

        // Keywords for detection
        List<String> authKeywords = List.of("password reset", "verification code", "security code", "login alert", "2fa", "otp");
        List<String> financeKeywords = List.of("bank statement", "account summary", "payment confirmation", "invoice #", "receipt for");
        List<String> idKeywords = List.of("passport application", "visa granted", "tax return", "aadhaar", "social security");

        for (Email email : allEmails) {
            String text = (email.getSubject() + " " + email.getSnippet()).toLowerCase();
            
            // 1. Check Auth Risks (High Priority)
            for (String key : authKeywords) {
                if (text.contains(key)) {
                    risks.add(new PrivacyRisk(email, "Credentials", RiskLevel.HIGH, key));
                    break; // Found a risk, move to next email
                }
            }

            // 2. Check Financials
            for (String key : financeKeywords) {
                if (text.contains(key)) {
                    risks.add(new PrivacyRisk(email, "Financial", RiskLevel.MEDIUM, key));
                    break;
                }
            }
            
            // 3. Check Identity
            for (String key : idKeywords) {
                if (text.contains(key)) {
                    risks.add(new PrivacyRisk(email, "Identity", RiskLevel.HIGH, key));
                    break;
                }
            }
        }
        
        // Sort by Risk Level (High first)
        risks.sort(Comparator.comparing(PrivacyRisk::level));
        return risks;
    }
}
