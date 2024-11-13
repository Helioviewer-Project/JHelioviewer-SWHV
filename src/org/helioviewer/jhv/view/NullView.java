package org.helioviewer.jhv.view;

import java.util.ArrayList;

import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.imagedata.ImageFilter;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.NullMetaData;
import org.helioviewer.jhv.time.JHVTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

    private final JHVTime time;
    private final MetaData metaData;

    private NullView(long milli) {
        time = new JHVTime(milli);
        metaData = new NullMetaData(time);
    }

    @Override
    public void setFilter(ImageFilter.Type t) {
    }

    @Override
    public ImageFilter.Type getFilter() {
        return ImageFilter.Type.None;
    }

    @Override
    public void setDataHandler(ImageData.Handler dataHandler) {
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

}
