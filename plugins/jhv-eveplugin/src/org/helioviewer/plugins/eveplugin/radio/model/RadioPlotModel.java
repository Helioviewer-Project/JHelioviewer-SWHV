package org.helioviewer.plugins.eveplugin.radio.model;

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
import org.helioviewer.plugins.eveplugin.base.Range;
import org.helioviewer.plugins.eveplugin.controller.DrawController;
import org.helioviewer.plugins.eveplugin.controller.ZoomController;
import org.helioviewer.plugins.eveplugin.controller.ZoomControllerListener;
import org.helioviewer.plugins.eveplugin.draw.YAxisElement;
import org.helioviewer.plugins.eveplugin.model.ChartModel;
import org.helioviewer.plugins.eveplugin.model.PlotAreaSpace;
import org.helioviewer.plugins.eveplugin.model.PlotAreaSpaceManager;
import org.helioviewer.plugins.eveplugin.radio.data.DownloadRequestData;
import org.helioviewer.plugins.eveplugin.radio.data.FrequencyInterval;
import org.helioviewer.plugins.eveplugin.radio.data.RadioDataManager;
import org.helioviewer.plugins.eveplugin.radio.data.RadioDataManagerListener;
import org.helioviewer.plugins.eveplugin.radio.gui.RadioImagePane;
import org.helioviewer.plugins.eveplugin.settings.EVEAPI.API_RESOLUTION_AVERAGES;

public class RadioPlotModel implements RadioDataManagerListener, ZoomControllerListener,// EVEDrawControllerListener,
        ZoomDataConfigListener {
    private static RadioPlotModel instance;
    private RadioDataManager radioDataManager;
    private ZoomManager zoomManager;
    private DrawController drawController;
    private Map<Long, BufferedImage> bufferedImages;
    private ChartModel chartModel;

    private Map<String, RadioPlotModelData> radioPlotModelData;

    private RadioPlotModel() {
        ZoomController.getSingletonInstance().addZoomControllerListener(this);
        radioDataManager = RadioDataManager.getSingletonInstance();
        this.radioDataManager.addRadioManagerListener(this);
        this.zoomManager = ZoomManager.getSingletonInstance();
        drawController = DrawController.getSingletonInstance();
        bufferedImages = new HashMap<Long, BufferedImage>();
        chartModel = ChartModel.getSingletonInstance();
        radioPlotModelData = new HashMap<String, RadioPlotModelData>();

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

    public void newRequestForData(Date startTime, Date endTime) {

    }

    @Override
    public void newDataAvailable(DownloadRequestData data, long ID) {
        synchronized (this) {
            getRadioPlotModelData(data.getPlotIdentifier()).getDownloadRequestData().put(ID, data);
            Map<Long, PlotConfig> plotConfigList = new HashMap<Long, PlotConfig>();
            getRadioPlotModelData(data.getPlotIdentifier()).getPlotConfigList().put(ID, plotConfigList);
        }
    }

    @Override
    public void downloadFinished(long ID) {
        // TODO Auto-generated method stub

    }

    @Override
    public void availableIntervalChanged(Interval<Date> newInterval) {
        Log.debug("Available interval changed : " + newInterval.toString());
        // radioDataManager.setVisibleParameters(newInterval, freqInterval,
        // areaAvailable);
    }

    @Override
    public void selectedIntervalChanged(Interval<Date> newInterval) {
        Log.debug("Selected interval changed : " + newInterval.toString());
    }

    @Override
    public void selectedResolutionChanged(API_RESOLUTION_AVERAGES newResolution) {
        Log.debug("Selected resolution changed : " + newResolution);
    }

    @Override
    public void dataNotChanged(Interval<Date> timeInterval, FrequencyInterval freqInterval, Rectangle area, List<Long> downloadIDList, String identifier, long radioImageID) {
        RadioPlotModelData rpmd = getRadioPlotModelData(identifier);
        YAxisElement yAxisElement = rpmd.getyAxisElement();
        Map<Long, Map<Long, PlotConfig>> plotConfigList = rpmd.getPlotConfigList();
        Map<Long, DownloadRequestData> downloadRequestData = rpmd.getDownloadRequestData();
        for (long ID : downloadIDList) {
            synchronized (this) {
                BufferedImage newImage = bufferedImages.get(radioImageID);
                DrawableAreaMap dam = zoomManager.getDrawableAreaMap(timeInterval.getStart(), timeInterval.getEnd(), freqInterval.getStart(), freqInterval.getEnd(), area, ID, identifier);
                Range selectedRange = defineSelectedRange(freqInterval.getStart(), freqInterval.getEnd(), identifier);
                yAxisElement.setMinValue(selectedRange.min);
                yAxisElement.setMaxValue(selectedRange.max);
                yAxisElement.setAvailableRange(new Range(freqInterval.getStart(), freqInterval.getEnd()));
                yAxisElement.setSelectedRange(selectedRange);
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

    private Range defineSelectedRange(int start, int end, String identifier) {
        PlotAreaSpaceManager manager = PlotAreaSpaceManager.getInstance();
        PlotAreaSpace plotAreaSpace = manager.getPlotAreaSpace(identifier);
        double ratioAvailable = 1.0 * (end - start) / (plotAreaSpace.getScaledMaxValue() - plotAreaSpace.getScaledMinValue());
        double selectedMinY = 1.0 * start + (plotAreaSpace.getScaledSelectedMinValue() - plotAreaSpace.getScaledMinValue()) * ratioAvailable;
        double selectedMaxY = 1.0 * start + (plotAreaSpace.getScaledSelectedMaxValue() - plotAreaSpace.getScaledMinValue()) * ratioAvailable;
        return new Range(selectedMinY, selectedMaxY);
    }

    @Override
    public void newGlobalFrequencyInterval(FrequencyInterval interval) {}

    @Override
    public void requestData(Date xStart, Date xEnd, double yStart, double yEnd, double xRatio, double yRatio, long ID, String plotIdentifier) {
        Thread t = new Thread(new Runnable() {

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
                List<Long> idList = new ArrayList<Long>();
                idList.add(ID);
                radioDataManager.requestForData(xStart, xEnd, yStart, yEnd, xRatio, yRatio, idList, plotIdentifier);
            }

            public Runnable init(Date xStart, Date xEnd, double yStart, double yEnd, double ratioX, double ratioY, Long ID, String plotIdentifier) {
                this.xStart = xStart;
                this.xEnd = xEnd;
                this.yStart = yStart;
                this.yEnd = yEnd;
                this.xRatio = ratioX;
                this.yRatio = ratioY;
                this.ID = ID;
                this.plotIdentifier = plotIdentifier;
                return this;
            }

        }.init(xStart, xEnd, yStart, yEnd, xRatio, yRatio, ID, plotIdentifier));

        t.start();
    }

    @Override
    public void downloadRequestAnswered(FrequencyInterval freqInterval, Interval<Date> timeInterval, long ID, String identifier) {
        zoomManager.addZoomDataConfig(freqInterval, timeInterval, this, ID, identifier);
    }

    @Override
    public void newDataReceived(byte[] data, Interval<Date> timeInterval, FrequencyInterval freqInterval, Rectangle area, List<Long> IDList, String identifier, Long radioImageID) {
        Log.debug("Size of buffered images: " + bufferedImages.size());
    }

    private void fireDrawNewBufferedImage(String identifier) {// BufferedImage
        RadioPlotModelData rpmd = getRadioPlotModelData(identifier);
        RadioImagePane radioImagePane = rpmd.getRadioImagePane();
        YAxisElement yAxisElement = rpmd.getyAxisElement();
        radioImagePane.setYAxisElement(yAxisElement);
        drawController.updateDrawableElement(radioImagePane, identifier);
        chartModel.chartRedrawRequest();
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
            return tempAllConfig;
        }
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
            plotConfigList.remove(ID);
            downloadRequestData.remove(ID);
            drawController.removeDrawableElement(radioImagePane, drd.getPlotIdentifier());
            for (Long imageID : drd.getRadioImages().keySet()) {
                bufferedImages.remove(imageID);
            }
            fireRemoveRadioImage(ID, drd.getPlotIdentifier());
        }
    }

    private void fireRemoveRadioImage(long ID, String plotIdentifier) {
        synchronized (this) {
            Set<RadioPlotModelListener> listeners = getRadioPlotModelData(plotIdentifier).getListeners();
            for (RadioPlotModelListener l : listeners) {
                l.removeDownloadRequestData(ID);
            }
            chartModel.chartRedrawRequest();
        }
    }

    @Override
    public void downloadRequestDataVisibilityChanged(DownloadRequestData drd, long ID) {
        synchronized (this) {
            RadioPlotModelData rpmd = getRadioPlotModelData(drd.getPlotIdentifier());
            Map<Long, DownloadRequestData> downloadRequestData = rpmd.getDownloadRequestData();
            Map<Long, Map<Long, PlotConfig>> plotConfigList = rpmd.getPlotConfigList();
            downloadRequestData.put(ID, drd);
            for (PlotConfig pc : plotConfigList.get(ID).values()) {
                pc.setVisible(drd.isVisible());
            }
            fireChangeVisibility(ID, drd.isVisible(), drd.getPlotIdentifier());
        }
    }

    private void fireChangeVisibility(long ID, boolean visible, String identifier) {
        Set<RadioPlotModelListener> listeners = getRadioPlotModelData(identifier).getListeners();
        for (RadioPlotModelListener l : listeners) {
            l.changeVisibility(ID);
        }
        chartModel.chartRedrawRequest();
    }

    @Override
    public void newDataForIDReceived(byte[] data, Interval<Date> timeInterval, FrequencyInterval freqInterval, Rectangle area, Long downloadID, String identifier, Long radioImageID) {
        synchronized (this) {
            RadioPlotModelData rpmd = getRadioPlotModelData(identifier);
            YAxisElement yAxisElement = rpmd.getyAxisElement();
            Map<Long, DownloadRequestData> downloadRequestData = rpmd.getDownloadRequestData();
            Map<Long, Map<Long, PlotConfig>> plotConfigList = rpmd.getPlotConfigList();
            BufferedImage newImage = createBufferedImage(area.width, area.height, data);
            bufferedImages.put(radioImageID, newImage);
            rpmd.getRadioImagePane().setIntervalTooBig(false);
            Log.trace("+===============================================================+");
            Log.trace("buffered images size : " + bufferedImages.size());
            Log.trace("Size of the data : "+ data.length);
            Log.trace("timeinterval start : " + timeInterval.getStart());
            Log.trace("timeinterval end : " + timeInterval.getEnd());
            Log.trace("freqinterval start : " + freqInterval.getStart());
            Log.trace("freqinterval end : " + freqInterval.getEnd());
            Log.trace("Area : " + area);
            Log.trace("DownloadID : "+ downloadID);
            Log.trace("Identifier : " + identifier);
            DrawableAreaMap dam = zoomManager.getDrawableAreaMap(timeInterval.getStart(), timeInterval.getEnd(), freqInterval.getStart(), freqInterval.getEnd(), area, downloadID, identifier);
            Log.trace("Drawable Area Map : " + dam);
            Log.trace("-===============================================================-");
            //Range selectedRange = defineSelectedRange(freqInterval.getStart(), freqInterval.getEnd(), identifier);
            yAxisElement.setMinValue(freqInterval.getStart());
            yAxisElement.setMaxValue(freqInterval.getEnd());
            //yAxisElement.setAvailableRange(new Range(freqInterval.getStart(), freqInterval.getEnd()));
            //yAxisElement.setSelectedRange(selectedRange);
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

    private RadioPlotModelData getRadioPlotModelData(String identifier) {
        if (!radioPlotModelData.containsKey(identifier)) {
            radioPlotModelData.put(identifier, new RadioPlotModelData(identifier));
        }
        return radioPlotModelData.get(identifier);
    }

    @Override
    public void additionDownloadRequestAnswered(Long downloadID) {}

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

}
