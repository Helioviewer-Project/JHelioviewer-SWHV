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

import javax.annotation.Nullable;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.NetFileCache;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.AbstractTimelineLayer;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.draw.TimeAxis;
import org.helioviewer.jhv.timelines.draw.YAxis;
import org.helioviewer.jhv.timelines.draw.YAxis.YAxisPositiveIdentityScale;
import org.helioviewer.jhv.view.DecodeExecutor;
import org.helioviewer.jhv.view.j2k.J2KViewCallisto;
import org.json.JSONObject;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;

public class RadioData extends AbstractTimelineLayer {

    static final YAxis yAxis = new YAxis(400, 20, new YAxisPositiveIdentityScale("MHz"));

    private static final int MAX_AMOUNT_OF_DAYS = 3;
    private static final int DAYS_IN_CACHE = MAX_AMOUNT_OF_DAYS + 4;

    private static void removalListener(RemovalNotification<Long, RadioJ2KData> removed) {
        removed.getValue().removeData();
    }

    private static final Cache<Long, RadioJ2KData> cache = CacheBuilder.newBuilder().maximumSize(DAYS_IN_CACHE).removalListener(RadioData::removalListener).build();

    private final RadioOptionsPanel optionsPanel;
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
        cache.asMap().values().forEach(data -> data.changeColormap(colorModel));
        DrawController.drawRequest();
    }

    static IndexColorModel getColorModel() {
        return colorModel;
    }

    private static void clearCache() {
        cache.invalidateAll();
    }

    private void requestAndOpenIntervals(long start) {
        long end = Math.min(TimeUtils.floorDay(start) + (DAYS_IN_CACHE - 2) * TimeUtils.DAY_IN_MILLIS, TimeUtils.floorDay(System.currentTimeMillis()));
        for (int i = 0; i < DAYS_IN_CACHE; i++) {
            long date = end - i * TimeUtils.DAY_IN_MILLIS;
            if (cache.getIfPresent(date) == null) {
                JHVGlobals.getExecutorService().execute(new RadioJPXDownload(date));
            }
        }
    }

    private static int isDownloading;

    private class RadioJPXDownload extends JHVWorker<RadioJ2KData, Void> {

        private final long date;

        RadioJPXDownload(long _date) {
            date = _date;
            isDownloading++;
            Timelines.getLayers().downloadStarted(RadioData.this);
            setThreadName("EVE--RadioDownloader");
        }

        @Nullable
        @Override
        protected RadioJ2KData backgroundWork() {
            try {
                APIRequest req = new APIRequest("ROB", APIRequest.CallistoID, date, date, APIRequest.CADENCE_ANY);
                URI uri = new URI(req.toFileRequest());
                DecodeExecutor executor = new DecodeExecutor();
                return new RadioJ2KData(new J2KViewCallisto(executor, req, NetFileCache.get(uri)), req.startTime, executor);
            } catch (Exception e) {
                Log.error("An error occured while opening the remote file: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void done() {
            isDownloading--;
            Timelines.getLayers().downloadFinished(RadioData.this);
            try {
                RadioJ2KData data = get();
                if (data != null) {
                    cache.put(date, data);
                    data.requestData(DrawController.selectedAxis);
                }
            } catch (InterruptedException | ExecutionException e) {
                Log.error("RadioData error: " + e.getCause().getMessage());
            }
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
        return isDownloading != 0;
    }

    @Override
    public Component getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    public boolean hasData() {
        for (RadioJ2KData data : cache.asMap().values()) {
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
        if (enabled && selectedAxis.end() - selectedAxis.start() <= TimeUtils.DAY_IN_MILLIS * MAX_AMOUNT_OF_DAYS) {
            cache.asMap().values().forEach(data -> data.requestData(selectedAxis));
            requestAndOpenIntervals(selectedAxis.start());
        }
    }

    @Override
    public void draw(Graphics2D g, Rectangle graphArea, TimeAxis timeAxis, Point mousePosition) {
        if (!enabled)
            return;

        if (timeAxis.end() - timeAxis.start() <= TimeUtils.DAY_IN_MILLIS * MAX_AMOUNT_OF_DAYS) {
            drawString(g, graphArea, timeAxis, "No data available");
            cache.asMap().values().forEach(data -> data.draw(g, graphArea, timeAxis));
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
        int dx0 = xAxis.value2pixel(ga.x, ga.width, xAxis.start());
        int dx1 = xAxis.value2pixel(ga.x, ga.width, xAxis.end());
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
