package org.helioviewer.jhv.view.jp2view.io.jpip;

import javax.annotation.Nullable;

import org.helioviewer.jhv.JHVGlobals;

import org.ehcache.Cache;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;

public class JPIPCacheManager {

    private static final PersistentCacheManager streamManager = CacheManagerBuilder.newCacheManagerBuilder()
        .with(CacheManagerBuilder.persistence(JHVGlobals.jpipStreamCacheDir))
        .withCache("JPIPStream", CacheConfigurationBuilder
                                        .newCacheConfigurationBuilder(Long.class, JPIPStream.class,
                                            ResourcePoolsBuilder.newResourcePoolsBuilder()
                                                .disk(8, MemoryUnit.GB, true)
        )).build(true);
    private static final Cache<Long, JPIPStream> streamCache = streamManager.getCache("JPIPStream", Long.class, JPIPStream.class);

    private static final PersistentCacheManager levelManager = CacheManagerBuilder.newCacheManagerBuilder()
        .with(CacheManagerBuilder.persistence(JHVGlobals.jpipLevelCacheDir))
        .withCache("JPIPLevel", CacheConfigurationBuilder
                                        .newCacheConfigurationBuilder(Long.class, Integer.class,
                                            ResourcePoolsBuilder.newResourcePoolsBuilder()
                                                .heap(10000, EntryUnit.ENTRIES)
                                                .disk(10, MemoryUnit.MB, true)
        )).build(true);
    private static final Cache<Long, Integer> levelCache = levelManager.getCache("JPIPLevel", Long.class, Integer.class);

    private static final Thread destroy = new Thread(() -> {
        try {
            streamManager.close();
            levelManager.close();
        } catch (Exception ignore) {
        }
    });

    static {
        Runtime.getRuntime().addShutdownHook(destroy);
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

    public static void put(long key, int level, JPIPStream stream) {
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
