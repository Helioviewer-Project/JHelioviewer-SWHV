package org.helioviewer.jhv.plugins.eveplugin.radio.data;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.helioviewer.jhv.base.Range;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.io.APIRequestManager;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawController;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawableElement;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawableElementType;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimeAxis;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxis;
import org.helioviewer.jhv.plugins.eveplugin.radio.gui.RadioOptionsPanel;
import org.helioviewer.jhv.plugins.eveplugin.radio.model.ColorLookupModel;
import org.helioviewer.jhv.plugins.eveplugin.radio.model.ColorLookupModelListener;
import org.helioviewer.jhv.plugins.eveplugin.settings.EVESettings;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2ViewCallisto;

/**
 * The radio data manager manages all the downloaded data for radio
 * spectrograms.
 *
 * It receives all of its input from the radio downloader as listener of the
 * radio downloader.
 *
 * @author Bram.Bourgoignie@oma.be
 *
 */
public class RadioDataManager implements ColorLookupModelListener, LineDataSelectorElement, DrawableElement {

    private static RadioDataManager instance;
    private LineDataSelectorModel lineDataSelectorModel;

    private final DrawController drawController;
    private Map<Long, BufferedImage> bufferedImages;
    private YAxis yAxis;

    private boolean isVisible;

    private final HashMap<Long, DownloadedJPXData> cache = new HashMap<Long, DownloadedJPXData>();;
    private static final String ROBserver = DataSources.ROBsettings.get("API.jp2images.path");
    public static final int MAX_AMOUNT_OF_DAYS = 3;
    public static final int DAYS_IN_CACHE = MAX_AMOUNT_OF_DAYS + 1;

    private RadioDataManager() {
        ColorLookupModel.getInstance().addFilterModelListener(this);
        drawController = EVEPlugin.dc;
        bufferedImages = new HashMap<Long, BufferedImage>();
        yAxis = new YAxis(new Range(400, 20), "Mhz", false);
        isVisible = true;
    }

    public static RadioDataManager getSingletonInstance() {
        if (instance == null) {
            instance = new RadioDataManager();
            instance.init();
        }
        return instance;
    }

    private void init() {
        lineDataSelectorModel = LineDataSelectorModel.getSingletonInstance();
    }

    public void clearCache() {
        for (DownloadedJPXData jpxData : cache.values()) {
            if (jpxData.isInited())
                jpxData.remove();
        }
        cache.clear();
    }

    public HashMap<Long, DownloadedJPXData> getCache() {
        return cache;
    }

    public void requestAndOpenIntervals(long start, long end) {
        final ArrayList<Long> toDownloadStartDates = new ArrayList<Long>();
        long startDate = start - start % TimeUtils.DAY_IN_MILLIS;

        ArrayList<Long> incomingStartDates = new ArrayList<Long>(DAYS_IN_CACHE);
        for (int i = 0; i < DAYS_IN_CACHE; i++) {
            incomingStartDates.add(startDate + i * TimeUtils.DAY_IN_MILLIS);
        }

        Iterator<Entry<Long, DownloadedJPXData>> it = cache.entrySet().iterator();
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
            if (!cache.containsKey(incomingStart)) {
                toDownloadStartDates.add(incomingStart);
                cache.put(incomingStart, new DownloadedJPXData(incomingStart, incomingStart + TimeUtils.DAY_IN_MILLIS));
            }
        }

        if (!toDownloadStartDates.isEmpty()) {
            LineDataSelectorModel.getSingletonInstance().downloadStarted(RadioDataManager.getSingletonInstance());
            JHVWorker<ArrayList<JP2ViewCallisto>, Void> imageDownloadWorker = new RadioJPXDownload().init(toDownloadStartDates);
            imageDownloadWorker.setThreadName("EVE--RadioDownloader");
            EVESettings.getExecutorService().execute(imageDownloadWorker);
        }

        Iterator<Entry<Long, DownloadedJPXData>> itt = cache.entrySet().iterator();

    }

    public void initJPX(ArrayList<JP2ViewCallisto> jpList, ArrayList<Long> datesToDownload) {
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
            }
            else {
                jpxData.downloadJPXFailed();
            }
        }
    }

    private void removeRadioData() {
        clearCache();
        lineDataSelectorModel.removeLineData(this);
    }

    public void radioDataVisibilityChanged() {
        if (isVisible) {
            drawController.updateDrawableElement(this, true);
        } else {
            drawController.removeDrawableElement(this);
        }

        lineDataSelectorModel.lineDataElementUpdated(this);
    }

    void requestForData() {
        for (DownloadedJPXData jpxData : cache.values()) {
            jpxData.requestData();
        }
    }

    @Override
    public void colorLUTChanged() {
        ColorModel cm = ColorLookupModel.getInstance().getColorModel();
        Map<Long, BufferedImage> newBufferedImages = new HashMap<Long, BufferedImage>();
        for (Map.Entry<Long, BufferedImage> entry : bufferedImages.entrySet()) {
            long index = entry.getKey();
            BufferedImage old = entry.getValue();
            BufferedImage newIm = new BufferedImage(cm, old.getRaster(), false, null);
            newBufferedImages.put(index, newIm);
        }
        bufferedImages = newBufferedImages;
    }

    @Override
    public YAxis getYAxis() {
        return yAxis;
    }

    @Override
    public void removeLineData() {
        removeRadioData();
    }

    @Override
    public void setVisibility(boolean visible) {
        isVisible = visible;
        radioDataVisibilityChanged();
    }

    @Override
    public boolean isVisible() {
        return isVisible;
    }

    @Override
    public String getName() {
        return "Callisto radiogram";
    }

    @Override
    public Color getDataColor() {
        return null;
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
        return new RadioOptionsPanel();
    }

    @Override
    public boolean hasData() {
        return true;
    }

    @Override
    public boolean isDeletable() {
        return true;
    }

    @Override
    public DrawableElementType getDrawableElementType() {
        return DrawableElementType.RADIO;
    }

    private long xmin;
    private long xmax;

    public boolean xAxisChanged(TimeAxis timeAxis) {
        boolean cond = timeAxis.min == xmin && timeAxis.max == xmax;
        xmin = timeAxis.min;
        xmax = timeAxis.max;
        return !cond;
    }

    private double ymin;
    private double ymax;

    public boolean yAxisChanged(YAxis yAxis) {
        boolean cond = yAxis.getSelectedRange().min == ymin && yAxis.getSelectedRange().max == ymax;
        ymin = yAxis.getSelectedRange().min;
        ymax = yAxis.getSelectedRange().max;
        return !cond;
    }

    @Override
    public void draw(Graphics2D g, Graphics2D leftAxisG, Rectangle graphArea, Rectangle leftAxisArea, TimeAxis timeAxis, Point mousePosition) {
        boolean timediffCond = timeAxis.max - timeAxis.min <= TimeUtils.DAY_IN_MILLIS * MAX_AMOUNT_OF_DAYS;
        if (timediffCond && (xAxisChanged(timeAxis) || yAxisChanged(yAxis))) {
            requestForData();
            requestAndOpenIntervals(timeAxis.min, timeAxis.max);
        }
        if (timediffCond) {
            for (DownloadedJPXData djpx : cache.values()) {
                djpx.draw(g, graphArea, timeAxis, yAxis);
            }
        } else {
            String text1 = "The selected interval is too big.";
            String text2 = "Reduce the interval to see the radio spectrograms.";
            final int text1Width = (int) g.getFontMetrics().getStringBounds(text1, g).getWidth();
            final int text2Width = (int) g.getFontMetrics().getStringBounds(text2, g).getWidth();
            final int text1height = (int) g.getFontMetrics().getStringBounds(text2, g).getHeight();
            final int text2height = (int) g.getFontMetrics().getStringBounds(text2, g).getHeight();
            final int x1 = graphArea.x + (graphArea.width / 2) - (text1Width / 2);
            final int y1 = (int) (graphArea.y + (graphArea.height / 2) - 1.5 * text1height);
            final int x2 = graphArea.x + (graphArea.width / 2) - (text2Width / 2);
            final int y2 = (int) (graphArea.y + graphArea.height / 2 + 0.5 * text2height);
            g.setColor(Color.black);
            g.drawString(text1, x1, y1);
            g.drawString(text2, x2, y2);
        }
    }

    @Override
    public void setYAxis(YAxis _yAxis) {
        yAxis = _yAxis;
    }

    @Override
    public boolean hasElementsToDraw() {
        return true;
    }

    @Override
    public long getLastDateWithData() {
        return -1;
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
                RadioDataManager.getSingletonInstance().initJPX(jpList, datesToDownload);

            } catch (InterruptedException e) {
                Log.error("ImageDownloadWorker execution interrupted: " + e.getMessage());
            } catch (ExecutionException e) {
                Log.error("ImageDownloadWorker execution error: " + e.getMessage());
            }
        }

    }
}
