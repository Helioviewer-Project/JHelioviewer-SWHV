package org.helioviewer.jhv.time;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

// Populate with put(), then buildIndex() before reading. Do not change entries while using the index.
public class TimeMap<V> {

    private TreeMap<JHVTime, V> entries;
    private JHVTime[] timeArray;
    private Object[] valueArray;

    public TimeMap() {
        entries = new TreeMap<>();
    }

    // Already ordered input needs no construction tree. Copies the items and builds the index directly.
    public TimeMap(List<? extends V> _items, Function<? super V, JHVTime> _timeOf) {
        if (_items.isEmpty())
            throw new IllegalArgumentException("Empty time map");
        valueArray = _items.toArray();
        timeArray = new JHVTime[valueArray.length];
        for (int i = 0; i < timeArray.length; i++) {
            @SuppressWarnings("unchecked")
            V item = (V) valueArray[i];
            JHVTime time = _timeOf.apply(item);
            if (i > 0 && time.milli <= timeArray[i - 1].milli)
                throw new IllegalArgumentException("Times out of order");
            timeArray[i] = time;
        }
    }

    public V put(JHVTime time, V value) {
        return entries.put(time, value);
    }

    public int size() {
        return entries == null ? valueArray.length : entries.size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public void clear() {
        entries = new TreeMap<>();
        timeArray = null;
        valueArray = null;
    }

    public void buildIndex() {
        if (entries == null)
            return; // Ordered input is already indexed.
        if (entries.isEmpty())
            throw new RuntimeException("Attempt to call buildIndex() on empty TimeMap");

        timeArray = new JHVTime[entries.size()];
        valueArray = new Object[entries.size()];
        int i = 0;
        for (Map.Entry<JHVTime, V> entry : entries.entrySet()) {
            timeArray[i] = entry.getKey();
            valueArray[i] = entry.getValue();
            i++;
        }
    }

    public JHVTime firstTime() {
        return timeArray[0];
    }

    public JHVTime lastTime() {
        return timeArray[timeArray.length - 1];
    }

    // The latest time before this one, or the first.
    public JHVTime previousTime(JHVTime time) {
        int idx = Arrays.binarySearch(timeArray, time);
        int i = (idx >= 0 ? idx : -idx - 1) - 1;
        return timeAt(i);
    }

    // The earliest time after this one, or the last.
    public JHVTime nextTime(JHVTime time) {
        int idx = Arrays.binarySearch(timeArray, time);
        int i = idx >= 0 ? idx + 1 : -idx - 1;
        return timeAt(i);
    }

    public int maxIndex() {
        return timeArray.length - 1;
    }

    private int clamp(int idx) {
        return Math.clamp(idx, 0, timeArray.length - 1);
    }

    public JHVTime timeAt(int idx) {
        return timeArray[clamp(idx)];
    }

    public JHVTime nearestTime(JHVTime time) {
        return timeArray[nearestIndex(time)];
    }

    public int nearestIndex(JHVTime time) {
        int idx = Arrays.binarySearch(timeArray, time);
        if (idx >= 0)
            return idx;

        int ip = -idx - 1;
        if (ip == 0)
            return 0;
        if (ip >= timeArray.length)
            return timeArray.length - 1;

        JHVTime f = timeArray[ip - 1];
        JHVTime c = timeArray[ip];
        return time.milli - f.milli < c.milli - time.milli ? ip - 1 : ip;
    }

    public V nearestValue(JHVTime time) {
        return valueAt(nearestIndex(time));
    }

    @SuppressWarnings("unchecked")
    public V valueAt(int idx) {
        return (V) valueArray[clamp(idx)];
    }

    public V floorValue(JHVTime time) {
        int idx = Arrays.binarySearch(timeArray, time);
        return valueAt(idx >= 0 ? idx : -idx - 2);
    }

    public V ceilingValue(JHVTime time) {
        int idx = Arrays.binarySearch(timeArray, time);
        return valueAt(idx >= 0 ? idx : -idx - 1);
    }

}
