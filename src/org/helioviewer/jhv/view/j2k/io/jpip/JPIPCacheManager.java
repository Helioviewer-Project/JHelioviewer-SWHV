package org.helioviewer.jhv.view.j2k.io.jpip;

import java.io.File;
import java.time.Duration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.JHVDirectory;
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

    private static Cache<Long, JPIPStream> streamCache;
    private static Cache<Long, Integer> levelCache;

    public static void init() {
        // delete old versions
        try {
            FileUtils.deleteDir(new File(JHVDirectory.CACHE.getFile(), "JPIPStream"));
        } catch (Exception ignore) {
        }
        try {
            FileUtils.deleteDir(new File(JHVDirectory.CACHE.getFile(), "JPIPLevel"));
        } catch (Exception ignore) {
        }

        File streamCacheDir = new File(JHVDirectory.CACHE.getFile(), "JPIPStream-2");
        File levelCacheDir = new File(JHVDirectory.CACHE.getFile(), "JPIPLevel-2");

        ExpiryPolicy<Object, Object> expiryPolicy = ExpiryPolicyBuilder.timeToIdleExpiration(Duration.ofDays(7));
        PersistentCacheManager streamManager = CacheManagerBuilder.newCacheManagerBuilder()
                .with(CacheManagerBuilder.persistence(streamCacheDir))
                .withCache("JPIPStream", CacheConfigurationBuilder
                        .newCacheConfigurationBuilder(Long.class, JPIPStream.class,
                                ResourcePoolsBuilder.newResourcePoolsBuilder()
                                        .disk(8, MemoryUnit.GB, true))
                        .withExpiry(expiryPolicy))
                .build(true);
        PersistentCacheManager levelManager = CacheManagerBuilder.newCacheManagerBuilder()
                .with(CacheManagerBuilder.persistence(levelCacheDir))
                .withCache("JPIPLevel", CacheConfigurationBuilder
                        .newCacheConfigurationBuilder(Long.class, Integer.class,
                                ResourcePoolsBuilder.newResourcePoolsBuilder()
                                        .heap(10000, EntryUnit.ENTRIES)
                                        .disk(10, MemoryUnit.MB, true))
                        .withExpiry(expiryPolicy))
                .build(true);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                streamManager.close();
                levelManager.close();
            } catch (Exception ignore) {
            }
        }));

        streamCache = streamManager.getCache("JPIPStream", Long.class, JPIPStream.class);
        levelCache = levelManager.getCache("JPIPLevel", Long.class, Integer.class);
    }

    @Nullable
    public static JPIPStream get(long key, int level) {
        try {
            Integer clevel = levelCache.get(key);
            if (clevel != null && clevel <= level)
                return streamCache.get(key);
        } catch (Exception e) { // might get interrupted
            e.printStackTrace();
        }
        return null;
    }

    public static void put(long key, int level, @Nonnull JPIPStream stream) {
        if (key != 0) {
            try {
                Integer clevel = levelCache.get(key);
                if (clevel == null || clevel > level) {
                    levelCache.put(key, level);
                    streamCache.put(key, stream);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
