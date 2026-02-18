package ru.protei.memory.tracking;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("NMT демонстрация с прямыми буферами");
        System.out.println("PID: " + ProcessHandle.current().pid());
        System.out.println("\nЗапустите в другом окне:");
        System.out.println("jcmd " + ProcessHandle.current().pid() + " VM.native_memory summary\n");

        List<ByteBuffer> buffers = new ArrayList<>();

        // Выделяем 100MB через прямой буфер (native memory)
        for (int i = 0; i < 10; i++) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(10 * 1024 * 1024); // 10MB
            buffers.add(buffer);
            System.out.println("Выделен прямой буфер #" + i + " (10MB)");
            Thread.sleep(1000);
        }

        System.out.println("\nВсего выделено: " + (buffers.size() * 10) + "MB native памяти");
        System.out.println("Смотрим статистику NMT...");

        Thread.sleep(10000); // Даем время посмотреть статистику

        // Освобождаем
        buffers.clear();
        System.gc(); // Подсказка для сборки прямых буферов
        System.out.println("Память освобождена");

        Thread.sleep(5000);
    }
}