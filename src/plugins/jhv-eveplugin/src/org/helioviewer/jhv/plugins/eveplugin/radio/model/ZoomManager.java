package org.helioviewer.jhv.plugins.eveplugin.radio.model;

import java.awt.Rectangle;

import org.helioviewer.jhv.base.Range;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawController;
import org.helioviewer.jhv.plugins.eveplugin.draw.GraphDimensionListener;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimingListener;
import org.helioviewer.jhv.plugins.eveplugin.draw.ValueSpaceListener;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxis;
import org.helioviewer.jhv.plugins.eveplugin.radio.data.RadioDataManager;

public class ZoomManager implements TimingListener, GraphDimensionListener, ValueSpaceListener {

    private static ZoomManager instance;
    private DrawController drawController;
    private YAxis yAxisElement;
    private RadioDataManager radioDataManager;

    private ZoomManager() {
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
        radioDataManager = RadioDataManager.getSingletonInstance();
        yAxisElement = radioDataManager.getYAxisElement();
    }

    public void addZoomDataConfig(Interval interval) {
        radioDataManager.getYAxisElement().addValueSpaceListener(this);
        if (interval != null) {
            requestData();
        }
    }

    public DrawableAreaMap getDrawableAreaMap(long startDate, long endDate, int visualStartFrequency, int visualEndFrequency, int imageStartFrequency, int imageEndFrequency, Rectangle area) {
        int sourceX0 = defineXInSourceArea(startDate, startDate, endDate, area);
        int sourceY0 = 0;
        int sourceX1 = defineXInSourceArea(endDate, startDate, endDate, area);
        int sourceY1 = area.height;
        if (sourceY0 == sourceY1) {
            sourceY1 = sourceY0 + 1;
        }
        if (sourceX0 == sourceX1) {
            sourceX1 = sourceX0 + 1;
        }

        Rectangle plotArea = drawController.getPlotArea();
        int destX0 = drawController.selectedAxis.value2pixel(plotArea.x, plotArea.width, startDate);
        int destY0 = defineYInDestinationArea(visualStartFrequency, yAxisElement);
        int destX1 = drawController.selectedAxis.value2pixel(plotArea.x, plotArea.width, endDate);
        int destY1 = defineYInDestinationArea(visualEndFrequency, yAxisElement);
        return new DrawableAreaMap(sourceX0, sourceY0, sourceX1, sourceY1, destX0, destY0, destX1, destY1);
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
    public DrawableAreaMap getDrawableAreaMap(long startDate, long endDate) {
        Rectangle plotArea = drawController.getPlotArea();
        int destX0 = drawController.selectedAxis.value2pixel(plotArea.x, plotArea.width, startDate);
        int destY0 = 0;
        int destX1 = drawController.selectedAxis.value2pixel(plotArea.x, plotArea.width, endDate);
        int destY1 = plotArea.height;
        return new DrawableAreaMap(0, 0, 0, 0, destX0, destY0, destX1, destY1);
    }

    private int defineYInDestinationArea(int frequencyToFind, YAxis yAxisElement) {
        Rectangle displaySize = drawController.getPlotArea();
        return displaySize.height - (int) Math.floor((frequencyToFind - yAxisElement.getSelectedRange().min) / (1.0 * (yAxisElement.getSelectedRange().max - yAxisElement.getSelectedRange().min) / displaySize.height));
    }

    private int defineXInSourceArea(long dateToFind, long startDateArea, long endDateArea, Rectangle area) {
        long timediff = dateToFind - startDateArea;
        long timeOfArea = endDateArea - startDateArea;
        return (int) Math.floor(timediff / (1.0 * (timeOfArea) / area.width));
    }

    @Override
    public void availableIntervalChanged() {
    }

    @Override
    public void selectedIntervalChanged(boolean keepFullValueRange) {
        requestData();
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
    public void removeZoomManagerDataConfig() {
    }

    @Override
    public void graphDimensionChanged() {
        requestData();
    }

    @Override
    public void valueSpaceChanged(Range availableRange, Range selectedRange) {
        requestData();
    }

    private void requestData() {
        Rectangle displaySize = drawController.getGraphArea();
        double xRatio = drawController.selectedAxis.getRatio(displaySize.width);
        double yRatio = 1.0 * (yAxisElement.getSelectedRange().max - yAxisElement.getSelectedRange().min) / displaySize.getHeight();
        Interval selectedInterval = drawController.getSelectedInterval();
        radioDataManager.requestData(selectedInterval.start, selectedInterval.end, yAxisElement.getSelectedRange().min, yAxisElement.getSelectedRange().max, xRatio, yRatio);
    }

}
