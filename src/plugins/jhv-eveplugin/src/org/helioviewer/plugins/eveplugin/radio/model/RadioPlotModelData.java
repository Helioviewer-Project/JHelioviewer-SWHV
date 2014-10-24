package org.helioviewer.plugins.eveplugin.radio.model;

import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.helioviewer.plugins.eveplugin.draw.YAxisElement;
import org.helioviewer.plugins.eveplugin.radio.data.DownloadRequestData;
import org.helioviewer.plugins.eveplugin.radio.gui.RadioImagePane;

/**
 * Collects per plot identifier some data needed by the radio plot model.
 * 
 * @author Bram.Bourgoignie@oma.be
 */
public class RadioPlotModelData {
    private YAxisElement yAxisElement;
    private RadioImagePane radioImagePane;
    private Set<RadioPlotModelListener> listeners;
    private Map<Long, DownloadRequestData> downloadRequestData;
    private Map<Long, Map<Long, PlotConfig>> plotConfigList;
    /** Map containing per download id a list of no data configurations */
    private Map<Long, List<NoDataConfig>> noDataConfigList;

    public RadioPlotModelData(String plotIdentifier) {
        yAxisElement = new RadioYAxisElement(plotIdentifier);
        yAxisElement.setColor(Color.BLACK);
        yAxisElement.setLabel("Mhz");
        yAxisElement.setIsLogScale(false);
        radioImagePane = new RadioImagePane(plotIdentifier);
        radioImagePane.setYAxisElement(yAxisElement);
        downloadRequestData = new HashMap<Long, DownloadRequestData>();
        plotConfigList = new HashMap<Long, Map<Long, PlotConfig>>();
        noDataConfigList = new HashMap<Long, List<NoDataConfig>>();
        listeners = new HashSet<RadioPlotModelListener>();
    }

    public void addRadioPlotModelListener(RadioPlotModelListener listener) {
        listeners.add(listener);
    }

    public void removeRadioPlotModelListener(RadioPlotModelListener listener) {
        listeners.remove(listener);
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

    /**
     * Gives the no data configuration list.
     * 
     * @return Map containing per download id a list of no data configurations
     */
    public Map<Long, List<NoDataConfig>> getNoDataConfigList() {
        return noDataConfigList;
    }

    /**
     * Sets the no data configuration list.
     * 
     * @param noDataConfigList
     *            The no data configuration list
     */
    public void setNoDataConfigList(Map<Long, List<NoDataConfig>> noDataConfigList) {
        this.noDataConfigList = noDataConfigList;
    }

}
