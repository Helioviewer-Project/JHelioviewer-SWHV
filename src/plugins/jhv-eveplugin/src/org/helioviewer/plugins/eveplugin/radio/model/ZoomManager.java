package org.helioviewer.plugins.eveplugin.radio.model;

import java.awt.EventQueue;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.helioviewer.base.interval.Interval;
import org.helioviewer.base.logging.Log;
import org.helioviewer.plugins.eveplugin.draw.DrawController;
import org.helioviewer.plugins.eveplugin.draw.GraphDimensionListener;
import org.helioviewer.plugins.eveplugin.draw.TimingListener;
import org.helioviewer.plugins.eveplugin.model.PlotAreaSpace;
import org.helioviewer.plugins.eveplugin.model.PlotAreaSpaceListener;

public class ZoomManager implements TimingListener, PlotAreaSpaceListener, GraphDimensionListener {
    private static ZoomManager instance;
    private final DrawController drawController;
    private final PlotAreaSpace plotAreaSpace;
    private final YValueModel yValueModel;

    private final Map<Long, ZoomDataConfig> zoomDataConfigMap;
    private boolean isAreaInitialized;
    private final List<ZoomManagerListener> listeners;
    private Rectangle displaySize;

    private ZoomManager() {
        // currentInterval = new Interval<Date>(new Date(), new Date());
        drawController = DrawController.getSingletonInstance();
        drawController.addTimingListener(this);
        drawController.addGraphDimensionListener(this);
        plotAreaSpace = PlotAreaSpace.getSingletonInstance();
        yValueModel = YValueModel.getSingletonInstance();

        zoomDataConfigMap = new HashMap<Long, ZoomDataConfig>();
        listeners = new ArrayList<ZoomManagerListener>();
        isAreaInitialized = false;
        displaySize = new Rectangle();

    }

    public static ZoomManager getSingletonInstance() {
        if (instance == null) {
            instance = new ZoomManager();
        }
        return instance;
    }

    public void calculateZoomXDirection() {

    }

    public void calculateZoomYDirection() {

    }

    public void calculateZoomXYDirection() {

    }

    public void addZoomDataConfig(Interval<Date> interval, ZoomDataConfigListener zoomDataConfigListener, long ID) {
        Interval<Date> currentInterval = drawController.getSelectedInterval();
        if (currentInterval == null) {
            currentInterval = interval;
        }
        if (interval != null) {
            ZoomDataConfig config;
            if (isAreaInitialized) {
                config = new ZoomDataConfig(currentInterval.getStart(), currentInterval.getEnd(), displaySize, ID);
            } else {
                config = new ZoomDataConfig(currentInterval.getStart(), currentInterval.getStart(), null, ID);
            }
            plotAreaSpace.addPlotAreaSpaceListener(config);
            // Log.trace("PlotAreaSpaceListener added");
            zoomDataConfigMap.put(ID, config);
            config.addListener(zoomDataConfigListener);
        }
    }

    public void fireDisplaySizeChanged() {
        for (ZoomManagerListener l : listeners) {
            l.displaySizeChanged(displaySize);
        }
    }

    public DrawableAreaMap getDrawableAreaMap(Date startDate, Date endDate, int startFrequency, int endFrequency, Rectangle area, long downloadID) {
        ZoomDataConfig zdc = zoomDataConfigMap.get(downloadID);
        int sourceX0 = defineXInSourceArea(startDate, startDate, endDate, area);
        int sourceY0 = defineYInSourceArea((int) yValueModel.getSelectedYMax(), startFrequency, endFrequency, area, zdc);
        int sourceX1 = defineXInSourceArea(endDate, startDate, endDate, area);
        int sourceY1 = defineYInSourceArea((int) yValueModel.getSelectedYMin(), startFrequency, endFrequency, area, zdc);
        int destX0 = defineXInDestinationArea(startDate, zdc);
        int destY0 = defineYInDestinationArea(startFrequency, yValueModel, zdc);
        int destX1 = defineXInDestinationArea(endDate, zdc);
        int destY1 = defineYInDestinationArea(endFrequency, yValueModel, zdc);
        // Log.trace("Selected interval in getDrawableAreaMap : [" +
        // yValueModel.getSelectedYMin() + ", " + yValueModel.getSelectedYMax()
        // + "]");
        return new DrawableAreaMap(sourceX0, sourceY0, sourceX1, sourceY1, destX0, destY0, destX1, destY1, downloadID);
    }

    /**
     * Creates a drawable area map based on start and end date. The source will
     * have the coordinates (0,0,0,0) and are meaningless, the destination
     * coordinates are corresponding with the time interval and the taking the
     * complete height of the plot area.
     *
     * @param startDate
     *            The start date of the interval
     * @param endDate
     *            The end date of the interval
     * @param downloadID
     *            The download id of the request
     * @param plotIdentifier
     *            The plot identifier of the request
     * @return Drawable area map with the correct coordinates
     */
    public DrawableAreaMap getDrawableAreaMap(Date startDate, Date endDate, long downloadID) {
        ZoomDataConfig zdc = zoomDataConfigMap.get(downloadID);
        int destX0 = defineXInDestinationArea(startDate, zdc);
        int destY0 = 0;
        int destX1 = defineXInDestinationArea(endDate, zdc);
        int destY1 = displaySize.height;
        return new DrawableAreaMap(0, 0, 0, 0, destX0, destY0, destX1, destY1, downloadID);
    }

    /**
     * Calculates the available space in the screen size for the requested time
     * interval and frequency interval. The frequency gets the complete height,
     * the time gets the portion of the width of the screen corresponding with
     * the portion of the complete time interval it takes.
     *
     *
     * @param startDate
     *            The start date of the requested time interval
     * @param endDate
     *            The end date of the requested time interval
     * @param startFreq
     *            The start frequency of the requested frequency interval
     * @param endFreq
     *            The end frequency of the requested frequency interval
     * @param downloadId
     *            The download id that requests the space
     * @return A rectangle with the dimensions of the available space for the
     *         requested intervals
     * @throws IllegalArgumentException
     *             If the given start date or end date fall outside the current
     *             interval or the given start frequency or end frequency fall
     *             outside the minimum and maximum frequency.
     */
    public Rectangle getAvailableSpaceForInterval(Date startDate, Date endDate, int startFreq, int endFreq, long downloadId) {
        YValueModel yValueModel = YValueModel.getSingletonInstance();
        Interval<Date> currentInterval = drawController.getSelectedInterval();
        if (currentInterval.containsPointInclusive(startDate) && currentInterval.containsPointInclusive(endDate) && (startFreq >= yValueModel.getAvailableYMin() && startFreq <= yValueModel.getAvailableYMax()) && (endFreq >= yValueModel.getAvailableYMin() && endFreq <= yValueModel.getAvailableYMax())) {
            int height = displaySize.height;
            double ratio = 1.0 * displaySize.getWidth() / (currentInterval.getEnd().getTime() - currentInterval.getStart().getTime());
            int width = (int) Math.round((endDate.getTime() - startDate.getTime()) * ratio);
            return new Rectangle(width, height);
        } else {
            return new Rectangle(0, 0);
        }
    }

    private int defineYInDestinationArea(int frequencyToFind, YValueModel yValueModel, ZoomDataConfig zdc) {
        return zdc.getDisplaySize().y + (int) Math.floor((frequencyToFind - yValueModel.getSelectedYMin()) / (1.0 * (yValueModel.getSelectedYMax() - yValueModel.getSelectedYMin()) / zdc.getDisplaySize().height));
    }

    private int defineXInDestinationArea(Date dateToFind, ZoomDataConfig zdc) {
        return zdc.getDisplaySize().x + (int) Math.floor((dateToFind.getTime() - zdc.getMinX().getTime()) / (1.0 * (zdc.getMaxX().getTime() - zdc.getMinX().getTime()) / zdc.getDisplaySize().width));
    }

    private int defineYInSourceArea(int frequencyToFind, int startFrequency, int endFrequency, Rectangle area, ZoomDataConfig zdc) {
        return (int) Math.floor((frequencyToFind - startFrequency) / (1.0 * (endFrequency - startFrequency) / area.height));
    }

    private int defineXInSourceArea(Date dateToFind, Date startDateArea, Date endDateArea, Rectangle area) {
        long timediff = dateToFind.getTime() - startDateArea.getTime();
        long timeOfArea = endDateArea.getTime() - startDateArea.getTime();
        return (int) Math.floor(timediff / (1.0 * (timeOfArea) / area.width));
    }

    @Override
    public void availableIntervalChanged() {
    }

    @Override
    public void selectedIntervalChanged() {
        if (!EventQueue.isDispatchThread()) {
            Log.error("Function called by other thread than eventqueue : " + Thread.currentThread().getName());
            Thread.dumpStack();
            System.exit(433);
        }
        Interval<Date> newInterval = drawController.getSelectedInterval();

        for (ZoomDataConfig zdc : zoomDataConfigMap.values()) {
            zdc.setMinX(newInterval.getStart());
            zdc.setMaxX(newInterval.getEnd());
            zdc.update();
        }

    }

    @Override
    public void plotAreaSpaceChanged(double scaledMinValue, double scaledMaxValue, double scaledMinTime, double scaledMaxTime, double scaledSelectedMinValue, double scaledSelectedMaxValue, double scaledSelectedMinTime, double scaledSelectedMaxTime, boolean forced) {
    }

    /**
     * Remove the zoom manager data from the zoom manager.
     *
     * @param downloadID
     *            The download identifier to remove from the zoom manager
     * @param plotIdentifier
     *            The plot identifier for which the download identifier should
     *            be removed
     */
    public void removeZoomManagerDataConfig(long downloadID) {
        PlotAreaSpace.getSingletonInstance().removePlotAreaSpaceListener(zoomDataConfigMap.get(downloadID));
        zoomDataConfigMap.remove(downloadID);

    }

    @Override
    public void availablePlotAreaSpaceChanged(double oldMinValue, double oldMaxValue, double oldMinTime, double oldMaxTime, double newMinValue, double newMaxValue, double newMinTime, double newMaxTime) {
    }

    public void addZoomManagerListener(ZoomManagerListener listener) {
        listeners.add(listener);
    }

    public void removeZoomManagerListener(ZoomManagerListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void graphDimensionChanged() {
        Rectangle newDisplaySize = drawController.getPlotArea();
        if (!displaySize.equals(newDisplaySize)) {
            displaySize = newDisplaySize;
            for (ZoomDataConfig zsc : zoomDataConfigMap.values()) {
                zsc.setDisplaySize(newDisplaySize);
            }
            isAreaInitialized = true;
            fireDisplaySizeChanged();
        }
    }
}
