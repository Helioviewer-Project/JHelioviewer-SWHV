package org.helioviewer.plugins.eveplugin.radio.model;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.helioviewer.plugins.eveplugin.draw.YAxisElement;
import org.helioviewer.plugins.eveplugin.radio.data.DownloadRequestData;
import org.helioviewer.plugins.eveplugin.radio.gui.RadioImagePane;

public class RadioPlotModelData {
    private YAxisElement yAxisElement;
    private RadioImagePane radioImagePane;
    // private Map<Long, BufferedImage> bufferedImages;
    private Set<RadioPlotModelListener> listeners;
    private Map<Long, DownloadRequestData> downloadRequestData;
    private Map<Long, Map<Long, PlotConfig>> plotConfigList;

    public RadioPlotModelData(String plotIdentifier) {
        yAxisElement = new YAxisElement();
        yAxisElement.setColor(Color.BLACK);
        yAxisElement.setLabel("Mhz");
        radioImagePane = new RadioImagePane(plotIdentifier);
        radioImagePane.setYAxisElement(yAxisElement);
        this.downloadRequestData = new HashMap<Long, DownloadRequestData>();
        plotConfigList = new HashMap<Long, Map<Long, PlotConfig>>();
        listeners = new HashSet<RadioPlotModelListener>();
        // bufferedImages = new HashMap<Long, BufferedImage>();
    }

    public void addRadioPlotModelListener(RadioPlotModelListener listener) {
        this.listeners.add(listener);
    }

    public void removeRadioPlotModelListener(RadioPlotModelListener listener) {
        this.listeners.remove(listener);
    }

    public YAxisElement getyAxisElement() {
        return yAxisElement;
    }

    public void setyAxisElement(YAxisElement yAxisElement) {
        this.yAxisElement = yAxisElement;
    }

    public RadioImagePane getRadioImagePane() {
        return radioImagePane;
    }

    public void setRadioImagePane(RadioImagePane radioImagePane) {
        this.radioImagePane = radioImagePane;
    }

    /*
     * public Map<Long, BufferedImage> getBufferedImages() { return
     * bufferedImages; }
     * 
     * public void setBufferedImages(Map<Long, BufferedImage> bufferedImages) {
     * this.bufferedImages = bufferedImages; }
     */

    public Map<Long, DownloadRequestData> getDownloadRequestData() {
        return downloadRequestData;
    }

    public void setDownloadRequestData(Map<Long, DownloadRequestData> downloadRequestData) {
        this.downloadRequestData = downloadRequestData;
    }

    public Map<Long, Map<Long, PlotConfig>> getPlotConfigList() {
        return plotConfigList;
    }

    public void setPlotConfigList(Map<Long, Map<Long, PlotConfig>> plotConfigList) {
        this.plotConfigList = plotConfigList;
    }

    public Set<RadioPlotModelListener> getListeners() {
        return listeners;
    }

    public void setListeners(Set<RadioPlotModelListener> listeners) {
        this.listeners = listeners;
    }

}
