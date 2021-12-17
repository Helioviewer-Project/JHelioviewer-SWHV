package org.helioviewer.jhv.view.j2k.io.jpip;

import java.io.File;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.io.FileUtils;

import org.ehcache.Cache;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.expiry.ExpiryPolicy;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;

public class JPIPCacheManager {

    static {
        Logger.getLogger("org.ehcache").setLevel(Level.WARNING); // shutup Ehcache info logs
    }

    private static final File levelCacheDir = new File(JHVDirectory.CACHE.getFile(), "JPIPLevel-3");
    private static final File streamCacheDir = new File(JHVDirectory.CACHE.getFile(), "JPIPStream-3");

    private static PersistentCacheManager levelManager;
    private static PersistentCacheManager streamManager;
    private static Cache<String, Integer> levelCache;
    private static Cache<String, JPIPStream> streamCache;
    private static Thread hook;

    public static void init() {
        deleteDirs("JPIPLevel", "JPIPStream", "JPIPLevel-2", "JPIPStream-2"); // delete old versions

        ExpiryPolicy<Object, Object> expiryPolicy = ExpiryPolicyBuilder.timeToIdleExpiration(Duration.ofDays(7));

        levelManager = CacheManagerBuilder.newCacheManagerBuilder()
                .with(CacheManagerBuilder.persistence(levelCacheDir))
                .withCache("JPIPLevel", CacheConfigurationBuilder
                        .newCacheConfigurationBuilder(String.class, Integer.class,
                                ResourcePoolsBuilder.newResourcePoolsBuilder()
                                        .heap(10000, EntryUnit.ENTRIES)
                                        .disk(10, MemoryUnit.MB, true))
                        .withExpiry(expiryPolicy))
                .build(true);
        streamManager = CacheManagerBuilder.newCacheManagerBuilder()
                .with(CacheManagerBuilder.persistence(streamCacheDir))
                .withCache("JPIPStream", CacheConfigurationBuilder
                        .newCacheConfigurationBuilder(String.class, JPIPStream.class,
                                ResourcePoolsBuilder.newResourcePoolsBuilder()
                                        .disk(8, MemoryUnit.GB, true))
                        .withExpiry(expiryPolicy))
                .build(true);

        if (hook == null) {
            hook = new Thread(JPIPCacheManager::close);
            Runtime.getRuntime().addShutdownHook(hook);
        }

        streamCache = streamManager.getCache("JPIPStream", String.class, JPIPStream.class);
        levelCache = levelManager.getCache("JPIPLevel", String.class, Integer.class);
    }

    @Nullable
    public static JPIPStream get(@Nonnull String key, int level) {
        try {
            Integer clevel = levelCache.get(key);
            if (clevel != null && clevel <= level)
                return streamCache.get(key);
        } catch (Exception e) { // might get interrupted
            Log.error(e);
        }
        return null;
    }

    public static void put(@Nonnull String key, int level, @Nonnull JPIPStream stream) {
        try {
            Integer clevel = levelCache.get(key);
            if (clevel == null || clevel > level) {
                levelCache.put(key, level);
                streamCache.put(key, stream);
            }
        } catch (Exception e) {
            Log.error(e);
        }
    }

    private static void deleteDirs(String... dirs) {
        for (String dir : dirs) { // delete old versions
            try {
                FileUtils.deleteDir(new File(JHVDirectory.CACHE.getFile(), dir));
            } catch (Exception ignore) {
            }
        }
    }

    private static void close() {
        try {
            levelManager.close();
            streamManager.close();
        } catch (Exception e) {
            Log.error(e);
        }
    }

    public static void clear() {
        close();
        try {
            levelManager.destroy();
            streamManager.destroy();
        } catch (Exception e) {
            Log.error(e);
        }
        init();
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
