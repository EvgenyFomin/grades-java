package ru.protei.counter;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class CounterService {
    private static final Logger logger = Logger.getLogger(CounterService.class.getName());
    private final RedissonClient redissonClient;

    public CounterService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public void initValue(long initValue) {
        RLock lock = redissonClient.getLock("initValue");

        boolean initResult;

        try {
            initResult = lock.tryLock(100, 2, TimeUnit.SECONDS);

            if (initResult) {
                try {
                    RAtomicLong counter = redissonClient.getAtomicLong("counter");
                    if (counter.isExists()) {
                        initResult = false;
                    } else {
                        counter.set(initValue);
                    }
                } finally {
                    lock.unlock();
                }
            }

        } catch (InterruptedException e) {
            initResult = false;
        }

        if (initResult) {
            logger.info("init value: " + initValue);
        } else {
            logger.info("value already set: " + get());
        }
    }

    public Long get() {
        RAtomicLong counter = redissonClient.getAtomicLong("counter");
        return counter == null ? null : counter.get();
    }

    public Long incrementAndGet() {
        RAtomicLong counter = redissonClient.getAtomicLong("counter");
        return counter == null ? null : counter.incrementAndGet();
    }
}
