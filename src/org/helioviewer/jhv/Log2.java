package org.helioviewer.jhv;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.helioviewer.jhv.time.TimeUtils;

class Log2 {

    private static final Level level = Level.INFO;

    static void init() {
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(level);
        for (Handler h : rootLogger.getHandlers()) {
            h.setLevel(level);
            h.setFormatter(new LogFormatter());
        }
    }

    private static class LogFormatter extends Formatter {
        @Override
        public String format(LogRecord record) { // traditional JHV style
            Throwable thrown = record.getThrown();
            String strThrown = thrown == null ? "" : " " + thrown.getMessage();
            return TimeUtils.format(record.getMillis()) +
                    " [" + Thread.currentThread().getName() + "] " +
                    record.getLevel() +
                    ' ' + record.getLoggerName() +
                    " - " + record.getMessage() + strThrown + '\n';
        }
    }

}
