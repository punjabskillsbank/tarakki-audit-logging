package com.tarakki.audit_logging.kafka;

import com.tarakki.audit_logging.datafactory.AuditEventMessageDataFactory;
import com.tarakki.audit_logging.dto.AuditEventMessage;
import com.tarakki.audit_logging.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = AuditKafkaConsumerIntegrationTest.TestApplication.class, properties = {
        "spring.kafka.consumer.group-id=" + AuditEventMessageDataFactory.KAFKA_CONSUMER_GROUP,
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.ErrorHandlingDeserializer",
        "spring.kafka.consumer.properties.spring.deserializer.value.delegate.class=org.springframework.kafka.support.serializer.JsonDeserializer",
        "spring.kafka.consumer.properties.spring.json.trusted.packages=com.tarakki.audit_logging.dto",
        "spring.kafka.consumer.properties.spring.json.value.default.type=com.tarakki.audit_logging.dto.AuditEventMessage",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
        "audit.logging.topic=" + AuditEventMessageDataFactory.KAFKA_TOPIC
})
@EmbeddedKafka(partitions = 1, topics = AuditEventMessageDataFactory.KAFKA_TOPIC,
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class AuditKafkaConsumerIntegrationTest {

    @MockitoBean
    private AuditLogService auditLogService;

    @org.springframework.beans.factory.annotation.Autowired
    private KafkaTemplate<String, AuditEventMessage> kafkaTemplate;

    @Test
    void shouldDeserializeAndDeliverKafkaAuditEvent() throws Exception {
        java.util.concurrent.CountDownLatch messageReceived = new java.util.concurrent.CountDownLatch(1);
        AuditEventMessage event = AuditEventMessageDataFactory.defaultEvent();
        doAnswer(invocation -> {
            messageReceived.countDown();
            return null;
        }).when(auditLogService).save(event);

        kafkaTemplate.send(AuditEventMessageDataFactory.KAFKA_TOPIC, event).get();

        assertTrue(messageReceived.await(10, TimeUnit.SECONDS));
        verify(auditLogService).save(event);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = {
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
            "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
            "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"
    })
    @Import(AuditKafkaConsumer.class)
    static class TestApplication {
    }
}
