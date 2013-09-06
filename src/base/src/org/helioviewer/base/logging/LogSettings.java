package org.helioviewer.base.logging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.helioviewer.base.FileUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

/**
 * Class which manages the loading and saving of the log settings. This class
 * uses the singleton pattern.
 * 
 * @author Andre Dau
 * 
 */
public class LogSettings {
    private Logger logger = Logger.getRootLogger();

    private static LogSettings instance;

    /**
     * Path to the custom user log settings.
     */
    private final String logSettingsPath;

    /**
     * Log levels sorted from ALL to OFF
     */
    public final Level[] LEVELS = { Level.ALL, Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.FATAL, Level.OFF };

    /**
     * Identifier for the file appender
     */
    public final String FILE_LOGGER = "file";

    /**
     * Identifier for the console appender
     */
    public final String CONSOLE_LOGGER = "console";

    private Properties defaultSettings;

    private Properties settings;

    private boolean modified;

    /**
     * Returns the only instance of this class.
     * 
     * @return the only instance of this class.
     * */
    public static LogSettings getSingletonInstance() {
        if (instance == null) {
            throw new NullPointerException("Logger not initialized");
        }
        return instance;
    }

    /**
     * Initializes the root logger. Must be called at least once before using
     * the logger.
     * 
     * @param defaultLogSettingsPath
     *            Resource path to the default settings file
     * @param logSettingsPath
     *            Path to the custom user log settings file
     * @param logsDirectory
     *            Directory to which the log files are written
     * @param useExistingTimeStamp
     *            If true, use timestamp from setting file instead of current
     *            time
     */
    public static void init(String defaultLogSettingsPath, String logSettingsPath, String logsDirectory, boolean useExistingTimeStamp) {
        instance = new LogSettings(defaultLogSettingsPath, logSettingsPath, logsDirectory, useExistingTimeStamp);

    }

    /**
     * Get the root logger, to which all logging messages should be sent.
     * 
     * @return root logger
     */
    public Logger getRootLogger() {
        return logger;
    }

    /**
     * Private constructor
     * 
     * @param defaultLogSettingsPath
     *            Path to the default log settings
     * @param logSettingsPath
     *            Path to the custom user log settings
     * @param logsDirectory
     *            Path to the directory where the log files are stored
     * @param useExistingTimeStamp
     *            If true, use timestamp from setting file instead of current
     *            time
     */
    private LogSettings(String defaultLogSettingsPath, String logSettingsPath, String logsDirectory, boolean useExistingTimeStamp) {
        this.logSettingsPath = logSettingsPath;

        // Use default log4j settings as a basis
        BasicConfigurator.configure();

        // User settings file
        File settingsFile = new File(logSettingsPath);
        FileInputStream settingsInputStream = null;

        // Default settings file
        InputStream defaultSettingsInputStream = FileUtils.getResourceInputStream(defaultLogSettingsPath);

        // Try to open default settings and to read the default values into a
        // Properties object
        try {
            if (defaultSettingsInputStream == null) {
                defaultSettings = null;
            } else {
                defaultSettings = new Properties();
                defaultSettings.load(defaultSettingsInputStream);
                // Set file path
                defaultSettings.setProperty("log4j.appender." + FILE_LOGGER + ".Directory", logsDirectory);
            }
        } catch (IOException e) {
            defaultSettings = null;
        }

        // If default settings were read successfully, use them as a basis
        if (defaultSettings != null) {
            settings = (Properties) defaultSettings.clone();
        } else {
            logger.error("Could not read JHelioviewer default log settings. Use initial custom settings as default settings.");
            settings = new Properties();
        }

        // Try to read user settings
        if (settingsFile.exists()) {
            try {
                settingsInputStream = new FileInputStream(settingsFile);
                settings.load(settingsInputStream);
                // Use initial user settings as default settings for this
                // session in case the default settings file could not be read
                if (defaultSettings == null) {
                    defaultSettings = (Properties) settings.clone();
                }
            } catch (IOException e) {
                // Reset to default settings if possible
                if (defaultSettings != null) {
                    settings = (Properties) defaultSettings.clone();
                    logger.error("Could not load custom settings from file. Default settings will be used.");
                    // Neither user nor default settings could be loaded. Use
                    // basic log4j settings.
                } else {
                    settings = null;
                }
            }
            // Neither user nor default settings could be loaded. Use basic
            // log4j settings.
        } else {
            modified = true;
            if (defaultSettings == null) {
                settings = null;
            }
        }

        // Configure settings
        if (settings != null) {
            SimpleDateFormat formatter = new SimpleDateFormat(settings.getProperty("log4j.appender." + FILE_LOGGER + ".Pattern"));
            formatter.setTimeZone(TimeZone.getTimeZone(System.getProperty("user.timezone")));
            if (!useExistingTimeStamp || settings.getProperty("log4j.appender." + FILE_LOGGER + ".TimeStamp") == null) {
                settings.setProperty("log4j.appender." + FILE_LOGGER + ".TimeStamp", formatter.format(new Date()));
            }
            PropertyConfigurator.configure(settings);
        } else {
            logger.error("Could neither load custom settings nor default settings. Use log4j BasicConfigurator default settings. Logging options will be disabled.");
        }

        // Close streams
        try {
            if (settingsInputStream != null) {
                settingsInputStream.close();
                settingsInputStream = null;
            }
        } catch (IOException e) {
            logger.error("Could not close FileInputStream for " + logSettingsPath, e);
        }
        try {
            if (defaultSettingsInputStream != null) {
                defaultSettingsInputStream.close();
                defaultSettingsInputStream = null;
            }
        } catch (IOException e) {
            logger.error("Could not close FileInputStream for " + defaultLogSettingsPath, e);
        }
    }

    /**
     * Checks if settings were changed and performs an update if necessary
     */
    public void update() {
        if (modified && (settings != null)) {
            Log.debug(">> LogSettings.update() > Log settings modified. Update changes.");
            PropertyConfigurator.configure(settings);
            modified = false;
            Log.info("Store log settings to " + logSettingsPath);
            FileOutputStream settingsOutputStream = null;
            try {
                settingsOutputStream = new FileOutputStream(logSettingsPath);
                settings.store(settingsOutputStream, "Logging settings for JHelioviewer.");
            } catch (IOException e) {
                Log.error("Could not write logging settings to file. The current changes will be discarded after program termination.", e);
            } finally {
                if (settingsOutputStream != null) {
                    try {
                        settingsOutputStream.close();
                        settingsOutputStream = null;
                    } catch (IOException e) {
                        Log.error("Could not close FileOutputStream for " + logSettingsPath, e);
                    }
                }
            }
        }
    }

    /**
     * Checks whether an appender is enabled
     * 
     * @param logger
     *            identifier of the appender
     * @return true, if the appender is enabled
     */
    public boolean getLoggingEnabled(String logger) {
        Level level = getLoggingLevel(logger);
        if (level == null) {
            return false;
        }
        return !level.equals(Level.OFF);
    }

    /**
     * Returns the current log level of an appender
     * 
     * @param logger
     *            identifier of the appender
     * @return the log level of the appender
     */
    public Level getLoggingLevel(String logger) {
        if (settings == null) {
            return null;
        }
        String level = settings.getProperty("log4j.appender." + logger + ".threshold");
        return Level.toLevel(level);
    }

    /**
     * Sets the log level of an appender
     * 
     * @param logger
     *            the identifier of the appender
     * @param level
     *            the new log level
     */
    public void setLoggingLevel(String logger, Level level) {
        Log.info("Set " + logger + " logging level to " + level);
        settings.setProperty("log4j.appender." + logger + ".threshold", level.toString());

        Level minLevel;
        if (getLoggingLevel(FILE_LOGGER).toInt() < getLoggingLevel(CONSOLE_LOGGER).toInt()) {
            minLevel = getLoggingLevel(FILE_LOGGER);
        } else {
            minLevel = getLoggingLevel(CONSOLE_LOGGER);
        }

        String rootLoggerSetting = settings.getProperty("log4j.rootLogger");
        rootLoggerSetting = minLevel.toString() + rootLoggerSetting.substring(rootLoggerSetting.indexOf(','));
        settings.setProperty("log4j.rootLogger", rootLoggerSetting);
        modified = true;
    }

    /**
     * Checks whether an appender is enabled by default
     * 
     * @param logger
     *            identifier of the appender
     * @return true, if the appender is enabled by default
     */
    public boolean getDefaultLoggingEnabled(String logger) {
        Level level = getDefaultLoggingLevel(logger);
        if (level == null) {
            return false;
        }
        return !level.equals(Level.OFF);
    }

    /**
     * Returns the default log level of an appender
     * 
     * @param logger
     *            identifier of the appender
     * @return the log level of the appender
     */
    public Level getDefaultLoggingLevel(String logger) {
        if (defaultSettings == null) {
            return null;
        }
        String level = defaultSettings.getProperty("log4j.appender." + logger + ".threshold");
        return Level.toLevel(level);
    }

    /**
     * Log files older than the maxium age (in days) are deleted when
     * initializing the file logger
     * 
     * @return Number of days to keep log files
     */
    public int getMaxiumLogFileAge(String logger) {
        if (settings == null) {
            return -1;
        }
        return Integer.parseInt(settings.getProperty("log4j.appender." + logger + ".Days"));
    }

    /**
     * Log files older than the maxium age (in days) are deleted when
     * initializing the file logger
     * 
     * @return Default number of days to keep log files
     */
    public int getDefaultMaxiumLogFileAge(String logger) {
        if (defaultSettings == null) {
            return -1;
        }
        return Integer.parseInt(defaultSettings.getProperty("log4j.appender." + logger + ".Days"));
    }

    /**
     * Log files older than the maxium age (in days) are deleted when
     * initializing the file logger
     * 
     * @param days
     *            Number of days to keep log files before thay are deleted
     */
    public void setMaxiumLogFileAge(String logger, int days) {
        settings.setProperty("log4j.appender." + logger + ".Days", Integer.toString(days));
    }

    /**
     * Returns the name of current log file.
     * 
     * If the name could not be retrieved, returns null.
     * 
     * @return name of the current log file
     */
    public String getCurrentLogFile() {
        Appender appender = logger.getAppender("file");
        if (appender instanceof FileAppender) {
            return ((FileAppender) appender).getFile();
        }
        return null;
    }
}
