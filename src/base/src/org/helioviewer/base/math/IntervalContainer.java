package org.helioviewer.base.math;

import java.util.Vector;

public class IntervalContainer<TimeFormat extends Comparable<TimeFormat>, ItemFormat extends IntervalComparison<TimeFormat>> {

    boolean partial = false;
    int downloadableEvents = 0;

    public boolean isPartial() {
        return partial;
    }

    public void setPartial(boolean partial) {
        this.partial = partial;
    }

    Vector<ItemFormat> items = new Vector<ItemFormat>();

    public IntervalContainer(Vector<ItemFormat> newItems) {
        items = newItems;
    }

    public IntervalContainer() {
    }

    public Vector<ItemFormat> getItems() {
        return items;
    }

    public void incDownloadableEvents() {
        downloadableEvents++;
    }

    public int getDownloadableEvents() {
        return downloadableEvents;
    }

    public String toString() {
        return "[ IntervalContainer: Partial: " + partial + ", Downloadable Events: " + downloadableEvents + ", Items: " + items + " ]";
    }

}
