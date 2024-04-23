package org.helioviewer.jhv.view;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.imagedata.ImageDataHandler;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DataUri;
import org.helioviewer.jhv.metadata.HelioviewerMetaData;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.time.JHVTime;

public class BaseView implements View {

    private static final AtomicBoolean fullCache = new AtomicBoolean(true);

    protected final DecodeExecutor executor;
    protected final APIRequest request;
    protected final DataUri dataUri;

    protected boolean mgn;
    protected LUT builtinLUT;
    protected MetaData[] metaData;

    public BaseView(DecodeExecutor _executor, APIRequest _request, DataUri _dataUri) {
        executor = _executor;
        request = _request;
        dataUri = _dataUri;
    }

    @Nullable
    @Override
    public URI getURI() {
        return dataUri.uri();
    }

    @Nullable
    @Override
    public APIRequest getAPIRequest() {
        return request;
    }

    @Override
    public void decode(Position viewpoint, double pixFactor, float factor) {
    }

    @Override
    public AtomicBoolean getFrameCacheStatus(int frame) {
        return fullCache;
    }

    @Override
    public boolean isComplete() {
        return true;
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
        return m instanceof HelioviewerMetaData ? LUT.get((HelioviewerMetaData) m) : null;
    }

    protected ImageDataHandler dataHandler;

    @Override
    public void setDataHandler(ImageDataHandler _dataHandler) {
        dataHandler = _dataHandler;
    }

    @Override
    public boolean isDownloading() {
        return false;
    }

    @Nonnull
    @Override
    public String getXMLMetaData() {
        return "<meta/>";
    }

    @Override
    public void abolish() {
    }

    @Override
    public void clearCache() {
    }

    @Override
    public void setMGN(boolean b) {
        mgn = b;
    }

    @Override
    public boolean getMGN() {
        return mgn;
    }

}
