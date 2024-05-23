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
import java.util.HashSet;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DataUri;
import org.helioviewer.jhv.io.NetFileCache;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.AbstractTimelineLayer;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.draw.TimeAxis;
import org.helioviewer.jhv.timelines.draw.YAxis;
import org.helioviewer.jhv.timelines.draw.YAxis.YAxisPositiveIdentityScale;
import org.helioviewer.jhv.threads.EDTCallbackExecutor;
import org.helioviewer.jhv.view.DecodeExecutor;
import org.helioviewer.jhv.view.j2k.J2KViewCallisto;
import org.json.JSONObject;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.FutureCallback;

public final class RadioData extends AbstractTimelineLayer {

    static final YAxis yAxis = new YAxis(400, 20, new YAxisPositiveIdentityScale("MHz"));

    private static final int MAX_AMOUNT_OF_DAYS = 3;
    private static final int DAYS_IN_CACHE = MAX_AMOUNT_OF_DAYS + 4;

    private static final Cache<Long, RadioJ2KData> cache = Caffeine.newBuilder().maximumSize(DAYS_IN_CACHE)
            .removalListener((Long k, RadioJ2KData v, RemovalCause c) -> {
                if (v != null)
                    v.removeData();
            }).build();
    private static final HashSet<Long> downloading = new HashSet<>();

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

    public static AbstractTimelineLayer deserialize(JSONObject jo) { // has to be implemented for state
        return new RadioData(jo);
    }

    @Override
    public void serialize(JSONObject jo) {
        jo.put("colormap", optionsPanel.getColormap());
    }

    private static IndexColorModel createIndexColorModelFromLUT(LUT lut2) {
        int[] source = lut2.lut8();
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
            if (!downloading.contains(date) && cache.getIfPresent(date) == null) {
                EDTCallbackExecutor.pool.submit(new RadioJPXDownload(date), new RadioJPXCallback(date));
            }
        }
    }

    private class RadioJPXDownload implements Callable<RadioJ2KData> {

        private final long date;

        RadioJPXDownload(long _date) {
            date = _date;
            downloading.add(date);
            Timelines.getLayers().downloadStarted(RadioData.this);
        }

        @Override
        public RadioJ2KData call() throws Exception {
            APIRequest req = new APIRequest("ROB", APIRequest.CallistoID, date, date, APIRequest.CADENCE_ALL);
            DataUri dataUri = NetFileCache.get(new URI(req.toFileRequest()));
            if (dataUri.format() != DataUri.Format.JP2) // paranoia
                throw new Exception("Invalid data format");

            DecodeExecutor executor = new DecodeExecutor();
            return new RadioJ2KData(new J2KViewCallisto(executor, req, dataUri), req.startTime(), executor);
        }

    }

    private class RadioJPXCallback implements FutureCallback<RadioJ2KData> {

        private final long date;

        RadioJPXCallback(long _date) {
            date = _date;
        }

        private void done() {
            downloading.remove(date);
            Timelines.getLayers().downloadFinished(RadioData.this);
        }

        @Override
        public void onSuccess(RadioJ2KData result) {
            done();
            cache.put(date, result);
            result.requestData(DrawController.selectedAxis);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            done();
            Log.error(Throwables.getStackTraceAsString(t));
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
        return UIGlobals.foreColor;
    }

    @Override
    public boolean isDownloading() {
        return !downloading.isEmpty();
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
    public void draw(Graphics2D g, Rectangle graphArea, TimeAxis xAxis, Point mousePosition) {
        if (!enabled)
            return;

        if (xAxis.end() - xAxis.start() <= TimeUtils.DAY_IN_MILLIS * MAX_AMOUNT_OF_DAYS) {
            drawString(g, graphArea, xAxis, "No data available");
            cache.asMap().values().forEach(data -> data.draw(g, graphArea, xAxis));
        } else {
            drawString(g, graphArea, xAxis, "Reduce the time interval to see the radio spectrograms.");
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
