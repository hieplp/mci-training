package dev.hieplp.mci.core;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

/**
 * Thin logging facade over {@link System.Logger} (JUL backend by default).
 *
 * <p>Centralizes log access for the module so the backend can be swapped
 * (e.g. to SLF4J when embedded in a Spring application) without touching
 * call sites. Obtain an instance via {@link #of(Class)}; instances are
 * cheap wrappers and safe to hold in a {@code static final} field.</p>
 *
 * <p>Messages use {@link java.text.MessageFormat}-style {@code {0}}
 * placeholders, evaluated lazily by the backend.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * private static final AppLogger LOG = AppLogger.of(MyClass.class);
 * LOG.info("Processed {0} items", count);
 * }</pre>
 *
 * @author HiepLP (hiepphuocly@gmail.com)
 */
public final class AppLogger {

    private final Logger delegate;

    private AppLogger(Logger delegate) {
        this.delegate = delegate;
    }

    /**
     * Returns a logger named after the given class.
     *
     * @param type class whose name identifies the logger
     * @return an {@code AppLogger} delegating to that named logger
     */
    public static AppLogger of(Class<?> type) {
        return new AppLogger(System.getLogger(type.getName()));
    }

    /**
     * Reports whether a message at {@code level} would be emitted by the
     * backend, so callers can skip building an expensive message.
     *
     * @param level level to test
     * @return {@code true} if a message at that level would be logged
     */
    public boolean isLoggable(Level level) {
        return delegate.isLoggable(level);
    }

    /** Logs an informational message (normal progress). */
    public void info(String message, Object... params) {
        delegate.log(Level.INFO, message, params);
    }

    /** Logs a warning (recoverable problem, e.g. rejected input). */
    public void warn(String message, Object... params) {
        delegate.log(Level.WARNING, message, params);
    }

    /** Logs an error (operation failure). */
    public void error(String message, Object... params) {
        delegate.log(Level.ERROR, message, params);
    }

    /** Logs a debug/trace message (disabled at default JUL INFO level). */
    public void debug(String message, Object... params) {
        delegate.log(Level.DEBUG, message, params);
    }

}
