package org.helioviewer.jhv.timelines.radio;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.imagedata.ImageDataHandler;
import org.helioviewer.jhv.imagedata.Single8ImageData;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.draw.TimeAxis;
import org.helioviewer.jhv.view.jp2view.JP2ViewCallisto;
import org.helioviewer.jhv.view.jp2view.image.ResolutionSet;

class RadioJP2Data implements ImageDataHandler {

    private JP2ViewCallisto view;

    private long startDate;
    private long endDate;
    private double startFreq;
    private double endFreq;
    private int jp2Width;
    private int jp2Height;

    private BufferedImage bufferedImage;
    private Region region;
    private boolean failed;

    RadioJP2Data(JP2ViewCallisto _view, long start) {
        if (_view != null) { // null for empty
            try {
                ResolutionSet.ResolutionLevel resLevel = _view.getResolutionLevel(0, 0);
                jp2Width = resLevel.width;
                jp2Height = resLevel.height;

                XMLMetaDataContainer hvMetaData = new XMLMetaDataContainer();

                hvMetaData.parseXML(_view.getXMLMetaData());
                endFreq = hvMetaData.getRequiredDouble("STARTFRQ");
                startFreq = hvMetaData.getRequiredDouble("END-FREQ");
                startDate = TimeUtils.parse(hvMetaData.getRequiredString("DATE-OBS"));
                endDate = TimeUtils.parse(hvMetaData.getRequiredString("DATE-END"));
                hvMetaData.destroyXML();

                if (startDate == start && endDate <= start + TimeUtils.DAY_IN_MILLIS) {
                    view = _view;
                    view.setDataHandler(this);
                    return;
                }
            } catch (Exception e) {
                Log.error("Some of the metadata could not be read: " + _view.getURI() + " " + e);
            }
            _view.abolish();
        }

        failed = true;
        startDate = start;
        endDate = start + TimeUtils.DAY_IN_MILLIS;
    }

    long getStartDate() {
        return startDate;
    }

    void removeData() {
        if (view != null) {
            view.setDataHandler(null);
            view.abolish();
            view = null;
        }
        bufferedImage = null;
    }

    @Override
    public void handleData(ImageData imageData) {
        if (imageData instanceof Single8ImageData) {
            int w = imageData.getWidth();
            int h = imageData.getHeight();
            if (w < 1 || h < 1) {
                Log.error("width: " + w + " height: " + h);
                return;
            }

            region = imageData.getRegion();
            byte[] data = (byte[]) imageData.getBuffer().array();
            bufferedImage = createBufferedImage(w, h, data);

            DrawController.drawRequest();
        }
    }

    private static BufferedImage createBufferedImage(int width, int height, byte[] data) {
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, RadioData.getColorModel());
        DataBufferByte dataBuffer = new DataBufferByte(data, width * height);
        Raster raster = Raster.createPackedRaster(dataBuffer, width, height, width, new int[] { 0xff }, new Point(0, 0));
        newImage.setData(raster);
        return newImage;
    }

    void requestData(TimeAxis xAxis) {
        if (view != null) {
            Rectangle roi = getROI(xAxis);
            if (decodingNeeded && roi.width > 0 && roi.height > 0) {
                view.setRegion(roi);
                view.render(null, null, last_resolution);
            }
        }
    }

    private double computeResolution(int height, TimeAxis xAxis) {
        double pixPerTime = jp2Width / (double) (endDate - startDate);
        int width = (int) ((xAxis.end - xAxis.start) * pixPerTime + 0.5);
        double pct = Math.min(width / (double) jp2Width, 1);

        double visibleImagePercentage = pct * height / jp2Height;
        if (visibleImagePercentage <= 0.03125)
            return 1;
        if (visibleImagePercentage <= 0.0625)
            return 0.5;
        if (visibleImagePercentage <= 0.125)
            return 0.25;
        if (visibleImagePercentage <= 0.25)
            return 0.125;
        if (visibleImagePercentage <= 0.5)
            return 0.0625;
        return 0.03125;
    }

    private boolean first = true;
    private boolean decodingNeeded = false;
    private double last_resolution = -1;
    private long last_padded_start = -1;
    private long last_padded_end = -1;
    private int last_y0 = -1;
    private int last_height = -1;

    private Rectangle getROI(TimeAxis xAxis) {
        double visibleStartFreq = startFreq;
        double visibleEndFreq = endFreq;
        if (visibleStartFreq < RadioData.yAxis.start) {
            visibleStartFreq = RadioData.yAxis.start;
        }
        if (visibleEndFreq > RadioData.yAxis.end) {
            visibleEndFreq = RadioData.yAxis.end;
        }

        double pixPerFreq = jp2Height / (endFreq - startFreq);
        int y0 = (int) ((endFreq - visibleEndFreq) * pixPerFreq + 0.5);
        int height = (int) ((visibleEndFreq - visibleStartFreq) * pixPerFreq + 0.5);

        long visibleStart = Math.max(startDate, xAxis.start);
        long visibleEnd = Math.min(endDate, xAxis.end);
        double resolution = computeResolution(height, xAxis);

        if (last_resolution == resolution && y0 == last_y0 && height == last_height && visibleStart >= last_padded_start && visibleEnd <= last_padded_end) {
            decodingNeeded = false;
            return new Rectangle(0, 0, -1, -1);
        }
        decodingNeeded = true;

        long ilen = xAxis.end - xAxis.start;
        long padded_start = xAxis.start - ilen;
        long padded_end = xAxis.end + ilen;

        long newVisibleStart = startDate;
        long newVisibleEnd = endDate;
        if (!first) {
            newVisibleStart = Math.max(startDate, padded_start);
            newVisibleEnd = Math.min(endDate, padded_end);
        }
        first = false;

        double pixPerTime = jp2Width / (double) (endDate - startDate);
        int x0 = (int) ((newVisibleStart - startDate) * pixPerTime + 0.5);
        int width = (int) ((newVisibleEnd - newVisibleStart) * pixPerTime + 0.5);

        last_padded_end = newVisibleEnd;
        last_padded_start = newVisibleStart;
        last_y0 = y0;
        last_height = height;
        last_resolution = resolution;

        return new Rectangle(x0, y0, width, height);
    }

    void draw(Graphics2D g, Rectangle ga, TimeAxis xAxis) {
        if (hasData()) {
            int sx0 = 0;
            int sy0 = 0;
            int sx1 = bufferedImage.getWidth();
            int sy1 = bufferedImage.getHeight();
            long imStart = (long) (startDate + (endDate - startDate) * region.llx / jp2Width);
            long imEnd = (long) (startDate + (endDate - startDate) * region.urx / jp2Width);

            double freqimStart = (startFreq + (endFreq - startFreq) * region.lly / jp2Height);
            double freqimEnd = (startFreq + (endFreq - startFreq) * region.ury / jp2Height);

            int dx0 = xAxis.value2pixel(ga.x, ga.width, imStart);
            int dx1 = xAxis.value2pixel(ga.x, ga.width, imEnd);

            int dy0 = RadioData.yAxis.value2pixel(ga.y, ga.height, freqimStart);
            int dy1 = RadioData.yAxis.value2pixel(ga.y, ga.height, freqimEnd);

            g.drawImage(bufferedImage, dx0, dy0, dx1, dy1, sx0, sy0, sx1, sy1, null);
        } else {
            drawNoData(g, ga, xAxis);
        }
    }

    private void drawNoData(Graphics2D g, Rectangle ga, TimeAxis xAxis) {
        int dx0 = xAxis.value2pixel(ga.x, ga.width, Math.max(startDate, xAxis.start));
        int dx1 = xAxis.value2pixel(ga.x, ga.width, Math.min(endDate, xAxis.end));
        int dwidth = dx1 - dx0;
        g.setColor(Color.GRAY);
        g.fillRect(dx0, ga.y, dwidth, ga.height);
        g.setColor(Color.WHITE);

        String text = failed ? "No data available" : "Fetching data";
        Rectangle2D r = g.getFontMetrics().getStringBounds(text, g);
        int tWidth = (int) r.getWidth();
        int tHeight = (int) r.getHeight();
        int x = dx0 + dwidth / 2 - tWidth / 2;
        int y = ga.y + ga.height / 2 - tHeight / 2;
        g.drawString(text, x, y);
    }

    boolean isDownloading() {
        return !failed && !hasData();
    }

    void changeColormap(ColorModel cm) {
        if (hasData()) {
            BufferedImage old = bufferedImage;
            bufferedImage = new BufferedImage(cm, old.getRaster(), false, null);
        }
    }

    public boolean hasData() {
        return bufferedImage != null;
    }

}
