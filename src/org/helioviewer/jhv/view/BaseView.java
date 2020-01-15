package org.helioviewer.jhv.view;

import java.lang.ref.Cleaner;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.imagedata.ImageDataHandler;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.metadata.HelioviewerMetaData;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.PixelBasedMetaData;
import org.helioviewer.jhv.position.Position;
import org.helioviewer.jhv.time.JHVDate;

public class BaseView implements View {

    private static final AtomicBoolean fullCache = new AtomicBoolean(true);
    protected static final Cleaner reaper = Cleaner.create();

    private final Cleaner.Cleanable baseAbolishable;
    private final boolean isLocal;

    protected final DecodeExecutor executor;
    protected final APIRequest request;
    protected final URI uri;

    protected LUT builtinLUT;
    protected MetaData[] metaData = new MetaData[1];

    public BaseView(DecodeExecutor _executor, APIRequest _request, URI _uri) {
        executor = _executor;
        request = _request;
        uri = _uri;
        isLocal = uri != null && "file".equals(uri.getScheme());
        metaData[0] = new PixelBasedMetaData(1, 1, 0, uri);
        baseAbolishable = reaper.register(this, new BaseAbolisher(executor));
    }

    @Override
    public URI getURI() {
        return uri;
    }

    @Override
    public boolean isLocal() {
        return isLocal;
    }

    @Nullable
    @Override
    public APIRequest getAPIRequest() {
        return request;
    }

    @Override
    public void decode(Position viewpoint, double pixFactor, double factor) {
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
    public JHVDate getFirstTime() {
        return metaData[0].getViewpoint().time;
    }

    @Override
    public JHVDate getLastTime() {
        return metaData[0].getViewpoint().time;
    }

    @Override
    public boolean setNearestFrame(JHVDate time) {
        return true;
    }

    @Override
    public JHVDate getNearestTime(JHVDate time) {
        return getFirstTime();
    }

    @Override
    public JHVDate getLowerTime(JHVDate time) {
        return getFirstTime();
    }

    @Override
    public JHVDate getHigherTime(JHVDate time) {
        return getLastTime();
    }

    @Override
    public JHVDate getFrameTime(int frame) {
        return getFirstTime();
    }

    @Override
    public MetaData getMetaData(JHVDate time) {
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
    public String getXMLMetaData() throws Exception {
        return "<meta/>";
    }

    private static class BaseAbolisher implements Runnable {

        private final DecodeExecutor aExecutor;

        BaseAbolisher(DecodeExecutor _executor) {
            aExecutor = _executor;
        }

        @Override
        public void run() {
            if (aExecutor != null)
                aExecutor.abolish();
        }

    }

    @Override
    public void abolish() {
        baseAbolishable.clean();
    }

}
