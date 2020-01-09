package org.helioviewer.jhv.time;

import java.util.TreeMap;

@SuppressWarnings("serial")
public class JHVDateMap<V> extends TreeMap<JHVDate, V> {

    private JHVDate[] times;
    private int maxIdx;

    public void index() {
        times = navigableKeySet().toArray(JHVDate[]::new);
        maxIdx = times.length - 1;
    }

    @Override
    public JHVDate firstKey() {
        return times[0];
    }

    @Override
    public JHVDate lastKey() {
        return times[maxIdx];
    }

    public JHVDate key(int idx) {
        if (idx < 0) {
            idx = 0;
        } else if (idx > maxIdx) {
            idx = maxIdx;
        }
        return times[idx];
    }

    public JHVDate nearestKey(JHVDate time) {
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

    @Override
    public JHVDate lowerKey(JHVDate time) {
        JHVDate k = super.lowerKey(time);
        return k == null ? times[0] : k;
    }

    @Override
    public JHVDate higherKey(JHVDate time) {
        JHVDate k = super.higherKey(time);
        return k == null ? times[maxIdx] : k;
    }

}
