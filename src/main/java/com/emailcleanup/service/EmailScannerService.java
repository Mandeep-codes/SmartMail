package com.emailcleanup.service;

import com.emailcleanup.model.Email;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class EmailScannerService {
    private static final Logger logger = LoggerFactory.getLogger(EmailScannerService.class);
    private static EmailScannerService instance;
    
    private final EmailCategorizerService categorizer;

    private EmailScannerService() {
        this.categorizer = EmailCategorizerService.getInstance();
    }

    public static synchronized EmailScannerService getInstance() {
        if (instance == null) {
            instance = new EmailScannerService();
        }
        return instance;
    }

    public void scanEmails(int maxResults, Consumer<String> progressCallback) throws Exception {
        Gmail service = GmailAuthService.getInstance().getGmailService();
        String user = "me";

        progressCallback.accept("Fetching email list...");
        logger.info("Starting email scan, max results: {}", maxResults);

        List<Message> messages = new ArrayList<>();
        String pageToken = null;
        
        do {
            ListMessagesResponse response = service.users().messages()
                    .list(user)
                    .setMaxResults(100L)
                    .setPageToken(pageToken)
                    .execute();

            if (response.getMessages() != null) {
                messages.addAll(response.getMessages());
            }

            pageToken = response.getNextPageToken();
            progressCallback.accept(String.format("Found %d emails...", messages.size()));

            if (messages.size() >= maxResults) {
                messages = messages.subList(0, maxResults);
                break;
            }
        } while (pageToken != null);

        logger.info("Found {} emails, processing...", messages.size());
        processMessages(service, user, messages, progressCallback);
    }

    private void processMessages(Gmail service, String user, List<Message> messages, 
                                 Consumer<String> progressCallback) throws Exception {
        List<Email> emails = new ArrayList<>();
        AtomicInteger processed = new AtomicInteger(0);
        int total = messages.size();

        for (Message message : messages) {
            try {
                Email email = fetchAndParseEmail(service, user, message.getId());
                if (email != null) {
                    emails.add(email);
                }

                int current = processed.incrementAndGet();
                if (current % 10 == 0 || current == total) {
                    progressCallback.accept(String.format("Processing %d/%d emails...", current, total));
                }

                if (emails.size() >= 50) {
                    DatabaseService.getInstance().saveEmails(emails);
                    emails.clear();
                }
            } catch (Exception e) {
                logger.error("Error processing message: " + message.getId(), e);
            }
        }

        if (!emails.isEmpty()) {
            DatabaseService.getInstance().saveEmails(emails);
        }

        progressCallback.accept(String.format("Scan complete! Processed %d emails.", total));
        logger.info("Email scan completed successfully");
    }

    private Email fetchAndParseEmail(Gmail service, String user, String messageId) throws IOException {
        Message message = service.users().messages()
                .get(user, messageId)
                .setFormat("full")
                .execute();

        Email email = new Email();
        email.setId(messageId);
        email.setMessageId(getHeader(message, "Message-ID"));
        email.setSnippet(message.getSnippet());
        
        String from = getHeader(message, "From");
        email.setFrom(extractEmail(from));
        email.setFromName(extractName(from));
        
        email.setSubject(getHeader(message, "Subject"));
        
        Long internalDate = message.getInternalDate();
        if (internalDate != null) {
            email.setDate(LocalDateTime.ofInstant(
                Instant.ofEpochMilli(internalDate), 
                ZoneId.systemDefault()
            ));
        }
        
        email.setSizeBytes(message.getSizeEstimate() != null ? message.getSizeEstimate() : 0);
        
        if (message.getLabelIds() != null) {
            email.setLabelIds(String.join(",", message.getLabelIds()));
            email.setRead(!message.getLabelIds().contains("UNREAD"));
        }
        
        String htmlBody = getHtmlBody(message.getPayload());
        parseUnsubscribeInfo(email, message, htmlBody);
        
        String category = categorizer.categorizeEmail(email, htmlBody);
        email.setCategory(category);

        return email;
    }

    private String getHeader(Message message, String name) {
        if (message.getPayload() == null || message.getPayload().getHeaders() == null) {
            return "";
        }
        
        for (MessagePartHeader header : message.getPayload().getHeaders()) {
            if (header.getName().equalsIgnoreCase(name)) {
                return header.getValue();
            }
        }
        return "";
    }

    private String extractEmail(String fromHeader) {
        if (fromHeader == null || fromHeader.isEmpty()) return "";
        
        int start = fromHeader.indexOf('<');
        int end = fromHeader.indexOf('>');
        
        if (start != -1 && end != -1 && end > start) {
            return fromHeader.substring(start + 1, end);
        }
        
        if (fromHeader.contains("@")) {
            return fromHeader.trim();
        }
        
        return fromHeader;
    }

    private String extractName(String fromHeader) {
        if (fromHeader == null || fromHeader.isEmpty()) return "";
        
        int start = fromHeader.indexOf('<');
        if (start != -1) {
            return fromHeader.substring(0, start).trim().replaceAll("\"", "");
        }
        
        return "";
    }

    private String getHtmlBody(MessagePart payload) {
        if (payload == null) return "";
        
        if (payload.getMimeType() != null && payload.getMimeType().equals("text/html")) {
            if (payload.getBody() != null && payload.getBody().getData() != null) {
                return new String(payload.getBody().decodeData());
            }
        }
        
        if (payload.getParts() != null) {
            for (MessagePart part : payload.getParts()) {
                String body = getHtmlBody(part);
                if (!body.isEmpty()) {
                    return body;
                }
            }
        }
        
        return "";
    }

    private void parseUnsubscribeInfo(Email email, Message message, String htmlBody) {
        String listUnsubscribe = getHeader(message, "List-Unsubscribe");
        if (listUnsubscribe != null && !listUnsubscribe.isEmpty()) {
            String url = extractUrlFromListUnsubscribe(listUnsubscribe);
            if (url != null) {
                email.setHasUnsubscribeLink(true);
                email.setUnsubscribeUrl(url);
                return;
            }
        }
        
        if (htmlBody != null && !htmlBody.isEmpty()) {
            try {
                Document doc = Jsoup.parse(htmlBody);
                Elements links = doc.select("a[href]");
                
                for (Element link : links) {
                    String text = link.text().toLowerCase();
                    String href = link.attr("href");
                    
                    if (text.contains("unsubscribe") || text.contains("opt out") || 
                        text.contains("remove") || href.toLowerCase().contains("unsubscribe")) {
                        email.setHasUnsubscribeLink(true);
                        email.setUnsubscribeUrl(href);
                        return;
                    }
                }
            } catch (Exception e) {
                logger.debug("Error parsing HTML for unsubscribe link", e);
            }
        }
        
        email.setHasUnsubscribeLink(false);
    }

    private String extractUrlFromListUnsubscribe(String listUnsubscribe) {
        if (listUnsubscribe.contains("<http")) {
            int start = listUnsubscribe.indexOf("<http");
            int end = listUnsubscribe.indexOf(">", start);
            if (end != -1) {
                return listUnsubscribe.substring(start + 1, end);
            }
        }
        return null;
    }
}
