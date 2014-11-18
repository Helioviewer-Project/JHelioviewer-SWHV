package org.helioviewer.plugins.eveplugin.radio.model;

import java.awt.Rectangle;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.plugins.eveplugin.controller.ZoomController;
import org.helioviewer.plugins.eveplugin.controller.ZoomControllerListener;
import org.helioviewer.plugins.eveplugin.model.PlotAreaSpaceListener;
import org.helioviewer.plugins.eveplugin.model.PlotAreaSpaceManager;
import org.helioviewer.plugins.eveplugin.settings.EVEAPI.API_RESOLUTION_AVERAGES;

public class ZoomManager implements ZoomControllerListener, PlotAreaSpaceListener {
    private static ZoomManager instance;
    private final Map<String, ZoomManagerData> zoomManagerData;
    private final ZoomController zoomController;
    private Interval<Date> currentInterval;
    private final PlotAreaSpaceManager plotAreaSpaceManager;
    private final Object intervalLock;
    private final YValueModelManager yValueModelManager;

    private ZoomManager() {
        // currentInterval = new Interval<Date>(new Date(), new Date());
        zoomController = ZoomController.getSingletonInstance();
        zoomController.addZoomControllerListener(this);
        zoomManagerData = new HashMap<String, ZoomManagerData>();
        plotAreaSpaceManager = PlotAreaSpaceManager.getInstance();
        intervalLock = new Object();
        yValueModelManager = YValueModelManager.getInstance();

    }

    public static ZoomManager getSingletonInstance() {
        if (instance == null) {
            instance = new ZoomManager();
        }
        return instance;
    }

    public Rectangle getDisplaySize(String identifier) {
        ZoomManagerData zmd = getZoomManagerData(identifier);
        return zmd.getDisplaySize();
    }

    public void setDisplaySize(Rectangle displaySize, String identifier) {
        ZoomManagerData zmd = getZoomManagerData(identifier);
        Rectangle idenSize = zmd.getDisplaySize();
        if (!idenSize.equals(displaySize)) {
            zmd.setDisplaySize(displaySize);
            fireDisplaySizeChanged(identifier);
        }
    }

    public void calculateZoomXDirection() {

    }

    public void calculateZoomYDirection() {

    }

    public void calculateZoomXYDirection() {

    }

    public void addZoomDataConfig(Interval<Date> interval, ZoomDataConfigListener zoomDataConfigListener, long ID, String identifier) {
        ZoomManagerData zmd = getZoomManagerData(identifier);
        synchronized (intervalLock) {
            if (currentInterval == null) {
                currentInterval = interval;
            }
            if (interval != null) {
                ZoomDataConfig config;
                if (zmd.isAreaInitialized()) {
                    config = new ZoomDataConfig(currentInterval.getStart(), currentInterval.getEnd(), zmd.getDisplaySize(), ID, identifier);
                } else {
                    config = new ZoomDataConfig(currentInterval.getStart(), currentInterval.getStart(), null, ID, identifier);
                }
                plotAreaSpaceManager.getPlotAreaSpace(identifier).addPlotAreaSpaceListener(config);
                // Log.trace("PlotAreaSpaceListener added");
                zmd.addToZoomDataConfigMap(ID, config);
                config.addListener(zoomDataConfigListener);
            }
        }
    }

    public void addZoomManagerListener(ZoomManagerListener listener, String identifier) {
        ZoomManagerData zmd = getZoomManagerData(identifier);
        zmd.getListeners().add(listener);
    }

    public void removeZoomManagerListener(ZoomManagerListener listener, String identifier) {
        ZoomManagerData zmd = getZoomManagerData(identifier);
        zmd.getListeners().remove(listener);
    }

    public void fireDisplaySizeChanged(String identifier) {
        ZoomManagerData zmd = getZoomManagerData(identifier);
        List<ZoomManagerListener> zoomManagerListeners = zmd.getListeners();
        for (ZoomManagerListener l : zoomManagerListeners) {
            l.displaySizeChanged(zmd.getDisplaySize());
        }
    }

    public DrawableAreaMap getDrawableAreaMap(Date startDate, Date endDate, int startFrequency, int endFrequency, Rectangle area,
            long downloadID, String plotIdentifier) {
        ZoomManagerData zmd = getZoomManagerData(plotIdentifier);
        ZoomDataConfig zdc = zmd.getZoomDataConfigMap().get(downloadID);
        YValueModel yValueModel = yValueModelManager.getYValueModel(plotIdentifier);
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
    public DrawableAreaMap getDrawableAreaMap(Date startDate, Date endDate, long downloadID, String plotIdentifier) {
        ZoomManagerData zmd = getZoomManagerData(plotIdentifier);
        ZoomDataConfig zdc = zmd.getZoomDataConfigMap().get(downloadID);
        int destX0 = defineXInDestinationArea(startDate, zdc);
        int destY0 = 0;
        int destX1 = defineXInDestinationArea(endDate, zdc);
        int destY1 = zmd.getDisplaySize().height;
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
     * @param plotIdentifier
     *            The plot for which the space is requested
     * @return A rectangle with the dimensions of the available space for the
     *         requested intervals
     * @throws IllegalArgumentException
     *             If the given start date or end date fall outside the current
     *             interval or the given start frequency or end frequency fall
     *             outside the minimum and maximum frequency.
     */
    public Rectangle getAvailableSpaceForInterval(Date startDate, Date endDate, int startFreq, int endFreq, long downloadId,
            String plotIdentifier) {
        ZoomManagerData zmd = getZoomManagerData(plotIdentifier);
        ZoomDataConfig zdc = zmd.getZoomDataConfigMap().get(downloadId);
        YValueModel yValueModel = yValueModelManager.getYValueModel(plotIdentifier);
        synchronized (intervalLock) {
            if (currentInterval.containsPointInclusive(startDate) && currentInterval.containsPointInclusive(endDate)
                    && (startFreq >= yValueModel.getAvailableYMin() && startFreq <= yValueModel.getAvailableYMax())
                    && (endFreq >= yValueModel.getAvailableYMin() && endFreq <= yValueModel.getAvailableYMax())) {
                int height = zmd.getDisplaySize().height;
                double ratio = 1.0 * zmd.getDisplaySize().getWidth()
                        / (currentInterval.getEnd().getTime() - currentInterval.getStart().getTime());
                int width = (int) Math.round((endDate.getTime() - startDate.getTime()) * ratio);
                return new Rectangle(width, height);
            } else {
                Log.trace("The requested start date, end date fall outside the current interval, "
                        + "or the start frequency or end frequency fall outside the minimum or maximum frequency.\n " + "Start date : "
                        + startDate
                        + " in milliseconds : "
                        + startDate.getTime()
                        + "\n"
                        + "End date : "
                        + endDate
                        + " in milliseconds : "
                        + endDate.getTime()
                        + "\n"
                        + "Start frequency : "
                        + startFreq
                        + "\n"
                        + "End frequency : "
                        + endFreq
                        + "\n"
                        + "Current time interval : "
                        + currentInterval
                        + " in milliseconds : ["
                        + currentInterval.getStart().getTime()
                        + ", "
                        + currentInterval.getEnd().getTime()
                        + "]\n"
                        + "Current frequency interval : ["
                        + yValueModel.getAvailableYMin()
                        + ","
                        + yValueModel.getAvailableYMax()
                        + "]\n"
                        + "current interval contains start : "
                        + currentInterval.containsPointInclusive(startDate)
                        + "\n"
                        + "current interval contains end : " + currentInterval.containsPointInclusive(endDate));
                // System.exit(1);
                return new Rectangle(0, 0);
                /*
                 * throw new IllegalArgumentException(
                 * "The requested start date, end date fall outside the current interval, "
                 * +
                 * "or the start frequency or end frequency fall outside the minimum or maximum frequency.\n "
                 * + "Start date : " + startDate + " in milliseconds : "+
                 * startDate.getTime() +"\n" + "End date : "+endDate +
                 * " in milliseconds : "+ endDate.getTime() +"\n" +
                 * "Start frequency : "+ startFreq + "\n" +
                 * "End frequency : "+endFreq+"\n"+ "Current time interval : " +
                 * currentInterval + " in milliseconds : ["+
                 * currentInterval.getStart().getTime() +", "+
                 * currentInterval.getEnd().getTime() +"]\n"+
                 * "Current frequency interval : [" +
                 * zdc.getMinY()+","+zdc.getMaxY()+"]\n" +
                 * "current interval contains start : " +
                 * currentInterval.containsPointInclusive(startDate) + "\n" +
                 * "current interval contains end : " +
                 * currentInterval.containsPointInclusive(endDate));
                 */

            }
        }
    }

    private int defineYInDestinationArea(int frequencyToFind, YValueModel yValueModel, ZoomDataConfig zdc) {
        // return zdc.getDisplaySize().y + (int) Math.floor((frequencyToFind -
        // zdc.getMinY()) / (1.0 * (zdc.getMaxY() - zdc.getMinY()) /
        // zdc.getDisplaySize().height));
        return zdc.getDisplaySize().y
                + (int) Math.floor((frequencyToFind - yValueModel.getSelectedYMin())
                        / (1.0 * (yValueModel.getSelectedYMax() - yValueModel.getSelectedYMin()) / zdc.getDisplaySize().height));
    }

    private int defineXInDestinationArea(Date dateToFind, ZoomDataConfig zdc) {
        // Log.debug(zdc);
        // Log.debug(zdc.getDisplaySize());
        // Log.debug(zdc.getDisplaySize().x);
        return zdc.getDisplaySize().x
                + (int) Math.floor((dateToFind.getTime() - zdc.getMinX().getTime())
                        / (1.0 * (zdc.getMaxX().getTime() - zdc.getMinX().getTime()) / zdc.getDisplaySize().width));
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
    public void availableIntervalChanged(Interval<Date> newInterval) {
    }

    @Override
    public void selectedIntervalChanged(Interval<Date> newInterval, boolean keepFullValueSpace) {
        synchronized (intervalLock) {
            currentInterval = newInterval;
            for (ZoomManagerData zmd : zoomManagerData.values()) {
                for (ZoomDataConfig zdc : zmd.getZoomDataConfigMap().values()) {
                    zdc.setMinX(newInterval.getStart());
                    zdc.setMaxX(newInterval.getEnd());
                    zdc.update();
                }
            }
        }
    }

    @Override
    public void selectedResolutionChanged(API_RESOLUTION_AVERAGES newResolution) {
    }

    private ZoomManagerData getZoomManagerData(String identifier) {
        ZoomManagerData zwd = zoomManagerData.get(identifier);
        if (zwd == null) {
            zwd = new ZoomManagerData();
            zoomManagerData.put(identifier, zwd);
        }
        return zwd;
    }

    @Override
    public void plotAreaSpaceChanged(double scaledMinValue, double scaledMaxValue, double scaledMinTime, double scaledMaxTime,
            double scaledSelectedMinValue, double scaledSelectedMaxValue, double scaledSelectedMinTime, double scaledSelectedMaxTime,
            boolean forced) {
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
    public void removeZoomManagerDataConfig(long downloadID, String plotIdentifier) {
        ZoomManagerData zmd = zoomManagerData.get(plotIdentifier);
        if (zmd != null) {
            Map<Long, ZoomDataConfig> zoomDataConfigMap = zmd.getZoomDataConfigMap();
            plotAreaSpaceManager.getPlotAreaSpace(plotIdentifier).removePlotAreaSpaceListener(zoomDataConfigMap.get(downloadID));
            zmd.getZoomDataConfigMap().remove(downloadID);
        }
    }
}
