package org.helioviewer.jhv.gui.interfaces;

public interface TimeSelector {

    int getCadence();
    void setStartTime(long time);
    void setEndTime(long time);
    long getStartTime();
    long getEndTime();

}
