package org.helioviewer.jhv;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.log.Log;

@SuppressWarnings("serial")
public class Settings {

    private static final File propFile = new File(JHVDirectory.SETTINGS.getPath() + "user.properties");
    private static final Properties defaults = new Properties() {
        {
            setProperty("startup.loadmovie", "true");
            setProperty("display.normalizeAIA", "true");
            setProperty("display.normalize", "false");
            setProperty("timeout.read", "120000");
            setProperty("timeout.connect", "30000");
            setProperty("update.next", "0");
        }
    };
    private static final Properties settings = new Properties(defaults);

    public static void load() {
        try {
            if (propFile.canRead()) {
                try (BufferedReader reader = Files.newBufferedReader(propFile.toPath(), StandardCharsets.UTF_8)) {
                    settings.load(reader);
                }
            }

            if (getProperty("path.local") == null) {
                setProperty("path.local", JHVDirectory.HOME.getPath());
            }
            String server = getProperty("default.server");
            if (server == null || DataSources.getServerSetting(server, "API.getDataSources") == null)
                Settings.setProperty("default.server", "IAS");
        } catch (Exception e) {
            Log.error("Settings.load() > Could not load settings", e);
        }
    }

    public static void setProperty(String key, String val) {
        if (!val.equals(getProperty(key))) {
            settings.setProperty(key, val);
            try (BufferedWriter writer = Files.newBufferedWriter(propFile.toPath(), StandardCharsets.UTF_8)) {
                settings.store(writer, null);
            } catch (Exception e) {
                Log.error("Settings.load() > Could not save settings", e);
            }
        }
    }

    public static String getProperty(String key) {
        return settings.getProperty(key);
    }

}
