package org.helioviewer.plugins.eveplugin.radio.model;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.helioviewer.plugins.eveplugin.model.PlotAreaSpaceListener;

public class ZoomDataConfig implements ZoomManagerListener, PlotAreaSpaceListener {
    private Date minX;
    private Date maxX;
    private Rectangle displaySize;
    private long ID;
    private String plotIdentifier;
    private YValueModel yValueModel;

    private List<ZoomDataConfigListener> listeners;

    public ZoomDataConfig(Date minX, Date maxX, Rectangle displaySize, long ID, String plotIdentifier) {
        listeners = new ArrayList<ZoomDataConfigListener>();

        this.maxX = maxX;
        this.minX = minX;
        this.plotIdentifier = plotIdentifier;
        this.yValueModel = YValueModelManager.getInstance().getYValueModel(plotIdentifier);
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
        l.requestData(minX, maxX, yValueModel.getSelectedYMin(), yValueModel.getSelectedYMax(), xRatio, yRatio, ID, plotIdentifier);
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
        this.displaySize = area;
        requestData();
    }

    private void requestData() {
        double xRatio = 1.0 * (maxX.getTime() - minX.getTime()) / displaySize.getWidth();
        double yRatio = 1.0 * (yValueModel.getSelectedYMax() - yValueModel.getSelectedYMin()) / displaySize.getHeight();
        for (ZoomDataConfigListener l : listeners) {
            l.requestData(minX, maxX, yValueModel.getSelectedYMin(), yValueModel.getSelectedYMax(), xRatio, yRatio, ID, plotIdentifier);
        }
    }

    @Override
    public void XValuesChanged(Date minX, Date maxX) {
        this.minX = minX;
        this.maxX = maxX;
        requestData();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MinX = ").append(minX).append("\n").append("MaxX = ").append(maxX).append("\n").append("DisplaySize = ").append(displaySize).append("\n");
        return sb.toString();
    }

    @Override
    public void plotAreaSpaceChanged(double scaledMinValue, double scaledMaxValue, double scaledMinTime, double scaledMaxTime, double scaledSelectedMinValue, double scaledSelectedMaxValue, double scaledSelectedMinTime, double scaledSelectedMaxTime) {
        synchronized (this) {
            requestData();
        }
    }
}
