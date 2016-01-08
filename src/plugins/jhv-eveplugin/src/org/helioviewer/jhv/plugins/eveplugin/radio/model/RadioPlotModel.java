package org.helioviewer.jhv.plugins.eveplugin.radio.model;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.helioviewer.jhv.base.Range;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawController;
import org.helioviewer.jhv.plugins.eveplugin.draw.PlotAreaSpace;
import org.helioviewer.jhv.plugins.eveplugin.radio.data.DownloadRequestData;
import org.helioviewer.jhv.plugins.eveplugin.radio.data.FrequencyInterval;
import org.helioviewer.jhv.plugins.eveplugin.radio.data.RadioDataManager;
import org.helioviewer.jhv.plugins.eveplugin.radio.data.RadioDataManagerListener;
import org.helioviewer.jhv.plugins.eveplugin.radio.gui.RadioImagePane;

public class RadioPlotModel implements RadioDataManagerListener, ZoomDataConfigListener, ColorLookupModelListener {

    private static RadioPlotModel instance;
    private final RadioDataManager radioDataManager;
    private final ZoomManager zoomManager;
    private final DrawController drawController;
    private Map<Long, BufferedImage> bufferedImages;
    // private final YValueModel yValueModel;
    private final RadioYAxisElement yAxisElement;
    private final RadioImagePane radioImagePane;
    private final Set<RadioPlotModelListener> listeners;
    private DownloadRequestData downloadRequestData;
    private Map<Long, PlotConfig> plotConfigList;
    /** Map containing per download id a list of no data configurations */
    private List<NoDataConfig> noDataConfigList;

    // private final RadioPlotModelData radioPlotModelData;

    private RadioPlotModel() {
        ColorLookupModel.getInstance().addFilterModelListener(this);
        radioDataManager = RadioDataManager.getSingletonInstance();
        radioDataManager.addRadioManagerListener(this);
        zoomManager = ZoomManager.getSingletonInstance();
        drawController = DrawController.getSingletonInstance();
        bufferedImages = new HashMap<Long, BufferedImage>();
        yAxisElement = new RadioYAxisElement();
        yAxisElement.setLabel("MHz");
        yAxisElement.setIsLogScale(false);
        radioImagePane = new RadioImagePane();
        radioImagePane.setYAxisElement(yAxisElement);
        plotConfigList = new HashMap<Long, PlotConfig>();
        noDataConfigList = new ArrayList<NoDataConfig>();
        listeners = new HashSet<RadioPlotModelListener>();
    }

    public static RadioPlotModel getSingletonInstance() {
        if (instance == null) {
            instance = new RadioPlotModel();
        }
        return instance;
    }

    public void addRadioPlotModelListener(RadioPlotModelListener listener, String plotIdentifier) {
        listeners.add(listener);
    }

    public void removeRadioPlotModelListener(RadioPlotModelListener listener, String plotIdentifier) {
        listeners.remove(listener);
    }

    public Collection<PlotConfig> getPlotConfigurations() {
        return plotConfigList.values();
    }

    /**
     * Creates a collection of no data configurations for a specific plot
     * identifier
     *
     * @return A collection containing all the no data configurations.
     */
    public Collection<NoDataConfig> getNoDataConfigurations() {
        return noDataConfigList;
    }

    /*
     * RadioDataManagerListener
     */
    @Override
    public void downloadRequestAnswered(Interval<Date> timeInterval) {
        zoomManager.addZoomDataConfig(timeInterval);
        PlotAreaSpace.getSingletonInstance().addValueSpace(yAxisElement);
    }

    @Override
    public void newDataAvailable(DownloadRequestData data) {
        downloadRequestData = data;
        plotConfigList = new HashMap<Long, PlotConfig>();
    }

    @Override
    public void downloadFinished() {
    }

    @Override
    public void newGlobalFrequencyInterval(FrequencyInterval interval) {
    }

    @Override
    public void newDataReceived(byte[] data, Interval<Date> timeInterval, FrequencyInterval freqInterval, Rectangle area, List<Long> IDList, long radioImageID) {
        // Log.debug("Size of buffered images: " + bufferedImages.size());
    }

    @Override
    public void clearAllSavedImages() {
        plotConfigList.clear();
    }

    @Override
    public void downloadRequestDataRemoved(DownloadRequestData drd) {
        PlotAreaSpace.getSingletonInstance().removeValueSpace(yAxisElement);
        noDataConfigList = new ArrayList<NoDataConfig>();
        plotConfigList = new HashMap<Long, PlotConfig>();
        downloadRequestData = null;
        zoomManager.removeZoomManagerDataConfig();
        drawController.removeDrawableElement(radioImagePane);
        for (long imageID : drd.getRadioImages().keySet()) {
            bufferedImages.remove(imageID);
        }
        fireRemoveRadioImage();
    }

    @Override
    public void downloadRequestDataVisibilityChanged(DownloadRequestData drd) {
        downloadRequestData = drd;
        for (PlotConfig pc : plotConfigList.values()) {
            pc.setVisible(drd.isVisible());
        }
        for (NoDataConfig ndc : noDataConfigList) {
            ndc.setVisible(drd.isVisible());
        }
        fireChangeVisibility();
    }

    @Override
    public void newDataForIDReceived(int[] data, Interval<Date> timeInterval, FrequencyInterval freqInterval, Rectangle area, long radioImageID) {
        BufferedImage newImage = createBufferedImage(area.width, area.height, data);
        bufferedImages.put(radioImageID, newImage);
        radioImagePane.setIntervalTooBig(false);
        DrawableAreaMap dam = zoomManager.getDrawableAreaMap(timeInterval.getStart(), timeInterval.getEnd(), freqInterval.getStart(), freqInterval.getEnd(), area);
        PlotConfig pc = new PlotConfig(newImage, dam, downloadRequestData.isVisible(), radioImageID);
        plotConfigList.put(radioImageID, pc);
        fireDrawNewBufferedImage();
    }

    @Override
    public void newDataForIDReceived(byte[] byteData, Interval<Date> timeInterval, FrequencyInterval freqInterval, Rectangle area, long radioImageID) {
        BufferedImage newImage = createBufferedImage(area.width, area.height, byteData);
        bufferedImages.put(radioImageID, newImage);
        radioImagePane.setIntervalTooBig(false);
        DrawableAreaMap dam = zoomManager.getDrawableAreaMap(timeInterval.getStart(), timeInterval.getEnd(), freqInterval.getStart(), freqInterval.getEnd(), area);
        PlotConfig pc = new PlotConfig(newImage, dam, downloadRequestData.isVisible(), radioImageID);
        plotConfigList.put(radioImageID, pc);
        fireDrawNewBufferedImage();
    }

    @Override
    public void clearAllSavedImagesForID(long imageID) {
        plotConfigList.remove(imageID);
    }

    @Override
    public void intervalTooBig() {
        plotConfigList = new HashMap<Long, PlotConfig>();
        radioImagePane.setIntervalTooBig(true);
        drawController.updateDrawableElement(radioImagePane);
    }

    @Override
    public void noDataInterval(List<Interval<Date>> noDataList) {
        radioImagePane.setIntervalTooBig(false);
        for (Interval<Date> noData : noDataList) {
            DrawableAreaMap dam = zoomManager.getDrawableAreaMap(noData.getStart(), noData.getEnd());
            noDataConfigList.add(new NoDataConfig(noData, dam, downloadRequestData.isVisible()));
        }
        fireDrawNewBufferedImage();
    }

    @Override
    public void frequencyIntervalUpdated(FrequencyInterval maxFrequencyInterval) {
        yAxisElement.setAvailableRange(new Range(maxFrequencyInterval.getStart(), maxFrequencyInterval.getEnd()));
    }

    /*
     * ZoomDataConfigListener
     */

    @Override
    public void requestData(Date xStart, Date xEnd, double yStart, double yEnd, double xRatio, double yRatio) {
        radioDataManager.requestForData(xStart, xEnd, yStart, yEnd, xRatio, yRatio);
        updateNoDataConfig();
    }

    private void fireDrawNewBufferedImage() {// BufferedImage
        radioImagePane.setYAxisElement(yAxisElement);
        drawController.updateDrawableElement(radioImagePane);
    }

    private BufferedImage createBufferedImage(int width, int height, int[] data) {
        int[] useData;
        if (width * height == data.length) {
            useData = data;
        } else {
            Log.error("Data array was too small; white image created");
            useData = new int[width * height];
        }
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        DataBufferInt dataBuffer = new DataBufferInt(useData, width * height);
        Raster raster = Raster.createPackedRaster(dataBuffer, width, height, width, new int[] { 0xFF0000, 0xFF00, 0xFF, 0xFF000000 }, new Point(0, 0));
        newImage.setData(raster);
        return newImage;
    }

    private BufferedImage createBufferedImage(int width, int height, byte[] data) {
        byte[] useData;
        if (width * height == data.length) {
            useData = data;
        } else {
            Log.error("Data array was too small; white image created");
            useData = new byte[width * height];
        }
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, ColorLookupModel.getInstance().getColorModel());
        DataBufferByte dataBuffer = new DataBufferByte(useData, width * height);
        Raster raster = Raster.createPackedRaster(dataBuffer, width, height, width, new int[] { 0xff }, new Point(0, 0));
        newImage.setData(raster);
        return newImage;
    }

    private void fireRemoveRadioImage() {
        for (RadioPlotModelListener l : listeners) {
            l.removeDownloadRequestData();
        }
    }

    private void fireChangeVisibility() {
        for (RadioPlotModelListener l : listeners) {
            l.changeVisibility();
        }
    }

    private void updateNoDataConfig() {
        for (NoDataConfig ndc : noDataConfigList) {
            DrawableAreaMap dam = zoomManager.getDrawableAreaMap(ndc.getDateInterval().getStart(), ndc.getDateInterval().getEnd());
            ndc.setDrawableAreaMap(dam);
        }
        fireDrawNewBufferedImage();
    }

    @Override
    public void colorLUTChanged() {
        ColorModel cm = ColorLookupModel.getInstance().getColorModel();
        Map<Long, BufferedImage> newBufferedImages = new HashMap<Long, BufferedImage>();
        for (long index : bufferedImages.keySet()) {
            BufferedImage old = bufferedImages.get(index);
            BufferedImage newIm = new BufferedImage(cm, old.getRaster(), false, null);
            newBufferedImages.put(index, newIm);
        }
        bufferedImages = newBufferedImages;
        updatePlotConfigurations();
        if (!getPlotConfigurations().isEmpty()) {
            fireDrawNewBufferedImage();
        }
    }

    private void updatePlotConfigurations() {
        for (long imageID : plotConfigList.keySet()) {
            PlotConfig tempPC = plotConfigList.get(imageID);
            if (bufferedImages.containsKey(imageID)) {
                tempPC.setBufferedImage(bufferedImages.get(imageID));
            }
        }

    }

    public RadioYAxisElement getYAxisElement() {
        return yAxisElement;
    }

}
