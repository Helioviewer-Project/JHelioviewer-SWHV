package org.helioviewer.jhv.plugins.pfss.data;

import java.lang.ref.SoftReference;
import java.util.TreeMap;

public class PfssCache {

    private final TreeMap<Long, SoftReference<PfssData>> map = new TreeMap<>();

    private PfssData get(Long time) {
        SoftReference<PfssData> ref = map.get(time);
        if (ref == null)
            return null;

        PfssData ret = ref.get();
        if (ret == null)
            map.remove(time); // mark as collected
        return ret;
    }

    public void addData(long time, PfssData data) {
        map.put(time, new SoftReference<>(data));
    }

    public PfssData getNearestData(long time) {
        Long c = map.ceilingKey(time);
        Long f = map.floorKey(time);

        if (c != null && f != null) {
            return Math.abs(f - time) < Math.abs(time - c) ? get(f) : get(c);
        }

        try {
            if (f == null)
                return get(c);
            return get(f);
        } catch (Exception ignore) {
        }

        return null;
    }

    public PfssData getData(long time) {
        return get(time);
    }

    public void clear() {
        map.clear();
    }

}
