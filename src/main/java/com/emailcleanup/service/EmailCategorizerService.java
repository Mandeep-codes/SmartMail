package com.emailcleanup.service;

import com.emailcleanup.model.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class EmailCategorizerService {
    private static final Logger logger = LoggerFactory.getLogger(EmailCategorizerService.class);
    private static EmailCategorizerService instance;

    private static final List<String> PROMOTIONAL_KEYWORDS = Arrays.asList(
        "sale", "discount", "offer", "deal", "promotion", "coupon", "save", "free shipping",
        "limited time", "exclusive", "special offer", "buy now", "shop now", "clearance",
        "% off", "unsubscribe", "newsletter", "marketing", "advertisement"
    );

    private static final List<String> SOCIAL_KEYWORDS = Arrays.asList(
        "liked", "commented", "shared", "followed", "mentioned", "tagged",
        "notification", "activity", "update from", "friend request", "message from"
    );

    private static final List<String> NEWSLETTER_KEYWORDS = Arrays.asList(
        "newsletter", "weekly digest", "monthly update", "daily brief",
        "subscribe", "subscription", "bulletin", "roundup"
    );

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "noreply|no-reply|donotreply|do-not-reply|notifications|automated|mailer",
        Pattern.CASE_INSENSITIVE
    );

    private EmailCategorizerService() {
    }

    public static synchronized EmailCategorizerService getInstance() {
        if (instance == null) {
            instance = new EmailCategorizerService();
        }
        return instance;
    }

    public String categorizeEmail(Email email, String htmlBody) {
        String lowerSubject = email.getSubject() != null ? email.getSubject().toLowerCase() : "";
        String lowerSnippet = email.getSnippet() != null ? email.getSnippet().toLowerCase() : "";
        String lowerFrom = email.getFrom() != null ? email.getFrom().toLowerCase() : "";
        String lowerBody = htmlBody != null ? htmlBody.toLowerCase() : "";
        
        String combinedText = lowerSubject + " " + lowerSnippet + " " + lowerBody;

        if (email.isHasUnsubscribeLink()) {
            if (containsKeywords(combinedText, NEWSLETTER_KEYWORDS)) {
                return "NEWSLETTER";
            }
            if (containsKeywords(combinedText, PROMOTIONAL_KEYWORDS)) {
                return "PROMOTIONAL";
            }
            if (containsKeywords(combinedText, SOCIAL_KEYWORDS)) {
                return "SOCIAL";
            }
            return "PROMOTIONAL";
        }

        if (EMAIL_PATTERN.matcher(lowerFrom).find()) {
            if (containsKeywords(combinedText, SOCIAL_KEYWORDS)) {
                return "SOCIAL";
            }
            return "AUTOMATED";
        }

        if (containsKeywords(combinedText, PROMOTIONAL_KEYWORDS)) {
            return "PROMOTIONAL";
        }

        if (containsKeywords(combinedText, SOCIAL_KEYWORDS)) {
            return "SOCIAL";
        }

        if (containsKeywords(combinedText, NEWSLETTER_KEYWORDS)) {
            return "NEWSLETTER";
        }

        if (email.getLabelIds() != null) {
            String labels = email.getLabelIds().toLowerCase();
            if (labels.contains("spam")) {
                return "SPAM";
            }
            if (labels.contains("promotions") || labels.contains("category_promotions")) {
                return "PROMOTIONAL";
            }
            if (labels.contains("social") || labels.contains("category_social")) {
                return "SOCIAL";
            }
            if (labels.contains("updates") || labels.contains("category_updates")) {
                return "NEWSLETTER";
            }
        }

        return "IMPORTANT";
    }

    private boolean containsKeywords(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
