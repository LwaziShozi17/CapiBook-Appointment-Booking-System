package com.capitec.capibook.notification;

import com.capitec.capibook.kafka.events.AppointmentEventMessage;
import com.capitec.capibook.user.User;
import com.capitec.capibook.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationProvider notificationProvider;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationProvider notificationProvider,
                                UserRepository userRepository,
                                NotificationRepository notificationRepository) {
        this.notificationProvider = notificationProvider;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    public void notify(AppointmentEventMessage message) {
        Optional<User> userOpt = userRepository.findById(message.customerId());
        if (userOpt.isEmpty()) {
            log.warn("Cannot send notification: customer {} not found for appointment {}",
                    message.customerId(), message.referenceNumber());
            return;
        }
        User user = userOpt.get();

        String subject = buildSubject(message);
        String body    = buildBody(message, user);

        Notification notification = new Notification();
        notification.setAppointmentId(message.appointmentId());
        notification.setUserId(message.customerId());
        notification.setChannel(NotificationChannel.EMAIL);

        try {
            notificationProvider.send(user.getEmail(), subject, body);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Notification delivery failed for appointment {}: {}",
                    message.referenceNumber(), e.getMessage());
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
        }

        notificationRepository.save(notification);
    }

    private String buildSubject(AppointmentEventMessage message) {
        return switch (message.eventType()) {
            case APPOINTMENT_CREATED     -> "Booking Confirmation — " + message.referenceNumber();
            case APPOINTMENT_CONFIRMED   -> "Appointment Confirmed — " + message.referenceNumber();
            case APPOINTMENT_CANCELLED   -> "Appointment Cancelled — " + message.referenceNumber();
            case APPOINTMENT_RESCHEDULED -> "Appointment Rescheduled — " + message.referenceNumber();
            default -> "CapiBook Appointment Update — " + message.referenceNumber();
        };
    }

    private String buildBody(AppointmentEventMessage message, User user) {
        String name = user.getFirstName();
        String date = message.appointmentDate().toString();
        String time = message.startTime().toString();
        String ref  = message.referenceNumber();

        return switch (message.eventType()) {
            case APPOINTMENT_CREATED ->
                "Dear " + name + ",\n\n" +
                "Your appointment booking has been received.\n\n" +
                "Reference: " + ref + "\nDate: " + date + "\nTime: " + time + "\n\n" +
                "We will confirm your appointment shortly.\n\nThank you for choosing CapiBook.";
            case APPOINTMENT_CONFIRMED ->
                "Dear " + name + ",\n\n" +
                "Your appointment has been confirmed.\n\n" +
                "Reference: " + ref + "\nDate: " + date + "\nTime: " + time + "\n\n" +
                "Please arrive 5 minutes before your scheduled time.\n\nThank you.";
            case APPOINTMENT_CANCELLED ->
                "Dear " + name + ",\n\n" +
                "Your appointment (" + ref + ") scheduled for " + date + " at " + time +
                " has been cancelled.\n\nIf you did not request this cancellation, please contact your branch.";
            case APPOINTMENT_RESCHEDULED ->
                "Dear " + name + ",\n\n" +
                "Your appointment (" + ref + ") has been rescheduled. " +
                "A new booking confirmation will follow with your updated appointment details.\n\nThank you.";
            default ->
                "Dear " + name + ",\n\nYour appointment (" + ref + ") has been updated.\n\nThank you.";
        };
    }
}
