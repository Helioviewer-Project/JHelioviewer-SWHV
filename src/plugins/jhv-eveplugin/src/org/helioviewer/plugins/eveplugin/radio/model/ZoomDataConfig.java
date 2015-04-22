package org.helioviewer.plugins.eveplugin.radio.model;

import java.awt.EventQueue;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.helioviewer.base.logging.Log;
import org.helioviewer.plugins.eveplugin.draw.PlotAreaSpaceListener;

public class ZoomDataConfig implements ZoomManagerListener, PlotAreaSpaceListener {
    private Date minX;
    private Date maxX;
    private Rectangle displaySize;
    private long ID;
    private final YValueModel yValueModel;

    private final List<ZoomDataConfigListener> listeners;

    public ZoomDataConfig(Date minX, Date maxX, Rectangle displaySize, long ID) {
        listeners = new ArrayList<ZoomDataConfigListener>();

        this.maxX = maxX;
        this.minX = minX;
        yValueModel = YValueModel.getSingletonInstance();
        this.displaySize = displaySize;
        if (displaySize != null) {
            requestData();
        }
        this.ID = ID;
    }

    public void addListener(ZoomDataConfigListener l) {
        listeners.add(l);
        double xRatio = 1.0 * (maxX.getTime() - minX.getTime()) / displaySize.getWidth();
        double yRatio = 1.0 * (yValueModel.getSelectedYMax() - yValueModel.getSelectedYMin()) / displaySize.getHeight();
        l.requestData(minX, maxX, yValueModel.getSelectedYMin(), yValueModel.getSelectedYMax(), xRatio, yRatio, ID);
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
        displaySize = area;
        requestData();
    }

    private void requestData() {
        double xRatio = 1.0 * (maxX.getTime() - minX.getTime()) / displaySize.getWidth();
        double yRatio = 1.0 * (yValueModel.getSelectedYMax() - yValueModel.getSelectedYMin()) / displaySize.getHeight();
        for (ZoomDataConfigListener l : listeners) {
            l.requestData(minX, maxX, yValueModel.getSelectedYMin(), yValueModel.getSelectedYMax(), xRatio, yRatio, ID);
        }
    }

    @Override
    public void XValuesChanged(Date minX, Date maxX) {
        this.minX = minX;
        this.maxX = maxX;
        requestData();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MinX = ").append(minX).append("\n").append("MaxX = ").append(maxX).append("\n").append("DisplaySize = ").append(displaySize).append("\n");
        return sb.toString();
    }

    @Override
    public void plotAreaSpaceChanged(double scaledMinValue, double scaledMaxValue, double scaledMinTime, double scaledMaxTime, double scaledSelectedMinValue, double scaledSelectedMaxValue, double scaledSelectedMinTime, double scaledSelectedMaxTime, boolean forced) {
        if (!EventQueue.isDispatchThread()) {
            Log.error("Function called by other thread than eventqueue : " + Thread.currentThread().getName());
            Thread.dumpStack();
            System.exit(400);
        }
        requestData();

    }

    @Override
    public void availablePlotAreaSpaceChanged(double oldMinValue, double oldMaxValue, double oldMinTime, double oldMaxTime, double newMinValue, double newMaxValue, double newMinTime, double newMaxTime) {
        // TODO Auto-generated method stub

    }
}
