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
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.draw.TimeAxis;
import org.helioviewer.jhv.timelines.draw.YAxis;
import org.helioviewer.jhv.timelines.draw.YAxis.YAxisPositiveIdentityScale;

import org.json.JSONObject;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public final class RadioData extends TimelineLayer {

    static final YAxis yAxis = new YAxis(400, 20, new YAxisPositiveIdentityScale("MHz"));

    private static final int MAX_AMOUNT_OF_DAYS = 3;
    private static final int DAYS_IN_CACHE = MAX_AMOUNT_OF_DAYS + 4;
    // Crop budget equivalent to two full-resolution Callisto images per retained day.
    private static final long MAX_DECODED_BYTES = 2L * DAYS_IN_CACHE * 86400 * 380;

    private record DecodeKey(RadioJ2KData day, RadioJ2KData.Crop crop) {}

    private final HashMap<Long, RadioJ2KData> cache = new HashMap<>();
    private final Cache<DecodeKey, byte[]> decoded = CacheBuilder.newBuilder()
            .concurrencyLevel(1)
            .softValues()
            .maximumWeight(MAX_DECODED_BYTES)
            .weigher((DecodeKey key, byte[] pixels) -> pixels.length)
            .build();
    private final HashMap<Long, RadioJP2Download> downloads = new HashMap<>();
    private final LUTComboBox lutCombo;
    private final JPanel optionsPanel;
    private IndexColorModel colorModel;

    public RadioData(JSONObject jo) {
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

    private void setLUT(LUT lut) {
        colorModel = createIndexColorModelFromLUT(lut);
        cache.values().forEach(data -> data.changeColormap(colorModel));
        DrawController.drawRequest();
    }

    IndexColorModel getColorModel() {
        return colorModel;
    }

    @Nullable
    byte[] getDecoded(RadioJ2KData day, RadioJ2KData.Crop crop) {
        return decoded.getIfPresent(new DecodeKey(day, crop));
    }

    void putDecoded(RadioJ2KData day, RadioJ2KData.Crop crop, byte[] pixels) {
        // A completed decode may arrive after this day left the window.
        if (!cache.containsValue(day))
            return;
        decoded.put(new DecodeKey(day, crop), pixels);
    }

    void removeDecoded(RadioJ2KData day) {
        decoded.asMap().keySet().removeIf(key -> key.day == day);
    }

    private void clearCache() {
        cache.values().forEach(RadioJ2KData::removeData);
        cache.clear();
        downloads.values().forEach(download -> download.thread.interrupt());
        downloads.clear();
        DrawController.drawRequest();
    }

    private void requestAndOpenIntervals(long start) {
        long end = Math.min(TimeUtils.floorDay(start) + (DAYS_IN_CACHE - 2) * TimeUtils.DAY_IN_MILLIS, TimeUtils.floorDay(System.currentTimeMillis()));
        long first = end - (DAYS_IN_CACHE - 1) * TimeUtils.DAY_IN_MILLIS;
        boolean changed = cache.entrySet().removeIf(entry -> {
            boolean remove = entry.getKey() < first || entry.getKey() > end;
            if (remove)
                entry.getValue().removeData();
            return remove;
        });
        Iterator<RadioJP2Download> pending = downloads.values().iterator();
        while (pending.hasNext()) {
            RadioJP2Download download = pending.next();
            if (download.date < first || download.date > end) {
                download.thread.interrupt();
                pending.remove();
                changed = true;
            }
        }
        for (int i = 0; i < DAYS_IN_CACHE; i++) {
            long date = end - i * TimeUtils.DAY_IN_MILLIS;
            if (!downloads.containsKey(date) && !cache.containsKey(date)) {
                RadioJP2Download download = new RadioJP2Download(date);
                downloads.put(date, download);
                changed = true;
                Task.submit(task -> download.thread = Thread.ofVirtual().name("Radio-Download").start(task),
                        download, result -> onSuccessRadioJP2(download, result),
                        t -> onFailureRadioJP2(download, t));
            }
        }
        if (changed)
            notifyStateChanged();
    }

    private final class RadioJP2Download implements Callable<RadioJ2KData> {
        private final long date;
        // Owned by the EDT. Interrupt I/O but keep the result callback to close obsolete days.
        private Thread thread;

        RadioJP2Download(long _date) {
            date = _date;
        }

        @Override
        public RadioJ2KData call() throws Exception {
            APIRequest req = new APIRequest("ROB", APIRequest.CallistoID, date, date, APIRequest.CADENCE_ALL);
            URI uri = new URI(req.toFileRequest());
            // ROB replaces the current day's file as observations arrive.
            if (date == TimeUtils.floorDay(System.currentTimeMillis()))
                NetFileCache.invalidate(uri);
            DataUri dataUri = NetFileCache.get(uri);
            if (dataUri.format() != DataUri.Format.JP2) // paranoia
                throw new Exception("Invalid data format");

            return new RadioJ2KData(RadioData.this, req, dataUri);
        }

    }

    private void onSuccessRadioJP2(RadioJP2Download download, @Nonnull RadioJ2KData result) {
        if (finishDownload(download)) {
            cache.put(download.date, result);
            fetchData(DrawController.selectedAxis);
        } else {
            result.removeData();
        }
    }

    private void onFailureRadioJP2(RadioJP2Download download, @Nonnull Throwable t) {
        if (finishDownload(download)) {
            if (AppThread.isInterrupted(t))
                Log.warn(t);
            else
                Log.errorStack(t);
        }
    }

    private boolean finishDownload(RadioJP2Download download) {
        boolean removed = downloads.remove(download.date, download);
        if (removed) {
            notifyStateChanged();
            DrawController.drawRequest();
        }
        return removed;
    }

    private static boolean canShow(TimeAxis timeAxis) {
        return timeAxis.end() - timeAxis.start() <= TimeUtils.DAY_IN_MILLIS * MAX_AMOUNT_OF_DAYS;
    }

    void dataUpdated() {
        notifyStateChanged();
    }

    @Override
    public YAxis getYAxis() {
        return yAxis;
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
        notifyStateChanged();
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
        return !downloads.isEmpty();
    }

    @Override
    public JPanel getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    public boolean hasData() {
        for (RadioJ2KData data : cache.values()) {
            if (data.hasData()) {
                return true;
            }
        }
        return false;
    }

    private boolean isLoading() {
        if (!downloads.isEmpty())
            return true;
        for (RadioJ2KData data : cache.values()) {
            if (data.isLoading())
                return true;
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
            requestAndOpenIntervals(selectedAxis.start());
            cache.values().forEach(data -> data.requestData(selectedAxis));
        }
    }

    @Override
    public void draw(Graphics2D g, Rectangle graphArea, TimeAxis xAxis, Point mousePosition) {
        if (!enabled)
            return;

        if (canShow(xAxis)) {
            drawMessage(g, graphArea, isLoading() ? "Fetching data" : "No data available");
            TimeAxis.Mapper xMapper = xAxis.mapper(graphArea.x, graphArea.width);
            YAxis.Mapper yMapper = yAxis.mapper(graphArea.y, graphArea.height);
            cache.values().forEach(data -> data.draw(g, xMapper, yMapper));
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
        availabilityBtn.addActionListener(e -> DesktopIntegration.openURL(DataSources.getServer("ROB").availabilityURL() +
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
