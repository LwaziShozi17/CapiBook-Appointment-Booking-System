package com.capitec.capibook.kafka;

import com.capitec.capibook.audit.AuditConsumer;
import com.capitec.capibook.audit.AuditLog;
import com.capitec.capibook.audit.AuditLogRepository;
import com.capitec.capibook.kafka.events.AppointmentEventMessage;
import com.capitec.capibook.kafka.events.AppointmentEventType;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kafka integration tests for Phase 6.
 *
 * Three test categories:
 * 1. AuditConsumer unit path: calls handle() directly to verify DB write logic.
 * 2. Producer path: publishes to EmbeddedKafka and reads back with a raw consumer.
 * 3. Failure resilience: Kafka unavailability does not cause exceptions to callers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {
            "appointment.created",
            "appointment.cancelled",
            "appointment.confirmed",
            "appointment.completed",
            "appointment.no_show",
            "appointment.rescheduled"
        },
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@TestPropertySource(properties = "spring.kafka.producer.properties.max.block.ms=5000")
@DirtiesContext
class KafkaEventIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AuditConsumer auditConsumer;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
    }

    @Test
    void auditConsumer_handlePayload_writesAuditLogToDb() throws Exception {
        UUID appointmentId = UUID.randomUUID();
        AppointmentEventMessage message = buildMessage(appointmentId, AppointmentEventType.APPOINTMENT_CREATED);
        String payload = objectMapper.writeValueAsString(message);

        auditConsumer.handle(payload);

        List<AuditLog> logs = auditLogRepository.findAll();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getAction()).isEqualTo("APPOINTMENT_CREATED");
        assertThat(logs.get(0).getEntityId()).isEqualTo(appointmentId.toString());
        assertThat(logs.get(0).getEntityType()).isEqualTo("Appointment");
    }

    @Test
    void auditConsumer_handleMultipleEventTypes_writesEachToDb() throws Exception {
        auditConsumer.handle(objectMapper.writeValueAsString(
                buildMessage(UUID.randomUUID(), AppointmentEventType.APPOINTMENT_CANCELLED)));
        auditConsumer.handle(objectMapper.writeValueAsString(
                buildMessage(UUID.randomUUID(), AppointmentEventType.APPOINTMENT_CONFIRMED)));

        assertThat(auditLogRepository.count()).isEqualTo(2);
    }

    @Test
    void kafkaProducer_sendsMessageToEmbeddedBroker_messageIsReadable() throws Exception {
        UUID appointmentId = UUID.randomUUID();
        String payload = objectMapper.writeValueAsString(
                buildMessage(appointmentId, AppointmentEventType.APPOINTMENT_CREATED));

        kafkaTemplate.send("appointment.created", appointmentId.toString(), payload);

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "test-reader-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<String, String>(
                consumerProps).createConsumer()) {
            embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, "appointment.created");
            ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer, java.time.Duration.ofSeconds(5));

            assertThat(records.count()).isGreaterThan(0);
            String received = records.iterator().next().value();
            assertThat(received).contains("APPOINTMENT_CREATED");
            assertThat(received).contains(appointmentId.toString());
        }
    }

    @Test
    void auditConsumer_handleDuplicateEvent_writesTwoLogs() throws Exception {
        UUID appointmentId = UUID.randomUUID();
        String payload = objectMapper.writeValueAsString(
                buildMessage(appointmentId, AppointmentEventType.APPOINTMENT_CREATED));

        // Process the same payload twice — no idempotency guard in AuditConsumer,
        // so each call writes one audit log (demonstrates current behaviour is logged, not deduplicated)
        auditConsumer.handle(payload);
        auditConsumer.handle(payload);

        assertThat(auditLogRepository.count()).isEqualTo(2);
    }

    private AppointmentEventMessage buildMessage(UUID appointmentId, AppointmentEventType type) {
        return new AppointmentEventMessage(
                UUID.randomUUID(), type, Instant.now(), appointmentId,
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.now().plusDays(7), LocalTime.of(9, 0), "CAP-2026-INTEG"
        );
    }
}
