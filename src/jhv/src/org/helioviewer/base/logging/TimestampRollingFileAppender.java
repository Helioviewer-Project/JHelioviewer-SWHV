package org.helioviewer.base.logging;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Allows to use syntax patterns of SimpleDateFormat for log file naming.
 * Overwrites the setFile method of FileAppender.
 * 
 * @author Andre Dau
 * 
 */
public class TimestampRollingFileAppender extends FileAppender {

    /**
     * Denotes the maximum number of days to keep log files before they are
     * deleted.
     */
    private long days;
    private String directory;
    private String pattern;
    private String timeStampString;
    private SimpleDateFormat formatter;

    public TimestampRollingFileAppender() {
        formatter = new SimpleDateFormat();
        formatter.setTimeZone(TimeZone.getTimeZone(System.getProperty("user.timezone")));
    }

    public void setTimeStamp(String timeStampString) {
        this.timeStampString = timeStampString;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public void setDirectory(String directory) {
        if (!directory.endsWith(System.getProperty("file.separator"))) {
            directory += System.getProperty("file.separator");
        }
        this.directory = directory;
    }

    public String getPattern() {
        return pattern;
    }

    public String getDirectory() {
        return directory;
    }

    public String getTimeStamp(String timeStampString) {
        return timeStampString;
    }

    public void setDays(long days) {
        this.days = days;
    }

    public long getDays() {
        return days;
    }

    /**
     * Deletes old log files older than the specified maximum age and calls
     * underlying org.apache.log4j.FileAppender.activateOptions()
     */
    public void activateOptions() {
        Date timeStamp = null;

        try {
            formatter.applyPattern(pattern);
            timeStamp = formatter.parse(timeStampString);
        } catch (ParseException e) {
            throw new RuntimeException("Invalid file name pattern", e);
        }
        setFile(directory + formatter.format(timeStamp));

        boolean deleteEmptyFile = (this.getThreshold() == Level.OFF) && !(new File(getFile()).exists());

        if (days != 0) {
            File fdir = new File(directory);
            String[] files = fdir.list();
            long maxMilliseconds = days * 24 * 60 * 60 * 1000;
            if (files != null) {
                for (int i = 0; i < files.length; ++i) {
                    File f = new File(fdir, files[i]);
                    if (f.isFile()) {
                        try {
                            Date timeStampCurrentFile = formatter.parse(files[i]);
                            if (timeStamp.getTime() - timeStampCurrentFile.getTime() > maxMilliseconds) {
                                if (f.delete()) {
                                    Logger.getRootLogger().info("Log file " + f.getAbsolutePath() + " deleted");
                                } else {
                                    Logger.getRootLogger().error("Could not delete log file " + f.getAbsolutePath());
                                }
                            }
                        } catch (ParseException e) {
                        }
                    }
                }
            }
        }
        super.activateOptions();
        if (deleteEmptyFile) {
            new File(getFile()).delete();
            closeFile();
        }

    }

}
