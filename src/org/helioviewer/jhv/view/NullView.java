package org.helioviewer.jhv.view;

import java.util.ArrayList;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.imagedata.ImageDataHandler;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.NullMetaData;
import org.helioviewer.jhv.time.JHVTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

public class NullView implements View {

    public static ManyView create(long start, long end, int cadence) {
        ArrayList<View> list = new ArrayList<>();

        list.add(new NullView(start));
        if (cadence > 0) {
            long t = start;
            while (true) {
                t += cadence * 1000L;
                if (t >= end) {
                    list.add(new NullView(end));
                    break;
                } else
                    list.add(new NullView(t));
            }
        }

        try {
            return new ManyView(list);
        } catch (Exception ignore) { // cannot happen
            return null;
        }
    }

    private static final String emptyXML = "<xml/>";
    private static final AtomicBoolean fullCache = new AtomicBoolean(true);

    private final JHVTime time;
    private final MetaData metaData;

    private NullView(long milli) {
        time = new JHVTime(milli);
        metaData = new NullMetaData(time);
    }

    @Nullable
    @Override
    public APIRequest getAPIRequest() {
        return null;
    }

    @Override
    public void abolish() {
    }

    @Override
    public void clearCache() {
    }

    @Override
    public void setMGN(boolean b) {
    }

    @Override
    public boolean getMGN() {
        return false;
    }

    @Override
    public void decode(Position viewpoint, double pixFactor, float factor) {
    }

    @Nullable
    @Override
    public URI getURI() {
        return null;
    }

    @Nullable
    @Override
    public LUT getDefaultLUT() {
        return null;
    }

    @Override
    public boolean isMultiFrame() {
        return false;
    }

    @Override
    public int getCurrentFrameNumber() {
        return 0;
    }

    @Override
    public int getMaximumFrameNumber() {
        return 0;
    }

    @Override
    public void setDataHandler(ImageDataHandler dataHandler) {
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
        return fullCache;
    }

    @Override
    public JHVTime getFrameTime(int frame) {
        return time;
    }

    @Override
    public JHVTime getFirstTime() {
        return time;
    }

    @Override
    public JHVTime getLastTime() {
        return time;
    }

    @Override
    public boolean setNearestFrame(JHVTime time) {
        return true;
    }

    @Override
    public JHVTime getNearestTime(JHVTime time) {
        return time;
    }

    @Override
    public JHVTime getLowerTime(JHVTime time) {
        return time;
    }

    @Override
    public JHVTime getHigherTime(JHVTime time) {
        return time;
    }

    @Override
    public MetaData getMetaData(JHVTime time) {
        return metaData;
    }

    @Nonnull
    @Override
    public String getXMLMetaData() {
        return emptyXML;
    }

}
