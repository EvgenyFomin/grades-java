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
                executorService.submit(this::sleep1000),
                executorService.submit(this::sleep1000),
                executorService.submit(this::sleep1000),
                executorService.submit(this::sleep1000),
                executorService.submit(this::sleep1000),
                executorService.submit(this::sleep1000)
        ).forEach(this::await);
    }

    @Test
    public void testSingleThreadPoolSync() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Arrays.asList(
                executorService.submit(this::sleep1000),
                executorService.submit(this::sleep1000),
                executorService.submit(this::sleep1000),
                executorService.submit(this::sleep1000),
                executorService.submit(this::sleep1000),
                executorService.submit(this::sleep1000)
        ).forEach(this::await);
    }

    @Test
    public void testScheduledThreadPoolSingle() {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        executorService.schedule(() -> System.out.println("hello once"), 1, TimeUnit.SECONDS);

        sleep3000();
    }

    @Test
    public void testScheduledThreadPoolRepeatWaitLessThanPeriod() {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(10);
        ScheduledExecutorService executorService2 = Executors.newScheduledThreadPool(10);

        long start = System.currentTimeMillis();
        executorService.scheduleAtFixedRate(() -> {
            System.out.println("hello repeat: " + (System.currentTimeMillis() - start));
            sleep1000();
        }, 0, 2, TimeUnit.SECONDS);
        executorService2.scheduleWithFixedDelay(() -> {
            System.out.println("hello repeat, fixed start delay: " + (System.currentTimeMillis() - start));
            sleep1000();
        }, 0, 2, TimeUnit.SECONDS);

        sleep20000();
    }

    @Test
    public void testScheduledThreadPoolRepeatWaitMoreThanPeriod() {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(10);
        ScheduledExecutorService executorService2 = Executors.newScheduledThreadPool(10);

        long start = System.currentTimeMillis();
        executorService.scheduleAtFixedRate(() -> {
            System.out.println("hello repeat: " + (System.currentTimeMillis() - start));
            sleep5000();
        }, 0, 2, TimeUnit.SECONDS);
        executorService2.scheduleWithFixedDelay(() -> {
            System.out.println("hello repeat, fixed start delay: " + (System.currentTimeMillis() - start));
            sleep5000();
        }, 0, 2, TimeUnit.SECONDS);

        sleep20000();
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

        Future<String> future1 = threadPoolExecutor.submit(this::sleep1000);
        Future<String> future2 = threadPoolExecutor.submit(this::sleep1000);

        try {
            threadPoolExecutor.submit(this::sleep1000);
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

    private String sleep1000() {
        try {
            System.out.println(Thread.currentThread().getName());
            Thread.sleep(1000);
            return Thread.currentThread().getName();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return Thread.currentThread().getName();
        }
    }

    private String sleep3000() {
        try {
            System.out.println(Thread.currentThread().getName());
            Thread.sleep(1000);
            return Thread.currentThread().getName();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return Thread.currentThread().getName();
        }
    }

    private String sleep5000() {
        try {
            System.out.println(Thread.currentThread().getName());
            Thread.sleep(5000);
            return Thread.currentThread().getName();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return Thread.currentThread().getName();
        }
    }

    private String sleep10000() {
        try {
            System.out.println(Thread.currentThread().getName());
            Thread.sleep(10000);
            return Thread.currentThread().getName();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return Thread.currentThread().getName();
        }
    }

    private String sleep20000() {
        try {
            System.out.println(Thread.currentThread().getName());
            Thread.sleep(20000);
            return Thread.currentThread().getName();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return Thread.currentThread().getName();
        }
    }
}
