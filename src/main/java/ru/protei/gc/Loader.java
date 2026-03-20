package ru.protei.gc;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class Loader {
    static final int WORKERS = 8;
    static final int OBJECT_SIZE = 1024;
    static final int ALLOC_RATE = 1000;

    // Счетчики
    static final AtomicLong totalRequestsPerSec = new AtomicLong();
    static final AtomicLong totalRequests = new AtomicLong();
    static final AtomicLong totalLatencyPerSecInNanos = new AtomicLong();
    static final AtomicLong totalLatency = new AtomicLong();
    static final AtomicLong totalGCTime = new AtomicLong();
    static final List<byte[]> cache = Collections.synchronizedList(new ArrayList<>());
    static final Random random = new Random();

    // Для мониторинга GC
    static final List<java.lang.management.GarbageCollectorMXBean> gcBeans =
            java.lang.management.ManagementFactory.getGarbageCollectorMXBeans();

    public static void main(String[] args) throws Exception {
        System.out.println("=== GC Comparison Demo ===");
        System.out.println("Java version: " + System.getProperty("java.version"));
        System.out.println("GC: " + getGCName());
        System.out.println("Heap: " + Runtime.getRuntime().maxMemory() / 1024 / 1024 + "MB");
        System.out.println("Workers: " + WORKERS);
        System.out.println("\nЗапуск теста на 30 секунд...\n");

        // Запоминаем начальные значения GC
        long startGCTime = gcBeans.stream().mapToLong(java.lang.management.GarbageCollectorMXBean::getCollectionTime).sum();
        long startGCCount = gcBeans.stream().mapToLong(java.lang.management.GarbageCollectorMXBean::getCollectionCount).sum();

        // Запускаем мониторинг
        ScheduledExecutorService monitor = Executors.newScheduledThreadPool(1);
        monitor.scheduleAtFixedRate(() -> printStats(), 1, 1, TimeUnit.SECONDS);

        // Запускаем воркеров
        ExecutorService workers = Executors.newFixedThreadPool(WORKERS);
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < WORKERS; i++) {
            final int workerId = i;
            workers.submit(() -> runWorker(workerId));
        }

        // Ждем окончания теста
        Thread.sleep(1000 * 60 * 5);
        long endTime = System.currentTimeMillis();
        long totalTimeMs = endTime - startTime;

        // Останавливаем все
        workers.shutdownNow();
        monitor.shutdown();
        workers.awaitTermination(5, TimeUnit.SECONDS);

        // Финальные значения
        long finalRequests = totalRequestsPerSec.get();
        long finalGCTime = gcBeans.stream().mapToLong(java.lang.management.GarbageCollectorMXBean::getCollectionTime).sum() - startGCTime;
        long finalGCCount = gcBeans.stream().mapToLong(java.lang.management.GarbageCollectorMXBean::getCollectionCount).sum() - startGCCount;

        // Финальная статистика
        printFinalStats(totalTimeMs, totalRequests.get(), finalGCTime, finalGCCount);
    }

    static void runWorker(int workerId) {
        Thread.currentThread().setName("Worker-" + workerId);
        List<byte[]> localCache = new ArrayList<>();

        while (!Thread.currentThread().isInterrupted()) {
            long start = System.nanoTime();

            try {
                // Три типа нагрузки
                int type = random.nextInt(100);

                if (type < 70) {
                    // 70% - короткоживущие
                    byte[] data = new byte[OBJECT_SIZE];
                    if (random.nextInt(100) < 5) {
                        localCache.add(data);
                    }

                } else if (type < 90) {
                    // 20% - средняя жизнь
                    byte[] data = new byte[OBJECT_SIZE * 10];
                    if (random.nextInt(100) < 30) {
                        cache.add(data);
                    } else {
                        localCache.add(data);
                    }

                } else {
                    // 10% - долгоживущие
                    byte[] data = new byte[OBJECT_SIZE * 100];
                    cache.add(data);

                    if (cache.size() > 1000) {
                        cache.subList(0, 100).clear();
                    }
                }

                // Имитация CPU работы
                for (int i = 0; i < 100; i++) {
                    Math.sin(i + workerId);
                }

                long latency = System.nanoTime() - start;
                totalLatencyPerSecInNanos.addAndGet(latency);
                totalRequestsPerSec.incrementAndGet();
                totalRequests.incrementAndGet();

                // Контроль скорости
                Thread.sleep(1);

                // Чистка локального кэша
                if (localCache.size() > 100) {
                    localCache.clear();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    static void printStats() {
        // Получаем значения за последнюю секунду
        long lastSecRequests = totalRequestsPerSec.getAndSet(0);
        long lastSecLatency = totalLatencyPerSecInNanos.getAndSet(0);

        if (lastSecRequests > 0) {
            double avgLatencyMs = (double) lastSecLatency / lastSecRequests / 1_000_000;
            Runtime rt = Runtime.getRuntime();
            long usedMem = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
            long totalMem = rt.totalMemory() / 1024 / 1024;

            System.out.printf("RPS: %4d | Avg Lat: %6.2fms | Heap: %3dMB/%3dMB | Cache: %4d\n",
                    lastSecRequests,
                    avgLatencyMs,
                    usedMem,
                    totalMem,
                    cache.size());
        } else {
            System.out.printf("RPS: %4d | (no requests) | Heap: ??? | Cache: %4d\n",
                    0, cache.size());
        }
    }

    static void printFinalStats(long durationMs, long totalRequests, long totalGCTime, long totalGCCount) {
        System.out.println("\n=== Финальная статистика ===");
        System.out.println("Длительность теста: " + durationMs + "ms");
        System.out.println("Всего обработано запросов: " + totalRequests);
        System.out.println("Средний RPS: " + totalRequests / (durationMs / 1000));
        System.out.println("Всего GC пауз: " + totalGCCount);
        System.out.println("Общее время в GC: " + totalGCTime + "ms");
        System.out.println("% времени в GC: " + String.format("%.2f%%", (double) totalGCTime / durationMs * 100));
        System.out.println("Финальный размер кэша: " + cache.size() + " объектов");
    }

    static String getGCName() {
        List<String> gcNames = new ArrayList<>();
        for (java.lang.management.GarbageCollectorMXBean bean : gcBeans) {
            gcNames.add(bean.getName());
        }
        return String.join(", ", gcNames);
    }
}