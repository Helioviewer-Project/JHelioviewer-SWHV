package org.helioviewer.jhv.time;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

@SuppressWarnings("serial")
public class TimeMap<V> extends TreeMap<JHVTime, V> {

    private HashSet<JHVTime> timeSet;
    private JHVTime[] timeArray;
    private int maxIdx;

    public void buildIndex() {
        Set<JHVTime> keySet = navigableKeySet();
        timeSet = new HashSet<>(keySet);
        timeArray = keySet.toArray(JHVTime[]::new);
        maxIdx = timeArray.length - 1;
    }

    @Override
    public JHVTime firstKey() {
        return timeArray[0];
    }

    @Override
    public JHVTime lastKey() {
        return timeArray[maxIdx];
    }

    @Override
    public JHVTime lowerKey(JHVTime time) {
        JHVTime k = super.lowerKey(time);
        return k == null ? timeArray[0] : k;
    }

    @Override
    public JHVTime higherKey(JHVTime time) {
        JHVTime k = super.higherKey(time);
        return k == null ? timeArray[maxIdx] : k;
    }

    public int maxIndex() {
        return maxIdx;
    }

    public JHVTime key(int idx) {
        if (idx < 0) {
            idx = 0;
        } else if (idx > maxIdx) {
            idx = maxIdx;
        }
        return timeArray[idx];
    }

    public JHVTime nearestKey(JHVTime time) {
        if (timeSet.contains(time)) // common case
            return time;

        JHVTime c = ceilingKey(time);
        JHVTime f = floorKey(time);

        if (f != null && c != null)
            return time.milli - f.milli < c.milli - time.milli ? f : c;
        if (f == null && c != null)
            return c;
        return f;
    }

    public V nearestValue(JHVTime time) {
        return get(nearestKey(time));
    }

    public V indexedValue(int idx) {
        return get(key(idx));
    }

    public V lowerValue(JHVTime time) {
        return get(lowerKey(time));
    }

    public V higherValue(JHVTime time) {
        return get(higherKey(time));
    }

}
