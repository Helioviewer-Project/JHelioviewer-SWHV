package org.helioviewer.jhv.gui.interfaces;

public interface ObservationSelector {

    int getCadence();
    void setStartTime(long time);
    void setEndTime(long time);
    long getStartTime();
    long getEndTime();
    void load(String server, int sourceId);

}
