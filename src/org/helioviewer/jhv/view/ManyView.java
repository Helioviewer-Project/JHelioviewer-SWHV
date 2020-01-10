package org.helioviewer.jhv.view;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
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

    private static class ViewFrame {

        final View view;
        final int frame;
        final JHVDate time;

        ViewFrame(View _view, int _frame, JHVDate _time) {
            view = _view;
            frame = _frame;
            time = _time;
        }

    }

    private final JHVDateMap<ViewFrame> dateMap = new JHVDateMap<>();
    private final HashMap<JHVDate, Integer> frameMap = new HashMap<>();
    private int targetFrame;

    public ManyView(List<View> views) throws IOException {
        if (views.isEmpty())
            throw new IOException("Empty list of views");

        views.forEach(view -> putDates(view));
        dateMap.index();
        for (int i = 0; i <= dateMap.maxIndex(); i++) {
            frameMap.put(dateMap.key(i), i);
        }
        // unused J2KViews should be abolished by their reaper
    }

    private View putDates(View v) {
        int m = v.getMaximumFrameNumber();
        for (int i = 0; i <= m; i++) {
            JHVDate t = v.getFrameTime(i);
            dateMap.put(t, new ViewFrame(v, i, t));
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
        JHVDate t = dateMap.nearestKey(time);
        ViewFrame viewFrame = dateMap.get(t);
        if (viewFrame.view.setNearestFrame(viewFrame.time)) {
            targetFrame = frameMap.get(t);
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
        ViewFrame viewFrame = dateMap.get(dateMap.nearestKey(time));
        return viewFrame.view.getMetaData(viewFrame.time);
    }

    @Nonnull
    @Override
    public String getXMLMetaData() throws Exception {
        return dateMap.get(dateMap.key(targetFrame)).view.getXMLMetaData();
    }

}
