package ru.protei.memory.gclogs;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Начало демонстрации GC логов...");

        for (int i = 0; i < 1000; i++) {
            // Создаем мусор
            byte[] garbage = new byte[10_000];
            Thread.sleep(10);

            if (i % 100 == 0) {
                System.out.println("Итерация: " + i);
                System.gc(); // Явный вызов GC для демонстрации
            }
        }
    }
}
