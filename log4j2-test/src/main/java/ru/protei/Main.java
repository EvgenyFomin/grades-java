package ru.protei;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.protei.abcdebug.AbcDebug;

import java.io.IOException;
import java.sql.SQLException;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws SQLException {
        logger.info("Simple message");
        new AbcDebug();
        differentLogs();
    }

    private static void differentLogs() {
        logger.debug("differentLogs1");
        logger.info("differentLogs2");
        logger.warn("differentLogs3");
        logger.error("differentLogs4");
        logger.fatal("differentLogs5", new IOException("differentLogs5IOEX"));
        logger.log(Level.forName("VERBOSE", 550), "differentLogs6");
    }
}