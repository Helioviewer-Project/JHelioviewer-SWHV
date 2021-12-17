package org.helioviewer.jhv;

import java.lang.StackWalker;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.time.TimeUtils;

public class Log {

    private static final Level loggedLevel = Level.INFO;
    private static final Logger root = Logger.getLogger("");

    private static final ZoneId zoneId = ZoneId.of(System.getProperty("user.timezone"));
    private static final DateTimeFormatter fileFormatterLocal = TimeUtils.fileFormatter.withZone(zoneId);
    private static final DateTimeFormatter milliFormatterLocal = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(zoneId);

    private static final String filename = JHVDirectory.LOGS.getPath() + "JHV_" + TimeUtils.format(fileFormatterLocal, System.currentTimeMillis()) + ".log";

    static void init() throws Exception {
        FileUtils.deleteDir(JHVDirectory.LOGS.getFile(), 7 * TimeUtils.DAY_IN_MILLIS, false);

        FileHandler fileHandler = new FileHandler(filename, 1024 * 1024, 1);
        fileHandler.setLevel(loggedLevel);
        fileHandler.setFormatter(new LogFormatter());
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(loggedLevel);
        consoleHandler.setFormatter(new LogFormatter());

        LogManager.getLogManager().reset();
        root.addHandler(fileHandler);
        root.addHandler(consoleHandler);
    }

    private static class LogFormatter extends Formatter {
        @Override
        public String format(LogRecord record) { // traditional JHV style
            Throwable thrown = record.getThrown();
            String strThrown = thrown == null ? "" : ": " + thrown.getMessage();
            return TimeUtils.format(milliFormatterLocal, record.getMillis()) +
                    " [" + Thread.currentThread().getName() + "] " +
                    record.getLevel() +
                    ' ' + record.getLoggerName() + ' ' +
                    record.getMessage() + strThrown + '\n';
        }
    }

    private static String getCaller(String msg) {
        Optional<StackWalker.StackFrame> frame = StackWalker.getInstance().walk(s -> s.skip(2).findFirst());
        String caller = frame.map(stackFrame -> stackFrame.getClassName() + '.' + stackFrame.getMethodName()).orElse("|unknown|");
        return msg == null ? caller : caller + " - " + msg;
    }

    static String getFilename() {
        return filename;
    }

    public static void info(String msg) {
        root.log(Level.INFO, getCaller(msg));
    }

    public static void warn(String msg) {
        root.log(Level.WARNING, getCaller(msg));
    }

    public static void warn(Throwable thrown) {
        root.log(Level.WARNING, getCaller(null), thrown);
    }

    public static void warn(String msg, Throwable thrown) {
        root.log(Level.WARNING, getCaller(msg), thrown);
    }

    public static void error(String msg) {
        root.log(Level.SEVERE, getCaller(msg));
    }

    public static void error(Throwable thrown) {
        root.log(Level.SEVERE, getCaller(null), thrown);
    }

    public static void error(String msg, Throwable thrown) {
        root.log(Level.SEVERE, getCaller(msg), thrown);
    }

}
