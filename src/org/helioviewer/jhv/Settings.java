package org.helioviewer.jhv;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.helioviewer.jhv.io.DataSources;

@SuppressWarnings("serial")
public class Settings {

    private static final Path userPath = Path.of(JHVDirectory.SETTINGS.getPath(), "user.properties");
    private static final Properties defaults = new Properties() {
        {
            super.setProperty("startup.sampHub", "true");
            super.setProperty("display.normalizeAIA", "true");
            super.setProperty("display.normalize", "false");
            super.setProperty("display.time", "Observer");
            super.setProperty("timeout.read", "120000");
            super.setProperty("timeout.connect", "30000");
            super.setProperty("update.next", "0");
            super.setProperty("video.format", "H264");
        }
    };
    private static final Properties settings = new Properties(defaults);

    public static void load() {
        try (BufferedReader reader = Files.newBufferedReader(userPath)) {
            settings.load(reader);
        } catch (Exception e) {
            Log.warn(e);
        }

        if (getProperty("path.local") == null)
            setProperty("path.local", JHVDirectory.DOWNLOADS.getPath());
        if (getProperty("path.state") == null)
            setProperty("path.state", JHVDirectory.STATES.getPath());
        String server = getProperty("default.server");
        if (server == null || DataSources.getServerSetting(server, "API.getDataSources") == null)
            setProperty("default.server", "IAS");
    }

    public static void setProperty(String key, String val) {
        if (!val.equals(getProperty(key))) {
            settings.setProperty(key, val);
            try (BufferedWriter writer = Files.newBufferedWriter(userPath)) {
                settings.store(writer, null);
            } catch (Exception e) {
                Log.warn(e);
            }
        }
    }

    public static String getProperty(String key) {
        return settings.getProperty(key);
    }

}
