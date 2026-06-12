package org.helioviewer.jhv.timelines.radio;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.nio.ByteBuffer;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DataUri;
import org.helioviewer.jhv.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.threads.LatestWorker;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.draw.TimeAxis;
import org.helioviewer.jhv.timelines.draw.YAxis;
import org.helioviewer.jhv.view.View;
import org.helioviewer.jhv.view.j2k.J2KViewCallisto;
import org.helioviewer.jhv.view.j2k.ResolutionSet;

class RadioJ2KData implements View.DataHandler {

    private final LatestWorker<ImageBuffer> executor = new LatestWorker<>("Radio-Decoder");
    private final J2KViewCallisto view;
    private boolean disposed;

    private final long startDate;
    private final long endDate;
    private final double startFreq;
    private final double endFreq;
    private final int j2kWidth;
    private final int j2kHeight;
    private final boolean willDraw;

    private BufferedImage bufferedImage;
    private Region region;

    RadioJ2KData(APIRequest req, DataUri dataUri) throws Exception {
        J2KViewCallisto v = null;
        try {
            v = new J2KViewCallisto(executor, req, dataUri);

            ResolutionSet.Level resLevel = v.getResolutionLevel(0, 0);
            j2kWidth = resLevel.width();
            j2kHeight = resLevel.height();

            XMLMetaDataContainer hvMetaData = new XMLMetaDataContainer(v.getXMLMetaData());
            endFreq = hvMetaData.getRequiredDouble("STARTFRQ");
            startFreq = hvMetaData.getRequiredDouble("END-FREQ");
            startDate = TimeUtils.parse(hvMetaData.getRequiredString("DATE-OBS"));
            endDate = TimeUtils.parse(hvMetaData.getRequiredString("DATE-END"));
            if (endDate <= startDate || startFreq <= endFreq) { // frequency is drawn upside down
                throw new IllegalArgumentException("Invalid radio metadata range");
            }

            v.setDataHandler(this);
            willDraw = startDate == req.startTime(); // didn't get closest
            view = v;
        } catch (Exception e) {
            executor.abolish();
            if (v != null) {
                v.setDataHandler(null);
                v.abolish();
            }
            throw e;
        }
    }

    void removeData() {
        if (disposed) {
            return;
        }
        disposed = true;
        executor.abolish();
        view.setDataHandler(null);
        view.abolish();
        bufferedImage = null;
    }

    @Override
    public void handleData(View.ImageData imageData) {
        ImageBuffer imageBuffer = imageData.imageBuffer();
        int w = imageBuffer.width;
        int h = imageBuffer.height;
        if (w < 1 || h < 1) {
            Log.error("width: " + w + " height: " + h);
            return;
        }

        region = imageData.region();
        boolean hadData = bufferedImage != null;
        bufferedImage = createIndexedImage((ByteBuffer) imageBuffer.buffer, w, h, RadioData.getColorModel());
        imageBuffer.allowExplicitFree();
        if (!hadData)
            RadioData.dataUpdated();
        DrawController.drawRequest();
    }

    private static BufferedImage createIndexedImage(ByteBuffer byteBuffer, int width, int height, IndexColorModel colorModel) {
        byte[] pixels = new byte[byteBuffer.remaining()];
        byteBuffer.slice().get(pixels);

        BufferedImage sample = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_INDEXED, colorModel);
        SampleModel sampleModel = sample.getSampleModel().createCompatibleSampleModel(width, height);
        DataBufferByte dataBuffer = new DataBufferByte(pixels, pixels.length);
        return new BufferedImage(colorModel, Raster.createWritableRaster(sampleModel, dataBuffer, null), false, null);
    }

    void requestData(TimeAxis xAxis) {
        if (willDraw && !disposed) {
            Rectangle roi = getROI(xAxis);
            if (roi != null) {
                view.setDecodeRegion(roi.x, roi.y, roi.width, roi.height);
                view.decode(null, 1, lastState.resolution);
            }
        }
    }

    private float computeResolution(TimeAxis xAxis) {
        double pct = Math.min((xAxis.end() - xAxis.start()) / (double) (endDate - startDate), 1.0);
        float res = 1f;
        while (res > 0.03125f && pct > 0.03125f / res) {
            res *= 0.5f;
        }
        return res;
    }

    private record DecodeState(float resolution, long paddedStart, long paddedEnd) {}

    private DecodeState lastState;

    private Rectangle getROI(TimeAxis xAxis) {
        long visibleStart = Math.max(startDate, xAxis.start());
        long visibleEnd = Math.min(endDate, xAxis.end());
        float resolution = computeResolution(xAxis);

        if (lastState != null
                && lastState.resolution == resolution
                && visibleStart >= lastState.paddedStart
                && visibleEnd <= lastState.paddedEnd) {
            return null;
        }

        long newVisibleStart = startDate;
        long newVisibleEnd = endDate;
        if (lastState != null) {
            long margin = xAxis.end() - xAxis.start();
            newVisibleStart = Math.max(startDate, xAxis.start() - margin);
            newVisibleEnd = Math.min(endDate, xAxis.end() + margin);
        }

        double pixPerTime = j2kWidth / (double) (endDate - startDate);
        int x0 = (int) Math.round((newVisibleStart - startDate) * pixPerTime);
        int width = (int) Math.round((newVisibleEnd - newVisibleStart) * pixPerTime);

        if (width <= 0) {
            return null;
        }

        lastState = new DecodeState(resolution, newVisibleStart, newVisibleEnd);

        return new Rectangle(x0, 0, width, j2kHeight);
    }

    void draw(Graphics2D g, Rectangle ga, TimeAxis.Mapper xMapper, YAxis.Mapper yMapper) {
        if (!willDraw) {
            return;
        }
        if (!hasData()) {
            RadioData.drawMessage(g, ga, "Fetching data");
            return;
        }

        long timeWidth = endDate - startDate;
        long imStart = (long) (startDate + timeWidth * region.llx / j2kWidth);
        long imEnd = (long) (startDate + timeWidth * region.urx / j2kWidth);

        double freqWidth = endFreq - startFreq;
        double freqimStart = startFreq + freqWidth * region.lly / j2kHeight;
        double freqimEnd = startFreq + freqWidth * region.ury / j2kHeight;

        g.drawImage(bufferedImage,
                xMapper.toPixel(imStart),
                yMapper.dataToPixel(freqimStart),
                xMapper.toPixel(imEnd),
                yMapper.dataToPixel(freqimEnd),
                0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), null);
    }

    void changeColormap(ColorModel cm) {
        if (hasData()) {
            bufferedImage = new BufferedImage(cm, bufferedImage.getRaster(), false, null);
        }
    }

    public boolean hasData() {
        return bufferedImage != null;
    }

}
