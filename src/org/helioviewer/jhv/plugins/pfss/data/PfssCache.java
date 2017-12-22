package org.helioviewer.jhv.plugins.pfss.data;

import java.util.TreeMap;

import org.helioviewer.jhv.plugins.pfss.PfssSettings;

public class PfssCache {

    private final TreeMap<Long, PfssData> map = new TreeMap<>();

    public void addData(long time, PfssData data) {
        assert map.size() < PfssSettings.CACHE_SIZE;
        map.put(time, data);
    }

    public PfssData getNearestData(long time) {
        Long c = map.ceilingKey(time);
        Long f = map.floorKey(time);

        if (c != null && f != null) {
            return Math.abs(f - time) < Math.abs(time - c) ? map.get(f) : map.get(c);
        }

        try {
            if (f == null)
                return map.get(c);
            return map.get(f);
        } catch (Exception ignore) {
        }

        return null;
    }

    public PfssData getData(long time) {
        return map.get(time);
    }

    public void clear() {
        map.clear();
    }

}
