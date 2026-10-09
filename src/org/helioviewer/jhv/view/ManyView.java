package org.helioviewer.jhv.view;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.image.lut.LUT;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.thread.AppThread;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeMap;

public class ManyView implements View {

    private record FrameInfo(View view, JHVTime timeView, int idxView) {}

    private final TimeMap<FrameInfo> frameMap = new TimeMap<>();
    private final List<View> views;
    private final boolean hasFITS;
    private final @Nullable ClipSet clipSet;
    private int targetFrame;

    public ManyView(List<View> _views) throws IOException {
        if (_views.isEmpty())
            throw new IOException("Empty list of views");

        hasFITS = _views.stream().anyMatch(View::hasFITS);
        _views.forEach(this::putDates);
        frameMap.buildIndex();
        views = List.copyOf(new LinkedHashSet<>(_views));
        List<ClipSet> clipSets = new ArrayList<>();
        for (FrameInfo frameInfo : frameMap.values()) {
            clipSets.add(frameInfo.view.getClipSet());
        }
        clipSet = ClipSet.median(clipSets);
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
        clearCache();
        AppThread.create(this::closeSources, "JHV-CollectionCloser").start();
    }

    @Override
    public void collectCacheOwners(Set<Object> owners) {
        for (View view : views)
            view.collectCacheOwners(owners);
    }

    @Override
    public void closeSources() {
        for (View view : views) {
            try {
                view.closeSources();
            } catch (Exception e) {
                Log.error(e);
            }
        }
    }

    @Override
    public void decode(Position viewpoint, double pixFactor, @Nullable ClipSet.Range clipRange) {
        frameMap.indexedValue(targetFrame).view.decode(viewpoint, pixFactor, clipRange);
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
    public void setDataHandler(@Nullable View.DataHandler dataHandler) {
        views.forEach(view -> view.setDataHandler(dataHandler));
    }

    @Nullable
    @Override
    public Boolean getFrameCompletion(int frame) {
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
    public String getXMLMetaData(JHVTime time) {
        FrameInfo frameInfo = frameMap.nearestValue(time);
        return frameInfo.view.getXMLMetaData(frameInfo.timeView);
    }

}
