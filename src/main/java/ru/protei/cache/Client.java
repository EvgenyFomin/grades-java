package ru.protei.cache;

import org.redisson.Redisson;
import org.redisson.config.Config;
import ru.protei.counter.CounterService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Client {
    private static final Logger logger = Logger.getLogger(CounterService.class.getName());

    public static void main(String[] args) throws InterruptedException {
        Config config = new Config();
        config.useSingleServer().setClientName("client-1").setPassword("123123123").setAddress("redis://127.0.0.1:6379");

        CacheService cacheService = new CacheService(Redisson.create(config));
        cacheService.put(3, "hello", 10);

        Thread.sleep(2000);

        logger.info("value: " + cacheService.get(3));

        Thread.sleep(9000);

        logger.info("value: " + cacheService.get(3));
    }
}
