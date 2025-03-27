package ru.protei.abcdebug;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AbcDebug {
    private static final Logger logger = LogManager.getLogger(AbcDebug.class);

    public AbcDebug() {
        logger.debug("DEBUG");
    }
}