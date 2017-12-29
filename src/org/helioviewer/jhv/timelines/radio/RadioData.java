package org.helioviewer.jhv.timelines.radio;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
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
import org.helioviewer.jhv.timelines.AbstractTimelineLayer;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.draw.TimeAxis;
import org.helioviewer.jhv.timelines.draw.YAxis;
import org.helioviewer.jhv.view.jp2view.JP2ViewCallisto;
import org.json.JSONObject;

public class RadioData extends AbstractTimelineLayer {

    static final YAxis yAxis = new YAxis(400, 20, "Mhz", false);

    private static final int MAX_AMOUNT_OF_DAYS = 3;
    private static final int DAYS_IN_CACHE = MAX_AMOUNT_OF_DAYS + 4;
    private static final HashMap<Long, RadioJP2Data> cache = new HashMap<>(DAYS_IN_CACHE);

    private static RadioOptionsPanel optionsPanel;
    private static IndexColorModel colorModel;

    public RadioData(JSONObject jo) {
        String cm = "Spectral";
        if (jo != null) {
            cm = jo.optString("colormap", cm);
            if (LUT.get(cm) == null)
                cm = "Spectral";
        }

        colorModel = createIndexColorModelFromLUT(LUT.get(cm));
        optionsPanel = new RadioOptionsPanel(cm);
        setEnabled(false);
    }

    @Override
    public void serialize(JSONObject jo) {
        jo.put("colormap", optionsPanel.getColormap());
    }

    private static IndexColorModel createIndexColorModelFromLUT(LUT lut2) {
        int[] source = lut2.getLut8();
        return new IndexColorModel(8, source.length, source, 0, false, -1, DataBuffer.TYPE_BYTE);
    }

    static void setLUT(LUT lut) {
        colorModel = createIndexColorModelFromLUT(lut);
        for (RadioJP2Data jp2Data : cache.values()) {
            jp2Data.changeColormap(colorModel);
        }
        DrawController.drawRequest();
    }

    static IndexColorModel getColorModel() {
        return colorModel;
    }

    private void clearCache() {
        for (RadioJP2Data jp2Data : cache.values()) {
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

        Iterator<Map.Entry<Long, RadioJP2Data>> it = cache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, RadioJP2Data> entry = it.next();
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
            EVEPlugin.executorService.execute(new RadioJPXDownload(toDownloadStartDates));
        }
    }

    private static int isDownloading;

    private class RadioJPXDownload extends JHVWorker<ArrayList<RadioJP2Data>, Void> {

        private final ArrayList<Long> toDownload;

        RadioJPXDownload(ArrayList<Long> _toDownload) {
            isDownloading++;
            toDownload = _toDownload;
            Timelines.getLayers().downloadStarted(RadioData.this);
            setThreadName("EVE--RadioDownloader");
        }

        @Override
        protected ArrayList<RadioJP2Data> backgroundWork() {
            ArrayList<RadioJP2Data> jpList = new ArrayList<>(DAYS_IN_CACHE);
            HashSet<URI> remotes = new HashSet<>(DAYS_IN_CACHE);
            for (long date : toDownload) {
                try {
                    APIRequest req = new APIRequest("ROB", APIRequest.CallistoID, date, date, APIRequest.CADENCE_ANY);
                    URI uri = APIRequestManager.requestRemoteFile(req);
                    if (uri == null)
                        continue;

                    if (remotes.contains(uri)) {
                        jpList.add(new RadioJP2Data(null, req.startTime));
                    } else {
                        remotes.add(uri);
                        jpList.add(new RadioJP2Data(new JP2ViewCallisto(uri, req), req.startTime));
                    }
                } catch (Exception e) {
                    Log.error("An error occured while opening the remote file: " + e.getMessage());
                }
            }
            return jpList;
        }

        @Override
        protected void done() {
            try {
                isDownloading--;
                for (RadioJP2Data jp2Data : get()) {
                    cache.put(jp2Data.getStartDate(), jp2Data);
                }
                Timelines.getLayers().downloadFinished(RadioData.this);
                requestForData();
            } catch (InterruptedException | ExecutionException e) {
                Log.error("RadioData error: " + e.getCause().getMessage());
            }
        }

    }

    private static void requestForData() {
        for (RadioJP2Data jp2Data : cache.values()) {
            jp2Data.requestData(DrawController.selectedAxis);
        }
    }

    @Override
    public YAxis getYAxis() {
        return yAxis;
    }

    @Override
    public boolean showYAxis() {
        return enabled;
    }

    @Override
    public void remove() {
        // clearCache();
    }

    @Override
    public void setEnabled(boolean _enabled) {
        super.setEnabled(_enabled);
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
        for (RadioJP2Data jp2Data : cache.values()) {
            if (jp2Data.isDownloading()) {
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
        for (RadioJP2Data jp2Data : cache.values()) {
            if (jp2Data.hasData()) {
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
        if (enabled && selectedAxis.end - selectedAxis.start <= TimeUtils.DAY_IN_MILLIS * MAX_AMOUNT_OF_DAYS) {
            requestForData();
            requestAndOpenIntervals(selectedAxis.start, selectedAxis.end);
        }
    }

    @Override
    public void draw(Graphics2D g, Rectangle graphArea, TimeAxis timeAxis, Point mousePosition) {
        if (!enabled)
            return;

        if (timeAxis.end - timeAxis.start <= TimeUtils.DAY_IN_MILLIS * MAX_AMOUNT_OF_DAYS) {
            for (RadioJP2Data jp2Data : cache.values()) {
                jp2Data.draw(g, graphArea, timeAxis);
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
