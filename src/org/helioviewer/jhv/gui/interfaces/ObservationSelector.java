package org.helioviewer.jhv.gui.interfaces;

public interface ObservationSelector {

    int getCadence();

    void setTime(long start, long end);

    long getStartTime();

    long getEndTime();

    void load(String server, int sourceId);

    void setAvailabilityEnabled(boolean enable);

}
