package ru.protei;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ForkJoinPoolTest {
    private int value = 0;

    @Test
    public void testForkJoinPoolInCompletableFuture() {
        System.out.println("CPU Core: " + Runtime.getRuntime().availableProcessors());
        System.out.println("CommonPool Parallelism: " + ForkJoinPool.commonPool().getParallelism());
        System.out.println("CommonPool Common Parallelism: " + ForkJoinPool.getCommonPoolParallelism());

        long start = System.nanoTime();
        List<CompletableFuture<Void>> futures = IntStream.range(0, 100)
                .mapToObj(i -> CompletableFuture.runAsync(this::sleep))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[]{})).join();
        System.out.println("Processed in " + TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start) + " sec");
    }

    @Test
    public void testForkJoinPoolInStreamReduce() {
        Integer sum = Stream.of(1, 2, 3).parallel().reduce(5, Integer::sum);
        System.out.println(sum);
    }

    @Test
    public void testForkJoinPoolInStreamFindAny() {
        List<Integer> result = Stream.of(10, 9, 8, 7, 6, 5, 4, 3, 2, 1)
                .parallel()
                .collect(Collectors.toList());
        System.out.println(result);

        Integer i = result.stream().parallel().findAny().orElse(null);
        System.out.println(i);
    }

    @Test
    public void testForkJoinPoolInStreamExternalState() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5)
                .parallel()
                .map(i -> i + value++)
                .collect(Collectors.toList());
        System.out.println(result);
    }

    @Test
    public void testForkJoinPoolInStreamForEach() {
        Stream.of(1, 2, 3, 4, 5)
                .parallel()
                .forEach(System.out::println);
    }

    private void sleep() {
        try {
            System.out.println(Thread.currentThread().getName());
            Thread.sleep(1000);
        } catch (InterruptedException ignore) {}
    }
}
