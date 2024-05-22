package ru.protei.producer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.protei.producer.producer.KafkaProducer;

import java.util.concurrent.ExecutionException;

@SpringBootApplication
public class ProducerApplication {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ConfigurableApplicationContext context = SpringApplication.run(ProducerApplication.class, args);
        KafkaProducer producer = context.getBean(KafkaProducer.class);
        producer.sendMessages();
    }
}