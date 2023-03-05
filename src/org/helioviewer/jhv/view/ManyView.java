package org.helioviewer.jhv.view;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.imagedata.ImageDataHandler;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeMap;

public class ManyView implements View {

    private static class FrameInfo {

        final View view;
        final JHVTime timeView;
        final int idxView;
        int idxMany;

        FrameInfo(View _view, JHVTime _timeView, int _idxView) {
            view = _view;
            timeView = _timeView;
            idxView = _idxView;
        }

    }

    private final TimeMap<FrameInfo> frameMap = new TimeMap<>();
    private int targetFrame;

    public ManyView(List<View> views) throws IOException {
        if (views.isEmpty())
            throw new IOException("Empty list of views");

        views.forEach(this::putDates);
        frameMap.buildIndex();
        for (int i = 0; i <= frameMap.maxIndex(); i++) {
            frameMap.indexedValue(i).idxMany = i;
        }
        // unused J2KViews should be abolished by their reaper
    }

    private void putDates(View v) {
        int m = v.getMaximumFrameNumber();
        for (int i = 0; i <= m; i++) {
            JHVTime t = v.getFrameTime(i);
            frameMap.put(t, new FrameInfo(v, t, i));
        }
    }

    @Nullable
    @Override
    public APIRequest getAPIRequest() {
        return null;
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
    public void setMGN(boolean b) {
        frameMap.values().forEach(frameInfo -> frameInfo.view.setMGN(b));
    }

    @Override
    public boolean getMGN() {
        return frameMap.indexedValue(0).view.getMGN();
    }

    @Override
    public void decode(Position viewpoint, double pixFactor, float factor) {
        frameMap.indexedValue(targetFrame).view.decode(viewpoint, pixFactor, factor);
    }

    @Nullable
    @Override
    public URI getURI() {
        return null;
    }

    @Nullable
    @Override
    public LUT getDefaultLUT() {
        return frameMap.indexedValue(0).view.getDefaultLUT();
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
    public void setDataHandler(ImageDataHandler dataHandler) {
        frameMap.values().forEach(frameInfo -> frameInfo.view.setDataHandler(dataHandler));
    }

    @Override
    public boolean isDownloading() {
        return false;
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Nullable
    @Override
    public AtomicBoolean getFrameCacheStatus(int frame) {
        FrameInfo frameInfo = frameMap.indexedValue(frame);
        return frameInfo.view.getFrameCacheStatus(frameInfo.idxView);
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
        FrameInfo frameInfo = frameMap.nearestValue(time);
        if (frameInfo.view.setNearestFrame(frameInfo.timeView)) {
            targetFrame = frameInfo.idxMany;
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
