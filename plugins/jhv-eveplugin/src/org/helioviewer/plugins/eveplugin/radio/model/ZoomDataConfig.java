package org.helioviewer.plugins.eveplugin.radio.model;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.helioviewer.base.logging.Log;
import org.helioviewer.plugins.eveplugin.model.PlotAreaSpaceListener;

public class ZoomDataConfig implements ZoomManagerListener, PlotAreaSpaceListener {
    private double minY;
    private double maxY;
    private double selectedMinY;
    private double selectedMaxY;
    private Date minX;
    private Date maxX;
    private Rectangle displaySize;
    private long ID;
    private String plotIdentifier;

    private List<ZoomDataConfigListener> listeners;

    public ZoomDataConfig(double minY, double maxY, Date minX, Date maxX, Rectangle displaySize, long ID, String plotIdentifier) {
        listeners = new ArrayList<ZoomDataConfigListener>();

        this.maxX = maxX;
        this.minX = minX;
        this.maxY = maxY;
        this.minY = minY;
        this.selectedMinY = minY;
        this.selectedMaxY = maxY;
        this.plotIdentifier = plotIdentifier;

        this.displaySize = displaySize;
        if (displaySize != null) {
            requestData();
        }
        this.ID = ID;
    }

    public void addListener(ZoomDataConfigListener l) {
        listeners.add(l);
        double xRatio = 1.0 * (maxX.getTime() - minX.getTime()) / displaySize.getWidth();
        double yRatio = 1.0 * (selectedMaxY - selectedMinY) / displaySize.getHeight();
        /*Thread t = new Thread((new Runnable() {
            ZoomDataConfigListener l;
            double xRatio;
            double yRatio;

            @Override
            public void run() {*/
                l.requestData(minX, maxX, selectedMinY, selectedMaxY, xRatio, yRatio, ID, plotIdentifier);
            /*}

            public Runnable init(ZoomDataConfigListener l, double xRatio, double yRatio) {
                this.l = l;
                this.xRatio = xRatio;
                this.yRatio = yRatio;
                return this;
            }

        }).init(l, xRatio, yRatio));
        t.start();*/

    }

    public long getID() {
        return ID;
    }

    public void setID(long iD) {
        ID = iD;
    }

    public void removeListener(ZoomDataConfigListener l) {
        listeners.remove(l);
    }

    public double getMinY() {
        return minY;
    }

    public void setMinY(double minY) {
        this.minY = minY;
    }

    public double getMaxY() {
        return maxY;
    }

    public void setMaxY(double maxY) {
        this.maxY = maxY;
    }

    public Date getMinX() {
        return minX;
    }

    public void setMinX(Date minX) {
        this.minX = minX;
    }

    public Date getMaxX() {
        return maxX;
    }

    public void setMaxX(Date maxX) {
        this.maxX = maxX;
    }

    public Rectangle getDisplaySize() {
        return displaySize;
    }

    public void update() {
        requestData();
    }

    public void setDisplaySize(Rectangle displaySize) {
        this.displaySize = displaySize;
        requestData();
    }

    @Override
    public void displaySizeChanged(Rectangle area) {
        this.displaySize = area;
        requestData();
    }

    private void requestData() {
        double xRatio = 1.0 * (maxX.getTime() - minX.getTime()) / displaySize.getWidth();
        double yRatio = 1.0 * (selectedMaxY - selectedMinY) / displaySize.getHeight();
        for (ZoomDataConfigListener l : listeners) {
            l.requestData(minX, maxX, selectedMinY, selectedMaxY, xRatio, yRatio, ID, plotIdentifier);
        }
    }

    @Override
    public void XValuesChanged(Date minX, Date maxX) {
        this.minX = minX;
        this.maxX = maxX;
        requestData();
    }

    @Override
    public void plotAreaSpaceChanged(double scaledMinValue, double scaledMaxValue, double scaledMinTime, double scaledMaxTime, double scaledSelectedMinValue, double scaledSelectedMaxValue, double scaledSelectedMinTime, double scaledSelectedMaxTime) {
        synchronized (this) {
            Log.trace("Plot area space changed");
            double ratioAvailable = (this.maxY - this.minY) / (scaledMaxValue - scaledMinValue);
            this.selectedMinY = this.minY + (scaledSelectedMinValue - scaledMinValue) * ratioAvailable;
            this.selectedMaxY = this.minY + (scaledSelectedMaxValue - scaledMinValue) * ratioAvailable;
            requestData();
        }

    }

    public double getSelectedMinY() {
        return selectedMinY;
    }

    public void setSelectedMinY(double selectedMinY) {
        this.selectedMinY = selectedMinY;
    }

    public double getSelectedMaxY() {
        return selectedMaxY;
    }

    public void setSelectedMaxY(double selectedMaxY) {
        this.selectedMaxY = selectedMaxY;
    }

    public String toString(){
    	StringBuilder sb = new StringBuilder();
    	sb.append("MinY = ").append(minY).append("\n")
    	.append("MaxY = ").append(maxY).append("\n")
    	.append("MinX = ").append(minX).append("\n")
    	.append("MaxX = ").append(maxX).append("\n")
    	.append("SelectedMinY = ").append(selectedMinY).append("\n")
    	.append("SelectedMaxY = ").append(selectedMaxY).append("\n")
    	.append("DisplaySize = ").append(displaySize).append("\n");
    	return sb.toString();
    }
    
}
