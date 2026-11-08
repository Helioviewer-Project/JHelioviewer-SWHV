package org.helioviewer.jhv.io;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.helioviewer.jhv.app.Platform;

// An enum containing all the directories mapped in a system independent way. If
// a new directory is required, just add it here, and it will be created at startup.
public enum Directories {
    HOME, CACHE, STATES, EXPORTS, LOGS, SETTINGS, KERNELS, DOWNLOADS;

    private static final String home = System.getProperty("user.home") + File.separator + "JHelioviewer-SWHV" + File.separator;

    // A String representation of the path of the directory
    public String getPath() {
        return switch (this) {
            case HOME -> home;
            case CACHE -> transientRoot() + "Cache" + File.separator;
            case STATES -> home + "States" + File.separator;
            case EXPORTS -> home + "Exports" + File.separator;
            case LOGS -> home + "Logs" + File.separator;
            case SETTINGS -> home + "Settings" + File.separator;
            case KERNELS -> home + "kernels" + File.separator;
            case DOWNLOADS -> transientRoot() + "Downloads" + File.separator;
        };
    }

    // A File representation of the path of the directory
    public File getFile() {
        return new File(getPath());
    }

    public static void createPersistentDirs() {
        for (Directories dir : Directories.values()) {
            if (dir == Directories.CACHE || dir == Directories.DOWNLOADS)
                continue;

            File f = dir.getFile();
            if (!f.isDirectory() && !f.mkdirs())
                throw new IllegalStateException("Failed to create directory: " + f);
        }
    }

    public static void createCacheDirs() {
        File cacheDir = Directories.CACHE.getFile();
        try {
            if (!cacheDir.isDirectory() && !cacheDir.mkdirs())
                throw new IllegalStateException("Failed to create directory: " + cacheDir);

            File downloadsDir = Directories.DOWNLOADS.getFile();
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

    private static String transientRoot() {
        if (!Platform.isWindows())
            return home;

        String tmp = System.getProperty("java.io.tmpdir");
        String root = appendJHV(tmp);
        if (isUsableAsciiDirectory(root))
            return root;

        String systemRoot = System.getenv("SystemRoot");
        if (systemRoot != null) {
            root = appendJHV(systemRoot + File.separator + "Temp");
            if (isUsableAsciiDirectory(root))
                return root;
        }

        String programData = System.getenv("ProgramData");
        root = appendJHV(programData);
        if (isUsableAsciiDirectory(root))
            return root;

        throw new IllegalStateException("No writable ASCII temporary directory found. Set java.io.tmpdir to an ASCII path.");
    }

    private static boolean isUsableAsciiDirectory(String path) {
        if (path == null || !StandardCharsets.US_ASCII.newEncoder().canEncode(path))
            return false;

        File dir = new File(path);
        return (dir.isDirectory() || dir.mkdirs()) && dir.canWrite();
    }

    private static String appendJHV(String path) {
        if (path == null)
            return null;
        return path + File.separator + "JHelioviewer-SWHV" + File.separator;
    }

}
