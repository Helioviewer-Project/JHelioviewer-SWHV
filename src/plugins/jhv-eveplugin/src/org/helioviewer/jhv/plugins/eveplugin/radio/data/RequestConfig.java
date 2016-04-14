package org.helioviewer.jhv.plugins.eveplugin.radio.data;

public class RequestConfig {

    private final long xStart;
    private final long xEnd;
    private final double yStart;
    private final double yEnd;
    private final double xRatio;
    private final double yRatio;

    public RequestConfig(long xStart, long xEnd, double yStart, double yEnd, double xRatio, double yRatio) {
        super();
        this.xStart = xStart;
        this.xEnd = xEnd;
        this.yStart = yStart;
        this.yEnd = yEnd;
        this.xRatio = xRatio;
        this.yRatio = yRatio;
    }

    public double getxRatio() {
        return xRatio;
    }

    public double getyRatio() {
        return yRatio;
    }

    public long getxStart() {
        return xStart;
    }

    public long getxEnd() {
        return xEnd;
    }

    public double getyStart() {
        return yStart;
    }

    public double getyEnd() {
        return yEnd;
    }

}
