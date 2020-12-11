package org.helioviewer.jhv.log;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.helioviewer.jhv.io.FileUtils;

public class LogSettings {

    // defaultLogSettingsPath Path to the default log settings
    // logsDirectory          Path to the directory where the log files are stored
    public static void init(String defaultLogSettingsPath, String logsDirectory) {
        Properties settings = new Properties();
        try (InputStream is = FileUtils.getResource(defaultLogSettingsPath)) {
            settings.load(is);
        } catch (IOException e) {
            Log.log.error("Could not load default log settings: " + e.getMessage());
        }

        String filePattern = "'jhv.'yyyy-MM-dd'T'HH-mm-ss'.log'";

        settings.setProperty("log4j.appender.file.Directory", logsDirectory);
        settings.setProperty("log4j.appender.file.Pattern", filePattern);

        SimpleDateFormat formatter = new SimpleDateFormat(filePattern);
        formatter.setTimeZone(TimeZone.getTimeZone(System.getProperty("user.timezone")));
        settings.setProperty("log4j.appender.file.TimeStamp", formatter.format(new Date()));

        BasicConfigurator.configure();
        PropertyConfigurator.configure(settings);
    }

}
