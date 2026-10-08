package org.helioviewer.jhv.view.j2k.jpip;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.io.FileUtils;

import org.ehcache.PersistentCacheManager;
import org.ehcache.Status;
import org.ehcache.config.builders.CacheManagerBuilder;

public final class JPIPCacheManagerTest {

    public static void main(String[] arguments) throws Exception {
        Path testHome = Files.createTempDirectory("jhv-jpip-cache-test-");
        System.setProperty("user.home", testHome.toString());
        System.setProperty("java.io.tmpdir", testHome.toString());
        Platform.init();
        if (arguments.length == 1 && arguments[0].equals("--windows-path")) {
            Field windows = Platform.class.getDeclaredField("isWindows");
            windows.setAccessible(true);
            windows.setBoolean(null, true); // Exercise Windows path selection without changing the host JVM's OS.
        }
        if (Platform.isWindows())
            check(StandardCharsets.US_ASCII.newEncoder().canEncode(testHome.toString()), "Windows cache test requires an ASCII temporary path");
        check(Directories.CACHE.getFile().toPath().normalize().startsWith(testHome), "cache escaped the isolated test directory");
        Directories.createCacheDirs();

        Path cacheDirectory = Path.of(Directories.CACHE.getPath(), "JPIPStream-8");
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

            lockHolder.close();
            JPIPCacheManager.init();
            byte[] coarse = {1, 2, 3}, fine = {4, 5, 6, 7};
            JPIPCacheManager.store("frame", 2, () -> coarse);
            check(Arrays.equals(JPIPCacheManager.get("frame", 2).block(), coarse) && JPIPCacheManager.get("frame", 3).level() == 2, "stored entry");
            check(JPIPCacheManager.get("frame", 1) == null, "coarse entry served a finer level");
            JPIPCacheManager.store("frame", 3, () -> {
                throw new AssertionError("exported a coarser level than the stored one");
            });
            JPIPCacheManager.store("frame", 0, () -> fine);
            check(Arrays.equals(JPIPCacheManager.get("frame", 2).block(), fine), "finer entry did not replace the coarser one");
            JPIPCacheManager.remove("frame");
            check(JPIPCacheManager.get("frame", 5) == null, "removed entry");
            check(logCounter.records == startupRecords, "cache use produced log records");
            System.out.println("PASS: entries by level, replacement by a finer level, removal");
        } finally {
            rootLogger.removeHandler(logCounter);
            for (Handler handler : existingHandlers)
                rootLogger.addHandler(handler);
            if (lockHolder.getStatus() == Status.AVAILABLE)
                lockHolder.close();
            Method close = JPIPCacheManager.class.getDeclaredMethod("close");
            close.setAccessible(true);
            close.invoke(null);
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
