package org.helioviewer.jhv.timelines.radio;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.net.URI;
import java.util.HashSet;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.gui.DesktopIntegration;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.image.lut.LUT;
import org.helioviewer.jhv.image.lut.LUTComboBox;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.io.DataUri;
import org.helioviewer.jhv.io.NetFileCache;
import org.helioviewer.jhv.thread.AppThread;
import org.helioviewer.jhv.thread.Task;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.TimelineLayer;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.draw.TimeAxis;
import org.helioviewer.jhv.timelines.draw.YAxis;
import org.helioviewer.jhv.timelines.draw.YAxis.YAxisPositiveIdentityScale;

import org.json.JSONObject;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;

public final class RadioData extends TimelineLayer {

    static final YAxis yAxis = new YAxis(400, 20, new YAxisPositiveIdentityScale("MHz"));

    private static final int MAX_AMOUNT_OF_DAYS = 3;
    private static final int DAYS_IN_CACHE = MAX_AMOUNT_OF_DAYS + 4;

    private static final Cache<Long, RadioJ2KData> cache = Caffeine.newBuilder().maximumSize(DAYS_IN_CACHE)
            .removalListener((Long k, RadioJ2KData v, RemovalCause c) -> {
                if (v != null)
                    v.removeData();
            }).build();
    private static final HashSet<Long> downloading = new HashSet<>();
    private static RadioData currentInstance;

    private final LUTComboBox lutCombo;
    private final JPanel optionsPanel;
    private static IndexColorModel colorModel;

    public RadioData(JSONObject jo) {
        currentInstance = this;
        LUT lut = LUT.spectral();
        if (jo != null) {
            LUT configured = LUT.get(jo.optString("colormap", lut.name()));
            if (configured != null)
                lut = configured;
        }

        colorModel = createIndexColorModelFromLUT(lut);

        lutCombo = new LUTComboBox();
        lutCombo.setSelectedItem(lut.name());
        lutCombo.addActionListener(e -> setLUT(lutCombo.getLUT()));
        optionsPanel = optionsPanel(lutCombo);

        setEnabled(false);
    }

    public static TimelineLayer deserialize(JSONObject jo) { // has to be implemented for state
        return new RadioData(jo);
    }

    @Override
    public void serialize(JSONObject jo) {
        jo.put("colormap", lutCombo.getColormap());
    }

    private static IndexColorModel createIndexColorModelFromLUT(LUT lut) {
        int[] source = lut.lut8();
        return new IndexColorModel(8, source.length, source, 0, false, -1, DataBuffer.TYPE_BYTE);
    }

    private static void setLUT(LUT lut) {
        colorModel = createIndexColorModelFromLUT(lut);
        cache.asMap().values().forEach(data -> data.changeColormap(colorModel));
        DrawController.drawRequest();
    }

    static IndexColorModel getColorModel() {
        return colorModel;
    }

    private static void clearCache() {
        cache.invalidateAll();
        downloading.clear();
        DrawController.drawRequest();
    }

    private void requestAndOpenIntervals(long start) {
        long end = Math.min(TimeUtils.floorDay(start) + (DAYS_IN_CACHE - 2) * TimeUtils.DAY_IN_MILLIS, TimeUtils.floorDay(System.currentTimeMillis()));
        for (int i = 0; i < DAYS_IN_CACHE; i++) {
            long date = end - i * TimeUtils.DAY_IN_MILLIS;
            if (!downloading.contains(date) && cache.getIfPresent(date) == null) {
                downloadStarted(date);
                Task.submit(Long.toString(date), new RadioJPXDownload(date), result -> onSuccessRadioJPX(date, result), (logContext, t) -> onFailureRadioJPX(date, t));
            }
        }
    }

    private record RadioJPXDownload(long date) implements Callable<RadioJ2KData> {

        @Override
        public RadioJ2KData call() throws Exception {
            APIRequest req = new APIRequest("ROB", APIRequest.CallistoID, date, date, APIRequest.CADENCE_ALL);
            DataUri dataUri = NetFileCache.get(new URI(req.toFileRequest()));
            if (dataUri.format() != DataUri.Format.Image.JP2) // paranoia
                throw new Exception("Invalid data format");

            return new RadioJ2KData(req, dataUri);
        }

    }

    private static void downloadStarted(long date) {
        downloading.add(date);
        refreshRow();
    }

    private void onSuccessRadioJPX(long date, @Nonnull RadioJ2KData result) {
        doneRadioJPX(date);
        if (currentInstance != this || !enabled) {
            result.removeData();
            return;
        }
        cache.put(date, result);
        fetchData(DrawController.selectedAxis);
    }

    private void onFailureRadioJPX(long date, @Nonnull Throwable t) {
        doneRadioJPX(date);
        if (AppThread.isInterrupted(t)) {
            Log.warn(t);
            return;
        }
        Log.errorStack(t);
    }

    private static void doneRadioJPX(long date) {
        downloading.remove(date);
        refreshRow();
    }

    private static boolean canShow(TimeAxis timeAxis) {
        return timeAxis.end() - timeAxis.start() <= TimeUtils.DAY_IN_MILLIS * MAX_AMOUNT_OF_DAYS;
    }

    private static void refreshRow() {
        if (currentInstance != null)
            Timelines.getLayers().updateRow(currentInstance);
    }

    static void dataUpdated() {
        refreshRow();
    }

    @Override
    public YAxis getYAxis() {
        return yAxis;
    }

    @Override
    public boolean hasYAxis() {
        return true;
    }

    @Override
    public void remove() {
        clearCache();
        if (currentInstance == this)
            currentInstance = null;
        refreshRow();
    }

    @Override
    public void setEnabled(boolean _enabled) {
        super.setEnabled(_enabled);
        if (!enabled)
            clearCache();
        refreshRow();
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
    public JPanel getOptionsPanel() {
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
        if (enabled && canShow(selectedAxis)) {
            cache.asMap().values().forEach(data -> data.requestData(selectedAxis));
            requestAndOpenIntervals(selectedAxis.start());
        }
    }

    @Override
    public void draw(Graphics2D g, Rectangle graphArea, TimeAxis xAxis, Point mousePosition) {
        if (!enabled)
            return;

        if (canShow(xAxis)) {
            drawMessage(g, graphArea, "No data available");
            TimeAxis.Mapper xMapper = xAxis.mapper(graphArea.x, graphArea.width);
            YAxis.Mapper yMapper = yAxis.mapper(graphArea.y, graphArea.height);
            cache.asMap().values().forEach(data -> data.draw(g, graphArea, xMapper, yMapper));
        } else {
            drawMessage(g, graphArea, "Reduce the time interval to see the radio spectrograms.");
        }
    }

    @Override
    public void zoomToFitAxis() {
        resetAxis();
    }

    @Override
    public void resetAxis() {
        yAxis.reset(400, 20);
    }

    static void drawMessage(Graphics2D g, Rectangle ga, String text) {
        int dx0 = ga.x;
        int dx1 = ga.x + ga.width;
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

    private static JPanel optionsPanel(LUTComboBox combo) {
        JButton availabilityBtn = new JButton("Available data");
        availabilityBtn.addActionListener(e -> DesktopIntegration.openURL(DataSources.getServerSetting("ROB", "availability.images") +
                "ID=" + APIRequest.CallistoID));

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.NONE;
        panel.add(combo, c);
        c.anchor = GridBagConstraints.LINE_END;
        c.gridx = 1;
        c.gridy = 0;
        panel.add(availabilityBtn, c);

        return panel;
    }

}
