package org.helioviewer.plugins.eveplugin.radio.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;

public class RadioImageCache {

    private final int CACHE_SIZE = 3;
    private final List<RadioImageCacheListener> listeners;
    private static RadioImageCache instance;
    private long cacheCounter;
    private final Map<Long, DownloadedJPXData> dataCache;
    private final NavigableMap<Long, DownloadedJPXData> useCache;
    private final Map<DownloadedJPXData, Long> reverseUseCache;
    private final Map<Date, DownloadedJPXData> startDates;
    private final Map<Date, Interval<Date>> noDataCache;

    private RadioImageCache() {
        listeners = new ArrayList<RadioImageCacheListener>();
        cacheCounter = 0;
        dataCache = new HashMap<Long, DownloadedJPXData>();
        useCache = new TreeMap<Long, DownloadedJPXData>();
        startDates = new HashMap<Date, DownloadedJPXData>();
        noDataCache = new HashMap<Date, Interval<Date>>();
        reverseUseCache = new HashMap<DownloadedJPXData, Long>();
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
        if (dataCache.size() > CACHE_SIZE) {
            Long key = useCache.firstKey();
            DownloadedJPXData oldestJPXData = useCache.get(key);
            dataCache.remove(oldestJPXData.getImageID());
            startDates.remove(oldestJPXData.getStartDate());
            useCache.remove(key);
            reverseUseCache.remove(oldestJPXData);
            oldestJPXData.remove();
            Log.debug("remove oldest jpx data " + oldestJPXData + " imageID " + oldestJPXData.getImageID());
        }

        dataCache.put(jpxData.getImageID(), jpxData);
        useCache.put(cacheCounter, jpxData);
        reverseUseCache.put(jpxData, cacheCounter);
        startDates.put(jpxData.getStartDate(), jpxData);
        noDataCache.remove(jpxData.getStartDate());
        cacheCounter++;
    }

    public void remove(DownloadedJPXData jpxData) {
        remove(jpxData.getImageID());
    }

    public void remove(Long ID) {
        if (dataCache.containsKey(ID)) {
            DownloadedJPXData data = dataCache.get(ID);
            Log.debug("Data for ID : " + data);
            dataCache.remove(ID);
            useCache.remove(reverseUseCache.get(data));
            reverseUseCache.remove(data);
            startDates.remove(data.getStartDate());
            data.remove();
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

    public RadioImageCacheResult getRadioImageCacheResultForInterval(Date start, Date end, Long stepsize) {
        Date localStart = findStartDate(start, stepsize);
        List<Interval<Date>> intervalList = new ArrayList<Interval<Date>>();
        List<DownloadedJPXData> dataList = new ArrayList<DownloadedJPXData>();
        List<Long> toRemove = new ArrayList<Long>(dataCache.keySet());
        List<Interval<Date>> noDataInterval = new ArrayList<Interval<Date>>();
        while (localStart.before(end) || localStart.equals(end)) {
            if (!startDates.containsKey(localStart) && !noDataCache.containsKey(localStart)) {
                intervalList.add(new Interval<Date>(localStart, new Date(localStart.getTime() + stepsize)));
            } else {
                if (startDates.containsKey(localStart)) {
                    DownloadedJPXData tempData = startDates.get(localStart);
                    useCache.remove(reverseUseCache.get(tempData));
                    useCache.put(cacheCounter, tempData);
                    reverseUseCache.put(tempData, cacheCounter);
                    dataList.add(tempData);
                    toRemove.remove(tempData.getImageID());
                    cacheCounter++;
                }
            }
            if (noDataCache.containsKey(localStart)) {
                noDataInterval.add(noDataCache.get(localStart));
            }
            localStart = new Date(localStart.getTime() + stepsize);
        }
        return new RadioImageCacheResult(dataList, intervalList, new ArrayList<Long>(toRemove), noDataInterval);
    }

    public boolean containsDate(Date date) {
        return startDates.containsKey(date);
    }

    public boolean addNoDataInterval(Interval<Date> interval) {
        boolean added = noDataCache.containsKey(interval.getStart());
        noDataCache.put(interval.getStart(), interval);
        return !added;
    }
}
