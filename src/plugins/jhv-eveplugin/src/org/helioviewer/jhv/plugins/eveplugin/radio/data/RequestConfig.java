package org.helioviewer.jhv.plugins.eveplugin.radio.data;

import java.util.Date;

public class RequestConfig {

    private Date xStart;
    private Date xEnd;
    private double yStart;
    private double yEnd;
    private double xRatio;
    private double yRatio;

    public RequestConfig(Date xStart, Date xEnd, double yStart, double yEnd, double xRatio, double yRatio) {
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

    public void setxRatio(double xRatio) {
        this.xRatio = xRatio;
    }

    public double getyRatio() {
        return yRatio;
    }

    public void setyRatio(double yRatio) {
        this.yRatio = yRatio;
    }

    public Date getxStart() {
        return xStart;
    }

    public void setxStart(Date xStart) {
        this.xStart = xStart;
    }

    public Date getxEnd() {
        return xEnd;
    }

    public void setxEnd(Date xEnd) {
        this.xEnd = xEnd;
    }

    public double getyStart() {
        return yStart;
    }

    public void setyStart(double yStart) {
        this.yStart = yStart;
    }

    public double getyEnd() {
        return yEnd;
    }

    public void setyEnd(double yEnd) {
        this.yEnd = yEnd;
    }

}
