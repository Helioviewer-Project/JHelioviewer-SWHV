package org.helioviewer.jhv.plugins.eveplugin.radio.data;

public class RequestConfig {

    public final long xStart;
    public final long xEnd;
    // private final double yStart;
    // private final double yEnd;
    public final double xRatio;
    public final double yRatio;

    public RequestConfig(long xStart, long xEnd, double yStart, double yEnd, double xRatio, double yRatio) {
        this.xStart = xStart;
        this.xEnd = xEnd;
        // this.yStart = yStart;
        // this.yEnd = yEnd;
        this.xRatio = xRatio;
        this.yRatio = yRatio;
    }

}
