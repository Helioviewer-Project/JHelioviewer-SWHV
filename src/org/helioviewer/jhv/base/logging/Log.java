package org.helioviewer.jhv.base.logging;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Wrapper class to conveniently access the root logger. For a description of
 * the different methods see {@link org.apache.log4j.Logger}
 */
public class Log {

    static final Logger log = Logger.getRootLogger();

    /**
     * Returns the name of current log file.
     * If the name could not be retrieved, returns null.
     *
     * @return name of the current log file
     */
    public static String getCurrentLogFile() {
        Appender appender = log.getAppender("file");
        if (appender instanceof FileAppender) {
            return ((FileAppender) appender).getFile();
        }
        return null;
    }

    public static void log(Level logLevel, Object message, Throwable error) {
        if (logLevel.equals(Level.TRACE)) {
            trace(message, error);
        } else if (logLevel.equals(Level.DEBUG)) {
            debug(message, error);
        } else if (logLevel.equals(Level.INFO)) {
            info(message, error);
        } else if (logLevel.equals(Level.WARN)) {
            warn(message, error);
        } else if (logLevel.equals(Level.ERROR)) {
            error(message, error);
        } else if (logLevel.equals(Level.FATAL)) {
            fatal(message, error);
        } else {
            error("Log.log(" + logLevel + ", " + message + ", " + error + ") > Invalid log level.", new IllegalArgumentException("Unknown log level"));
        }
    }

    public static void log(Level logLevel, Object message) {
        if (logLevel.equals(Level.TRACE)) {
            trace(message);
        } else if (logLevel.equals(Level.DEBUG)) {
            debug(message);
        } else if (logLevel.equals(Level.INFO)) {
            info(message);
        } else if (logLevel.equals(Level.WARN)) {
            warn(message);
        } else if (logLevel.equals(Level.ERROR)) {
            error(message);
        } else if (logLevel.equals(Level.FATAL)) {
            fatal(message);
        } else {
            error("Log.log(" + logLevel + ", " + message + ") > Invalid log level.", new IllegalArgumentException("Unknown log level"));
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
