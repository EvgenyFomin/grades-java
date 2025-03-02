package ru.protei.counter;

import org.redisson.Redisson;
import org.redisson.config.Config;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Client {
    private static final Logger logger = Logger.getLogger(CounterService.class.getName());

    public static void main(String[] args) {
        Config config = new Config();
        config.useSingleServer().setClientName("client-1").setPassword("123123123").setAddress("redis://127.0.0.1:6379");

        CounterService counterService = new CounterService(Redisson.create(config));
        counterService.initValue(1);

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            logger.info(counterService.incrementAndGet() + " <- value");
        }, 0, 2, TimeUnit.SECONDS);
    }
}
