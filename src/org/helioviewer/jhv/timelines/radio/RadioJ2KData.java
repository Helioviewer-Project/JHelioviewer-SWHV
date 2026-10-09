package org.helioviewer.jhv.timelines.radio;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DataUri;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.source.J2KSource;
import org.helioviewer.jhv.source.ResolutionSet;
import org.helioviewer.jhv.thread.AppThread;
import org.helioviewer.jhv.thread.LatestWorker;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.draw.TimeAxis;
import org.helioviewer.jhv.timelines.draw.YAxis;

class RadioJ2KData {

    private static final int QUANTA = 32;

    // Columns of a level, over its whole height.
    record Crop(int level, int x, int width, int height) {}

    private final RadioData owner;
    private final J2KSource source;
    private final LatestWorker<byte[]> executor;

    private final long startDate;
    private final long endDate;
    private final double startFreq;
    private final double endFreq;
    private final int j2kWidth;
    private final boolean willDraw;

    private BufferedImage bufferedImage;
    private Crop displayedCrop;

    RadioJ2KData(RadioData _owner, APIRequest req, DataUri dataUri) throws Exception {
        owner = _owner;
        source = new J2KSource(dataUri.file().toPath());
        try {
            ResolutionSet.Level resLevel = source.level(0, 0);
            j2kWidth = resLevel.width();

            String xml = source.xml(0);
            if (xml == null)
                throw new Exception("Missing XML metadata");
            XMLMetaDataContainer hvMetaData = new XMLMetaDataContainer(xml);
            endFreq = hvMetaData.getRequiredDouble("STARTFRQ");
            startFreq = hvMetaData.getRequiredDouble("END-FREQ");
            startDate = TimeUtils.parse(hvMetaData.getRequiredString("DATE-OBS"));
            endDate = TimeUtils.parse(hvMetaData.getRequiredString("DATE-END"));
            if (endDate <= startDate || startFreq <= endFreq) { // frequency is drawn upside down
                throw new IllegalArgumentException("Invalid radio metadata range");
            }

            willDraw = startDate == req.startTime(); // didn't get closest
            executor = new LatestWorker<>("Radio-Decoder");
        } catch (Exception | Error e) {
            source.close();
            throw e;
        }
    }

    void removeData() {
        executor.dispose();
        owner.removeDecoded(this);
        AppThread.create(source::close, "Radio-Close").start(); // not on the EDT
        bufferedImage = null;
    }

    private void show(Crop crop, byte[] pixels) {
        displayedCrop = crop;
        boolean hadData = bufferedImage != null;
        DataBufferByte dataBuffer = new DataBufferByte(pixels, pixels.length);
        bufferedImage = new BufferedImage(owner.getColorModel(),
                Raster.createInterleavedRaster(dataBuffer, crop.width, crop.height, crop.width, 1, new int[]{0}, null), false, null);
        if (!hadData)
            owner.dataUpdated();
        DrawController.drawRequest();
    }

    // The columns of a level which cover a full-resolution range, rounded to 32 pixels at full resolution.
    static Crop levelCrop(int x, int width, int fullWidth, ResolutionSet.Level res) {
        x = Math.min(MathUtils.roundDownTo(x, QUANTA), fullWidth - 1);
        width = Math.min(MathUtils.roundUpTo(width + QUANTA, QUANTA), fullWidth - x);
        int shift = res.level(), round = (1 << shift) - 1;
        int x0 = x >> shift;
        int x1 = Math.min((x + width + round) >> shift, res.width());
        return new Crop(shift, x0, x1 - x0, res.height());
    }

    void requestData(TimeAxis xAxis) {
        if (!willDraw)
            return;
        Crop crop = getCrop(xAxis);
        if (crop == null)
            return;

        byte[] pixels = owner.getDecoded(this, crop);
        if (pixels != null) {
            executor.invalidate(); // a running decode is stale
            show(crop, pixels);
            return;
        }
        executor.submit(() -> source.decode(0, crop.level, crop.x, 0, crop.width, crop.height), new LatestWorker.Callback<>() {
            @Override
            public void onSuccess(byte[] result, boolean fresh) {
                owner.putDecoded(RadioJ2KData.this, crop, result);
                if (fresh)
                    show(crop, result);
            }

            @Override
            public void onFailure(@Nonnull Throwable t, boolean fresh) {
                LatestWorker.Callback.super.onFailure(t, fresh);
                if (fresh) { // still the last request: ask again
                    lastState = null;
                    DrawController.drawRequest();
                }
            }
        });
    }

    // Full resolution up to 1/32 of the day; one level coarser per doubling.
    private int computeLevel(TimeAxis xAxis) {
        double pct = Math.min((xAxis.end() - xAxis.start()) / (double) (endDate - startDate), 1.0);
        int level = 0;
        while (level < 5 && pct * 32 > 1 << level) {
            level++;
        }
        return level;
    }

    private record DecodeState(int level, long paddedStart, long paddedEnd) {}

    private DecodeState lastState;

    // Null while the last request covers the view.
    @Nullable
    private Crop getCrop(TimeAxis xAxis) {
        long visibleStart = Math.max(startDate, xAxis.start());
        long visibleEnd = Math.min(endDate, xAxis.end());
        int level = computeLevel(xAxis);

        if (lastState != null
                && lastState.level == level
                && visibleStart >= lastState.paddedStart
                && visibleEnd <= lastState.paddedEnd) {
            return null;
        }

        long margin = xAxis.end() - xAxis.start();
        long newVisibleStart = Math.max(startDate, xAxis.start() - margin);
        long newVisibleEnd = Math.min(endDate, xAxis.end() + margin);

        double pixPerTime = j2kWidth / (double) (endDate - startDate);
        int x0 = (int) Math.round((newVisibleStart - startDate) * pixPerTime);
        int width = (int) Math.round((newVisibleEnd - newVisibleStart) * pixPerTime);

        if (width <= 0) {
            return null;
        }

        lastState = new DecodeState(level, newVisibleStart, newVisibleEnd);

        return levelCrop(x0, width, j2kWidth, source.level(0, level));
    }

    void draw(Graphics2D g, TimeAxis.Mapper xMapper, YAxis.Mapper yMapper) {
        if (!hasData())
            return;

        long timeWidth = endDate - startDate;
        double firstColumn = displayedCrop.x << displayedCrop.level;
        double endColumn = Math.min(firstColumn + ((long) displayedCrop.width << displayedCrop.level), j2kWidth);
        long imStart = (long) (startDate + timeWidth * firstColumn / j2kWidth);
        long imEnd = (long) (startDate + timeWidth * endColumn / j2kWidth);

        g.drawImage(bufferedImage,
                xMapper.toPixel(imStart),
                yMapper.dataToPixel(startFreq),
                xMapper.toPixel(imEnd),
                yMapper.dataToPixel(endFreq),
                0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), null);
    }

    boolean isLoading() {
        return lastState != null && !hasData();
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
