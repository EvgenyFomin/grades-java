package ru.protei.producer.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.protei.common.dto.JsonMessage;

import java.util.concurrent.ExecutionException;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaProducer {

    @Value("${kafka.topics.test-topic}")
    private String topic;

    private int messageNumber = 0;

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    public void sendMessages() throws ExecutionException, InterruptedException {
        for (int i = 0; i < 1000; i++) {
            messageNumber++;
            JsonMessage jsonMessage = JsonMessage.builder()
                    .number(messageNumber)
                    .message("message number " + messageNumber)
                    .build();
            Thread.sleep(1000);
            kafkaTemplate.send(topic, String.valueOf(messageNumber), jsonMessage);
            log.info("Отправлено сообщение номер {}", messageNumber);
        }
    }
}