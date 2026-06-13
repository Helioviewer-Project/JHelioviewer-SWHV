package org.helioviewer.jhv;

import java.io.File;

import org.helioviewer.jhv.io.FileUtils;

import com.jidesoft.comparator.AlphanumComparator;

public class JHVGlobals {

    public static final AlphanumComparator alphanumComparator = new AlphanumComparator(true);

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
