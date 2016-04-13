package org.helioviewer.jhv.plugins.eveplugin.radio.data;

import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.base.interval.Interval;

public class RadioImageCacheResult {

    private List<DownloadedJPXData> availableData;
    private List<Interval> missingInterval;
    private List<Long> toRemove;
    private List<Interval> noDataInterval;

    public RadioImageCacheResult(List<DownloadedJPXData> availableData, List<Interval> missingInterval, List<Long> toRemove, List<Interval> noDataInterval) {
        super();
        this.availableData = availableData;
        this.missingInterval = missingInterval;
        this.toRemove = toRemove;
        this.noDataInterval = noDataInterval;
    }

    public RadioImageCacheResult() {
        availableData = new ArrayList<DownloadedJPXData>();
        missingInterval = new ArrayList<Interval>();
        toRemove = new ArrayList<Long>();
    }

    public List<DownloadedJPXData> getAvailableData() {
        return availableData;
    }

    public void setAvailableData(List<DownloadedJPXData> availableData) {
        this.availableData = availableData;
    }

    public List<Interval> getMissingInterval() {
        return missingInterval;
    }

    public void setMissingIntervalt(List<Interval> missingIntervalt) {
        this.missingInterval = missingIntervalt;
    }

    public List<Long> getToRemove() {
        return toRemove;
    }

    public void setToRemove(List<Long> toRemove) {
        this.toRemove = toRemove;
    }

    public List<Interval> getNoDataInterval() {
        return noDataInterval;
    }

    public void setNoDataInterval(List<Interval> noDataInterval) {
        this.noDataInterval = noDataInterval;
    }

}
