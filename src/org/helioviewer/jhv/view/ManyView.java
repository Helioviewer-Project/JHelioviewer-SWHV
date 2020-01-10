package org.helioviewer.jhv.view;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.imagedata.ImageDataHandler;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.position.Position;
import org.helioviewer.jhv.time.JHVDate;
import org.helioviewer.jhv.time.JHVDateMap;

public class ManyView implements View {

    private static class FrameInfo {

        final View view;
        final JHVDate timeView;
        final int frameView;
        int frameMany;

        FrameInfo(View _view, JHVDate _timeView, int _frameView) {
            view = _view;
            timeView = _timeView;
            frameView = _frameView;
        }

    }

    private final JHVDateMap<FrameInfo> frameMap = new JHVDateMap<>();
    private int targetFrame;

    public ManyView(List<View> views) throws IOException {
        if (views.isEmpty())
            throw new IOException("Empty list of views");

        views.forEach(this::putDates);
        frameMap.buildIndex();
        for (int i = 0; i <= frameMap.maxIndex(); i++) {
            frameMap.indexValue(i).frameMany = i;
        }
        // unused J2KViews should be abolished by their reaper
    }

    private void putDates(View v) {
        int m = v.getMaximumFrameNumber();
        for (int i = 0; i <= m; i++) {
            JHVDate t = v.getFrameTime(i);
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
    public void decode(Position viewpoint, double pixFactor, double factor) {
        frameMap.indexValue(targetFrame).view.decode(viewpoint, pixFactor, factor);
    }

    @Override
    public URI getURI() {
        return null;
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    @Nullable
    @Override
    public LUT getDefaultLUT() {
        return frameMap.indexValue(0).view.getDefaultLUT();
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
        return false;
    }

    @Nullable
    @Override
    public AtomicBoolean getFrameCacheStatus(int frame) {
        FrameInfo frameInfo = frameMap.indexValue(frame);
        return frameInfo.view.getFrameCacheStatus(frameInfo.frameView);
    }

    @Override
    public JHVDate getFrameTime(int frame) {
        return frameMap.key(frame);
    }

    @Override
    public JHVDate getFirstTime() {
        return frameMap.firstKey();
    }

    @Override
    public JHVDate getLastTime() {
        return frameMap.lastKey();
    }

    @Override
    public boolean setNearestFrame(JHVDate time) {
        FrameInfo frameInfo = frameMap.nearestValue(time);
        if (frameInfo.view.setNearestFrame(frameInfo.timeView)) {
            targetFrame = frameInfo.frameMany;
            return true;
        }
        return false;
    }

    @Override
    public JHVDate getNearestTime(JHVDate time) {
        return frameMap.nearestKey(time);
    }

    @Override
    public JHVDate getLowerTime(JHVDate time) {
        return frameMap.lowerKey(time);
    }

    @Override
    public JHVDate getHigherTime(JHVDate time) {
        return frameMap.higherKey(time);
    }

    @Override
    public MetaData getMetaData(JHVDate time) {
        FrameInfo frameInfo = frameMap.nearestValue(time);
        return frameInfo.view.getMetaData(frameInfo.timeView);
    }

    @Nonnull
    @Override
    public String getXMLMetaData() throws Exception {
        return frameMap.indexValue(targetFrame).view.getXMLMetaData();
    }

}
