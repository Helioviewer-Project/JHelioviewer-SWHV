package org.helioviewer.jhv.plugins.pfss.data;

import java.util.TreeMap;

import org.helioviewer.jhv.plugins.pfss.PfssSettings;

public class PfssCache {

    private final TreeMap<Long, PfssData> map = new TreeMap<>();

    public void addData(PfssData data) {
        assert map.size() < PfssSettings.CACHE_SIZE;
        map.put(data.dateObs.milli, data);
    }

    public PfssData getData(long timestamp) {
        Long c = map.ceilingKey(timestamp);
        Long f = map.floorKey(timestamp);

        if (c != null && f != null) {
            if (Math.abs(f - timestamp) < Math.abs(timestamp - c))
                return map.get(f);
            else
                return map.get(c);
        }

        try {
            if (f == null)
                return map.get(c);
            return map.get(f);
        } catch (Exception ignore) {
        }

        return null;
    }

    public void clear() {
        map.clear();
    }

}
