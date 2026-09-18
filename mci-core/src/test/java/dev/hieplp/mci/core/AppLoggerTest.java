package dev.hieplp.mci.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

class AppLoggerTest {

    /**
     * Subject whose logger name is unique to this test, so records cannot
     * leak in from other tests (or out to them).
     */
    private static final class Subject {
    }

    private final AppLogger log = AppLogger.of(Subject.class);

    @Test
    void infoFormatsParameters() {
        List<LogRecord> records = capture(Level.ALL, () -> log.info("Processed {0} items in {1} ms", 7, 12));

        assertEquals(1, records.size());
        assertEquals(Level.INFO, records.get(0).getLevel());
        assertEquals("Processed 7 items in 12 ms", message(records.get(0)));
    }

    @Test
    void warnAndErrorUseTheirOwnLevels() {
        List<LogRecord> records = capture(Level.ALL, () -> {
            log.warn("Invalid operand {0}", "stn1");
            log.error("Addition failed for {0}", "stn2");
        });

        assertEquals(2, records.size());
        assertEquals(Level.WARNING, records.get(0).getLevel());
        assertEquals(Level.SEVERE, records.get(1).getLevel());
        assertEquals("Invalid operand stn1", message(records.get(0)));
        assertEquals("Addition failed for stn2", message(records.get(1)));
    }

    @Test
    void debugIsSuppressedAtInfoLevel() {
        List<LogRecord> records = capture(Level.INFO, () -> log.debug("carry {0}", 1));

        assertTrue(records.isEmpty());
    }

    @Test
    void debugIsEmittedWhenTheLevelAllowsIt() {
        List<LogRecord> records = capture(Level.ALL, () -> log.debug("carry {0}", 1));

        assertEquals(1, records.size());
        assertEquals("carry 1", message(records.get(0)));
    }

    /**
     * Runs {@code action} with a record collector attached to the JUL logger
     * that {@link AppLogger#of(Class)} must be routing to, then restores the
     * logger's previous configuration.
     */
    private static List<LogRecord> capture(Level level, Runnable action) {
        Logger jul = Logger.getLogger(Subject.class.getName());
        List<LogRecord> records = new ArrayList<>();
        Handler collector = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };

        Level previousLevel = jul.getLevel();
        boolean parentHandlers = jul.getUseParentHandlers();
        jul.setLevel(level);
        jul.setUseParentHandlers(false);
        jul.addHandler(collector);
        try {
            action.run();
        } finally {
            jul.removeHandler(collector);
            jul.setLevel(previousLevel);
            jul.setUseParentHandlers(parentHandlers);
        }
        return records;
    }

    private static String message(LogRecord record) {
        Object[] params = record.getParameters();
        return params == null ? record.getMessage() : MessageFormat.format(record.getMessage(), params);
    }
}
