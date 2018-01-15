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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;

public class RadioData extends AbstractTimelineLayer {

    static final YAxis yAxis = new YAxis(400, 20, "Mhz", false);

    private static final int MAX_AMOUNT_OF_DAYS = 3;
    private static final int DAYS_IN_CACHE = MAX_AMOUNT_OF_DAYS + 4;

    private static final RemovalListener<Long, RadioJP2Data> removalListener = removed -> removed.getValue().removeData();
    private static final Cache<Long, RadioJP2Data> cache = CacheBuilder.newBuilder().maximumSize(DAYS_IN_CACHE).removalListener(removalListener).build();

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
        for (RadioJP2Data data : cache.asMap().values()) {
            data.changeColormap(colorModel);
        }
        DrawController.drawRequest();
    }

    static IndexColorModel getColorModel() {
        return colorModel;
    }

    private void clearCache() {
        cache.invalidateAll();
    }

    private void requestAndOpenIntervals(long start, long end) {
        long now = System.currentTimeMillis();
        start -= start % TimeUtils.DAY_IN_MILLIS + 2 * TimeUtils.DAY_IN_MILLIS;
        end = Math.min(start + DAYS_IN_CACHE * TimeUtils.DAY_IN_MILLIS, now - now % TimeUtils.DAY_IN_MILLIS + TimeUtils.DAY_IN_MILLIS);

        for (int i = 0; i < DAYS_IN_CACHE; i++) {
            long date = end - i * TimeUtils.DAY_IN_MILLIS;
            if (cache.getIfPresent(date) == null) {
                EVEPlugin.executorService.execute(new RadioJPXDownload(date));
            }
        }
    }

    private static int isDownloading;

    private class RadioJPXDownload extends JHVWorker<RadioJP2Data, Void> {

        private final long date;

        RadioJPXDownload(long _date) {
            isDownloading++;
            date = _date;
            Timelines.getLayers().downloadStarted(RadioData.this);
            setThreadName("EVE--RadioDownloader");
        }

        @Override
        protected RadioJP2Data backgroundWork() {
            try {
                APIRequest req = new APIRequest("ROB", APIRequest.CallistoID, date, date, APIRequest.CADENCE_ANY);
                URI uri = APIRequestManager.requestRemoteFile(req);
                return uri == null ? null : new RadioJP2Data(new JP2ViewCallisto(uri, req), req.startTime);
            } catch (RuntimeException ignore) { // got closest
            } catch (Exception e) {
                Log.error("An error occured while opening the remote file: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void done() {
            try {
                isDownloading--;
                Timelines.getLayers().downloadFinished(RadioData.this);
                RadioJP2Data data = get();
                if (data != null) {
                    cache.put(date, data);
                    data.requestData(DrawController.selectedAxis);
                }
            } catch (InterruptedException | ExecutionException e) {
                Log.error("RadioData error: " + e.getCause().getMessage());
            }
        }

    }

    private static void requestForData() {
        for (RadioJP2Data jp2Data : cache.asMap().values()) {
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
        clearCache();
    }

    @Override
    public void setEnabled(boolean _enabled) {
        super.setEnabled(_enabled);
        if (!enabled)
            clearCache();
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
        for (RadioJP2Data data : cache.asMap().values()) {
            if (data.isDownloading()) {
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
        for (RadioJP2Data data : cache.asMap().values()) {
            if (data.hasData()) {
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
            drawString(g, graphArea, timeAxis, "No data available");
            for (RadioJP2Data data : cache.asMap().values()) {
                data.draw(g, graphArea, timeAxis);
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

    static void drawString(Graphics2D g, Rectangle ga, TimeAxis xAxis, String text) {
        int dx0 = xAxis.value2pixel(ga.x, ga.width, xAxis.start);
        int dx1 = xAxis.value2pixel(ga.x, ga.width, xAxis.end);
        int dwidth = dx1 - dx0;
        g.setColor(Color.GRAY);
        g.fillRect(dx0, ga.y, dwidth, ga.height);
        g.setColor(Color.WHITE);

        Rectangle2D r = g.getFontMetrics().getStringBounds(text, g);
        int tWidth = (int) r.getWidth();
        int tHeight = (int) r.getHeight();
        int y = ga.y + ga.height / 2 - tHeight / 2;

        for (int x = dx0 + tWidth / 2; x < dx1; x += tWidth + tWidth / 2)
            g.drawString(text, x, y);
    }

}
