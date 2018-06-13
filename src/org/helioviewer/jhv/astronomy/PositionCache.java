package org.helioviewer.jhv.astronomy;

import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nullable;

@SuppressWarnings("serial")
public class PositionCache extends TreeMap<Long, Position> {

    @Nullable
    public Position last() {
        Map.Entry<Long, Position> entry = lastEntry();
        return entry == null ? null : get(entry.getKey());
    }

}
