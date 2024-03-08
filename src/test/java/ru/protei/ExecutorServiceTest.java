package ru.protei;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

public class ExecutorServiceTest {
    @Test
    public void testFixedThreadPoolSync() throws ExecutionException, InterruptedException {
        int countOfThreads = 3;
        Set<String> names = new HashSet<>();

        ExecutorService executorService = Executors.newFixedThreadPool(countOfThreads);
        executorService.submit(() -> names.add(Thread.currentThread().getName())).get();
        executorService.submit(() -> names.add(Thread.currentThread().getName())).get();
        executorService.submit(() -> names.add(Thread.currentThread().getName())).get();
        executorService.submit(() -> names.add(Thread.currentThread().getName())).get();
        executorService.submit(() -> names.add(Thread.currentThread().getName())).get();
        executorService.submit(() -> names.add(Thread.currentThread().getName())).get();

        Assertions.assertEquals(countOfThreads, names.size());
    }

    @Test
    public void testCachedThreadPoolSync() throws ExecutionException, InterruptedException {
        Set<String> names = new HashSet<>();

        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.submit(() -> names.add(Thread.currentThread().getName())).get();
        executorService.submit(() -> names.add(Thread.currentThread().getName())).get();
        executorService.submit(() -> names.add(Thread.currentThread().getName())).get();
        executorService.submit(() -> names.add(Thread.currentThread().getName())).get();
        executorService.submit(() -> names.add(Thread.currentThread().getName())).get();
        executorService.submit(() -> names.add(Thread.currentThread().getName())).get();

        Assertions.assertEquals(1, names.size());
    }

    @Test
    public void testCachedThreadPoolAsync() {
        ExecutorService executorService = Executors.newCachedThreadPool();
        Arrays.asList(
                executorService.submit(this::sleep),
                executorService.submit(this::sleep),
                executorService.submit(this::sleep),
                executorService.submit(this::sleep),
                executorService.submit(this::sleep),
                executorService.submit(this::sleep)
        ).forEach(this::await);
    }

    @Test
    public void testSingleThreadPoolSync() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Arrays.asList(
                executorService.submit(this::sleep),
                executorService.submit(this::sleep),
                executorService.submit(this::sleep),
                executorService.submit(this::sleep),
                executorService.submit(this::sleep),
                executorService.submit(this::sleep)
        ).forEach(this::await);
    }

    @Test
    public void customThreadPool() {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1),
                (r, executor) -> {
                    throw new RuntimeException("CANNOT EXECUTE TASK");
                }
        );

        Future<String> future1 = threadPoolExecutor.submit(this::sleep);
        Future<String> future2 = threadPoolExecutor.submit(this::sleep);

        try {
            threadPoolExecutor.submit(this::sleep);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        await(future1);
        await(future2);
    }

    private void await(Future<?> future) {
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private String sleep() {
        try {
            System.out.println(Thread.currentThread().getName());
            Thread.sleep(1000);
            return Thread.currentThread().getName();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return Thread.currentThread().getName();
        }
    }
}
