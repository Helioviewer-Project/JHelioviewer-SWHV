package org.helioviewer.jhv.plugins.eveplugin.radio.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.io.APIRequestManager;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.plugins.eveplugin.settings.EVESettings;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2ViewCallisto;

public class RadioDownloader {

    private static RadioDownloader instance;
    private final HashMap<Long, DownloadedJPXData> requestDateCache;
    private static final String ROBserver = DataSources.ROBsettings.get("API.jp2images.path");
    public static final int MAX_AMOUNT_OF_DAYS = 3;
    public static final int DAYS_IN_CACHE = MAX_AMOUNT_OF_DAYS + 1;

    private RadioDownloader() {
        requestDateCache = new HashMap<Long, DownloadedJPXData>();
    }

    public static RadioDownloader getSingletonInstance() {
        if (instance == null) {
            instance = new RadioDownloader();
        }
        return instance;
    }

    public void clearCache() {
        for (DownloadedJPXData jpxData : requestDateCache.values()) {
            if (jpxData.isInited())
                jpxData.remove();
        }
        requestDateCache.clear();
    }

    public HashMap<Long, DownloadedJPXData> getCache() {
        return requestDateCache;
    }

    public void requestAndOpenIntervals(long start, long end) {
        final ArrayList<Long> toDownloadStartDates = new ArrayList<Long>();
        long startDate = start - start % TimeUtils.DAY_IN_MILLIS;

        ArrayList<Long> incomingStartDates = new ArrayList<Long>(DAYS_IN_CACHE);
        for (int i = 0; i < DAYS_IN_CACHE; i++) {
            incomingStartDates.add(startDate + i * TimeUtils.DAY_IN_MILLIS);
        }

        Iterator<Entry<Long, DownloadedJPXData>> it = requestDateCache.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Long, DownloadedJPXData> entry = it.next();
            Long key = entry.getKey();
            if (!incomingStartDates.contains(key)) {
                DownloadedJPXData jpxData = entry.getValue();
                if (jpxData.isInited()) {
                    jpxData.remove();
                }
                it.remove();
            }
        }

        for (long incomingStart : incomingStartDates) {
            if (!requestDateCache.containsKey(incomingStart)) {
                toDownloadStartDates.add(incomingStart);
                requestDateCache.put(incomingStart, new DownloadedJPXData(incomingStart, incomingStart + TimeUtils.DAY_IN_MILLIS));
            }
        }

        if (!toDownloadStartDates.isEmpty()) {
            LineDataSelectorModel.getSingletonInstance().downloadStarted(RadioDataManager.getSingletonInstance());
            JHVWorker<ArrayList<JP2ViewCallisto>, Void> imageDownloadWorker = new RadioJPXDownload().init(toDownloadStartDates);
            imageDownloadWorker.setThreadName("EVE--RadioDownloader");
            EVESettings.getExecutorService().execute(imageDownloadWorker);
        }

        Iterator<Entry<Long, DownloadedJPXData>> itt = requestDateCache.entrySet().iterator();

    }

    public void initJPX(ArrayList<JP2ViewCallisto> jpList, ArrayList<Long> datesToDownload) {
        for (int i = 0; i < jpList.size(); i++) {
            JP2ViewCallisto v = jpList.get(i);
            long date = datesToDownload.get(i);
            DownloadedJPXData jpxData = requestDateCache.get(date);
            if (v != null) {
                if (jpxData != null) {
                    jpxData.init(v);
                } else {
                    v.abolish();
                }
            }
            else {
                jpxData.downloadJPXFailed();
            }
        }
    }

    private static class RadioJPXDownload extends JHVWorker<ArrayList<JP2ViewCallisto>, Void> {

        private ArrayList<Long> datesToDownload;

        public RadioJPXDownload init(ArrayList<Long> toDownload) {
            datesToDownload = toDownload;
            return this;
        }

        @Override
        protected ArrayList<JP2ViewCallisto> backgroundWork() {
            ArrayList<JP2ViewCallisto> jpList = new ArrayList<JP2ViewCallisto>();
            for (int i = 0; i < datesToDownload.size(); i++) {
                long date = datesToDownload.get(i);

                JP2ViewCallisto v = null;
                try {
                    v = (JP2ViewCallisto) APIRequestManager.requestAndOpenRemoteFile(ROBserver, null, TimeUtils.apiDateFormat.format(date), TimeUtils.apiDateFormat.format(date), "ROB-Humain", "CALLISTO", "CALLISTO", "RADIOGRAM", false);
                } catch (IOException e) {
                    Log.error("An error occured while opening the remote file!", e);
                }
                jpList.add(v);
            }

            return jpList;
        }

        @Override
        protected void done() {
            try {
                ArrayList<JP2ViewCallisto> jpList = get();
                RadioDownloader.getSingletonInstance().initJPX(jpList, datesToDownload);

            } catch (InterruptedException e) {
                Log.error("ImageDownloadWorker execution interrupted: " + e.getMessage());
            } catch (ExecutionException e) {
                Log.error("ImageDownloadWorker execution error: " + e.getMessage());
            }
        }

    }
}