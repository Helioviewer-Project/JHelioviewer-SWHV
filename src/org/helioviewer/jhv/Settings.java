package org.helioviewer.jhv;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    private final Properties defaultProperties = new Properties();
    private final Properties userProperties = new Properties();
    private final File propFile = new File(JHVDirectory.SETTINGS.getPath() + "user.properties");

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
                try (InputStream is = FileUtils.newBufferedInputStream(propFile)) {
                    userProperties.load(is);
                }
            }

            if (getProperty("default.save.path") == null) {
                setProperty("default.save.path", JHVDirectory.EXPORTS.getPath());
            }
            if (getProperty("default.local.path") == null) {
                setProperty("default.local.path", JHVDirectory.HOME.getPath());
            }
        } catch (Exception e) {
            if (verbose) {
                Log.error("Settings.load() > Could not load settings", e);
            } else {
                e.printStackTrace();
            }
        }
    }

    public void save() {
        save(userProperties);
    }

    private void save(Properties props) {
        try (OutputStream os = FileUtils.newBufferedOutputStream(propFile)) {
            props.store(os, null);
        } catch (IOException e) {
            e.printStackTrace();
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
            try (InputStream is = FileUtils.newBufferedInputStream(propFile)) {
                props.load(is);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return props;
    }

    public void setProperty(String key, String val) {
        if (!val.equals(getProperty(key))) {
            userProperties.setProperty(key, val);
        }
    }

    public String getProperty(String key) {
        String val = userProperties.getProperty(key);
        if (val == null) {
            val = defaultProperties.getProperty(key);
        }
        return val;
    }

}
