package ru.protei;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.*;

public class CompletableFutureTest {
    @Test
    public void runAsyncTest() throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
//            try {
//                TimeUnit.SECONDS.sleep(5);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
            System.out.println("Future Thread: " + Thread.currentThread().getName());
        });
        future.get();
    }

    @Test
    public void supplyAsyncTest() throws ExecutionException, InterruptedException {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(5);
                return "Future Thread: " + Thread.currentThread().getName();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println(future.get());
    }

    @Test
    public void thenRunTest() throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        CompletableFuture<Void> future = CompletableFuture
                .runAsync(() -> {
                        try {
                            TimeUnit.SECONDS.sleep(2);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    System.out.println("Future Thread: " + Thread.currentThread().getName());
                })
                .thenRun(() -> {
//                    try {
//                        TimeUnit.SECONDS.sleep(5);
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }
                    System.out.println("Stage 2 Future Thread: " + Thread.currentThread().getName());
                })
                .thenRun(() -> System.out.println("Stage 3 Future Thread: " + Thread.currentThread().getName()))
                .thenRun(() -> System.out.println("Stage 4 Future Thread: " + Thread.currentThread().getName()))
                .thenRunAsync(() -> {
//                        try {
//                            TimeUnit.SECONDS.sleep(5);
//                        } catch (InterruptedException e) {
//                            throw new RuntimeException(e);
//                        }
                    System.out.println("Stage 5 Future Thread: " + Thread.currentThread().getName());
                }, executorService);

        System.out.println("sleeping...");

        Thread.sleep(5000);
        future.get();
    }

    @Test
    public void thenApplyTest() throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> future = CompletableFuture
                .supplyAsync(() -> "Future Thread: " + Thread.currentThread().getName())
                .thenApply(r -> "-345")
                .thenApply(Integer::parseInt);

        System.out.println(future.get());
    }

    @Test
    public void thenAcceptTest() throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = CompletableFuture
                .supplyAsync(() -> "Future Thread: " + Thread.currentThread().getName())
                .thenApply(r -> "-345")
                .thenAccept(System.out::println);

        future.get();
    }

    @Test
    public void thenComposeTest() throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> future = CompletableFuture
                .supplyAsync(() -> (int) Thread.currentThread().getId())
                .thenCompose(this::getRandomNumber);

        future.get();
    }

    @Test
    public void thenCombineTest() throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> future = getRandomNumber(10)
                .thenCombine(getRandomNumber(10), Integer::sum);

        System.out.println("Result = " + future.get());
    }

    @Test
    public void thenAcceptBothTest() throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = getRandomNumber(10)
                .thenAcceptBoth(getRandomNumber(10), (i1, i2) -> System.out.println(i1 + i2));

        future.get();
    }

    @Test
    public void thenAcceptBothTest2() throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = longOperation()
                .thenAcceptBoth(shortOperation(), (s, i) -> System.out.println(s + i))
                .exceptionally(t -> {
                    System.out.println("Exception: " + t.getMessage());
                    return null;
                });

        future.get();
    }

    private CompletableFuture<String> longOperation() {
        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        TimeUnit.SECONDS.sleep(10);
                        return "asdf";
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private CompletableFuture<Integer> shortOperation() {
        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        TimeUnit.SECONDS.sleep(2);
                        throw new RuntimeException();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Test
    public void allOfTest() {
        List<CompletableFuture<Integer>> completableFutures = new ArrayList<>();
        completableFutures.add(getRandomNumber(10, 2));
        completableFutures.add(getRandomNumber(20, 3));
        completableFutures.add(getRandomNumber(30, 4));

        CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[]{}))
                .thenAccept(v -> completableFutures
                        .stream()
                        .map(CompletableFuture::join)
                        .forEach(System.out::println)
                ).join();
    }

    @Test
    public void anyOfTest() throws ExecutionException, InterruptedException {
        List<CompletableFuture<Integer>> completableFutures = new ArrayList<>();
        completableFutures.add(getRandomNumber(10, 2));
        completableFutures.add(getRandomNumber(20, 3));
        completableFutures.add(getRandomNumber(30, 4));

        System.out.println(CompletableFuture.anyOf(completableFutures.toArray(new CompletableFuture[]{})).get());
        System.out.println("---");
        completableFutures.forEach(cf -> {
            try {
                System.out.println(cf.get());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void eitherTest() throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> randomNumber = getRandomNumber(10, 1);
        CompletableFuture<Integer> integerCompletableFuture = getRandomNumber(15, 3)
                .applyToEither(randomNumber, s -> s);

        System.out.println(integerCompletableFuture.get());
    }

    @Test
    public void exceptionallyTest() throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = getRandomNumber(10)
                .thenAccept(System.out::println)
                .thenRun(() -> System.out.println("1"))
                .thenRun(() -> System.out.println("2"))
                .thenRun(() -> {
                    throw new RuntimeException("Something went wrong");
                })
                .thenRun(() -> System.out.println("3"))
                .thenRun(() -> System.out.println("4"))
                .exceptionally(exception -> {
                    System.out.println(exception.getMessage());
                    System.out.println("Exception was handled");
                    return null;
                })
                .thenRun(() -> System.out.println("after exception"))
                .thenRun(() -> {
                    throw new RuntimeException("Something went wrong2");
                })
                .exceptionally(exception -> {
                    System.out.println(exception.getMessage());
                    System.out.println("Exception was handled2");
                    return null;
                });

        future.get();
    }

    @Test
    public void handleTest() throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> future = getRandomNumber(10)
                .thenApply(value -> {
                    if (value < 4) {
                        throw new RuntimeException("Something went wrong, the value is " + value);
                    }

                    return value;
                }).handle((result, exception) -> {
                    System.out.printf("%s, %s", result, exception == null ? null : exception.getMessage());
                    return result;
                });

        future.get();
    }

    @Test
    public void completeTest() throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> future = CompletableFuture
                .supplyAsync(() -> "Future Thread: " + Thread.currentThread().getName())
                .thenComposeAsync(r -> getRandomNumber(10, 2));

//        Thread.sleep(3000);
        future.complete(-1);

        System.out.println(future.get());
    }

    @Test
    public void completeExceptionallyTest() throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> future = CompletableFuture
                .supplyAsync(() -> "Future Thread: " + Thread.currentThread().getName())
                .thenComposeAsync(r -> getRandomNumber(10, 4));

        future.completeExceptionally(new RuntimeException("Completed exceptionally"));
        System.out.println(future.get());
    }

    private CompletableFuture<Integer> getRandomNumber(int bound) {
        return getRandomNumber(bound, 0);
    }

    private CompletableFuture<Integer> getRandomNumber(int bound, int timeToWaitSec) {
        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        TimeUnit.SECONDS.sleep(timeToWaitSec);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    return new Random().nextInt(bound);
                })
                .thenApply(result -> {
                    System.out.println("Bound = " + bound);
                    System.out.println("Random number = " + result);
                    return result;
                });
    }
}
