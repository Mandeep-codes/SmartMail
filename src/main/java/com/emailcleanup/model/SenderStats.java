package com.emailcleanup.model;

public class SenderStats {
    private String senderEmail;
    private String senderName;
    private int emailCount;
    private long totalSizeBytes;
    private String category;

    public SenderStats() {
    }

    public SenderStats(String senderEmail, String senderName, int emailCount, long totalSizeBytes) {
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.emailCount = emailCount;
        this.totalSizeBytes = totalSizeBytes;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public int getEmailCount() {
        return emailCount;
    }

    public void setEmailCount(int emailCount) {
        this.emailCount = emailCount;
    }

    public long getTotalSizeBytes() {
        return totalSizeBytes;
    }

    public void setTotalSizeBytes(long totalSizeBytes) {
        this.totalSizeBytes = totalSizeBytes;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSizeFormatted() {
        if (totalSizeBytes < 1024) {
            return totalSizeBytes + " B";
        } else if (totalSizeBytes < 1024 * 1024) {
            return String.format("%.2f KB", totalSizeBytes / 1024.0);
        } else if (totalSizeBytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", totalSizeBytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", totalSizeBytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    public String getDisplayName() {
        return senderName != null && !senderName.isEmpty() ? senderName : senderEmail;
    }
}
