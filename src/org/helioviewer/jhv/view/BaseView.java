package org.helioviewer.jhv.view;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.imagedata.ImageDataHandler;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.metadata.HelioviewerMetaData;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.PixelBasedMetaData;
import org.helioviewer.jhv.position.Position;
import org.helioviewer.jhv.time.JHVDate;

public class BaseView implements View {

    private static final AtomicBoolean fullCache = new AtomicBoolean(true);

    private final boolean isLocal;
    protected final APIRequest request;
    protected final URI uri;

    protected ImageData imageData;
    protected LUT builtinLUT;
    protected MetaData[] metaData = {new PixelBasedMetaData(1, 1, 0)};
    protected int maxFrame = 0;

    public BaseView(APIRequest _request, URI _uri) {
        request = _request;
        uri = _uri;
        isLocal = uri != null && "file".equals(uri.getScheme());
    }

    @Override
    public URI getURI() {
        return uri;
    }

    @Override
    public boolean isLocal() {
        return isLocal;
    }

    @Override
    public String getName() {
        MetaData m = metaData[0];
        if (m instanceof HelioviewerMetaData)
            return ((HelioviewerMetaData) m).getFullName();
        else if (uri == null)
            return "Loading...";
        else {
            String name = uri.getPath();
            return name.substring(name.lastIndexOf('/') + 1, name.lastIndexOf('.'));
        }
    }

    @Nullable
    @Override
    public APIRequest getAPIRequest() {
        return request;
    }

    @Override
    public void abolish() {
    }

    @Override
    public void decode(Position viewpoint, double pixFactor, double factor) {
        if (dataHandler != null) {
            imageData.setViewpoint(viewpoint);
            dataHandler.handleData(imageData);
        }
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
        return maxFrame > 0;
    }

    @Override
    public int getCurrentFrameNumber() {
        return 0;
    }

    @Override
    public int getMaximumFrameNumber() {
        return maxFrame;
    }

    @Override
    public JHVDate getFirstTime() {
        return metaData[0].getViewpoint().time;
    }

    @Override
    public JHVDate getLastTime() {
        return metaData[maxFrame].getViewpoint().time;
    }

    @Override
    public void setNearestFrame(JHVDate time) {
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
    public String getXMLMetaData(int frame) throws Exception {
        return "<meta/>";
    }

}
