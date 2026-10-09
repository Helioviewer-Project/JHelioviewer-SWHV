package org.helioviewer.jhv.source.jpip;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
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
import org.ehcache.spi.serialization.SerializerException;

public final class JPIPCacheManagerTest {

    public static void main(String[] arguments) throws Exception {
        checkSerialization();
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
            JPIPCacheManager.clear();
            check(JPIPCacheManager.get("frame", 5) == null, "cleared entry");
            JPIPCacheManager.store("frame", 0, () -> fine);
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

    private static void checkSerialization() {
        JPIPCacheManager.EntrySerializer serializer = new JPIPCacheManager.EntrySerializer();
        byte[] block = new byte[30000];
        for (int i = 0; i < block.length; i++)
            block[i] = (byte) (i * 7);
        JPIPCacheManager.Entry original = new JPIPCacheManager.Entry(2, block);
        ByteBuffer binary = serializer.serialize(original);
        ByteBuffer padded = ByteBuffer.allocateDirect(binary.remaining() + 9);
        padded.position(5).put(binary.duplicate()).flip().position(5);
        ByteBuffer input = padded.asReadOnlyBuffer();
        JPIPCacheManager.Entry restored = serializer.read(input);
        check(input.position() == 5, "read changed buffer position");
        check(restored.level() == 2 && Arrays.equals(restored.block(), block), "entry round trip");
        check(serializer.equals(original, input), "equal encoding");
        check(input.position() == 5, "equals changed buffer position");
        check(!serializer.equals(new JPIPCacheManager.Entry(1, block), input), "different level");
        JPIPCacheManager.Entry empty = new JPIPCacheManager.Entry(0, new byte[0]);
        check(serializer.read(serializer.serialize(empty)).block().length == 0, "empty block");
        // The block is the client's: only a missing level can be told here.
        for (int length = 0; length < Integer.BYTES; length++) {
            try {
                serializer.read(binary.duplicate().limit(length));
                throw new AssertionError("Accepted malformed entry");
            } catch (SerializerException expected) {}
        }
        System.out.println("PASS: JPIP cache entry round trips, buffer positions and truncated entries");
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
