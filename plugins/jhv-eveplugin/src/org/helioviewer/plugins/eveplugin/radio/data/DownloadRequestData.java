package org.helioviewer.plugins.eveplugin.radio.data;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;

public class DownloadRequestData implements LineDataSelectorElement {

    private Map<Long, RadioImage> radioImages;
    private Long downloadID;
    private boolean isDownloading;
    private String plotIdentifier = "plot.identifier.master";
    private boolean isVisible;

    private RadioDataManager radioDataManager;

    public DownloadRequestData(long ID, String plotIdentifier) {
        this.radioDataManager = RadioDataManager.getSingletonInstance();
        this.downloadID = ID;
        this.radioImages = new HashMap<Long, RadioImage>();
        this.plotIdentifier = plotIdentifier;
        this.isVisible = true;
    }

    public DownloadRequestData(long ID, Map<Long, RadioImage> radioImages, String plotIdentifier) {
        this.downloadID = ID;
        this.radioImages = radioImages;
        this.plotIdentifier = plotIdentifier;
        this.isVisible = true;
    }

    public void addRadioImage(RadioImage radioImage) {
        this.radioImages.put(radioImage.getRadioImageID(), radioImage);
    }

    public Map<Long, RadioImage> getRadioImages() {
        return radioImages;
    }

    public void setRadioImages(Map<Long, RadioImage> radioImages) {
        this.radioImages = radioImages;
    }

    public Long getDownloadID() {
        return downloadID;
    }

    public void setDownloadID(Long iD) {
        downloadID = iD;
    }

    @Override
    public void removeLineData() {
        radioDataManager.removeDownloadRequestData(this);

    }

    @Override
    public void setVisibility(boolean visible) {
        this.isVisible = visible;
        radioDataManager.downloadRequestDataVisibilityChanged(this);

    }

    @Override
    public boolean isVisible() {
        return this.isVisible;
    }

    @Override
    public String getName() {
        return "Callisto radiogram";
    }

    @Override
    public Color getDataColor() {
        return Color.black;
    }

    @Override
    public void setDataColor(Color c) {
    }

    @Override
    public boolean isDownloading() {
        return isDownloading;
    }

    @Override
    public String getPlotIdentifier() {
        return plotIdentifier;
    }

    @Override
    public void setPlotIndentifier(String plotIdentifier) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isAvailable() {
        // TODO Auto-generated method stub
        return true;
    }

    public void setDownloading(boolean isDownloading) {
        this.isDownloading = isDownloading;
    }

    @Override
    public String getUnitLabel() {
        // TODO Auto-generated method stub
        return "Don't know yet";
    }

    /**
     * Merges the download request data in this download request data. If the
     * plot identifier and the download identifier are the same, the extra radio
     * images are added to the radio images of this download request data. The
     * downloading status and visibility status of this download request data is
     * kept.
     * 
     * @param downloadRequestData
     *            The download request data to be merged in this
     */
    public void mergeDownloadRequestData(DownloadRequestData downloadRequestData) {
        if (downloadRequestData.getDownloadID() == downloadID && downloadRequestData.getPlotIdentifier() == plotIdentifier) {
            radioImages.putAll(downloadRequestData.getRadioImages());
        }
    }
}
