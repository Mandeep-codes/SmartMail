package com.emailcleanup.service;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.BatchDeleteMessagesRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.List;

public class EmailActionService {
    private static final Logger logger = LoggerFactory.getLogger(EmailActionService.class);
    private static EmailActionService instance;

    private EmailActionService() {
    }

    public static synchronized EmailActionService getInstance() {
        if (instance == null) {
            instance = new EmailActionService();
        }
        return instance;
    }

    public void deleteEmails(List<String> emailIds) throws Exception {
        if (emailIds.isEmpty()) {
            logger.warn("No emails to delete");
            return;
        }

        Gmail service = GmailAuthService.getInstance().getGmailService();
        String user = "me";

        if (emailIds.size() == 1) {
            service.users().messages().trash(user, emailIds.get(0)).execute();
            logger.info("Moved email to trash: {}", emailIds.get(0));
        } else {
            BatchDeleteMessagesRequest batchRequest = new BatchDeleteMessagesRequest()
                    .setIds(emailIds);
            service.users().messages().batchDelete(user, batchRequest).execute();
            logger.info("Batch deleted {} emails", emailIds.size());
        }

        DatabaseService.getInstance().deleteEmailsByIds(emailIds);
    }

    public void openUnsubscribeLink(String url) {
        if (url == null || url.isEmpty()) {
            logger.warn("No unsubscribe URL provided");
            return;
        }

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                logger.info("Opened unsubscribe URL: {}", url);
            }
        } catch (Exception e) {
            logger.error("Failed to open unsubscribe URL", e);
        }
    }
}
