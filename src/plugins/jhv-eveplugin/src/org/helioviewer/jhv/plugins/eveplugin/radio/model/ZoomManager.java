package org.helioviewer.jhv.plugins.eveplugin.radio.model;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawController;
import org.helioviewer.jhv.plugins.eveplugin.draw.GraphDimensionListener;
import org.helioviewer.jhv.plugins.eveplugin.draw.PlotAreaSpace;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimingListener;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxisElement;

public class ZoomManager implements TimingListener, GraphDimensionListener {
    private static ZoomManager instance;
    private DrawController drawController;
    private PlotAreaSpace plotAreaSpace;
    private YAxisElement yAxisElement;

    private final Map<Long, ZoomDataConfig> zoomDataConfigMap;
    private boolean isAreaInitialized;
    private final List<ZoomManagerListener> listeners;
    private Rectangle displaySize;

    private ZoomManager() {
        // currentInterval = new Interval<Date>(new Date(), new Date());
        zoomDataConfigMap = new HashMap<Long, ZoomDataConfig>();
        listeners = new ArrayList<ZoomManagerListener>();
        isAreaInitialized = false;
        displaySize = new Rectangle();

    }

    public static ZoomManager getSingletonInstance() {
        if (instance == null) {
            instance = new ZoomManager();
            instance.init();
        }
        return instance;
    }

    private void init() {
        drawController = DrawController.getSingletonInstance();
        drawController.addTimingListener(this);
        drawController.addGraphDimensionListener(this);
        plotAreaSpace = PlotAreaSpace.getSingletonInstance();
        yAxisElement = RadioPlotModel.getSingletonInstance().getYAxisElement();
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
        int sourceY0 = defineYInSourceArea((int) yAxisElement.getSelectedRange().max, startFrequency, endFrequency, area, zdc, false);
        int sourceX1 = defineXInSourceArea(endDate, startDate, endDate, area);
        int sourceY1 = defineYInSourceArea((int) yAxisElement.getSelectedRange().min, startFrequency, endFrequency, area, zdc, true);
        int destX0 = defineXInDestinationArea(startDate, zdc);
        int destY0 = defineYInDestinationArea(startFrequency, yAxisElement, zdc);
        int destX1 = defineXInDestinationArea(endDate, zdc);
        int destY1 = defineYInDestinationArea(endFrequency, yAxisElement, zdc);
        if (sourceY0 == sourceY1) {
            sourceY1 = sourceY0 + 1;
        }
        if (sourceX0 == sourceX1) {
            sourceX1 = sourceX0 + 1;
        }
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
        Interval<Date> currentInterval = drawController.getSelectedInterval();
        if (currentInterval.containsPointInclusive(startDate) && currentInterval.containsPointInclusive(endDate) && (startFreq >= yAxisElement.getAvailableRange().min && startFreq <= yAxisElement.getAvailableRange().max) && (endFreq >= yAxisElement.getAvailableRange().min && endFreq <= yAxisElement.getAvailableRange().max)) {
            int height = displaySize.height;
            double ratio = 1.0 * displaySize.getWidth() / (currentInterval.getEnd().getTime() - currentInterval.getStart().getTime());
            int width = (int) Math.round((endDate.getTime() - startDate.getTime()) * ratio);
            return new Rectangle(width, height);
        } else {
            return new Rectangle(0, 0);
        }
    }

    private int defineYInDestinationArea(int frequencyToFind, YAxisElement yAxisElement, ZoomDataConfig zdc) {
        return zdc.getDisplaySize().y + (int) Math.floor((frequencyToFind - yAxisElement.getSelectedRange().min) / (1.0 * (yAxisElement.getSelectedRange().max - yAxisElement.getSelectedRange().min) / zdc.getDisplaySize().height));
    }

    private int defineXInDestinationArea(Date dateToFind, ZoomDataConfig zdc) {
        return zdc.getDisplaySize().x + (int) Math.floor((dateToFind.getTime() - zdc.getMinX().getTime()) / (1.0 * (zdc.getMaxX().getTime() - zdc.getMinX().getTime()) / zdc.getDisplaySize().width));
    }

    private int defineYInSourceArea(int frequencyToFind, int startFrequency, int endFrequency, Rectangle area, ZoomDataConfig zdc, boolean ceil) {
        if (!ceil) {
            return (int) Math.floor((frequencyToFind - startFrequency) / (1.0 * (endFrequency - startFrequency) / area.height));
        } else {
            return (int) Math.ceil((frequencyToFind - startFrequency) / (1.0 * (endFrequency - startFrequency) / area.height));
        }
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
    public void selectedIntervalChanged(boolean keepFullValueRange) {
        Interval<Date> newInterval = drawController.getSelectedInterval();

        for (ZoomDataConfig zdc : zoomDataConfigMap.values()) {
            zdc.setMinX(newInterval.getStart());
            zdc.setMaxX(newInterval.getEnd());
            zdc.update();
        }

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
