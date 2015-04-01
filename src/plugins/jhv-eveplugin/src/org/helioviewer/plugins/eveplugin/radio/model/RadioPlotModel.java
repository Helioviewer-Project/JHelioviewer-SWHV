package org.helioviewer.plugins.eveplugin.radio.model;

import java.awt.EventQueue;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.plugins.eveplugin.controller.DrawController;
import org.helioviewer.plugins.eveplugin.draw.YAxisElement;
import org.helioviewer.plugins.eveplugin.radio.data.DownloadRequestData;
import org.helioviewer.plugins.eveplugin.radio.data.FrequencyInterval;
import org.helioviewer.plugins.eveplugin.radio.data.RadioDataManager;
import org.helioviewer.plugins.eveplugin.radio.data.RadioDataManagerListener;
import org.helioviewer.plugins.eveplugin.radio.gui.RadioImagePane;

public class RadioPlotModel implements RadioDataManagerListener, ZoomDataConfigListener, ColorLookupModelListener {
    private static RadioPlotModel instance;
    private final RadioDataManager radioDataManager;
    private final ZoomManager zoomManager;
    private final DrawController drawController;
    private Map<Long, BufferedImage> bufferedImages;
    private final YValueModel yValueModel;

    private final RadioPlotModelData radioPlotModelData;

    private RadioPlotModel() {
        ColorLookupModel.getInstance().addFilterModelListener(this);
        radioDataManager = RadioDataManager.getSingletonInstance();
        radioDataManager.addRadioManagerListener(this);
        zoomManager = ZoomManager.getSingletonInstance();
        drawController = DrawController.getSingletonInstance();
        bufferedImages = new HashMap<Long, BufferedImage>();
        radioPlotModelData = new RadioPlotModelData();
        yValueModel = YValueModel.getSingletonInstance();

    }

    public static RadioPlotModel getSingletonInstance() {
        if (instance == null) {
            instance = new RadioPlotModel();
        }
        return instance;
    }

    public void addRadioPlotModelListener(RadioPlotModelListener listener, String plotIdentifier) {
        radioPlotModelData.addRadioPlotModelListener(listener);
    }

    public void removeRadioPlotModelListener(RadioPlotModelListener listener, String plotIdentifier) {
        radioPlotModelData.removeRadioPlotModelListener(listener);
    }

    public Collection<PlotConfig> getPlotConfigurations() {
        if (!EventQueue.isDispatchThread()) {
            Log.error("Called from an other thread : " + Thread.currentThread().getName());
            Thread.dumpStack();
            System.exit(345);
        }
        Map<Long, Map<Long, PlotConfig>> plotConfigList = radioPlotModelData.getPlotConfigList();
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
        Map<Long, List<NoDataConfig>> noDataConfigList = radioPlotModelData.getNoDataConfigList();
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
        /*
         * YValueModel yValueModel =
         * this.yValueModelManager.getYValueModel(identifier);
         * yValueModel.setAvailableYMax(freqInterval.getStart());
         * yValueModel.setAvailableYMax(freqInterval.getEnd());
         */
    }

    @Override
    public void newDataAvailable(DownloadRequestData data, long downloadID) {
        Map<Long, DownloadRequestData> drd = radioPlotModelData.getDownloadRequestData();
        if (drd.containsKey(downloadID)) {
            drd.get(downloadID).mergeDownloadRequestData(data);
        } else {
            drd.put(downloadID, data);
        }
        Map<Long, Map<Long, PlotConfig>> plotConfigListMap = radioPlotModelData.getPlotConfigList();
        if (!plotConfigListMap.containsKey(downloadID)) {
            Map<Long, PlotConfig> plotConfigList = new HashMap<Long, PlotConfig>();
            radioPlotModelData.getPlotConfigList().put(downloadID, plotConfigList);
        }
    }

    @Override
    public void downloadFinished(long ID) {
    }

    @Override
    public void dataNotChanged(Interval<Date> timeInterval, FrequencyInterval freqInterval, Rectangle area, List<Long> downloadIDList, long radioImageID) {
        Map<Long, Map<Long, PlotConfig>> plotConfigList = radioPlotModelData.getPlotConfigList();
        Map<Long, DownloadRequestData> downloadRequestData = radioPlotModelData.getDownloadRequestData();
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
        radioPlotModelData.getPlotConfigList().clear();
    }

    @Override
    public void downloadRequestDataRemoved(DownloadRequestData drd, long ID) {
        Map<Long, Map<Long, PlotConfig>> plotConfigList = radioPlotModelData.getPlotConfigList();
        Map<Long, DownloadRequestData> downloadRequestData = radioPlotModelData.getDownloadRequestData();
        RadioImagePane radioImagePane = radioPlotModelData.getRadioImagePane();
        Map<Long, List<NoDataConfig>> noDataList = radioPlotModelData.getNoDataConfigList();
        noDataList.remove(ID);
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
        Map<Long, DownloadRequestData> downloadRequestData = radioPlotModelData.getDownloadRequestData();
        Map<Long, Map<Long, PlotConfig>> plotConfigList = radioPlotModelData.getPlotConfigList();
        Map<Long, List<NoDataConfig>> noDataList = radioPlotModelData.getNoDataConfigList();
        downloadRequestData.put(ID, drd);
        for (PlotConfig pc : plotConfigList.get(ID).values()) {
            pc.setVisible(drd.isVisible());
        }
        for (NoDataConfig ndc : noDataList.get(ID)) {
            ndc.setVisible(drd.isVisible());
        }

        fireChangeVisibility(ID, drd.isVisible());
    }

    @Override
    public void newDataForIDReceived(int[] data, Interval<Date> timeInterval, FrequencyInterval freqInterval, Rectangle area, Long downloadID, Long radioImageID) {
        Map<Long, DownloadRequestData> downloadRequestData = radioPlotModelData.getDownloadRequestData();
        Map<Long, Map<Long, PlotConfig>> plotConfigList = radioPlotModelData.getPlotConfigList();
        BufferedImage newImage = createBufferedImage(area.width, area.height, data);
        data = new int[0];
        bufferedImages.put(radioImageID, newImage);
        radioPlotModelData.getRadioImagePane().setIntervalTooBig(false);
        // Log.trace("+===============================================================+");
        // Log.trace("buffered images size : " + bufferedImages.size());
        // Log.trace("Size of the data : " + data.length);
        // Log.trace("timeinterval start : " + timeInterval.getStart());
        // Log.trace("timeinterval end : " + timeInterval.getEnd());
        // Log.trace("freqinterval start : " + freqInterval.getStart());
        // Log.trace("freqinterval end : " + freqInterval.getEnd());
        // Log.trace("Area : " + area);
        // Log.trace("DownloadID : " + downloadID);
        // Log.trace("Identifier : " + identifier);
        DrawableAreaMap dam = zoomManager.getDrawableAreaMap(timeInterval.getStart(), timeInterval.getEnd(), freqInterval.getStart(), freqInterval.getEnd(), area, downloadID);
        // Log.trace("Drawable Area Map : " + dam);
        // Log.trace("-===============================================================-");
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
        Map<Long, DownloadRequestData> downloadRequestData = radioPlotModelData.getDownloadRequestData();
        Map<Long, Map<Long, PlotConfig>> plotConfigList = radioPlotModelData.getPlotConfigList();
        BufferedImage newImage = createBufferedImage(area.width, area.height, byteData);
        byteData = new byte[0];
        bufferedImages.put(radioImageID, newImage);
        radioPlotModelData.getRadioImagePane().setIntervalTooBig(false);
        // Log.trace("+===============================================================+");
        // Log.trace("buffered images size : " + bufferedImages.size());
        // Log.trace("Size of the data : " + data.length);
        // Log.trace("timeinterval start : " + timeInterval.getStart());
        // Log.trace("timeinterval end : " + timeInterval.getEnd());
        // Log.trace("freqinterval start : " + freqInterval.getStart());
        // Log.trace("freqinterval end : " + freqInterval.getEnd());
        // Log.trace("Area : " + area);
        // Log.trace("DownloadID : " + downloadID);
        // Log.trace("Identifier : " + identifier);
        DrawableAreaMap dam = zoomManager.getDrawableAreaMap(timeInterval.getStart(), timeInterval.getEnd(), freqInterval.getStart(), freqInterval.getEnd(), area, downloadID);
        // Log.trace("Drawable Area Map : " + dam);
        // Log.trace("-===============================================================-");
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
        Map<Long, Map<Long, PlotConfig>> plotConfigList = radioPlotModelData.getPlotConfigList();
        Map<Long, PlotConfig> plotConfigPerDID = plotConfigList.get(downloadID);
        if (plotConfigPerDID != null) {
            plotConfigPerDID.remove(imageID);
        }
    }

    @Override
    public void intervalTooBig(long iD) {
        RadioImagePane radioImagePane = radioPlotModelData.getRadioImagePane();
        Map<Long, PlotConfig> plotConfigList = new HashMap<Long, PlotConfig>();
        radioPlotModelData.getPlotConfigList().put(iD, plotConfigList);
        radioImagePane.setIntervalTooBig(true);
        drawController.updateDrawableElement(radioImagePane);
    }

    @Override
    public void noDataInterval(List<Interval<Date>> noDataList, Long downloadID) {
        RadioImagePane radioImagePane = radioPlotModelData.getRadioImagePane();
        radioImagePane.setIntervalTooBig(false);
        Map<Long, List<NoDataConfig>> noDataConfigList = radioPlotModelData.getNoDataConfigList();
        Map<Long, DownloadRequestData> downloadRequestData = radioPlotModelData.getDownloadRequestData();
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
        yValueModel.setAvailableYMin(maxFrequencyInterval.getStart());
        yValueModel.setAvailableYMax(maxFrequencyInterval.getEnd());
    }

    /*
     * ZoomDataConfigListener
     */

    @Override
    public void requestData(Date xStart, Date xEnd, double yStart, double yEnd, double xRatio, double yRatio, long ID) {
        if (!EventQueue.isDispatchThread()) {
            Log.error("Called by other thread than event queue : " + Thread.currentThread().getName());
            Thread.dumpStack();
            System.exit(666);
        }
        List<Long> idList = new ArrayList<Long>();
        idList.add(ID);
        radioDataManager.requestForData(xStart, xEnd, yStart, yEnd, xRatio, yRatio, idList);
        updateNoDataConfig(ID);
    }

    private void fireDrawNewBufferedImage() {// BufferedImage
        RadioImagePane radioImagePane = radioPlotModelData.getRadioImagePane();
        YAxisElement yAxisElement = radioPlotModelData.getyAxisElement();
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
        Set<RadioPlotModelListener> listeners = radioPlotModelData.getListeners();
        for (RadioPlotModelListener l : listeners) {
            l.removeDownloadRequestData(ID);
        }
    }

    private void fireChangeVisibility(long ID, boolean visible) {
        Set<RadioPlotModelListener> listeners = radioPlotModelData.getListeners();
        for (RadioPlotModelListener l : listeners) {
            l.changeVisibility(ID);
        }
    }

    private void updateNoDataConfig(Long downloadID) {
        Map<Long, List<NoDataConfig>> noDataConfigList = radioPlotModelData.getNoDataConfigList();
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
        for (Long downloadID : radioPlotModelData.getPlotConfigList().keySet()) {
            for (Long imageID : radioPlotModelData.getPlotConfigList().get(downloadID).keySet()) {
                PlotConfig tempPC = radioPlotModelData.getPlotConfigList().get(downloadID).get(imageID);
                if (bufferedImages.containsKey(imageID)) {
                    tempPC.setBufferedImage(bufferedImages.get(imageID));
                }
            }
        }

    }

}
