package org.helioviewer.jhv.view.jp2view.io.jpip;

import org.helioviewer.jhv.JHVGlobals;

import org.ehcache.Cache;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
//import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;

public class JPIPCacheManager {

    private static final PersistentCacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
        .with(CacheManagerBuilder.persistence(JHVGlobals.jpipCacheDir))
        .withCache("JPIPStream", CacheConfigurationBuilder
                                        .newCacheConfigurationBuilder(Long.class, JPIPStream.class,
                                            ResourcePoolsBuilder.newResourcePoolsBuilder()
                                                //.heap(100, EntryUnit.ENTRIES)
                                                .disk(8, MemoryUnit.GB, true)
        )).build(true);
    private static final Cache<Long, JPIPStream> ehcache = cacheManager.getCache("JPIPStream", Long.class, JPIPStream.class);

    private static final Thread destroy = new Thread(() -> {
        try {
            //cacheManager.destroy();
            cacheManager.close();
        } catch (Exception ignore) {
        }
    });

    static {
        Runtime.getRuntime().addShutdownHook(destroy);
    }

    public static JPIPStream get(long key, int level) {
        try {
            return ehcache.get(key);
        } catch (Exception e) { // might get interrupted
            e.printStackTrace();
        }
        return null;
    }

    public static void put(long key, int level, JPIPStream stream) {
        if (key != 0)
            ehcache.put(key, stream);
    }

}
