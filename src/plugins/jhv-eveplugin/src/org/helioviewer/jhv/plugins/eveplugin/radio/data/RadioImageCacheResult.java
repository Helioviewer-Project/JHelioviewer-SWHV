package org.helioviewer.jhv.plugins.eveplugin.radio.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.helioviewer.base.interval.Interval;

public class RadioImageCacheResult {
    private List<DownloadedJPXData> availableData;
    private List<Interval<Date>> missingInterval;
    private List<Long> toRemove;
    private List<Interval<Date>> noDataInterval;

    public RadioImageCacheResult(List<DownloadedJPXData> availableData, List<Interval<Date>> missingInterval, List<Long> toRemove, List<Interval<Date>> noDataInterval) {
        super();
        this.availableData = availableData;
        this.missingInterval = missingInterval;
        this.toRemove = toRemove;
        this.noDataInterval = noDataInterval;
    }

    public RadioImageCacheResult() {
        availableData = new ArrayList<DownloadedJPXData>();
        missingInterval = new ArrayList<Interval<Date>>();
        toRemove = new ArrayList<Long>();
    }

    public List<DownloadedJPXData> getAvailableData() {
        return availableData;
    }

    public void setAvailableData(List<DownloadedJPXData> availableData) {
        this.availableData = availableData;
    }

    public List<Interval<Date>> getMissingInterval() {
        return missingInterval;
    }

    public void setMissingIntervalt(List<Interval<Date>> missingIntervalt) {
        this.missingInterval = missingIntervalt;
    }

    public List<Long> getToRemove() {
        return toRemove;
    }

    public void setToRemove(List<Long> toRemove) {
        this.toRemove = toRemove;
    }

    public List<Interval<Date>> getNoDataInterval() {
        return noDataInterval;
    }

    public void setNoDataInterval(List<Interval<Date>> noDataInterval) {
        this.noDataInterval = noDataInterval;
    }   
}
