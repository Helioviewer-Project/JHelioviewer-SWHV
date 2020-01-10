package org.helioviewer.jhv.time;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

@SuppressWarnings("serial")
public class JHVDateMap<V> extends TreeMap<JHVDate, V> {

    private HashSet<JHVDate> timeSet;
    private JHVDate[] timeArray;
    private int maxIdx;

    public void buildIndex() {
        Set<JHVDate> keySet = navigableKeySet();
        timeSet = new HashSet<>(keySet);
        timeArray = keySet.toArray(JHVDate[]::new);
        maxIdx = timeArray.length - 1;
    }

    @Override
    public JHVDate firstKey() {
        return timeArray[0];
    }

    @Override
    public JHVDate lastKey() {
        return timeArray[maxIdx];
    }

    @Override
    public JHVDate lowerKey(JHVDate time) {
        JHVDate k = super.lowerKey(time);
        return k == null ? timeArray[0] : k;
    }

    @Override
    public JHVDate higherKey(JHVDate time) {
        JHVDate k = super.higherKey(time);
        return k == null ? timeArray[maxIdx] : k;
    }

    public int maxIndex() {
        return maxIdx;
    }

    public JHVDate key(int idx) {
        if (idx < 0) {
            idx = 0;
        } else if (idx > maxIdx) {
            idx = maxIdx;
        }
        return timeArray[idx];
    }

    public JHVDate nearestKey(JHVDate time) {
        if (timeSet.contains(time)) // common case
            return time;

        JHVDate c = ceilingKey(time);
        JHVDate f = floorKey(time);

        if (f != null && c != null)
            return time.milli - f.milli < c.milli - time.milli ? f : c;
        if (f == null && c != null)
            return c;
        return f;
    }

    public V nearestValue(JHVDate time) {
        return get(nearestKey(time));
    }

    public V indexValue(int idx) {
        return get(key(idx));
    }

}
