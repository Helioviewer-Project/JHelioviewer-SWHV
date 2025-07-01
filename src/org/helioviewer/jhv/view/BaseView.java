package org.helioviewer.jhv.view;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.imagedata.ImageFilter;
import org.helioviewer.jhv.io.DataUri;
import org.helioviewer.jhv.metadata.HelioviewerMetaData;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.PixelBasedMetaData;
import org.helioviewer.jhv.time.JHVTime;

public class BaseView implements View {

    protected final DecodeExecutor executor;
    protected final DataUri dataUri;

    protected ImageFilter.Type filterType = ImageFilter.Type.None;
    protected LUT builtinLUT;
    protected MetaData[] metaData = new MetaData[]{PixelBasedMetaData.EMPTY}; // paranoia

    public BaseView(DecodeExecutor _executor, DataUri _dataUri) {
        executor = _executor;
        dataUri = _dataUri;
    }

    @Nullable
    @Override
    public String getBaseName() {
        return dataUri == null ? null : dataUri.baseName();
    }

    @Override
    public JHVTime getFirstTime() {
        return metaData[0].getViewpoint().time;
    }

    @Override
    public JHVTime getLastTime() {
        return metaData[0].getViewpoint().time;
    }

    @Override
    public boolean setNearestFrame(JHVTime time) {
        return true;
    }

    @Override
    public JHVTime getNearestTime(JHVTime time) {
        return getFirstTime();
    }

    @Override
    public JHVTime getLowerTime(JHVTime time) {
        return getFirstTime();
    }

    @Override
    public JHVTime getHigherTime(JHVTime time) {
        return getLastTime();
    }

    @Override
    public JHVTime getFrameTime(int frame) {
        return getFirstTime();
    }

    @Override
    public MetaData getMetaData(JHVTime time) {
        return metaData[0];
    }

    @Nullable
    @Override
    public LUT getDefaultLUT() {
        if (builtinLUT != null)
            return builtinLUT;
        MetaData m = metaData[0];
        return m instanceof HelioviewerMetaData hm ? LUT.get(hm) : null;
    }

    protected ImageData.Handler dataHandler;

    @Override
    public void setDataHandler(ImageData.Handler _dataHandler) {
        dataHandler = _dataHandler;
    }

    @Override
    public void setFilter(ImageFilter.Type t) {
        filterType = t;
    }

    @Override
    public ImageFilter.Type getFilter() {
        return filterType;
    }

}
