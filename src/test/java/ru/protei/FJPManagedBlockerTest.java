package ru.protei;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public class FJPManagedBlockerTest {
    @Test
    public void testWithoutManagedBlocker() {
        ForkJoinPool forkJoinPool = new ForkJoinPool(3);
        System.out.println("init pool size = " + forkJoinPool.getPoolSize());

        forkJoinPool.invokeAll(Arrays.asList(
                () -> {
                    System.out.println(forkJoinPool.getPoolSize());
                    return sleep3000();
                },
                () -> {
                    System.out.println(forkJoinPool.getPoolSize());
                    return sleep3000();
                },
                () -> {
                    System.out.println(forkJoinPool.getPoolSize());
                    return sleep3000();
                },
                () -> {
                    System.out.println(forkJoinPool.getPoolSize());
                    return sleep3000();
                },
                () -> {
                    System.out.println(forkJoinPool.getPoolSize());
                    return sleep3000();
                }
        )).forEach(this::await);

        int poolSize = forkJoinPool.getPoolSize();

        long start = System.currentTimeMillis();
        while (poolSize != 0) {
            sleep1000();
            poolSize = forkJoinPool.getPoolSize();
            System.out.println("current pool size = " + poolSize);
        }

        System.out.println(System.currentTimeMillis() - start);
    }

    @Test
    public void testWithManagedBlocker() {
        ForkJoinPool forkJoinPool = new ForkJoinPool(4);

        forkJoinPool.invokeAll(Arrays.asList(
                () -> {
                    System.out.println(forkJoinPool.getPoolSize());
                    return BlockingTasks.callInManagedBlock(this::sleep3000);
                },
                () -> {
                    System.out.println(forkJoinPool.getPoolSize());
                    return BlockingTasks.callInManagedBlock(this::sleep3000);
                },
                () -> {
                    System.out.println(forkJoinPool.getPoolSize());
                    return BlockingTasks.callInManagedBlock(this::sleep3000);
                },
                () -> {
                    System.out.println(forkJoinPool.getPoolSize());
                    return BlockingTasks.callInManagedBlock(this::sleep3000);
                },
                () -> {
                    System.out.println(forkJoinPool.getPoolSize());
                    return BlockingTasks.callInManagedBlock(this::sleep3000);
                }
        )).forEach(this::await);

        int poolSize = forkJoinPool.getPoolSize();

        long start = System.currentTimeMillis();
        while (poolSize != 0) {
            sleep1000();
            poolSize = forkJoinPool.getPoolSize();
            System.out.println("current pool size = " + poolSize);
        }

        System.out.println(System.currentTimeMillis() - start);
    }

    private void await(Future<?> future) {
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private String sleep3000() {
        try {
            System.out.println(Thread.currentThread().getName());
            Thread.sleep(3000);
            return Thread.currentThread().getName();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return Thread.currentThread().getName();
        }
    }

    private String sleep1000() {
        try {
            Thread.sleep(1000);
            return Thread.currentThread().getName();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return Thread.currentThread().getName();
        }
    }

    private static class BlockingTasks {
        private static <T> T callInManagedBlock(final Supplier<T> supplier) {
            final SupplierManagedBlock<T> managedBlock = new SupplierManagedBlock<>(supplier);
            try {
                ForkJoinPool.managedBlock(managedBlock);
            } catch (InterruptedException e) {
                throw new Error(e);
            }
            return managedBlock.getResult();
        }

        private static class SupplierManagedBlock<T> implements ForkJoinPool.ManagedBlocker {
            private final Supplier<T> supplier;
            private T result;
            private boolean done = false;

            private SupplierManagedBlock(final Supplier<T> supplier) {
                this.supplier = supplier;
            }

            @Override
            public boolean block() {
                result = supplier.get();
                done = true;
                return true;
            }

            @Override
            public boolean isReleasable() {
                return done;
            }

            public T getResult() {
                return result;
            }
        }
    }
}
