package org.helioviewer.jhv.view.j2k.jpip;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.time.Duration;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.io.FileUtils;

import org.ehcache.Cache;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.expiry.ExpiryPolicy;
import org.ehcache.spi.serialization.Serializer;
import org.ehcache.spi.serialization.SerializerException;

public class JPIPCacheManager {

    static {
        Log.setLoggerLevel("org.ehcache", Level.WARNING); // shut-up Ehcache info logs
    }

    public record Entry(int level, JPIPStream stream) {}

    static final class EntrySerializer implements Serializer<Entry> {
        @Override
        public ByteBuffer serialize(Entry entry) {
            long size = Integer.BYTES + entry.stream().encodedSize();
            if (size > Integer.MAX_VALUE)
                throw new SerializerException("JPIP cache entry is too large");
            ByteBuffer buffer = ByteBuffer.allocate((int) size);
            buffer.putInt(entry.level());
            entry.stream().write(buffer);
            return buffer.flip();
        }

        @Override
        public Entry read(ByteBuffer binary) {
            ByteBuffer buffer = binary.slice();
            try {
                int level = buffer.getInt();
                return new Entry(level, JPIPStream.read(buffer));
            } catch (BufferUnderflowException | IllegalArgumentException e) {
                throw new SerializerException("Invalid JPIP cache entry", e);
            }
        }

        @Override
        public boolean equals(Entry entry, ByteBuffer binary) {
            return serialize(entry).equals(binary);
        }
    }

    private static final Path cacheDir = Path.of(Directories.CACHE.getPath(), "JPIPStream-7");

    private static PersistentCacheManager cacheManager;
    private static Cache<String, Entry> cache;
    private static Thread hook;

    public static void init() {
        deleteDirs("JPIPLevel-4", "JPIPStream-4", "JPIPLevel-5", "JPIPStream-5", "JPIPLevel-6", "JPIPStream-6");

        ExpiryPolicy<Object, Object> expiryPolicy = ExpiryPolicyBuilder.timeToIdleExpiration(Duration.ofDays(7));

        try {
            cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                    .with(CacheManagerBuilder.persistence(cacheDir.toString()))
                    .withCache("JPIPStream", CacheConfigurationBuilder
                            .newCacheConfigurationBuilder(String.class, Entry.class,
                                    ResourcePoolsBuilder.newResourcePoolsBuilder()
                                            .disk(8, MemoryUnit.GB, true))
                            .withExpiry(expiryPolicy)
                            .withValueSerializer(new EntrySerializer()))
                    .build(true);

            cache = cacheManager.getCache("JPIPStream", String.class, Entry.class);
        } catch (RuntimeException e) {
            close();
            throw e;
        }

        if (hook == null) {
            hook = new Thread(JPIPCacheManager::close);
            Runtime.getRuntime().addShutdownHook(hook);
        }
    }

    @Nullable
    public static Entry get(@Nonnull String key, int level) {
        Cache<String, Entry> currentCache = cache;
        if (currentCache == null)
            return null;

        try {
            Entry entry = currentCache.get(key);
            if (entry != null && entry.level() <= level)
                return entry;
        } catch (Exception e) { // might get interrupted
            Log.error(e);
        }
        return null;
    }

    public static void store(@Nonnull String key, int level, @Nonnull JPIPCache source, int frame) {
        Cache<String, Entry> currentCache = cache;
        if (currentCache == null)
            return;

        try {
            Entry entry = currentCache.get(key);
            if (entry == null || entry.level() > level)
                currentCache.put(key, new Entry(level, source.scan(frame)));
        } catch (Exception e) {
            Log.error(e);
        }
    }

    private static void deleteDirs(String... dirs) {
        for (String dir : dirs) { // delete old versions
            try {
                FileUtils.deleteDir(Path.of(Directories.CACHE.getPath(), dir));
            } catch (Exception ignore) {}
        }
    }

    private static void close() {
        PersistentCacheManager oldManager = cacheManager;
        cache = null;
        cacheManager = null;
        close(oldManager);
    }

    private static void close(PersistentCacheManager manager) {
        if (manager == null)
            return;
        try {
            manager.close();
        } catch (Exception e) {
            Log.error(e);
        }
    }

    public static void clear() {
        PersistentCacheManager oldManager = cacheManager;
        if (oldManager == null)
            return;

        close();
        destroy(oldManager);
        init();
    }

    private static void destroy(PersistentCacheManager manager) {
        try {
            manager.destroy();
        } catch (Exception e) {
            Log.error(e);
        }
    }

    public static long getSize() {
        long size = 0;
        try {
            size = FileUtils.diskUsage(cacheDir);
        } catch (Exception e) {
            Log.error(e);
        }
        return size;
    }

}
