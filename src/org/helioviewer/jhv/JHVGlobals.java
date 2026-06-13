package org.helioviewer.jhv;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.io.FileUtils;

import com.jidesoft.comparator.AlphanumComparator;

public class JHVGlobals {

    public static final String programName = "ESA JHelioviewer";
    public static final String downloadURL = "https://swhv.oma.be/download/";
    public static final String documentationURL = "https://swhv.oma.be/user_manual/";
    public static final String bugURL = "https://github.com/Helioviewer-Project/JHelioviewer-SWHV/issues";
    public static final String emailAddress = "swhv@oma.be";
    public static String version = "2.-1.-1";
    public static String revision = "-1";
    public static String userAgent = "JHV/SWHV-";
    public static String versionDetail = "";

    public static final AlphanumComparator alphanumComparator = new AlphanumComparator(true);

    public static void getVersion() {
        try (InputStream is = JHVGlobals.class.getResourceAsStream("/version.properties")) {
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

    static void createPersistentDirs() {
        for (JHVDirectory dir : JHVDirectory.values()) {
            if (dir == JHVDirectory.CACHE || dir == JHVDirectory.DOWNLOADS)
                continue;

            File f = dir.getFile();
            if (!f.isDirectory() && !f.mkdirs())
                throw new IllegalStateException("Failed to create directory: " + f);
        }
    }

    static void createCacheDirs() {
        File cacheDir = JHVDirectory.CACHE.getFile();
        try {
            if (!cacheDir.isDirectory() && !cacheDir.mkdirs())
                throw new IllegalStateException("Failed to create directory: " + cacheDir);

            File downloadsDir = JHVDirectory.DOWNLOADS.getFile();
            if (!downloadsDir.isDirectory() && !downloadsDir.mkdirs())
                throw new IllegalStateException("Failed to create directory: " + downloadsDir);

            libCacheDir = FileUtils.tempDir(cacheDir, "lib").getAbsolutePath();
            dataCacheDir = FileUtils.tempDir(cacheDir, "data").getAbsolutePath();
            fileCacheDir = FileUtils.tempDir(cacheDir, "file");
            clientCacheDir = FileUtils.tempDir(cacheDir, "client");
            exportCacheDir = FileUtils.tempDir(cacheDir, "export");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize cache directory: " + cacheDir, e);
        }
    }

    public static String libCacheDir;
    public static String dataCacheDir;
    public static File fileCacheDir;
    public static File clientCacheDir;
    public static File exportCacheDir;

}
