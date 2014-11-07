package org.helioviewer.plugins.eveplugin.radio.model;

import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
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

public class RadioPlotModel implements RadioDataManagerListener, ZoomDataConfigListener {
    private static RadioPlotModel instance;
    private final RadioDataManager radioDataManager;
    private final ZoomManager zoomManager;
    private final DrawController drawController;
    private final Map<Long, BufferedImage> bufferedImages;
    private final YValueModelManager yValueModelManager;
    private long counter;

    private final Map<String, RadioPlotModelData> radioPlotModelData;

    private RadioPlotModel() {
        radioDataManager = RadioDataManager.getSingletonInstance();
        radioDataManager.addRadioManagerListener(this);
        zoomManager = ZoomManager.getSingletonInstance();
        drawController = DrawController.getSingletonInstance();
        bufferedImages = new HashMap<Long, BufferedImage>();
        radioPlotModelData = new HashMap<String, RadioPlotModelData>();
        yValueModelManager = YValueModelManager.getInstance();

    }

    public static RadioPlotModel getSingletonInstance() {
        if (instance == null) {
            instance = new RadioPlotModel();
        }
        return instance;
    }

    public void addRadioPlotModelListener(RadioPlotModelListener listener, String plotIdentifier) {
        getRadioPlotModelData(plotIdentifier).addRadioPlotModelListener(listener);
    }

    public void removeRadioPlotModelListener(RadioPlotModelListener listener, String plotIdentifier) {
        getRadioPlotModelData(plotIdentifier).removeRadioPlotModelListener(listener);
    }

    public Collection<PlotConfig> getPlotConfigurations(String identifier) {
        synchronized (this) {
            Map<Long, Map<Long, PlotConfig>> plotConfigList = getRadioPlotModelData(identifier).getPlotConfigList();
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
    }

    /**
     * Creates a collection of no data configurations for a specific plot
     * identifier
     * 
     * @param identifier
     *            The plot identifier
     * @return A collection containing all the no data configurations.
     */
    public Collection<NoDataConfig> getNoDataConfigurations(String identifier) {
        Map<Long, List<NoDataConfig>> noDataConfigList = getRadioPlotModelData(identifier).getNoDataConfigList();
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
    public void downloadRequestAnswered(Interval<Date> timeInterval, long ID, String identifier) {
        zoomManager.addZoomDataConfig(timeInterval, this, ID, identifier);
        /*
         * YValueModel yValueModel =
         * this.yValueModelManager.getYValueModel(identifier);
         * yValueModel.setAvailableYMax(freqInterval.getStart());
         * yValueModel.setAvailableYMax(freqInterval.getEnd());
         */
    }

    @Override
    public void newDataAvailable(DownloadRequestData data, long downloadID) {
        synchronized (this) {
            Map<Long, DownloadRequestData> drd = getRadioPlotModelData(data.getPlotIdentifier()).getDownloadRequestData();
            if (drd.containsKey(downloadID)) {
                drd.get(downloadID).mergeDownloadRequestData(data);
            } else {
                drd.put(downloadID, data);
            }
            Map<Long, Map<Long, PlotConfig>> plotConfigListMap = getRadioPlotModelData(data.getPlotIdentifier()).getPlotConfigList();
            if (!plotConfigListMap.containsKey(downloadID)) {
                Map<Long, PlotConfig> plotConfigList = new HashMap<Long, PlotConfig>();
                getRadioPlotModelData(data.getPlotIdentifier()).getPlotConfigList().put(downloadID, plotConfigList);
            }
        }
    }

    @Override
    public void downloadFinished(long ID) {
    }

    @Override
    public void dataNotChanged(Interval<Date> timeInterval, FrequencyInterval freqInterval, Rectangle area, List<Long> downloadIDList,
            String identifier, long radioImageID) {
        RadioPlotModelData rpmd = getRadioPlotModelData(identifier);
        Map<Long, Map<Long, PlotConfig>> plotConfigList = rpmd.getPlotConfigList();
        Map<Long, DownloadRequestData> downloadRequestData = rpmd.getDownloadRequestData();
        for (long ID : downloadIDList) {
            synchronized (this) {
                BufferedImage newImage = bufferedImages.get(radioImageID);
                DrawableAreaMap dam = zoomManager.getDrawableAreaMap(timeInterval.getStart(), timeInterval.getEnd(),
                        freqInterval.getStart(), freqInterval.getEnd(), area, ID, identifier);
                PlotConfig pc = new PlotConfig(newImage, dam, downloadRequestData.get(ID).isVisible(), ID, radioImageID);
                if (plotConfigList.containsKey(ID)) {
                    plotConfigList.get(ID).put(radioImageID, pc);
                } else {
                    Map<Long, PlotConfig> tempList = new HashMap<Long, PlotConfig>();
                    tempList.put(radioImageID, pc);
                    plotConfigList.put(ID, tempList);
                }

            }
        }
        fireDrawNewBufferedImage(identifier);
    }

    @Override
    public void newGlobalFrequencyInterval(FrequencyInterval interval) {
    }

    @Override
    public void newDataReceived(byte[] data, Interval<Date> timeInterval, FrequencyInterval freqInterval, Rectangle area,
            List<Long> IDList, String identifier, Long radioImageID) {
        // Log.debug("Size of buffered images: " + bufferedImages.size());
    }

    @Override
    public void clearAllSavedImages(String plotIdentifier) {
        synchronized (this) {
            getRadioPlotModelData(plotIdentifier).getPlotConfigList().clear();
        }
    }

    @Override
    public void downloadRequestDataRemoved(DownloadRequestData drd, long ID) {
        synchronized (this) {
            RadioPlotModelData rpmd = getRadioPlotModelData(drd.getPlotIdentifier());
            Map<Long, Map<Long, PlotConfig>> plotConfigList = rpmd.getPlotConfigList();
            Map<Long, DownloadRequestData> downloadRequestData = rpmd.getDownloadRequestData();
            RadioImagePane radioImagePane = rpmd.getRadioImagePane();
            Map<Long, List<NoDataConfig>> noDataList = rpmd.getNoDataConfigList();
            noDataList.remove(ID);
            plotConfigList.remove(ID);
            downloadRequestData.remove(ID);
            zoomManager.removeZoomManagerDataConfig(ID, drd.getPlotIdentifier());
            drawController.removeDrawableElement(radioImagePane, drd.getPlotIdentifier());
            for (Long imageID : drd.getRadioImages().keySet()) {
                bufferedImages.remove(imageID);
            }
            fireRemoveRadioImage(ID, drd.getPlotIdentifier());
        }
    }

    @Override
    public void downloadRequestDataVisibilityChanged(DownloadRequestData drd, long ID) {
        synchronized (this) {
            RadioPlotModelData rpmd = getRadioPlotModelData(drd.getPlotIdentifier());
            Map<Long, DownloadRequestData> downloadRequestData = rpmd.getDownloadRequestData();
            Map<Long, Map<Long, PlotConfig>> plotConfigList = rpmd.getPlotConfigList();
            Map<Long, List<NoDataConfig>> noDataList = rpmd.getNoDataConfigList();
            downloadRequestData.put(ID, drd);
            for (PlotConfig pc : plotConfigList.get(ID).values()) {
                pc.setVisible(drd.isVisible());
            }
            for (NoDataConfig ndc : noDataList.get(ID)) {
                ndc.setVisible(drd.isVisible());
            }

            fireChangeVisibility(ID, drd.isVisible(), drd.getPlotIdentifier());
        }
    }

    @Override
    public void newDataForIDReceived(byte[] data, Interval<Date> timeInterval, FrequencyInterval freqInterval, Rectangle area,
            Long downloadID, String identifier, Long radioImageID) {
        synchronized (this) {
            RadioPlotModelData rpmd = getRadioPlotModelData(identifier);
            Map<Long, DownloadRequestData> downloadRequestData = rpmd.getDownloadRequestData();
            Map<Long, Map<Long, PlotConfig>> plotConfigList = rpmd.getPlotConfigList();
            BufferedImage newImage = createBufferedImage(area.width, area.height, data);
            bufferedImages.put(radioImageID, newImage);
            rpmd.getRadioImagePane().setIntervalTooBig(false);
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
            DrawableAreaMap dam = zoomManager.getDrawableAreaMap(timeInterval.getStart(), timeInterval.getEnd(), freqInterval.getStart(),
                    freqInterval.getEnd(), area, downloadID, identifier);
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
        }
        fireDrawNewBufferedImage(identifier);
    }

    @Override
    public void clearAllSavedImagesForID(Long downloadID, Long imageID, String plotIdentifer) {
        synchronized (this) {
            Map<Long, Map<Long, PlotConfig>> plotConfigList = radioPlotModelData.get(plotIdentifer).getPlotConfigList();
            Map<Long, PlotConfig> plotConfigPerDID = plotConfigList.get(downloadID);
            if (plotConfigPerDID != null) {
                plotConfigPerDID.remove(imageID);
            }
        }
    }

    @Override
    public void intervalTooBig(long iD, String identifier) {
        RadioImagePane radioImagePane = getRadioPlotModelData(identifier).getRadioImagePane();
        Map<Long, PlotConfig> plotConfigList = new HashMap<Long, PlotConfig>();
        getRadioPlotModelData(identifier).getPlotConfigList().put(iD, plotConfigList);
        radioImagePane.setIntervalTooBig(true);
        drawController.updateDrawableElement(radioImagePane, identifier);
    }

    @Override
    public void noDataInterval(List<Interval<Date>> noDataList, Long downloadID, String plotIdentifier) {
        RadioPlotModelData rpmd = getRadioPlotModelData(plotIdentifier);
        RadioImagePane radioImagePane = rpmd.getRadioImagePane();
        radioImagePane.setIntervalTooBig(false);
        Map<Long, List<NoDataConfig>> noDataConfigList = rpmd.getNoDataConfigList();
        Map<Long, DownloadRequestData> downloadRequestData = rpmd.getDownloadRequestData();
        if (!noDataConfigList.containsKey(downloadID)) {
            noDataConfigList.put(downloadID, new ArrayList<NoDataConfig>());
        } else {
            // noDataConfigList.get(downloadID).clear();
        }
        for (Interval<Date> noData : noDataList) {
            DrawableAreaMap dam = zoomManager.getDrawableAreaMap(noData.getStart(), noData.getEnd(), downloadID, plotIdentifier);
            noDataConfigList.get(downloadID)
                    .add(new NoDataConfig(noData, dam, downloadID, downloadRequestData.get(downloadID).isVisible()));
        }
        fireDrawNewBufferedImage(plotIdentifier);
    }

    @Override
    public void frequencyIntervalUpdated(String plotIdentifier, FrequencyInterval maxFrequencyInterval) {
        YValueModel yValueModel = yValueModelManager.getYValueModel(plotIdentifier);
        yValueModel.setAvailableYMin(maxFrequencyInterval.getStart());
        yValueModel.setAvailableYMax(maxFrequencyInterval.getEnd());
    }

    /*
     * ZoomDataConfigListener
     */

    @Override
    public void requestData(Date xStart, Date xEnd, double yStart, double yEnd, double xRatio, double yRatio, long ID, String plotIdentifier) {
        // Log.debug("Request for data in : " + xStart + " - " + xEnd);
        // Thread.dumpStack();
        EventQueue.invokeLater(new Runnable() {

            Date xStart;
            Date xEnd;
            double yStart;
            double yEnd;
            double xRatio;
            double yRatio;
            Long ID;
            String plotIdentifier;

            @Override
            public void run() {
                long start = System.currentTimeMillis();
                List<Long> idList = new ArrayList<Long>();
                idList.add(ID);
                radioDataManager.requestForData(xStart, xEnd, yStart, yEnd, xRatio, yRatio, idList, plotIdentifier);
                Log.debug("requestData time:" + (System.currentTimeMillis() - start));
            }

            public Runnable init(Date xStart, Date xEnd, double yStart, double yEnd, double ratioX, double ratioY, Long ID,
                    String plotIdentifier) {
                this.xStart = xStart;
                this.xEnd = xEnd;
                this.yStart = yStart;
                this.yEnd = yEnd;
                xRatio = ratioX;
                yRatio = ratioY;
                this.ID = ID;
                this.plotIdentifier = plotIdentifier;
                return this;
            }

        }.init(xStart, xEnd, yStart, yEnd, xRatio, yRatio, ID, plotIdentifier));
        counter++;
        updateNoDataConfig(ID, plotIdentifier);
    }

    private void fireDrawNewBufferedImage(String identifier) {// BufferedImage
        RadioPlotModelData rpmd = getRadioPlotModelData(identifier);
        RadioImagePane radioImagePane = rpmd.getRadioImagePane();
        YAxisElement yAxisElement = rpmd.getyAxisElement();
        radioImagePane.setYAxisElement(yAxisElement);
        drawController.updateDrawableElement(radioImagePane, identifier);
    }

    private BufferedImage createBufferedImage(int width, int height, byte[] data) {
        byte[] useData = new byte[0];
        if (width * height == data.length) {
            useData = data;
        } else {
            Log.error("Data array was to small created white image");
            useData = new byte[width * height];
        }
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        DataBufferByte dataBuffer = new DataBufferByte(useData, width * height);
        useData = new byte[0];
        Raster raster = Raster.createPackedRaster(dataBuffer, width, height, width, new int[] { 0xff }, new Point(0, 0));
        newImage.setData(raster);
        return newImage;
    }

    private void fireRemoveRadioImage(long ID, String plotIdentifier) {
        synchronized (this) {
            Set<RadioPlotModelListener> listeners = getRadioPlotModelData(plotIdentifier).getListeners();
            for (RadioPlotModelListener l : listeners) {
                l.removeDownloadRequestData(ID);
            }
        }
    }

    private void fireChangeVisibility(long ID, boolean visible, String identifier) {
        Set<RadioPlotModelListener> listeners = getRadioPlotModelData(identifier).getListeners();
        for (RadioPlotModelListener l : listeners) {
            l.changeVisibility(ID);
        }
    }

    private RadioPlotModelData getRadioPlotModelData(String identifier) {
        if (!radioPlotModelData.containsKey(identifier)) {
            radioPlotModelData.put(identifier, new RadioPlotModelData(identifier));
        }
        return radioPlotModelData.get(identifier);
    }

    private void updateNoDataConfig(Long downloadID, String plotIdentifier) {
        RadioPlotModelData rpmd = getRadioPlotModelData(plotIdentifier);
        Map<Long, List<NoDataConfig>> noDataConfigList = rpmd.getNoDataConfigList();
        if (!noDataConfigList.containsKey(downloadID)) {
            noDataConfigList.put(downloadID, new ArrayList<NoDataConfig>());
        } else {
            // noDataConfigList.get(downloadID).clear();
        }
        for (NoDataConfig ndc : noDataConfigList.get(downloadID)) {
            DrawableAreaMap dam = zoomManager.getDrawableAreaMap(ndc.getDateInterval().getStart(), ndc.getDateInterval().getEnd(),
                    downloadID, plotIdentifier);
            ndc.setDrawableAreaMap(dam);
        }
        fireDrawNewBufferedImage(plotIdentifier);
    }

}
