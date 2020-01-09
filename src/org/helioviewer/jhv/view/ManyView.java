package org.helioviewer.jhv.view;

import java.net.URI;
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

    private static class ViewFrame {

        final View view;
        final int frame;

        ViewFrame(View _view, int _frame) {
            view = _view;
            frame = _frame;
        }

    }

    private final JHVDateMap<ViewFrame> dateMap = new JHVDateMap<>();
    private final JHVDateMap<Integer> frameMap = new JHVDateMap<>();
    private int targetFrame;

    public ManyView(View view, View ... views) {
        putDates(view);
        for (int i = 0; i < views.length; i++) {
            putDates(views[i]);
        }
        dateMap.index();
        for (int i = 0; i <= dateMap.maxIndex(); i++) {
            frameMap.put(dateMap.key(i), i);
        }
    }

    private View putDates(View v) {
        int m = v.getMaximumFrameNumber();
        for (int i = 0; i <= m; i++) {
            dateMap.put(v.getFrameTime(i), new ViewFrame(v, i));
        }
        return v;
    }

    @Nullable
    @Override
    public APIRequest getAPIRequest() {
        return null;
    }

    @Override
    public void abolish() {
        dateMap.values().forEach(viewFrame -> viewFrame.view.abolish());
    }

    @Override
    public void decode(Position viewpoint, double pixFactor, double factor) {
        dateMap.get(dateMap.key(targetFrame)).view.decode(viewpoint, pixFactor, factor);
    }

    @Override
    public URI getURI() {
        return null;
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    @Override
    public String getName() {
        return dateMap.get(dateMap.firstKey()).view.getName();
    }

    @Nullable
    @Override
    public LUT getDefaultLUT() {
        return dateMap.get(dateMap.firstKey()).view.getDefaultLUT();
    }

    @Override
    public boolean isMultiFrame() {
        return dateMap.maxIndex() > 0;
    }

    @Override
    public int getCurrentFrameNumber() {
        return targetFrame;
    }

    @Override
    public int getMaximumFrameNumber() {
        return dateMap.maxIndex();
    }

    @Override
    public void setDataHandler(ImageDataHandler dataHandler) {
        dateMap.values().forEach(viewFrame -> viewFrame.view.setDataHandler(dataHandler));
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
        ViewFrame viewFrame = dateMap.get(dateMap.key(frame));
        return viewFrame.view.getFrameCacheStatus(viewFrame.frame);
    }

    @Override
    public JHVDate getFrameTime(int frame) {
        return dateMap.key(frame);
    }

    @Override
    public JHVDate getFirstTime() {
        return dateMap.firstKey();
    }

    @Override
    public JHVDate getLastTime() {
        return dateMap.lastKey();
    }

    @Override
    public boolean setNearestFrame(JHVDate time) {
        ViewFrame viewFrame = dateMap.nearestValue(time);
        if (viewFrame.view.setNearestFrame(time)) {
            targetFrame = frameMap.nearestValue(time);
            return true;
        }
        return false;
    }

    @Override
    public JHVDate getNearestTime(JHVDate time) {
        return dateMap.nearestKey(time);
    }

    @Override
    public JHVDate getLowerTime(JHVDate time) {
        return dateMap.lowerKey(time);
    }

    @Override
    public JHVDate getHigherTime(JHVDate time) {
        return dateMap.higherKey(time);
    }

    @Override
    public MetaData getMetaData(JHVDate time) {
        return dateMap.nearestValue(time).view.getMetaData(time);
    }

    @Nonnull
    @Override
    public String getXMLMetaData(JHVDate time) throws Exception {
        return dateMap.nearestValue(time).view.getXMLMetaData(time);
    }

}
