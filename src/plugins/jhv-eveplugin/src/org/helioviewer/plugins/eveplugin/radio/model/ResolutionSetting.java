package org.helioviewer.plugins.eveplugin.radio.model;

import java.awt.Rectangle;

import org.helioviewer.base.math.Vector2dInt;

public class ResolutionSetting {

    private double xRatio;
    private double yRatio;
    private int resolutionNb;
    private int height;
    private int width;
    private int resolutionLevel;

    public ResolutionSetting() {
        xRatio = 1.0;
        yRatio = 1.0;
        resolutionNb = -1;
        width = 0;
        height = 0;
    }

    public ResolutionSetting(double xRatio, double yRatio, int resolutionNb, int width, int height, int resolutionLevel) {
        this.xRatio = xRatio;
        this.yRatio = yRatio;
        this.resolutionNb = resolutionNb;
        this.width = width;
        this.height = height;
        this.resolutionLevel = resolutionLevel;
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

    public int getResolutionNb() {
        return resolutionNb;
    }

    public void setResolutionNb(int resolutionNb) {
        this.resolutionNb = resolutionNb;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getResolutionLevel() {
        return resolutionLevel;
    }

    public void setResolutionLevel(int resolutionLevel) {
        this.resolutionLevel = resolutionLevel;
    }

    public Vector2dInt getVec2dIntRepresentation() {
        return new Vector2dInt(width, height);
    }

    public Rectangle getRectangleRepresentation() {
        return new Rectangle(width, height);
    }

    public boolean equals(Object other) {
        if (other instanceof ResolutionSetting) {
            ResolutionSetting temprs = (ResolutionSetting) other;
            return temprs.getHeight() == height && temprs.getWidth() == width && temprs.getxRatio() == xRatio && temprs.getyRatio() == yRatio;
        }
        return false;
    }

    public String toString() {
        return "x Ratio : " + xRatio + " " + "y Ratio : " + yRatio + " " + "nb : " + resolutionNb + " " + "level : " + resolutionLevel + " " + "height : " + height + " " + "width : " + width;
    }

}
