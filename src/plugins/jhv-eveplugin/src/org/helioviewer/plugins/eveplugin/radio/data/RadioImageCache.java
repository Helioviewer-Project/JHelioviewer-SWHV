package org.helioviewer.plugins.eveplugin.radio.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;

public class RadioImageCache {
    private final Map<String, RadioImageCacheData> radioImageCacheData;

    private final List<RadioImageCacheListener> listeners;

    private static RadioImageCache instance;

    private RadioImageCache() {
        listeners = new ArrayList<RadioImageCacheListener>();
        radioImageCacheData = new HashMap<String, RadioImageCacheData>();
    }

    public void addRadioImageCacheListener(RadioImageCacheListener listener) {
        listeners.add(listener);
    }

    public void removeRadioImageListener(RadioImageCacheListener listener) {
        listeners.remove(listener);
    }

    public static RadioImageCache getInstance() {
        if (instance == null) {
            instance = new RadioImageCache();
        }
        return instance;
    }

    public void add(DownloadedJPXData jpxData) {
        Log.trace("Try to add data in cache");
        synchronized (instance) {
            RadioImageCacheData data = getRadioImageCache(jpxData.getPlotIdentifier());
            data.getDataCache().put(jpxData.getImageID(), jpxData);
            data.getUseCache().put(jpxData.getImageID(), 0L);
            data.getStartDates().put(jpxData.getStartDate(), jpxData);
            data.getNoDataCache().remove(jpxData.getStartDate());
        }
    }

    public void remove(DownloadedJPXData jpxData) {
        remove(jpxData.getImageID(), jpxData.getPlotIdentifier());
    }

    public void remove(Long ID, String plotIdentifier) {
        synchronized (instance) {
            if (radioImageCacheData.containsKey(plotIdentifier)) {
                RadioImageCacheData cacheData = radioImageCacheData.get(plotIdentifier);
                DownloadedJPXData data = cacheData.getDataCache().get(ID);
                cacheData.getDataCache().remove(ID);
                cacheData.getUseCache().remove(ID);
                cacheData.getStartDates().remove(data.getStartDate());
                data.remove();
            }
        }
    }

    private Date findStartDate(Date start, Long stepsize) {
        long divider = 1L;
        if (stepsize < 1000L) {
            divider = 1000L;
        } else if (stepsize < 60L * 1000) {
            divider = 60L * 1000;
        } else if (stepsize < 60L * 60 * 1000) {
            divider = 60L * 60 * 1000;
        } else {
            divider = 24L * 60 * 60 * 1000;
        }
        return new Date(start.getTime() - start.getTime() % divider);
    }

    public RadioImageCacheResult getRadioImageCacheResultForInterval(Date start, Date end, Long stepsize, String plotIdentifier) {
        if (radioImageCacheData.containsKey(plotIdentifier)) {
            RadioImageCacheData cacheData = radioImageCacheData.get(plotIdentifier);
            Date localStart = findStartDate(start, stepsize);
            List<Interval<Date>> intervalList = new ArrayList<Interval<Date>>();
            List<DownloadedJPXData> dataList = new ArrayList<DownloadedJPXData>();
            List<Long> toRemove = new ArrayList<Long>(cacheData.getDataCache().keySet());
            List<Interval<Date>> noDataInterval = new ArrayList<Interval<Date>>();
            while (localStart.before(end) || localStart.equals(end)) {
                if (!cacheData.getStartDates().containsKey(localStart)) {
                    intervalList.add(new Interval<Date>(localStart, new Date(localStart.getTime() + stepsize)));
                } else {
                    dataList.add(cacheData.getStartDates().get(localStart));
                    toRemove.remove(cacheData.getStartDates().get(localStart).getImageID());
                }
                if (cacheData.getNoDataCache().containsKey(localStart)) {
                    noDataInterval.add(cacheData.getNoDataCache().get(localStart));
                }
                localStart = new Date(localStart.getTime() + stepsize);
            }
            return new RadioImageCacheResult(dataList, intervalList, new ArrayList<Long>(toRemove), noDataInterval);
        } else {
            Date localStart = findStartDate(start, stepsize);
            List<Interval<Date>> intervalList = new ArrayList<Interval<Date>>();
            List<DownloadedJPXData> dataList = new ArrayList<DownloadedJPXData>();
            List<Long> toRemove = new ArrayList<Long>();
            while (localStart.before(end) || localStart.equals(end)) {
                intervalList.add(new Interval<Date>(localStart, new Date(localStart.getTime() + stepsize)));
                localStart = new Date(localStart.getTime() + stepsize);
            }
            return new RadioImageCacheResult(dataList, intervalList, new ArrayList<Long>(toRemove), new ArrayList<Interval<Date>>());
        }
    }

    public boolean containsDate(Date date, String plotIdentifier) {
        if (radioImageCacheData.containsKey(plotIdentifier)) {
            RadioImageCacheData cacheData = radioImageCacheData.get(plotIdentifier);
            return cacheData.getStartDates().containsKey(date);
        } else {
            return false;
        }
    }

    public boolean addNoDataInterval(Interval<Date> interval, String plotIdentifier) {
        synchronized (instance) {
            RadioImageCacheData data = getRadioImageCache(plotIdentifier);
            boolean added = data.getNoDataCache().containsKey(interval.getStart());
            data.getNoDataCache().put(interval.getStart(), interval);
            Log.trace("Added : " + !added + ". Size of noDataCache : " + data.getNoDataCache().size());
            return !added;
        }
    }

    private RadioImageCacheData getRadioImageCache(String plotIdentifier) {
        RadioImageCacheData data = new RadioImageCacheData();
        if (radioImageCacheData.containsKey(plotIdentifier)) {
            data = radioImageCacheData.get(plotIdentifier);
        } else {
            radioImageCacheData.put(plotIdentifier, data);
        }
        return data;
    }

}
