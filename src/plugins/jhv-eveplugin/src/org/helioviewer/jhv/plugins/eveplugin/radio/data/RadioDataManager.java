package org.helioviewer.jhv.plugins.eveplugin.radio.data;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.HashMap;
import java.util.Map;

import org.helioviewer.jhv.base.Range;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawController;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawableElement;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawableElementType;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimeAxis;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxis;
import org.helioviewer.jhv.plugins.eveplugin.radio.gui.RadioOptionsPanel;
import org.helioviewer.jhv.plugins.eveplugin.radio.model.ColorLookupModel;
import org.helioviewer.jhv.plugins.eveplugin.radio.model.ColorLookupModelListener;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;

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

    private final boolean isDownloading;
    private boolean isVisible;

    private RadioDataManager() {
        ColorLookupModel.getInstance().addFilterModelListener(this);
        drawController = EVEPlugin.dc;
        bufferedImages = new HashMap<Long, BufferedImage>();
        yAxis = new YAxis(new Range(400, 20), "Mhz", false);
        isVisible = true;
        isDownloading = false;
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

    private void clearRadioData() {
        RadioDownloader.getSingletonInstance().clearCache();
    }

    private void removeRadioData() {
        clearRadioData();
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
        for (DownloadedJPXData jpxData : RadioDownloader.getSingletonInstance().getCache().values()) {
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
        for (DownloadedJPXData jpxData : RadioDownloader.getSingletonInstance().getCache().values()) {
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
        boolean timediffCond = timeAxis.max - timeAxis.min <= TimeUtils.DAY_IN_MILLIS * RadioDownloader.MAX_AMOUNT_OF_DAYS;
        if (timediffCond && (xAxisChanged(timeAxis) || yAxisChanged(yAxis))) {
            requestForData();
            RadioDownloader.getSingletonInstance().requestAndOpenIntervals(timeAxis.min, timeAxis.max);
        }
        if (timediffCond) {
            HashMap<Long, DownloadedJPXData> cache = RadioDownloader.getSingletonInstance().getCache();
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
}
