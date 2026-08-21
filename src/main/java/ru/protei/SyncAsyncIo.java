package ru.protei;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * §9 — Синхронный и асинхронный I/O на Java
 *
 * Три модели — три syscall'а под капотом:
 *   FileInputStream   → read()        — поток блокируется
 *   FileChannel       → read()        — NIO, буферизованное
 *   AsynchronousFileChannel → io_uring_enter() — поток свободен
 */
public class SyncAsyncIo {

    private static final Path TEST_FILE = Paths.get("/tmp/io-demo-data.bin");

    public static void main(String[] args) throws Exception {

        // Создаём тестовый файл ~1 MB
        byte[] data = new byte[1024 * 1024];
        java.util.Arrays.fill(data, (byte) 'A');
        Files.write(TEST_FILE, data);
        System.out.println("Создан тестовый файл: " + TEST_FILE + " (" + data.length + " байт)\n");

        showProcIo("До всех операций");

        demoBlockingIo();
        demoNioFileChannel();
        demoAsynchronousIo();

        showProcIo("После всех операций");

        Files.deleteIfExists(TEST_FILE);
    }

    /* ------------------------------------------------------------------ */
    /*  1. Блокирующий (синхронный) I/O                                   */
    /*     FileInputStream.read() → syscall read()                         */
    /*     Поток блокируется на read(), пока ядро не вернёт данные.        */
    /* ------------------------------------------------------------------ */
    private static void demoBlockingIo() throws IOException {
        System.out.println("=== 1. Блокирующий I/O: FileInputStream ===");
        System.out.println("  Поток блокируется на read(), пока данные не придут из ядра.");

        long start = System.nanoTime();
        long totalRead = 0;

        try (FileInputStream fis = new FileInputStream(TEST_FILE.toFile())) {
            byte[] buffer = new byte[4096];
            int n;
            while ((n = fis.read(buffer)) != -1) {
                totalRead += n;
            }
        }

        long elapsed = System.nanoTime() - start;
        System.out.printf("  Прочитано %d байт за %.2f ms%n", totalRead, elapsed / 1e6);
    }

    /* ------------------------------------------------------------------ */
    /*  2. NIO: FileChannel + ByteBuffer                                  */
    /*     FileChannel.read(bb) → syscall read(), но с прямым доступом     */
    /*     к ByteBuffer (zero-copy potential, mmap для больших файлов).     */
    /* ------------------------------------------------------------------ */
    private static void demoNioFileChannel() throws IOException {
        System.out.println("\n=== 2. NIO: FileChannel + ByteBuffer ===");
        System.out.println("  FileChannel работает с ByteBuffer, минимизирует копирование данных.");

        long start = System.nanoTime();
        long totalRead = 0;

        try (FileChannel channel = FileChannel.open(TEST_FILE, StandardOpenOption.READ)) {
            ByteBuffer bb = ByteBuffer.allocate(8192);
            int n;
            while ((n = channel.read(bb)) != -1) {
                bb.flip();
                totalRead += n;
                bb.clear();
            }
        }

        long elapsed = System.nanoTime() - start;
        System.out.printf("  Прочитано %d байт за %.2f ms%n", totalRead, elapsed / 1e6);
    }

    /* ------------------------------------------------------------------ */
    /*  3. Асинхронный I/O: AsynchronousFileChannel + CompletionHandler   */
    /*     → io_uring_enter() / AIO. Поток НЕ блокируется.                */
    /*     Ядро завершает операцию и вызывает callback.                    */
    /* ------------------------------------------------------------------ */
    private static void demoAsynchronousIo() throws Exception {
        System.out.println("\n=== 3. Асинхронный I/O: AsynchronousFileChannel ===");
        System.out.println("  Поток НЕ блокируется. Ядро вызывает callback при завершении.");

        CountDownLatch latch = new CountDownLatch(1);
        long[] totalRead = {0};
        long start = System.nanoTime();

        AsynchronousFileChannel channel = AsynchronousFileChannel.open(TEST_FILE);
        int fileSize = (int) channel.size();
        ByteBuffer bb = ByteBuffer.allocate(8192);

        // Рекурсивное асинхронное чтение
        readChunk(channel, bb, 0, fileSize, totalRead, latch, 0);

        latch.await(10, TimeUnit.SECONDS);
        channel.close();

        long elapsed = System.nanoTime() - start;
        System.out.printf("  Прочитано %d байт за %.2f ms (асинхронно)%n", totalRead[0], elapsed / 1e6);
    }

    /**
     * Рекурсивно читает файл кусками по bufferSize через callback.
     */
    private static void readChunk(AsynchronousFileChannel channel, ByteBuffer buffer,
                                  long position, int fileSize, long[] totalRead,
                                  CountDownLatch latch, int depth) {
        if (position >= fileSize) {
            latch.countDown();
            return;
        }

        buffer.clear();
        long pos = position;

        channel.read(buffer, pos, null, new CompletionHandler<Integer, Void>() {
            @Override
            public void completed(Integer bytesRead, Void attachment) {
                if (bytesRead <= 0) {
                    latch.countDown();
                    return;
                }
                totalRead[0] += bytesRead;
                readChunk(channel, buffer, pos + bytesRead, fileSize, totalRead, latch, depth + 1);
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                System.err.println("  Async read error: " + exc.getMessage());
                latch.countDown();
            }
        });
    }

    /* ------------------------------------------------------------------ */
    /*  Утилита: чтение /proc/self/io                                     */
    /*  rchar   — байт прочитано через read()                              */
    /*  wchar   — байт записано через write()                              */
    /*  syscr   — число syscall read()                                     */
    /*  syscw   — число syscall write()                                    */
    /* ------------------------------------------------------------------ */
    private static void showProcIo(String label) {
        System.out.println("\n--- " + label + " (/proc/self/io) ---");
        try {
            String content = Files.readString(Paths.get("/proc/self/io"));
            for (String line : content.split("\n")) {
                line = line.trim();
                if (!line.isEmpty()) {
                    System.out.println("  " + line);
                }
            }
        } catch (IOException e) {
            System.out.println("  Не удалось прочитать /proc/self/io: " + e.getMessage());
        }
    }
}
