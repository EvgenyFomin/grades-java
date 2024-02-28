package ru.protei;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockTest {
    @Test
    public void testTryLock() throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        ReentrantLock lock = new ReentrantLock();

        Future<Boolean> future1 = executorService.submit(() -> tryLockTask(lock));
        Future<Boolean> future2 = executorService.submit(() -> tryLockTask(lock));

        Boolean future1Result = future1.get();
        Boolean future2Result = future2.get();

        Assertions.assertTrue(future1Result || future2Result);
        Assertions.assertFalse(future1Result && future2Result);
    }

    @Test
    public void testLock() throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        ReentrantLock lock = new ReentrantLock();

        Future<Boolean> future1 = executorService.submit(() -> lockTask(lock));
        Future<Boolean> future2 = executorService.submit(() -> lockTask(lock));

        Boolean future1Result = future1.get();
        Boolean future2Result = future2.get();

        Assertions.assertTrue(future1Result || future2Result);
        Assertions.assertTrue(future1Result && future2Result);
    }

    @Test
    public void testHoldCount() {
        ReentrantLock lock = new ReentrantLock();

        for (int i = 0; i < 5; i++) {
            lock.lock();
        }

        Assertions.assertEquals(5, lock.getHoldCount());
    }

    private boolean tryLockTask(ReentrantLock lock) {
        try {
            boolean locked = lock.tryLock(1, TimeUnit.SECONDS);
            if (!locked) {
                return false;
            } else {
                TimeUnit.SECONDS.sleep(3);
                lock.unlock();
                return true;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean lockTask(ReentrantLock lock) {
        try {
            lock.lock();
            TimeUnit.SECONDS.sleep(3);
            lock.unlock();
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }
}
