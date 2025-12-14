package com.emailcleanup.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Email {
    private String id;
    private String messageId;
    private String from;
    private String fromName;
    private String subject;
    private String snippet;
    private LocalDateTime date;
    private long sizeBytes;
    private String category;
    private boolean hasUnsubscribeLink;
    private String unsubscribeUrl;
    private boolean isRead;
    private String labelIds;

    public Email() {
    }

    public Email(String id, String messageId, String from, String fromName, String subject, 
                 String snippet, LocalDateTime date, long sizeBytes) {
        this.id = id;
        this.messageId = messageId;
        this.from = from;
        this.fromName = fromName;
        this.subject = subject;
        this.snippet = snippet;
        this.date = date;
        this.sizeBytes = sizeBytes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isHasUnsubscribeLink() {
        return hasUnsubscribeLink;
    }

    public void setHasUnsubscribeLink(boolean hasUnsubscribeLink) {
        this.hasUnsubscribeLink = hasUnsubscribeLink;
    }

    public String getUnsubscribeUrl() {
        return unsubscribeUrl;
    }

    public void setUnsubscribeUrl(String unsubscribeUrl) {
        this.unsubscribeUrl = unsubscribeUrl;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String getLabelIds() {
        return labelIds;
    }

    public void setLabelIds(String labelIds) {
        this.labelIds = labelIds;
    }

    public String getSizeFormatted() {
        if (sizeBytes < 1024) {
            return sizeBytes + " B";
        } else if (sizeBytes < 1024 * 1024) {
            return String.format("%.2f KB", sizeBytes / 1024.0);
        } else {
            return String.format("%.2f MB", sizeBytes / (1024.0 * 1024.0));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Email email = (Email) o;
        return Objects.equals(id, email.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Email{" +
                "id='" + id + '\'' +
                ", from='" + from + '\'' +
                ", subject='" + subject + '\'' +
                ", date=" + date +
                ", size=" + getSizeFormatted() +
                '}';
    }
}
