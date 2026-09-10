package org.helioviewer.jhv.view;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.image.lut.LUT;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeMap;

public class ManyView implements View {

    private record FrameInfo(View view, JHVTime timeView, int idxView) {}

    private final TimeMap<FrameInfo> frameMap = new TimeMap<>();
    private final boolean hasFITS;
    private final @Nullable ClipSet clipSet;
    private int targetFrame;

    public ManyView(List<View> views) throws IOException {
        if (views.isEmpty())
            throw new IOException("Empty list of views");

        hasFITS = views.stream().anyMatch(View::hasFITS);
        views.forEach(this::putDates);
        frameMap.buildIndex();
        List<ClipSet> clipSets = new ArrayList<>();
        for (FrameInfo frameInfo : frameMap.values()) {
            clipSets.add(frameInfo.view.getClipSet());
        }
        clipSet = ClipSet.median(clipSets);
        // unused J2KViews should be abolished by their reaper
    }

    private void putDates(View v) {
        if (v instanceof ManyView manyView) {
            frameMap.putAll(manyView.frameMap);
            return;
        }
        int m = v.getMaximumFrameNumber();
        for (int i = 0; i <= m; i++) {
            JHVTime t = v.getFrameTime(i);
            frameMap.put(t, new FrameInfo(v, t, i));
        }
    }

    @Override
    public void abolish() {
        frameMap.values().forEach(frameInfo -> frameInfo.view.abolish());
    }

    @Override
    public void clearCache() {
        frameMap.values().forEach(frameInfo -> frameInfo.view.clearCache());
    }

    @Override
    public void decode(Position viewpoint, double pixFactor, float factor, @Nullable ClipSet.Range clipRange) {
        frameMap.indexedValue(targetFrame).view.decode(viewpoint, pixFactor, factor, clipRange);
    }

    @Nullable
    @Override
    public LUT getDefaultLUT() {
        return frameMap.indexedValue(0).view.getDefaultLUT();
    }

    @Nullable
    @Override
    public ClipSet getClipSet() {
        return clipSet;
    }

    @Override
    public boolean hasFITS() {
        return hasFITS;
    }

    @Override
    public boolean isMultiFrame() {
        return frameMap.maxIndex() > 0;
    }

    @Override
    public int getCurrentFrameNumber() {
        return targetFrame;
    }

    @Override
    public int getMaximumFrameNumber() {
        return frameMap.maxIndex();
    }

    @Override
    public void setDataHandler(View.DataHandler dataHandler) {
        frameMap.values().forEach(frameInfo -> frameInfo.view.setDataHandler(dataHandler));
    }

    @Nullable
    @Override
    public AtomicBoolean getFrameCompletion(int frame) {
        FrameInfo frameInfo = frameMap.indexedValue(frame);
        return frameInfo.view.getFrameCompletion(frameInfo.idxView);
    }

    @Override
    public JHVTime getFrameTime(int frame) {
        return frameMap.key(frame);
    }

    @Override
    public JHVTime getFirstTime() {
        return frameMap.firstKey();
    }

    @Override
    public JHVTime getLastTime() {
        return frameMap.lastKey();
    }

    @Override
    public boolean setNearestFrame(JHVTime time) {
        int frame = frameMap.nearestIndex(time);
        FrameInfo frameInfo = frameMap.indexedValue(frame);
        if (frameInfo.view.setNearestFrame(frameInfo.timeView)) {
            targetFrame = frame;
            return true;
        }
        return false;
    }

    @Override
    public JHVTime getNearestTime(JHVTime time) {
        return frameMap.nearestKey(time);
    }

    @Override
    public JHVTime getLowerTime(JHVTime time) {
        return frameMap.lowerKey(time);
    }

    @Override
    public JHVTime getHigherTime(JHVTime time) {
        return frameMap.higherKey(time);
    }

    @Override
    public MetaData getMetaData(JHVTime time) {
        FrameInfo frameInfo = frameMap.nearestValue(time);
        return frameInfo.view.getMetaData(frameInfo.timeView);
    }

    @Nonnull
    @Override
    public String getXMLMetaData() {
        return frameMap.indexedValue(targetFrame).view.getXMLMetaData();
    }

}
