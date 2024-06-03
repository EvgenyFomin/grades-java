package ru.protei.consumer.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import ru.protei.common.dto.JsonMessage;

/**
 * Kafka consumers.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerListeners {

    @KafkaListener(
            groupId = "group1",
            topics = "${kafka.topics.test-topic}",
            containerFactory = "kafkaListenerContainerFactory")
    public void handle(@Payload JsonMessage message) {
        readMessage(message, "listener1");
    }

    @KafkaListener(
            groupId = "group2",
            topics = "${kafka.topics.test-topic}",
            containerFactory = "kafkaListenerContainerFactory")
    public void handle2(@Payload JsonMessage message) {
        readMessage(message, "listener2");
    }

    @KafkaListener(
            topics = "${kafka.topics.test-topic}",
            groupId = "group2",
            containerFactory = "kafkaListenerContainerFactory")
    public void handle3(@Payload JsonMessage message) {
        readMessage(message, "listener3");
    }

    @KafkaListener(
            topics = "${kafka.topics.test-topic}",
            groupId = "group2",
            containerFactory = "kafkaListenerContainerFactory")
    public void handle4(@Payload JsonMessage message) {
        readMessage(message, "listener4");
    }

    @KafkaListener(
            topics = "${kafka.topics.test-topic}",
            groupId = "group2",
            containerFactory = "kafkaListenerContainerFactory")
    public void handle5(@Payload JsonMessage message) {
        readMessage(message, "listener5");
    }

    public void readMessage(JsonMessage message, String source) {
        log.info("read by {}", source);
        long number = message.getNumber();
        String currentThreadName = Thread.currentThread().getName();
        log.info("Прочитано сообщение с номером {} в потоке {}: {}", number, currentThreadName, message.getMessage());
        if (number % 100 == 0) {
            log.info("Сообщение кратно 100");
            throw new RuntimeException("Получено сообщение с номером кратным 100");
        }
    }
}