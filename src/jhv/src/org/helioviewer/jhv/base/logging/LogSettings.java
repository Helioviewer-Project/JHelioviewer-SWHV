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
    private Properties settings;

    /**
     * @param defaultLogSettingsPath
     *            Path to the default log settings
     * @param logSettingsPath
     *            Path to the custom user log settings
     * @param logsDirectory
     *            Path to the directory where the log files are stored
     */
    public LogSettings(String defaultLogSettingsPath, String logSettingsPath, String logsDirectory) {
        this.logSettingsPath = logSettingsPath;

        // Use default log4j settings as a basis
        BasicConfigurator.configure();

        settings = new Properties();

        try {
            InputStream is = FileUtils.getResourceInputStream(defaultLogSettingsPath);
            try {
                settings.load(is);
            } finally {
                is.close();
            }
        } catch (IOException e) {
            Log.log.error("Could not load default settings: " + e.getMessage());
        }

        try {
            File userFile = new File(logSettingsPath);
            if (userFile.exists()) {
                FileInputStream is = new FileInputStream(userFile);
                try {
                    settings.load(is);
                } finally {
                    is.close();
                }
            }
        } catch (IOException e) {
            Log.log.error("Could not load user settings: " + e.getMessage());
        }

        String filePattern = "'jhv.'yyyy-MM-dd'T'HH-mm-ss'.log'";

        settings.setProperty("log4j.appender.file.Directory", logsDirectory);
        settings.setProperty("log4j.appender.file.Pattern", filePattern);

        SimpleDateFormat formatter = new SimpleDateFormat(filePattern);
        formatter.setTimeZone(TimeZone.getTimeZone(System.getProperty("user.timezone")));
        settings.setProperty("log4j.appender.file.TimeStamp", formatter.format(new Date()));

        PropertyConfigurator.configure(settings);
    }

    public void update() {
        Log.info("Store log settings to " + logSettingsPath);
        try {
            FileOutputStream os = new FileOutputStream(logSettingsPath);
            try {
                settings.store(os, "Logging settings for JHelioviewer.");
            } finally {
                os.close();
            }
        } catch (IOException e) {
            Log.error("Could not write logging settings to file. The current changes will be discarded after program termination.", e);
        }
    }

}
