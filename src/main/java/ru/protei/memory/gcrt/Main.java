package ru.protei.memory.gcrt;

public class Main {
    static volatile int counter = 0;

    public static void main(String[] args) throws InterruptedException {
        // Поток, который постоянно работает
        Thread worker = new Thread(() -> {
            while (true) {
                counter++; // Бесконечный цикл без safepoint
            }
        });
        worker.setDaemon(true);
        worker.start();

        // Периодически вызываем GC
        for (int i = 0; i < 5; i++) {
            Thread.sleep(2000);
            System.out.println("Вызов System.gc()...");
            System.gc();
        }
    }
}
