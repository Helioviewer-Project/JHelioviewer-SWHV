package org.helioviewer.jhv.plugins.eveplugin.radio;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.gui.filters.lut.LUT;
import org.helioviewer.jhv.io.APIRequestManager;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimeAxis;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxis;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.AbstractLineDataSelectorElement;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2ViewCallisto;

public class RadioData extends AbstractLineDataSelectorElement {

    private final YAxis yAxis;

    private static final HashMap<Long, DownloadedJPXData> cache = new HashMap<Long, DownloadedJPXData>();;
    private static final String ROBserver = DataSources.ROBsettings.get("API.jp2images.path");

    public static final int MAX_AMOUNT_OF_DAYS = 3;
    public static final int DAYS_IN_CACHE = MAX_AMOUNT_OF_DAYS + 4;

    private final RadioOptionsPanel optionsPanel;
    private IndexColorModel colorModel;

    public RadioData() {
        isVisible = false;

        String cm = "Rainbow 2";
        colorModel = createIndexColorModelFromLUT(LUT.getStandardList().get(cm));
        optionsPanel = new RadioOptionsPanel(cm);
        yAxis = new YAxis(400, 20, "Mhz", false);
        EVEPlugin.ldsm.addLineData(this);
    }

    private IndexColorModel createIndexColorModelFromLUT(LUT lut2) {
        return new IndexColorModel(8, lut2.getLut8().length, lut2.getLut8(), 0, false, -1, DataBuffer.TYPE_BYTE);
    }

    void setLUT(LUT lut) {
        colorModel = createIndexColorModelFromLUT(lut);
        for (Entry<Long, DownloadedJPXData> entry : cache.entrySet()) {
            DownloadedJPXData jpxData = entry.getValue();
            jpxData.changeColormap(colorModel);
        }
        EVEPlugin.dc.fireRedrawRequest();
    }

    IndexColorModel getColorModel() {
        return colorModel;
    }

    private void clearCache() {
        for (DownloadedJPXData jpxData : cache.values()) {
            jpxData.remove();
        }
        cache.clear();
        latest_cache_start = -1;
        latest_cache_end = -1;
    }

    private long latest_cache_start = -1;
    private long latest_cache_end = -1;

    private void requestAndOpenIntervals(long start, long end) {
        if (latest_cache_start != -1 && latest_cache_end != -1 && start >= latest_cache_start && end <= latest_cache_end)
            return;
        latest_cache_start = start - start % TimeUtils.DAY_IN_MILLIS - 2 * TimeUtils.DAY_IN_MILLIS;
        latest_cache_end = latest_cache_start + DAYS_IN_CACHE * TimeUtils.DAY_IN_MILLIS;
        ArrayList<Long> toDownloadStartDates = new ArrayList<Long>();
        ArrayList<Long> incomingStartDates = new ArrayList<Long>(DAYS_IN_CACHE);
        for (int i = 0; i < DAYS_IN_CACHE; i++) {
            incomingStartDates.add(latest_cache_start + i * TimeUtils.DAY_IN_MILLIS);
        }

        Iterator<Entry<Long, DownloadedJPXData>> it = cache.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Long, DownloadedJPXData> entry = it.next();
            Long key = entry.getKey();
            if (!incomingStartDates.contains(key)) {
                DownloadedJPXData jpxData = entry.getValue();
                jpxData.remove();
                it.remove();
            }
        }

        for (long incomingStart : incomingStartDates) {
            if (!cache.containsKey(incomingStart)) {
                toDownloadStartDates.add(incomingStart);
                cache.put(incomingStart, new DownloadedJPXData(incomingStart, incomingStart + TimeUtils.DAY_IN_MILLIS));
            }
        }

        if (!toDownloadStartDates.isEmpty()) {
            EVEPlugin.ldsm.downloadStarted(this);
            JHVWorker<ArrayList<JP2ViewCallisto>, Void> imageDownloadWorker = new RadioJPXDownload(toDownloadStartDates);
            imageDownloadWorker.setThreadName("EVE--RadioDownloader");
            EVEPlugin.executorService.execute(imageDownloadWorker);
        }
    }

    private void initJPX(ArrayList<JP2ViewCallisto> jpList, ArrayList<Long> datesToDownload) {
        for (int i = 0; i < jpList.size(); i++) {
            JP2ViewCallisto v = jpList.get(i);
            long date = datesToDownload.get(i);
            DownloadedJPXData jpxData = cache.get(date);
            if (v != null) {
                if (jpxData != null) {
                    jpxData.init(v);
                } else {
                    v.abolish();
                }
            } else {
                if (jpxData != null)
                    jpxData.downloadJPXFailed();
            }
        }
    }

    void requestForData() {
        for (DownloadedJPXData jpxData : cache.values()) {
            jpxData.requestData();
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
    public void removeLineData() {
        //clearCache();
        //EVEPlugin.ldsm.removeLineData(this);
    }

    @Override
    public void setVisibility(boolean visible) {
        isVisible = visible;
        clearCache();
        fetchData(EVEPlugin.dc.selectedAxis);
        // EVEPlugin.dc.fireRedrawRequest();

        if (isVisible)
            EVEPlugin.ldsm.fireLineDataSelectorElementAdded(this);
        else
            EVEPlugin.ldsm.fireLineDataSelectorElementRemoved(this);
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
        return false;
    }

    @Override
    public Component getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    public boolean hasData() {
        for (DownloadedJPXData djpx : cache.values()) {
            if (djpx.hasData())
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
        if (isVisible) {
            boolean timediffCond = selectedAxis.end - selectedAxis.start <= TimeUtils.DAY_IN_MILLIS * MAX_AMOUNT_OF_DAYS;
            if (timediffCond) {
                requestForData();
                requestAndOpenIntervals(selectedAxis.start, selectedAxis.end);
            }
        }
    }

    @Override
    public void draw(Graphics2D g, Graphics2D fullG, Rectangle graphArea, TimeAxis timeAxis, Point mousePosition) {
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
            int t1Width = (int) r1.getWidth();
            int t1Height = (int) r1.getHeight();

            String text2 = "Reduce the interval to see the radio spectrograms.";
            Rectangle2D r2 = g.getFontMetrics().getStringBounds(text2, g);
            int t2Width = (int) r2.getWidth();
            int t2Height = (int) r2.getHeight();

            int x1 = graphArea.x + (graphArea.width / 2) - (t1Width / 2);
            int y1 = (int) (graphArea.y + (graphArea.height / 2) - 1.5 * t1Height);
            int x2 = graphArea.x + (graphArea.width / 2) - (t2Width / 2);
            int y2 = (int) (graphArea.y + graphArea.height / 2 + 0.5 * t2Height);

            g.setColor(Color.black);
            g.drawString(text1, x1, y1);
            g.drawString(text2, x2, y2);
        }
    }

    private class RadioJPXDownload extends JHVWorker<ArrayList<JP2ViewCallisto>, Void> {

        private final ArrayList<Long> datesToDownload;

        public RadioJPXDownload(ArrayList<Long> toDownload) {
            datesToDownload = toDownload;
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
                initJPX(jpList, datesToDownload);
            } catch (InterruptedException e) {
                Log.error("ImageDownloadWorker execution interrupted: " + e.getMessage());
            } catch (ExecutionException e) {
                Log.error("ImageDownloadWorker execution error: " + e.getMessage());
            }
        }

    }

    @Override
    public void yaxisChanged() {
    }

    @Override
    public void zoomToFitAxis() {
        yAxis.reset(400, 20);
    }

    @Override
    public void resetAxis() {
        yAxis.reset(400, 20);
    }

    @Override
    public boolean highLightChanged(Point p) {
        return false;
    }

}
