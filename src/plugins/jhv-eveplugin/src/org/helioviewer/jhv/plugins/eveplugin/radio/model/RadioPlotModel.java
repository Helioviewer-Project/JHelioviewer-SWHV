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

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.plugins.eveplugin.base.Range;
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
    private final Map<Long, DownloadRequestData> downloadRequestData;
    private final Map<Long, Map<Long, PlotConfig>> plotConfigList;
    /** Map containing per download id a list of no data configurations */
    private final Map<Long, List<NoDataConfig>> noDataConfigList;

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
        downloadRequestData = new HashMap<Long, DownloadRequestData>();
        plotConfigList = new HashMap<Long, Map<Long, PlotConfig>>();
        noDataConfigList = new HashMap<Long, List<NoDataConfig>>();
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
        List<PlotConfig> tempAllConfig = new ArrayList<PlotConfig>();
        for (Map<Long, PlotConfig> map : plotConfigList.values()) {
            for (PlotConfig pc : map.values()) {
                tempAllConfig.add(pc);
            }
        }
        // Log.trace("Number of plot configs returned : " +
        // tempAllConfig.size());
        return tempAllConfig;

    }

    /**
     * Creates a collection of no data configurations for a specific plot
     * identifier
     *
     * @return A collection containing all the no data configurations.
     */
    public Collection<NoDataConfig> getNoDataConfigurations() {
        List<NoDataConfig> allNoDataConfigs = new ArrayList<NoDataConfig>();
        for (List<NoDataConfig> ndcl : noDataConfigList.values()) {
            allNoDataConfigs.addAll(ndcl);
        }
        return allNoDataConfigs;
    }

    /*
     * RadioDataManagerListener
     */
    @Override
    public void downloadRequestAnswered(Interval<Date> timeInterval, long ID) {
        zoomManager.addZoomDataConfig(timeInterval, this, ID);
        PlotAreaSpace.getSingletonInstance().addValueSpace(yAxisElement);
    }

    @Override
    public void newDataAvailable(DownloadRequestData data, long downloadID) {
        if (downloadRequestData.containsKey(downloadID)) {
            downloadRequestData.get(downloadID).mergeDownloadRequestData(data);
        } else {
            downloadRequestData.put(downloadID, data);
        }
        if (!plotConfigList.containsKey(downloadID)) {
            Map<Long, PlotConfig> tempPlotConfigList = new HashMap<Long, PlotConfig>();
            plotConfigList.put(downloadID, tempPlotConfigList);
        }
    }

    @Override
    public void downloadFinished(long ID) {
    }

    @Override
    public void dataNotChanged(Interval<Date> timeInterval, FrequencyInterval freqInterval, Rectangle area, List<Long> downloadIDList, long radioImageID) {
        for (long ID : downloadIDList) {
            BufferedImage newImage = bufferedImages.get(radioImageID);
            DrawableAreaMap dam = zoomManager.getDrawableAreaMap(timeInterval.getStart(), timeInterval.getEnd(), freqInterval.getStart(), freqInterval.getEnd(), area, ID);
            PlotConfig pc = new PlotConfig(newImage, dam, downloadRequestData.get(ID).isVisible(), ID, radioImageID);
            if (plotConfigList.containsKey(ID)) {
                plotConfigList.get(ID).put(radioImageID, pc);
            } else {
                Map<Long, PlotConfig> tempList = new HashMap<Long, PlotConfig>();
                tempList.put(radioImageID, pc);
                plotConfigList.put(ID, tempList);
            }
        }
        fireDrawNewBufferedImage();
    }

    @Override
    public void newGlobalFrequencyInterval(FrequencyInterval interval) {
    }

    @Override
    public void newDataReceived(byte[] data, Interval<Date> timeInterval, FrequencyInterval freqInterval, Rectangle area, List<Long> IDList, Long radioImageID) {
        // Log.debug("Size of buffered images: " + bufferedImages.size());
    }

    @Override
    public void clearAllSavedImages() {
        plotConfigList.clear();
    }

    @Override
    public void downloadRequestDataRemoved(DownloadRequestData drd, long ID) {
        PlotAreaSpace.getSingletonInstance().removeValueSpace(yAxisElement);
        noDataConfigList.remove(ID);
        plotConfigList.remove(ID);
        downloadRequestData.remove(ID);
        zoomManager.removeZoomManagerDataConfig(ID);
        drawController.removeDrawableElement(radioImagePane);
        for (Long imageID : drd.getRadioImages().keySet()) {
            bufferedImages.remove(imageID);
        }
        fireRemoveRadioImage(ID);
    }

    @Override
    public void downloadRequestDataVisibilityChanged(DownloadRequestData drd, long ID) {
        downloadRequestData.put(ID, drd);
        for (PlotConfig pc : plotConfigList.get(ID).values()) {
            pc.setVisible(drd.isVisible());
        }
        for (NoDataConfig ndc : noDataConfigList.get(ID)) {
            ndc.setVisible(drd.isVisible());
        }

        fireChangeVisibility(ID);
    }

    @Override
    public void newDataForIDReceived(int[] data, Interval<Date> timeInterval, FrequencyInterval freqInterval, Rectangle area, Long downloadID, Long radioImageID) {
        BufferedImage newImage = createBufferedImage(area.width, area.height, data);
        data = new int[0];
        bufferedImages.put(radioImageID, newImage);
        radioImagePane.setIntervalTooBig(false);
        DrawableAreaMap dam = zoomManager.getDrawableAreaMap(timeInterval.getStart(), timeInterval.getEnd(), freqInterval.getStart(), freqInterval.getEnd(), area, downloadID);
        PlotConfig pc = new PlotConfig(newImage, dam, downloadRequestData.get(downloadID).isVisible(), downloadID, radioImageID);
        if (plotConfigList.containsKey(downloadID)) {
            plotConfigList.get(downloadID).put(radioImageID, pc);
        } else {
            Map<Long, PlotConfig> tempList = new HashMap<Long, PlotConfig>();
            tempList.put(radioImageID, pc);
            plotConfigList.put(downloadID, tempList);
        }
        fireDrawNewBufferedImage();
    }

    @Override
    public void newDataForIDReceived(byte[] byteData, Interval<Date> timeInterval, FrequencyInterval freqInterval, Rectangle area, long downloadID, long radioImageID) {
        BufferedImage newImage = createBufferedImage(area.width, area.height, byteData);
        byteData = new byte[0];
        bufferedImages.put(radioImageID, newImage);
        radioImagePane.setIntervalTooBig(false);
        DrawableAreaMap dam = zoomManager.getDrawableAreaMap(timeInterval.getStart(), timeInterval.getEnd(), freqInterval.getStart(), freqInterval.getEnd(), area, downloadID);
        PlotConfig pc = new PlotConfig(newImage, dam, downloadRequestData.get(downloadID).isVisible(), downloadID, radioImageID);
        if (plotConfigList.containsKey(downloadID)) {
            plotConfigList.get(downloadID).put(radioImageID, pc);
        } else {
            Map<Long, PlotConfig> tempList = new HashMap<Long, PlotConfig>();
            tempList.put(radioImageID, pc);
            plotConfigList.put(downloadID, tempList);
        }
        fireDrawNewBufferedImage();
    }

    @Override
    public void clearAllSavedImagesForID(Long downloadID, Long imageID) {
        Map<Long, PlotConfig> plotConfigPerDID = plotConfigList.get(downloadID);
        if (plotConfigPerDID != null) {
            plotConfigPerDID.remove(imageID);
        }
    }

    @Override
    public void intervalTooBig(long iD) {
        Map<Long, PlotConfig> plotConfigMap = new HashMap<Long, PlotConfig>();
        plotConfigList.put(iD, plotConfigMap);
        radioImagePane.setIntervalTooBig(true);
        drawController.updateDrawableElement(radioImagePane);
    }

    @Override
    public void noDataInterval(List<Interval<Date>> noDataList, Long downloadID) {
        radioImagePane.setIntervalTooBig(false);
        if (!noDataConfigList.containsKey(downloadID)) {
            noDataConfigList.put(downloadID, new ArrayList<NoDataConfig>());
        } else {
            // noDataConfigList.get(downloadID).clear();
        }
        for (Interval<Date> noData : noDataList) {
            DrawableAreaMap dam = zoomManager.getDrawableAreaMap(noData.getStart(), noData.getEnd(), downloadID);
            noDataConfigList.get(downloadID).add(new NoDataConfig(noData, dam, downloadID, downloadRequestData.get(downloadID).isVisible()));
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
    public void requestData(Date xStart, Date xEnd, double yStart, double yEnd, double xRatio, double yRatio, long ID) {
        List<Long> idList = new ArrayList<Long>();
        idList.add(ID);
        radioDataManager.requestForData(xStart, xEnd, yStart, yEnd, xRatio, yRatio, idList);
        updateNoDataConfig(ID);
    }

    private void fireDrawNewBufferedImage() {// BufferedImage
        radioImagePane.setYAxisElement(yAxisElement);
        drawController.updateDrawableElement(radioImagePane);
    }

    private BufferedImage createBufferedImage(int width, int height, int[] data) {
        int[] useData = new int[0];
        if (width * height == data.length) {
            useData = data;
        } else {
            Log.error("Data array was to small created white image");
            useData = new int[width * height];
        }
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        DataBufferInt dataBuffer = new DataBufferInt(useData, width * height);
        useData = new int[0];
        Raster raster = Raster.createPackedRaster(dataBuffer, width, height, width, new int[] { 0xFF0000, 0xFF00, 0xFF, 0xFF000000 }, new Point(0, 0));
        newImage.setData(raster);
        return newImage;
    }

    private BufferedImage createBufferedImage(int width, int height, byte[] data) {
        byte[] useData = new byte[0];
        if (width * height == data.length) {
            useData = data;
        } else {
            Log.error("Data array was to small created white image");
            useData = new byte[width * height];
        }
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, ColorLookupModel.getInstance().getColorModel());
        DataBufferByte dataBuffer = new DataBufferByte(useData, width * height);
        useData = new byte[0];
        Raster raster = Raster.createPackedRaster(dataBuffer, width, height, width, new int[] { 0xff }, new Point(0, 0));
        newImage.setData(raster);
        return newImage;
    }

    private void fireRemoveRadioImage(long ID) {
        for (RadioPlotModelListener l : listeners) {
            l.removeDownloadRequestData(ID);
        }
    }

    private void fireChangeVisibility(long ID) {
        for (RadioPlotModelListener l : listeners) {
            l.changeVisibility(ID);
        }
    }

    private void updateNoDataConfig(Long downloadID) {
        if (!noDataConfigList.containsKey(downloadID)) {
            noDataConfigList.put(downloadID, new ArrayList<NoDataConfig>());
        } else {
            // noDataConfigList.get(downloadID).clear();
        }
        for (NoDataConfig ndc : noDataConfigList.get(downloadID)) {
            DrawableAreaMap dam = zoomManager.getDrawableAreaMap(ndc.getDateInterval().getStart(), ndc.getDateInterval().getEnd(), downloadID);
            ndc.setDrawableAreaMap(dam);
        }
        fireDrawNewBufferedImage();
    }

    @Override
    public void colorLUTChanged() {
        ColorModel cm = ColorLookupModel.getInstance().getColorModel();
        Map<Long, BufferedImage> newBufferedImages = new HashMap<Long, BufferedImage>();
        for (Long index : bufferedImages.keySet()) {
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
        for (Long downloadID : plotConfigList.keySet()) {
            for (Long imageID : plotConfigList.get(downloadID).keySet()) {
                PlotConfig tempPC = plotConfigList.get(downloadID).get(imageID);
                if (bufferedImages.containsKey(imageID)) {
                    tempPC.setBufferedImage(bufferedImages.get(imageID));
                }
            }
        }
    }

    public RadioYAxisElement getYAxisElement() {
        return yAxisElement;
    }
}
