package org.helioviewer.plugins.eveplugin.radio.data;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.helioviewer.base.math.Interval;

public class RadioImageCacheData {

    private Map<Long, DownloadedJPXData> dataCache;
    private NavigableMap<Long, DownloadedJPXData> useCache;
    private Map<DownloadedJPXData, Long> reverseUseCache;
    private Map<Date, DownloadedJPXData> startDates;
    private Map<Date, Interval<Date>> noDataCache;

    public RadioImageCacheData() {
        dataCache = new HashMap<Long, DownloadedJPXData>();
        useCache = new TreeMap<Long, DownloadedJPXData>();
        startDates = new HashMap<Date, DownloadedJPXData>();
        noDataCache = new HashMap<Date, Interval<Date>>();
        reverseUseCache = new HashMap<DownloadedJPXData, Long>();
    }

    public Map<Long, DownloadedJPXData> getDataCache() {
        return dataCache;
    }

    public void setDataCache(Map<Long, DownloadedJPXData> dataCache) {
        this.dataCache = dataCache;
    }

    public NavigableMap<Long, DownloadedJPXData> getUseCache() {
        return useCache;
    }

    public void setUseCache(TreeMap<Long, DownloadedJPXData> useCache) {
        this.useCache = useCache;
    }

    public Map<Date, DownloadedJPXData> getStartDates() {
        return startDates;
    }

    public void setStartDates(Map<Date, DownloadedJPXData> startDates) {
        this.startDates = startDates;
    }

    public Map<Date, Interval<Date>> getNoDataCache() {
        return noDataCache;
    }

    public void setNoDataCache(Map<Date, Interval<Date>> noDataCache) {
        this.noDataCache = noDataCache;
    }

    public Map<DownloadedJPXData, Long> getReverseUseCache() {
        return reverseUseCache;
    }

    public void setReverseUseCache(Map<DownloadedJPXData, Long> reverseUseCache) {
        this.reverseUseCache = reverseUseCache;
    }

}
