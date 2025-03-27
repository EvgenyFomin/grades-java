package ru.protei;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainAsync {
    private static final Logger asyncLogger = LogManager.getLogger("ASYNC_JSON_FILE_APPENDER");
    private static int idx = 1;

    public static void main(String[] args) {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

        scheduledExecutorService.scheduleAtFixedRate(() -> {
            asyncLogger.info("{}_{}", Thread.currentThread().getName(), idx++);
        }, 0, 200, TimeUnit.MILLISECONDS);
    }
}