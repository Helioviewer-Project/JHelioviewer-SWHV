package org.helioviewer.jhv.time;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeMap;

@SuppressWarnings("serial")
public class TimeMap<V> extends TreeMap<JHVTime, V> {

    private JHVTime[] timeArray;
    private int maxIdx;

    public void buildIndex() {
        if (isEmpty())
            throw new RuntimeException("Attempt to call buildIndex() on empty TimeMap");

        Set<JHVTime> keySet = navigableKeySet();
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
        int idx = Arrays.binarySearch(timeArray, time);
        if (idx >= 0)
            return timeArray[idx];

        int ip = -idx - 1;
        if (ip == 0)
            return timeArray[0];
        if (ip > maxIdx)
            return timeArray[maxIdx];

        JHVTime f = timeArray[ip - 1];
        JHVTime c = timeArray[ip];
        return time.milli - f.milli < c.milli - time.milli ? f : c;
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
