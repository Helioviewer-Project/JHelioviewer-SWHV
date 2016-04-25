package org.helioviewer.jhv.plugins.eveplugin.radio.data;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.util.Date;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimeAxis;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxis;
import org.helioviewer.jhv.plugins.eveplugin.radio.model.ColorLookupModel;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.ImageDataHandler;
import org.helioviewer.jhv.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.jhv.viewmodel.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2Image.ReaderMode;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2ImageCallisto;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2ViewCallisto;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet;

public class DownloadedJPXData implements ImageDataHandler {
    private JP2ViewCallisto view;
    private boolean inited = false;
    private boolean hasData = false;

    private final long startDate;
    private final long endDate;
    private double startFreq = 400;
    private double endFreq = 20;
    private int jp2Width;
    private int jp2Height;

    private BufferedImage bufferedImage;
    private Region region;
    private boolean downloadJPXFailed = false;

    public DownloadedJPXData(long _startDate, long _endDate) {
        startDate = _startDate;
        endDate = _endDate;
    }

    public void init(JP2ViewCallisto _view) {
        view = _view;
        view.setDataHandler(this);
        JP2ImageCallisto image = view.getJP2Image();
        image.setReaderMode(ReaderMode.ONLYFIREONCOMPLETE);
        ResolutionSet resolutionSet = image.getResolutionSet(0);
        jp2Width = resolutionSet.getResolutionLevel(0).width;
        jp2Height = resolutionSet.getResolutionLevel(0).height;
        XMLMetaDataContainer hvMetaData = new XMLMetaDataContainer();
        try {
            hvMetaData.parseXML(image.getXML(0));
            endFreq = hvMetaData.tryGetDouble("STARTFRQ");
            startFreq = hvMetaData.tryGetDouble("END-FREQ");

            long start = JHVDate.parseDateTime(hvMetaData.get("DATE-OBS")).milli;
            long end = JHVDate.parseDateTime(hvMetaData.get("DATE-END")).milli;
            if (startDate != start || endDate != end)
                Log.warn("something is wrong with the jpx dates " + start + " " + end + " " + startDate + " " + endDate);

            hvMetaData.destroyXML();
        } catch (Exception e) {
            Log.error("Some of the metadata could not be read, aborting...");
            return;
        }
        requestData();
        inited = true;
    }

    public boolean isInited() {
        return inited;
    }

    public boolean hasData() {
        return hasData;
    }

    public JP2ViewCallisto getView() {
        return view;
    }

    public long getStartDate() {
        return startDate;
    }

    public long getEndDate() {
        return endDate;
    }

    public void remove() {
        if (view != null) {
            view.setDataHandler(null);
            view.abolish();
            view = null;
        }
        System.out.println("ABOLISH JPX " + new Date(startDate) + " " + this);

    }

    @Override
    public void handleData(ImageData imageData) {
        if (imageData instanceof SingleChannelByte8ImageData) {
            if (imageData.getWidth() < 1 || imageData.getHeight() < 1) {
                Log.error("width: " + imageData.getWidth() + " height: " + imageData.getHeight());
                return;
            }
            byte[] data = (byte[]) imageData.getBuffer().array();

            region = imageData.getRegion();
            bufferedImage = createBufferedImage(imageData.getWidth(), imageData.getHeight(), data);
            if (!hasData) {
                LineDataSelectorModel.getSingletonInstance().downloadFinished(RadioDataManager.getSingletonInstance());
            }
            hasData = true;
            EVEPlugin.dc.updateDrawableElement(RadioDataManager.getSingletonInstance(), true);

        }
    }

    private BufferedImage createBufferedImage(int width, int height, byte[] data) {
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, ColorLookupModel.getInstance().getColorModel());
        DataBufferByte dataBuffer = new DataBufferByte(data, width * height);
        Raster raster = Raster.createPackedRaster(dataBuffer, width, height, width, new int[] { 0xff }, new Point(0, 0));
        newImage.setData(raster);
        return newImage;
    }

    public void requestData() {
        if (inited) {
            JP2ViewCallisto jp2View = getView();
            JP2ImageCallisto image = jp2View.getJP2Image();
            Rectangle roi = getROI(EVEPlugin.dc.selectedAxis, RadioDataManager.getSingletonInstance().getYAxis());

            if (roi.width > 0 && roi.height > 0) {
                image.setRegion(roi);
                jp2View.render(null, null, defineFactor(getVisibleImagePercentage(roi)));
            }
        }
    }

    public double getVisibleImagePercentage(Rectangle roi) {
        return roi.getWidth() * roi.getHeight() / (jp2Width * jp2Height);
    }

    //private final double[] factors = { 0.03125, 0.0625, 0.125, 0.25, 0.5 };

    private double defineFactor(double visibleImagePercentage) {
        if (visibleImagePercentage <= 0.03125) {
            return 1;
        } else if (visibleImagePercentage <= 0.0625) {
            return 0.5;
        } else if (visibleImagePercentage <= 0.125) {
            return 0.25;
        } else if (visibleImagePercentage <= 0.25) {
            return 0.125;
        } else if (visibleImagePercentage <= 0.5) {
            return 0.0625;
        } else {
            return 0.03125;
        }
    }

    private boolean first = true;

    public Rectangle getROI(TimeAxis xAxis, YAxis yAxis) {
        long imageTimesize = endDate - startDate;
        int imageFrequencySize = (int) (endFreq - startFreq);
        double timePerPix = 1.0 * imageTimesize / jp2Width;
        double freqPerPix = 1.0 * imageFrequencySize / jp2Height;

        long visibleStart = startDate;
        long visibleEnd = endDate;

        if (!first) {
            if (visibleStart <= xAxis.min)
                visibleStart = xAxis.min;

            if (visibleEnd >= xAxis.max)
                visibleEnd = xAxis.max;
        }
        first = false;

        double visibleStartFreq = startFreq;
        double visibleEndFreq = endFreq;
        if (visibleStartFreq < yAxis.getSelectedRange().min) {
            visibleStartFreq = yAxis.getSelectedRange().min;
        }
        if (visibleEndFreq > yAxis.getSelectedRange().max) {
            visibleEndFreq = yAxis.getSelectedRange().max;
        }

        int x0 = (int) Math.round((visibleStart - startDate) / timePerPix);
        int y0 = (int) Math.round((endFreq - visibleEndFreq) / freqPerPix);
        int width = (int) Math.round((visibleEnd - visibleStart) / timePerPix);
        int height = (int) Math.round((visibleEndFreq - visibleStartFreq) / freqPerPix);
        return new Rectangle(x0, y0, width, height);
    }

    public void draw(Graphics2D g, Rectangle ga, TimeAxis xAxis, YAxis yAxis) {
        if (hasData) {
            int sx0 = 0;
            int sx1 = bufferedImage.getWidth();
            int sy0 = 0;
            int sy1 = bufferedImage.getHeight();
            long imStart = (long) (startDate + (endDate - startDate) * region.llx / jp2Width);
            long imEnd = (long) (startDate + (endDate - startDate) * region.urx / jp2Width);

            double freqimStart = (startFreq + (endFreq - startFreq) * region.lly / jp2Height);
            double freqimEnd = (startFreq + (endFreq - startFreq) * region.ury / jp2Height);

            int dx0 = xAxis.value2pixel(ga.x, ga.width, imStart);
            int dx1 = xAxis.value2pixel(ga.x, ga.width, imEnd);

            int dy0 = yAxis.value2pixel(ga.y, ga.height, freqimStart);
            int dy1 = yAxis.value2pixel(ga.y, ga.height, freqimEnd);

            g.drawImage(bufferedImage, dx0, dy0, dx1, dy1, sx0, sy0, sx1, sy1, null);
        }
        else {
            drawNoData(g, ga, xAxis);
        }
    }

    public void drawNoData(Graphics2D g, Rectangle ga, TimeAxis xAxis) {
        int dx0 = xAxis.value2pixel(ga.x, ga.width, Math.max(startDate, xAxis.min));
        int dx1 = xAxis.value2pixel(ga.x, ga.width, Math.min(endDate, xAxis.max));
        int dy0 = 0;
        int dy1 = ga.height;
        int dwidth = dx1 - dx0;
        int dheight = dy1 - dy0;
        g.setColor(Color.GRAY);
        g.fillRect(dx0, dy0, dwidth, dheight);
        g.setColor(Color.WHITE);

        String text = "Fetching data";
        if (downloadJPXFailed) {
            text = "No data available";
        }
        int textWidth = (int) g.getFontMetrics().getStringBounds(text, g).getWidth();
        int textHeight = (int) g.getFontMetrics().getStringBounds(text, g).getHeight();
        int x1 = dx0 + dwidth / 2 - textWidth / 2;
        int y1 = dheight / 2 - textHeight / 2;
        g.drawString(text, x1, y1);
    }

    public void downloadJPXFailed() {
        downloadJPXFailed = true;
    }

    public boolean isDownloading() {
        return !hasData && !downloadJPXFailed;
    }

}
