package org.helioviewer.jhv.log;

import javax.annotation.Nullable;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

// Wrapper class to conveniently access the root logger. For a description of
// the different methods see {@link org.apache.log4j.Logger}
public class Log {

    static final Logger log = Logger.getRootLogger();

    @Nullable
    public static String getCurrentLogFile() {
        Appender appender = log.getAppender("file");
        if (appender instanceof FileAppender) {
            return ((FileAppender) appender).getFile();
        }
        return null;
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

}
