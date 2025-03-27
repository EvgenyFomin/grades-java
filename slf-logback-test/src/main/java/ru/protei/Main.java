package ru.protei;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.debug("hello world");
        logger.info("hello world");
        logger.warn("hello world");
        logger.error("hello world");
    }
}