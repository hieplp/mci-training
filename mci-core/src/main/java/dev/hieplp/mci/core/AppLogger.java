package dev.hieplp.mci.core;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

/**
 * Thin logging facade over {@link System.Logger} (JUL backend).
 * Obtain via {@link #of(Class)}.
 */
public final class AppLogger {

    private final Logger delegate;

    private AppLogger(Logger delegate) {
        this.delegate = delegate;
    }

    /** Returns a logger named after the given class. */
    public static AppLogger of(Class<?> type) {
        return new AppLogger(System.getLogger(type.getName()));
    }

    public void info(String message, Object... params) {
        delegate.log(Level.INFO, message, params);
    }

    public void warn(String message, Object... params) {
        delegate.log(Level.WARNING, message, params);
    }

    public void error(String message, Object... params) {
        delegate.log(Level.ERROR, message, params);
    }

    public void debug(String message, Object... params) {
        delegate.log(Level.DEBUG, message, params);
    }
}
