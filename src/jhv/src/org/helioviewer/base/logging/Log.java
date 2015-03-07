package org.helioviewer.base.logging;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Wrapper class to conveniently access the root logger. For a description of
 * the different methods see {@link org.apache.log4j.Logger}
 * 
 * @author Andre Dau
 * 
 */
public class Log {
    private static final Logger log = LogSettings.getSingletonInstance().getRootLogger();

    public static void log(Level logLevel, Object message, Throwable error) {
        if (logLevel.equals(Level.TRACE)) {
            Log.trace(message, error);
        } else if (logLevel.equals(Level.DEBUG)) {
            Log.debug(message, error);
        } else if (logLevel.equals(Level.INFO)) {
            Log.info(message, error);
        } else if (logLevel.equals(Level.WARN)) {
            Log.warn(message, error);
        } else if (logLevel.equals(Level.ERROR)) {
            Log.error(message, error);
        } else if (logLevel.equals(Level.FATAL)) {
            Log.fatal(message, error);
        } else {
            Log.error(">> Log.log(" + logLevel.toString() + ", " + message + ", " + error.toString() + ") > Invalid log level.", new IllegalArgumentException("Unknown log level"));
        }
    }

    public static void log(Level logLevel, Object message) {
        if (logLevel.equals(Level.TRACE)) {
            Log.trace(message);
        } else if (logLevel.equals(Level.DEBUG)) {
            Log.debug(message);
        } else if (logLevel.equals(Level.INFO)) {
            Log.info(message);
        } else if (logLevel.equals(Level.WARN)) {
            Log.warn(message);
        } else if (logLevel.equals(Level.ERROR)) {
            Log.error(message);
        } else if (logLevel.equals(Level.FATAL)) {
            Log.fatal(message);
        } else {
            Log.error(">> Log.log(" + logLevel.toString() + ", " + message + ") > Invalid log level.", new IllegalArgumentException("Unknown log level"));
        }
    }

    public static void fatal(Object obj, Throwable error) {
        if (log.isEnabledFor(Level.FATAL)) {
            log.fatal(obj, error);
        }
    }

    public static void fatal(Object obj) {
        if (log.isEnabledFor(Level.FATAL)) {
            log.fatal(obj);
        }
    }

    public static void error(Object obj, Throwable error) {
        if (log.isEnabledFor(Level.ERROR)) {
            log.error(obj, error);
        }
    }

    public static void error(Object obj) {
        if (log.isEnabledFor(Level.ERROR)) {
            log.error(obj);
        }
    }

    public static void warn(Object obj, Throwable error) {
        if (log.isEnabledFor(Level.WARN)) {
            log.warn(obj, error);
        }
    }

    public static void warn(Object obj) {
        if (log.isEnabledFor(Level.WARN)) {
            log.warn(obj);
        }
    }

    public static void info(Object obj, Throwable error) {
        if (log.isEnabledFor(Level.INFO)) {
            log.info(obj, error);
        }
    }

    public static void info(Object obj) {
        if (log.isEnabledFor(Level.INFO)) {
            log.info(obj);
        }
    }

    public static void debug(Object obj, Throwable error) {
        if (log.isEnabledFor(Level.DEBUG)) {
            log.debug(obj, error);
        }
    }

    public static void debug(Object obj) {
        if (log.isEnabledFor(Level.DEBUG)) {
            log.debug(obj);
        }
    }

    public static void trace(Object obj, Throwable error) {
        if (log.isEnabledFor(Level.TRACE)) {
            log.trace(obj, error);
        }
    }

    public static void trace(Object obj) {
        if (log.isEnabledFor(Level.TRACE)) {
            log.trace(obj);
        }
    }

}
