package org.helioviewer.jhv.plugins.eveplugin.radio.model;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.helioviewer.jhv.plugins.eveplugin.base.Range;
import org.helioviewer.jhv.plugins.eveplugin.draw.PlotAreaSpaceListener;
import org.helioviewer.jhv.plugins.eveplugin.draw.ValueSpaceListener;

public class ZoomDataConfig implements ZoomManagerListener, PlotAreaSpaceListener, ValueSpaceListener {

    private Date minX;
    private Date maxX;
    private Rectangle displaySize;
    private long ID;
    private final RadioYAxisElement yAxisElement;

    private final List<ZoomDataConfigListener> listeners;

    public ZoomDataConfig(Date minX, Date maxX, Rectangle displaySize, long ID) {
        listeners = new ArrayList<ZoomDataConfigListener>();

        this.maxX = maxX;
        this.minX = minX;
        yAxisElement = RadioPlotModel.getSingletonInstance().getYAxisElement();
        this.displaySize = displaySize;
        RadioPlotModel.getSingletonInstance().getYAxisElement().addValueSpaceListener(this);
        if (displaySize != null) {
            requestData();
        }
        this.ID = ID;
    }

    public void addListener(ZoomDataConfigListener l) {
        listeners.add(l);
        double xRatio = 1.0 * (maxX.getTime() - minX.getTime()) / displaySize.getWidth();
        double yRatio = 1.0 * (yAxisElement.getSelectedRange().max - yAxisElement.getSelectedRange().min) / displaySize.getHeight();
        l.requestData(minX, maxX, yAxisElement.getSelectedRange().min, yAxisElement.getSelectedRange().max, xRatio, yRatio, ID);
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
        double yRatio = 1.0 * (yAxisElement.getSelectedRange().max - yAxisElement.getSelectedRange().min) / displaySize.getHeight();
        for (ZoomDataConfigListener l : listeners) {
            l.requestData(minX, maxX, yAxisElement.getSelectedRange().min, yAxisElement.getSelectedRange().max, xRatio, yRatio, ID);
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
        sb.append("MinX = ").append(minX).append("\nMaxX = ").append(maxX).append("\nDisplaySize = ").append(displaySize).append("\n");
        return sb.toString();
    }

    @Override
    public void plotAreaSpaceChanged(double scaledMinTime, double scaledMaxTime, double scaledSelectedMinTime, double scaledSelectedMaxTime, boolean forced) {
        requestData();

    }

    @Override
    public void availablePlotAreaSpaceChanged(double oldMinTime, double oldMaxTime, double newMinTime, double newMaxTime) {
    }

    @Override
    public void valueSpaceChanged(Range availableRange, Range selectedRange) {
        requestData();
    }

}
