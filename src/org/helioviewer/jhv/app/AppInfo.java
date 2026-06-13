package org.helioviewer.jhv.app;

import java.io.InputStream;
import java.util.Properties;

public final class AppInfo {

    public static final String programName = "ESA JHelioviewer";
    public static final String downloadURL = "https://swhv.oma.be/download/";
    public static final String documentationURL = "https://swhv.oma.be/user_manual/";
    public static final String bugURL = "https://github.com/Helioviewer-Project/JHelioviewer-SWHV/issues";
    public static final String emailAddress = "swhv@oma.be";
    public static String version = "2.-1.-1";
    public static String revision = "-1";
    public static String userAgent = "JHV/SWHV-";
    public static String versionDetail = "";

    public static void loadVersion() {
        try (InputStream is = AppInfo.class.getResourceAsStream("/version.properties")) {
            Properties p = new Properties();
            p.load(is);
            p.stringPropertyNames().forEach(key -> System.setProperty(key, p.getProperty(key)));
        } catch (Exception e) {
            Log.warn(e);
        }

        String v = System.getProperty("jhv.version");
        String r = System.getProperty("jhv.revision");
        version = v == null ? version : v;
        revision = r == null ? revision : r;

        userAgent += version + '.' + revision + " (" +
                System.getProperty("os.arch") + ' ' + System.getProperty("os.name") + ' ' + System.getProperty("os.version") + ") " +
                System.getProperty("java.vendor") + " JRE " + System.getProperty("java.version");
        versionDetail = String.format("%s %.1fGB %dCPU", userAgent, Runtime.getRuntime().maxMemory() / (1024 * 1024 * 1024.), Runtime.getRuntime().availableProcessors());
        Log.info(versionDetail);
    }

    private AppInfo() {}
}
