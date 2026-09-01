package com.capitec.capibook.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class MockNotificationProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(MockNotificationProvider.class);

    @Override
    public void send(String recipientEmail, String subject, String body) {
        log.info("[MOCK-EMAIL] To: {} | Subject: {}", recipientEmail, subject);
        log.debug("[MOCK-EMAIL] Body: {}", body);
    }
}
