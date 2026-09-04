package org.helioviewer.jhv.view.j2k.jpip;

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
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.expiry.ExpiryPolicy;

public class JPIPCacheManager {

    static {
        Log.setLoggerLevel("org.ehcache", Level.WARNING); // shut-up Ehcache info logs
    }

    private static final Path levelCacheDir = Path.of(Directories.CACHE.getPath(), "JPIPLevel-4");
    private static final Path streamCacheDir = Path.of(Directories.CACHE.getPath(), "JPIPStream-4");

    private static PersistentCacheManager levelManager;
    private static PersistentCacheManager streamManager;
    private static Cache<String, Integer> levelCache;
    private static Cache<String, JPIPStream> streamCache;
    private static Thread hook;

    public static void init() {
        deleteDirs("JPIPLevel-3", "JPIPStream-3", "JPIPLevel-5", "JPIPStream-5", "JPIPLevel-6", "JPIPStream-6");

        ExpiryPolicy<Object, Object> expiryPolicy = ExpiryPolicyBuilder.timeToIdleExpiration(Duration.ofDays(7));

        try {
            levelManager = CacheManagerBuilder.newCacheManagerBuilder()
                    .with(CacheManagerBuilder.persistence(levelCacheDir.toString()))
                    .withCache("JPIPLevel", CacheConfigurationBuilder
                            .newCacheConfigurationBuilder(String.class, Integer.class,
                                    ResourcePoolsBuilder.newResourcePoolsBuilder()
                                            .heap(10000, EntryUnit.ENTRIES)
                                            .disk(10, MemoryUnit.MB, true))
                            .withExpiry(expiryPolicy))
                    .build(true);
            streamManager = CacheManagerBuilder.newCacheManagerBuilder()
                    .with(CacheManagerBuilder.persistence(streamCacheDir.toString()))
                    .withCache("JPIPStream", CacheConfigurationBuilder
                            .newCacheConfigurationBuilder(String.class, JPIPStream.class,
                                    ResourcePoolsBuilder.newResourcePoolsBuilder()
                                            .disk(8, MemoryUnit.GB, true))
                            .withExpiry(expiryPolicy))
                    .build(true);

            streamCache = streamManager.getCache("JPIPStream", String.class, JPIPStream.class);
            levelCache = levelManager.getCache("JPIPLevel", String.class, Integer.class);
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
    public static JPIPStream get(@Nonnull String key, int level) {
        Cache<String, Integer> levels = levelCache;
        Cache<String, JPIPStream> streams = streamCache;
        if (levels == null || streams == null)
            return null;

        try {
            Integer clevel = levels.get(key);
            if (clevel != null && clevel <= level)
                return streams.get(key);
        } catch (Exception e) { // might get interrupted
            Log.error(e);
        }
        return null;
    }

    public static void store(@Nonnull String key, int level, @Nonnull JPIPCache source, int frame) {
        Cache<String, Integer> levels = levelCache;
        Cache<String, JPIPStream> streams = streamCache;
        if (levels == null || streams == null)
            return;

        try {
            Integer clevel = levels.get(key);
            if (clevel == null || clevel > level || !streams.containsKey(key)) {
                JPIPStream stream = source.get(frame);
                if (stream != null) {
                    levels.put(key, level);
                    streams.put(key, stream);
                }
            }
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
        PersistentCacheManager oldLevelManager = levelManager;
        PersistentCacheManager oldStreamManager = streamManager;
        levelCache = null;
        streamCache = null;
        levelManager = null;
        streamManager = null;
        close(oldLevelManager);
        close(oldStreamManager);
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
        PersistentCacheManager oldLevelManager = levelManager;
        PersistentCacheManager oldStreamManager = streamManager;
        if (oldLevelManager == null || oldStreamManager == null)
            return;

        close();
        destroy(oldLevelManager);
        destroy(oldStreamManager);
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
            size += FileUtils.diskUsage(levelCacheDir);
            size += FileUtils.diskUsage(streamCacheDir);
        } catch (Exception e) {
            Log.error(e);
        }
        return size;
    }

}
