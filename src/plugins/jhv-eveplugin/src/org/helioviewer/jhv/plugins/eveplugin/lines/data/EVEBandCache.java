package org.helioviewer.jhv.plugins.eveplugin.lines.data;

import java.awt.Rectangle;
import java.util.Date;
import java.util.HashMap;

import org.helioviewer.base.interval.Interval;

/**
 *
 * @author Stephan Pagel
 * */
public class EVEBandCache {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    private final HashMap<Band, EVECache> cacheMap = new HashMap<Band, EVECache>();

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    public void add(Band band, float[] values, long[] dates) {
        EVECache cache = cacheMap.get(band);
        if (cache == null) {
            cache = new EVECache();
            cacheMap.put(band, cache);
        }

        cache.add(values, dates);
    }

    public EVEValues getValuesInInterval(final Band band, final Interval<Date> interval, Rectangle plotArea) {
        EVECache cache = cacheMap.get(band);
        if (cache == null) {
            return new EVEValues();
        }

        return cache.getValuesInInterval(interval, plotArea);

    }

}
