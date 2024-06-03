package ru.protei.consumer.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import ru.protei.common.dto.JsonMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * Конфигурация Kafka.
 */
@Configuration
@EnableKafka
@RequiredArgsConstructor
public class KafkaConfiguration {

    private static final String DLT_TOPIC_SUFFIX = ".dlt";

//    private final ProducerFactory<Object, Object> producerFactory;
//    private final ConsumerFactory<Object, Object> consumerFactory;

    @Bean
    public ProducerFactory<Object, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:29092,kafka2:29093");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, DltMessageSerializer.class);
//        Любая строка. Применяется для журналирования, показателей и при задании квот.
        configProps.put(ProducerConfig.CLIENT_ID_CONFIG, "kafka-example-consumer-dlt-producer");
//        Ожидание производителем ответа от брокера.
//        0 - ответа не ожидаем.
//        1 - ожидаем получения ведущей репликой.
//        all - ожидаем получения всеми репликами.
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
//        Время максимальной блокировки при отправке сообщения.
//        Блокируемся при переполнении буфера отправки или недоступности метаданных.
        configProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 1000);
//        Количество памяти для буферизации сообщений, ожидающих отправки.
//        Если достигнем лимита, будем ждать MAX_BLOCK_MS_CONFIG времени
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 6000);
//        Время ожидания между повторными попытками, в случае временных ошибок
        configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
//        Количество повторных попыток. Если не хотим повторять, выставляем 0
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
//        Время ожидания ответа от сервера при отправке данных.
//        Должен быть больше, чем replica.lag.time.max.ms (настройка брокера),
//        чтобы избежать нежелательного дублирования в результате повторных попыток
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 1000);
//        Время ожидания дополнительных сообщений перед отправкой всего пакета.
//        Увеличивает задержку, но также увеличивает пропускную способность + улучшает сжатие
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 3000);
//        Время от момента, когда запись поместилась в пакет, до момента ответа брокера или отказа от отправки + время, потраченное на повторные попытки.
//        Должен быть >= LINGER_MS_CONFIG + REQUEST_TIMEOUT_MS_CONFIG
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 7000);
//        Тип сжатия
//        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");
//        Определяет размер пакета в байтах
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 100);
//        Максимальный размер отправляемого запроса брокеру. Рекомендуется выставлять равным message.max.bytes
        configProps.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 1024 * 1024);
//        Количество пакетов сообщений, которые производитель может отправить серверу, не получая ответов
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
//        Механизм, позволяющий избежать дублей сообщений. Работает только при:
//        1. MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION <= 5
//        2. acks = all
//        3. retries > 0
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public ConsumerFactory<Object, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:29092,kafka2:29093");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, IntegerDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, JsonMessage.class.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "ru.protei.common.dto");
//        Минимальный объем данных, при получении от брокера.
//        Брокер не отдаст данные до тех пор, пока не появятся новые сообщения
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 5000);
//        Задержка перед появлением достаточного количества данных перед отправкой
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 5000);
//        Ограничение по памяти на данные, которые может принять потребитель.
//        Исключение - первый пакет.
//        На брокере можно настроить максимальный размер пакета:
//        1. message.max.bytes - со стороны брокера,
//        2. max.message.bytes - со стороны топика
        props.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, 50 * 1024 * 1024);
//        Максимальное количество записей, которое возвращает брокер потребителю за одну итерацию цикла опроса
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
//        Частота отправки контрольных сигналов координатору группы (брокеру) фоновым потоком. Обычно 1/3 от SESSION_TIMEOUT_MS_CONFIG
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 1000);
//        Время, в течение которого потребитель может обходиться без контрольных сигналов
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 1000 * 30);
//        Время, в течение которого потребитель может не запрашивать новые данные
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 1000 * 60 * 5);
//        Время ожидания ответа от брокера
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 1000 * 30);
//        Стратегия сброса смещения, если смещение у потребителя отсутствует, либо некорректно. earliest/latest/none
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
//        Автоматическая фиксация смещений. При false можно контролировать самостоятельно.
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000 * 5);

//        Стратегия назначения партиций потребителям. Можно задать кастомно, реализуя интерфейс ConsumerPartitionAssignor
        props.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, "org.apache.kafka.clients.consumer.RoundRobinAssignor");
//        Используется брокерами для идентификации отправленных запросов потребителями.
//        Применяется для журналирования, показателей и при задании квот.
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, "kafka-example-consumer");

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<Object, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
            DefaultErrorHandler errorHandler
//            , ConsumerFactory<Object, Object> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory = new ConcurrentKafkaListenerContainerFactory<>();
        ConsumerFactory<Object, Object> consumerFactory = consumerFactory();
        kafkaListenerContainerFactory.setConsumerFactory(consumerFactory);
        kafkaListenerContainerFactory.setCommonErrorHandler(errorHandler);
        kafkaListenerContainerFactory.setConcurrency(2);
        kafkaListenerContainerFactory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return kafkaListenerContainerFactory;
    }

    /**
     * dead-letter topic.
     */
    @Bean
    public DeadLetterPublishingRecoverer publisher(KafkaTemplate<Object, Object> bytesTemplate) {
        return new DeadLetterPublishingRecoverer(bytesTemplate, (consumerRecord, exception) ->
                new TopicPartition(consumerRecord.topic() + DLT_TOPIC_SUFFIX, consumerRecord.partition()));
    }

    /**
     * Обработчик исключений при получении сообщений из kafka по умолчанию.
     */
    @Bean
    public DefaultErrorHandler errorHandler(DeadLetterPublishingRecoverer deadLetterPublishingRecoverer) {
        final var handler = new DefaultErrorHandler(deadLetterPublishingRecoverer);
        handler.addNotRetryableExceptions(Exception.class);
        return handler;
    }
}
