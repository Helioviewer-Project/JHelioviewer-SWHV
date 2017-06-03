package org.helioviewer.jhv.plugins.eve.radio;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.APIRequestManager;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.plugins.eve.EVEPlugin;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.draw.TimeAxis;
import org.helioviewer.jhv.timelines.draw.YAxis;
import org.helioviewer.jhv.timelines.view.linedataselector.AbstractTimelineRenderable;
import org.helioviewer.jhv.view.View;
import org.helioviewer.jhv.view.jp2view.JP2ViewCallisto;
import org.json.JSONObject;

public class RadioData extends AbstractTimelineRenderable {

    private static final YAxis yAxis = new YAxis(400, 20, "Mhz", false);

    private static final int MAX_AMOUNT_OF_DAYS = 3;
    private static final int DAYS_IN_CACHE = MAX_AMOUNT_OF_DAYS + 4;
    private static final HashMap<Long, DownloadedJPXData> cache = new HashMap<>(DAYS_IN_CACHE);

    private static RadioOptionsPanel optionsPanel;
    private static IndexColorModel colorModel;

    public RadioData() {
        isVisible = false;

        String cm = "Spectral";
        colorModel = createIndexColorModelFromLUT(LUT.get(cm));
        optionsPanel = new RadioOptionsPanel(cm);
    }

    public RadioData(JSONObject jo) {
        this();
    }

    @Override
    public void serialize(JSONObject jo) {
    }

    private static IndexColorModel createIndexColorModelFromLUT(LUT lut2) {
        int[] source = lut2.getLut8();
        return new IndexColorModel(8, source.length, source, 0, false, -1, DataBuffer.TYPE_BYTE);
    }

    static void setLUT(LUT lut) {
        colorModel = createIndexColorModelFromLUT(lut);
        for (DownloadedJPXData jp2Data : cache.values()) {
            jp2Data.changeColormap(colorModel);
        }
        DrawController.drawRequest();
    }

    static IndexColorModel getColorModel() {
        return colorModel;
    }

    private void clearCache() {
        for (DownloadedJPXData jp2Data : cache.values()) {
            jp2Data.removeData();
        }
        cache.clear();
        latest_cache_start = -1;
        latest_cache_end = -1;
    }

    private long latest_cache_start = -1;
    private long latest_cache_end = -1;

    private void requestAndOpenIntervals(long start, long end) {
        if (latest_cache_start != -1 && latest_cache_end != -1 && start >= latest_cache_start && end <= latest_cache_end) {
            return;
        }
        latest_cache_start = start - start % TimeUtils.DAY_IN_MILLIS - 2 * TimeUtils.DAY_IN_MILLIS;
        latest_cache_end = latest_cache_start + DAYS_IN_CACHE * TimeUtils.DAY_IN_MILLIS;

        ArrayList<Long> incomingStartDates = new ArrayList<>(DAYS_IN_CACHE);
        for (int i = 0; i < DAYS_IN_CACHE; i++) {
            incomingStartDates.add(latest_cache_start + i * TimeUtils.DAY_IN_MILLIS);
        }

        Iterator<Map.Entry<Long, DownloadedJPXData>> it = cache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, DownloadedJPXData> entry = it.next();
            Long key = entry.getKey();
            if (!incomingStartDates.contains(key)) {
                entry.getValue().removeData();
                it.remove();
            }
        }

        ArrayList<Long> toDownloadStartDates = new ArrayList<>();
        for (long incomingStart : incomingStartDates) {
            if (!cache.containsKey(incomingStart)) {
                toDownloadStartDates.add(incomingStart);
            }
        }

        if (!toDownloadStartDates.isEmpty()) {
            JHVWorker<ArrayList<DownloadedJPXData>, Void> imageDownloadWorker = new RadioJPXDownload(toDownloadStartDates);
            imageDownloadWorker.setThreadName("EVE--RadioDownloader");
            EVEPlugin.executorService.execute(imageDownloadWorker);
        }
    }

    private static int isDownloading;

    private class RadioJPXDownload extends JHVWorker<ArrayList<DownloadedJPXData>, Void> {

        private final ArrayList<Long> toDownload;

        public RadioJPXDownload(ArrayList<Long> _toDownload) {
            isDownloading++;
            toDownload = _toDownload;
            Timelines.getModel().downloadStarted(RadioData.this);
        }

        @Override
        protected ArrayList<DownloadedJPXData> backgroundWork() {
            ArrayList<DownloadedJPXData> jpList = new ArrayList<>(DAYS_IN_CACHE);
            HashSet<URI> remotes = new HashSet<>(DAYS_IN_CACHE);
            for (long date : toDownload) {
                try {
                    APIRequest req = new APIRequest("ROB", APIRequest.CallistoID, date, date, APIRequest.CADENCE_ANY);
                    URI uri = APIRequestManager.requestRemoteFile(req);
                    if (uri == null) {
                        continue;
                    }

                    if (remotes.contains(uri)) {
                        jpList.add(new DownloadedJPXData(null, req.startTime));
                    } else {
                        remotes.add(uri);
                        View v = APIRequestManager.loadView(uri, req);
                        if (v instanceof JP2ViewCallisto) {
                            jpList.add(new DownloadedJPXData((JP2ViewCallisto) v, req.startTime));
                        }
                    }
                } catch (IOException e) {
                    Log.error("An error occured while opening the remote file: " + e.getMessage());
                }
            }
            return jpList;
        }

        @Override
        protected void done() {
            try {
                isDownloading--;
                ArrayList<DownloadedJPXData> jpList = get();
                for (DownloadedJPXData jp2Data : jpList) {
                    cache.put(jp2Data.getStartDate(), jp2Data);
                }
                Timelines.getModel().downloadFinished(RadioData.this);
                // DrawController.fireRedrawRequest();
                requestForData();
            } catch (InterruptedException | ExecutionException e) {
                Log.error("RadioData error: " + e.getCause().getMessage());
            }
        }

    }

    private static void requestForData() {
        for (DownloadedJPXData jpxData : cache.values()) {
            jpxData.requestData(DrawController.selectedAxis, yAxis);
        }
    }

    @Override
    public YAxis getYAxis() {
        return yAxis;
    }

    @Override
    public boolean showYAxis() {
        return isVisible;
    }

    @Override
    public void remove() {
        // clearCache();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        clearCache();
        fetchData(DrawController.selectedAxis);
    }

    @Override
    public String getName() {
        return "Callisto Radiogram";
    }

    @Override
    public Color getDataColor() {
        return Color.BLACK;
    }

    @Override
    public boolean isDownloading() {
        for (DownloadedJPXData jpxData : cache.values()) {
            if (jpxData.isDownloading()) {
                return true;
            }
        }
        return isDownloading != 0;
    }

    @Override
    public Component getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    public boolean hasData() {
        for (DownloadedJPXData djpx : cache.values()) {
            if (djpx.hasData()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public void fetchData(TimeAxis selectedAxis) {
        if (isVisible) {
            boolean timediffCond = selectedAxis.end - selectedAxis.start <= TimeUtils.DAY_IN_MILLIS * MAX_AMOUNT_OF_DAYS;
            if (timediffCond) {
                requestForData();
                requestAndOpenIntervals(selectedAxis.start, selectedAxis.end);
            }
        }
    }

    @Override
    public void draw(Graphics2D g, Rectangle graphArea, TimeAxis timeAxis, Point mousePosition) {
        if (!isVisible) {
            return;
        }
        boolean timediffCond = timeAxis.end - timeAxis.start <= TimeUtils.DAY_IN_MILLIS * MAX_AMOUNT_OF_DAYS;
        if (timediffCond) {
            for (DownloadedJPXData djpx : cache.values()) {
                djpx.draw(g, graphArea, timeAxis, yAxis);
            }
        } else {
            String text1 = "The selected interval is too big.";
            Rectangle2D r1 = g.getFontMetrics().getStringBounds(text1, g);

            String text2 = "Reduce the interval to see the radio spectrograms.";
            Rectangle2D r2 = g.getFontMetrics().getStringBounds(text2, g);

            int x1 = (int) (graphArea.x + 0.5 * graphArea.width - 0.5 * r1.getWidth());
            int y1 = (int) (graphArea.y + 0.5 * graphArea.height - 1.5 * r1.getHeight());
            int x2 = (int) (graphArea.x + 0.5 * graphArea.width - 0.5 * r2.getWidth());
            int y2 = (int) (graphArea.y + 0.5 * graphArea.height + 0.5 * r2.getHeight());

            g.setColor(Color.black);
            g.drawString(text1, x1, y1);
            g.drawString(text2, x2, y2);
        }
    }

    @Override
    public void zoomToFitAxis() {
        yAxis.reset(400, 20);
    }

    @Override
    public void resetAxis() {
        yAxis.reset(400, 20);
    }

}
