package org.helioviewer.jhv.plugins.eveplugin.radio.model;


public interface ZoomDataConfigListener {
    public abstract void requestData(long xStart, long xEnd, double yStart, double yEnd, double xRatio, double yRatio);
}
