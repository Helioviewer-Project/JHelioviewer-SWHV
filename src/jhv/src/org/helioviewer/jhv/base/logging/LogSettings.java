package org.helioviewer.jhv.base.logging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.helioviewer.jhv.base.FileUtils;

// Class which manages the loading and saving of the log settings
public class LogSettings {

    private final String logSettingsPath;
    private Properties globalSettings;
    private boolean modified;

    /**
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
    public LogSettings(String defaultLogSettingsPath, String logSettingsPath, String logsDirectory, boolean useExistingTimeStamp) {
        this.logSettingsPath = logSettingsPath;

        // Use default log4j settings as a basis
        BasicConfigurator.configure();

        // User settings file
        File settingsFile = new File(logSettingsPath);
        FileInputStream settingsInputStream = null;

        // Default settings file
        InputStream defaultSettingsInputStream = FileUtils.getResourceInputStream(defaultLogSettingsPath);

        // Try to open default settings and to read the default values into a Properties object
        Properties defaultSettings;
        try {
            if (defaultSettingsInputStream == null) {
                defaultSettings = null;
            } else {
                defaultSettings = new Properties();
                defaultSettings.load(defaultSettingsInputStream);
                // Set file path
                defaultSettings.setProperty("log4j.appender.file.Directory", logsDirectory);
            }
        } catch (IOException e) {
            defaultSettings = null;
        }

        // If default settings were read successfully, use them as a basis
        Properties settings;
        if (defaultSettings != null) {
            settings = (Properties) defaultSettings.clone();
        } else {
            Log.log.error("Could not read JHelioviewer default log settings. Use initial custom settings as default settings.");
            settings = new Properties();
        }

        // Try to read user settings
        if (settingsFile.exists()) {
            try {
                settingsInputStream = new FileInputStream(settingsFile);
                settings.load(settingsInputStream);
                // Use initial user settings as default settings for this session in case the default settings file could not be read
                if (defaultSettings == null) {
                    defaultSettings = (Properties) settings.clone();
                }
            } catch (IOException e) {
                // Reset to default settings if possible
                if (defaultSettings != null) {
                    settings = (Properties) defaultSettings.clone();
                    Log.log.error("Could not load custom settings from file. Default settings will be used.");
                    // Neither user nor default settings could be loaded. Use basic log4j settings.
                } else {
                    settings = null;
                }
            }
            // Neither user nor default settings could be loaded. Use basic log4j settings.
        } else {
            modified = true;
            if (defaultSettings == null) {
                settings = null;
            }
        }

        // Configure settings
        if (settings != null) {
            SimpleDateFormat formatter = new SimpleDateFormat(settings.getProperty("log4j.appender.file.Pattern"));
            formatter.setTimeZone(TimeZone.getTimeZone(System.getProperty("user.timezone")));
            if (!useExistingTimeStamp || settings.getProperty("log4j.appender.file.TimeStamp") == null) {
                settings.setProperty("log4j.appender.file.TimeStamp", formatter.format(new Date()));
            }
            PropertyConfigurator.configure(settings);
            globalSettings = settings;
        } else {
            Log.log.error("Could neither load custom settings nor default settings. Use log4j BasicConfigurator default settings. Logging options will be disabled.");
        }

        // Close streams
        try {
            if (settingsInputStream != null) {
                settingsInputStream.close();
            }
        } catch (IOException e) {
            Log.log.error("Could not close FileInputStream for " + logSettingsPath, e);
        }
        try {
            if (defaultSettingsInputStream != null) {
                defaultSettingsInputStream.close();
            }
        } catch (IOException e) {
            Log.log.error("Could not close FileInputStream for " + defaultLogSettingsPath, e);
        }
    }

    // Checks if settings were changed and performs an update if necessary
    public void update() {
        if (modified && (globalSettings != null)) {
            Log.debug("LogSettings.update() > Log settings modified. Update changes.");
            PropertyConfigurator.configure(globalSettings);
            modified = false;
            Log.info("Store log settings to " + logSettingsPath);
            FileOutputStream settingsOutputStream = null;
            try {
                settingsOutputStream = new FileOutputStream(logSettingsPath);
                globalSettings.store(settingsOutputStream, "Logging settings for JHelioviewer.");
            } catch (IOException e) {
                Log.error("Could not write logging settings to file. The current changes will be discarded after program termination.", e);
            } finally {
                if (settingsOutputStream != null) {
                    try {
                        settingsOutputStream.close();
                    } catch (IOException e) {
                        Log.error("Could not close FileOutputStream for " + logSettingsPath, e);
                    }
                }
            }
        }
    }

}
