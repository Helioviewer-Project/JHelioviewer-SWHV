package org.helioviewer.jhv.view.j2k.jpip;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.io.FileUtils;

import org.ehcache.PersistentCacheManager;
import org.ehcache.config.builders.CacheManagerBuilder;

public final class JPIPCacheManagerTest {

    public static void main(String[] arguments) throws Exception {
        Path testHome = Files.createTempDirectory("jhv-jpip-cache-test-");
        System.setProperty("user.home", testHome.toString());
        Platform.init();
        Directories.createCacheDirs();

        Path cacheDirectory = Path.of(Directories.CACHE.getPath(), "JPIPStream-7");
        PersistentCacheManager lockHolder = CacheManagerBuilder.newCacheManagerBuilder()
                .with(CacheManagerBuilder.persistence(cacheDirectory.toString()))
                .build(true);
        CountingHandler logCounter = new CountingHandler();
        Logger rootLogger = Logger.getLogger("");
        Handler[] existingHandlers = rootLogger.getHandlers();
        for (Handler handler : existingHandlers)
            rootLogger.removeHandler(handler);
        rootLogger.addHandler(logCounter);
        try {
            try {
                JPIPCacheManager.init();
                throw new AssertionError("cache initialization succeeded despite the persistence lock");
            } catch (RuntimeException e) {
                Log.error("JPIP cache initialization error", e);
            }

            int startupRecords = logCounter.records;
            for (int i = 0; i < 500; i++)
                check(JPIPCacheManager.get("test", 1) == null, "disabled cache returned data");
            JPIPCacheManager.clear();
            check(logCounter.records == startupRecords, "disabled cache produced additional log records");
            System.out.println("PASS: failed cache initialization stays disabled without repeated logging");
        } finally {
            rootLogger.removeHandler(logCounter);
            for (Handler handler : existingHandlers)
                rootLogger.addHandler(handler);
            lockHolder.close();
            FileUtils.deleteDir(testHome);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }

    private static final class CountingHandler extends Handler {
        private int records;

        @Override
        public void publish(LogRecord record) {
            records++;
        }

        @Override
        public void flush() {}

        @Override
        public void close() {}
    }

    private JPIPCacheManagerTest() {}
}
