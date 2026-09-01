package com.capitec.capibook.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration.
 *
 * An explicit {@link KafkaTemplate}{@code <String, String>} bean is declared here so that
 * Spring can resolve the typed injection in {@link AppointmentEventPublisher}.
 * Spring Boot's {@code KafkaAutoConfiguration} skips its own KafkaTemplate
 * (@ConditionalOnMissingBean) once this bean is present.
 *
 * Consumer factories are still autoconfigured by Spring Boot via application.yml.
 *
 * Stable consumer group IDs:
 *   capibook-notification-group  — appointment.* → notification delivery (Phase 7)
 *   capibook-audit-group         — appointment.* → audit log persistence
 *   capibook-analytics-group     — appointment.completed / no_show → analytics (Phase 8)
 *
 * Transactional outbox pattern: DEFERRED.
 * Events are published via @TransactionalEventListener(AFTER_COMMIT), which guarantees
 * Kafka publication only occurs after the database transaction commits. Events CAN be
 * lost if the broker is unavailable at publication time. A full outbox implementation
 * (outbox_events table + polling relay) will be added if reliability requirements increase.
 */
@Configuration
@EnableConfigurationProperties(KafkaTopicsProperties.class)
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // Configurable per profile: test profile sets this to 1000ms to avoid blocking
    // when no broker is available; production uses the default 30000ms.
    @Value("${spring.kafka.producer.properties.max.block.ms:30000}")
    private long maxBlockMs;

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, maxBlockMs);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
