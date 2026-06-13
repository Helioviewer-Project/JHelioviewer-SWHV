package org.helioviewer.jhv.app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.time.TimeUtils;

public class Log {

    private static final Level loggedLevel = Level.INFO;
    private static final Logger root = Logger.getLogger("");
    private static final HashMap<String, Logger> configuredLoggers = new HashMap<>();

    private static final String filename = JHVDirectory.LOGS.getPath() + "JHV_" + TimeUtils.formatFilename(System.currentTimeMillis()) + ".log";

    public static void init() throws Exception {
        FileUtils.deleteFromDir(Path.of(JHVDirectory.LOGS.getPath()), 7 * TimeUtils.DAY_IN_MILLIS, false);

        LogFormatter logFormatter = new LogFormatter();
        FileHandler fileHandler = new FileHandler(filename, 1024 * 1024, 1);
        fileHandler.setLevel(loggedLevel);
        fileHandler.setFormatter(logFormatter);
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(loggedLevel);
        consoleHandler.setFormatter(logFormatter);

        LogManager.getLogManager().reset();
        root.addHandler(fileHandler);
        root.addHandler(consoleHandler);
    }

    public static void setLoggerLevel(String name, Level level) {
        Logger logger = configuredLoggers.computeIfAbsent(name, Logger::getLogger);
        logger.setLevel(level);
    }

    private static class LogFormatter extends Formatter {
        @Override
        public String format(LogRecord record) { // traditional JHV style
            Throwable thrown = record.getThrown();
            String strThrown = thrown == null ? "" : ": " + thrown.getMessage();
            return TimeUtils.formatLog(record.getMillis()) +
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

    public static String get() {
        try {
            Path path = Path.of(filename);
            if (!Files.isRegularFile(path))
                return "Log file not initialized";
            return Files.readString(path);
        } catch (Exception e) {
            String message = e.getMessage();
            return message == null || message.isBlank() ? "Unable to retrieve log" : "Unable to retrieve log: " + message;
        }
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
