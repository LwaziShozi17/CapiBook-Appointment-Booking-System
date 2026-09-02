package com.capitec.capibook.admin;

import com.capitec.capibook.kafka.events.AppointmentEventMessage;
import com.capitec.capibook.kafka.events.AppointmentEventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsConsumerTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AnalyticsConsumer analyticsConsumer;

    @Test
    void handle_validPayload_doesNotThrow() throws Exception {
        AppointmentEventMessage message = new AppointmentEventMessage(
                UUID.randomUUID(), AppointmentEventType.APPOINTMENT_CREATED,
                Instant.now(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.now().plusDays(7), LocalTime.of(9, 0), "CAP-2026-ABCDE"
        );

        when(objectMapper.readValue(any(String.class), eq(AppointmentEventMessage.class)))
                .thenReturn(message);

        assertThatNoException().isThrownBy(() -> analyticsConsumer.handle("{\"valid\":\"payload\"}"));
    }

    @Test
    void handle_invalidPayload_logsErrorAndDoesNotThrow() throws Exception {
        when(objectMapper.readValue(any(String.class), eq(AppointmentEventMessage.class)))
                .thenThrow(new RuntimeException("Parse error"));

        assertThatNoException().isThrownBy(() -> analyticsConsumer.handle("not-valid-json"));
    }
}
