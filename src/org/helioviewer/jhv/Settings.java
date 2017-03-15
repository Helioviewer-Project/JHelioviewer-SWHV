package org.helioviewer.jhv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.helioviewer.jhv.base.FileUtils;
import org.helioviewer.jhv.base.logging.Log;

public class Settings {

    private static final Settings instance = new Settings();

    private Settings() {
    }

    public static Settings getSingletonInstance() {
        return instance;
    }

    /** The properties object */
    private final Properties defaultProperties = new Properties();
    private final Properties userProperties = new Properties();

    /** The properties file */
    private final File propFile = new File(JHVDirectory.SETTINGS.getPath() + "user.properties");

    /**
     * Method loads the settings from a user file or the default settings file
     * */
    public void load(boolean verbose) {
        try {
            defaultProperties.clear();
            userProperties.clear();

            try (InputStream is = FileUtils.getResourceInputStream("/settings/defaults.properties")) {
                defaultProperties.load(is);
            }

            if (verbose) {
                Log.debug("Settings.load() > Load default system settings: " + defaultProperties);
            }
            if (propFile.exists()) {
                try (FileInputStream fileInput = new FileInputStream(propFile)) {
                    userProperties.load(fileInput);
                }
            }

            if (getProperty("default.save.path") == null) {
                setProperty("default.save.path", JHVDirectory.EXPORTS.getPath());
            }
            if (getProperty("default.local.path") == null) {
                setProperty("default.local.path", JHVDirectory.HOME.getPath());
            }
        } catch (Exception ex) {
            if (verbose) {
                Log.error("Settings.load() > Could not load settings", ex);
            } else {
                ex.printStackTrace();
            }
        }
    }

    /**
     * This method saves all the values in the user properties file.
     */
    public void save() {
        save(userProperties);
    }

    private void save(Properties props) {
        try {
            propFile.createNewFile();
            try (FileOutputStream fileOutput = new FileOutputStream(propFile)) {
                props.store(fileOutput, null);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void save(String key) {
        Properties props = loadCopyUser();
        props.setProperty(key, getProperty(key));
        save(props);
    }

    private Properties loadCopyUser() {
        Properties props = new Properties();
        if (propFile.exists()) {
            try (FileInputStream fileInput = new FileInputStream(propFile)) {
                props.load(fileInput);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return props;
    }

    /**
     * Method sets the value of a specified property and saves it as a user
     * setting
     *
     * @param key
     *            Default field to be written to
     * @param val
     *            Value to be set to
     */
    public void setProperty(String key, String val) {
        if (!val.equals(getProperty(key))) {
            userProperties.setProperty(key, val);
        }
    }

    /**
     * Method that returns the value of the specified property. User defined
     * properties are always preferred over the default settings.
     *
     * @param key
     *            Default field to read
     */
    public String getProperty(String key) {
        String val = userProperties.getProperty(key);
        if (val == null) {
            val = defaultProperties.getProperty(key);
        }
        return val;
    }

}
