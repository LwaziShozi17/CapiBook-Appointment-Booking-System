package com.capitec.capibook.notification;

public interface NotificationProvider {

    void send(String recipientEmail, String subject, String body);
}
