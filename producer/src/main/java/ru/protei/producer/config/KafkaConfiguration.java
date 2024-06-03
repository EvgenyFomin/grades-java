package ru.protei.producer.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@RequiredArgsConstructor
public class KafkaConfiguration {
    @Bean
    public ProducerFactory<Object, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:29092,kafka2:29093");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
//        Любая строка. Применяется для журналирования, показателей и при задании квот.
        configProps.put(ProducerConfig.CLIENT_ID_CONFIG, "asdfasdf");
//        Ожидание производителем ответа от брокера.
//        0 - ответа не ожидаем.
//        1 - ожидаем получения ведущей репликой.
//        all - ожидаем получения всеми репликами.
        configProps.put(ProducerConfig.ACKS_CONFIG, "0");
//        Время максимальной блокировки при отправке сообщения.
//        Блокируемся при переполнении буфера отправки или недоступности метаданных.
        configProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);
//        Количество памяти для буферизации сообщений, ожидающих отправки.
//        Если достигнем лимита, будем ждать MAX_BLOCK_MS_CONFIG времени
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 5000); // 175
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
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10000);
//        Время от момента, когда запись поместилась в пакет, до момента ответа брокера или отказа от отправки + время, потраченное на повторные попытки.
//        Должен быть >= LINGER_MS_CONFIG + REQUEST_TIMEOUT_MS_CONFIG
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 12000);
//        Тип сжатия
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");
//        Определяет размер пакета в байтах
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 50);
//        Максимальный размер отправляемого запроса брокеру. Рекомендуется выставлять равным message.max.bytes
        configProps.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 1024 * 1024);
//        Количество пакетов сообщений, которые производитель может отправить серверу, не получая ответов
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
//        Механизм, позволяющий избежать дублей сообщений. Работает только при:
//        1. MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION <= 5
//        2. acks = all
//        3. retries > 0
//        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<Object, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
