package org.helioviewer.jhv.view;

import java.awt.EventQueue;
import java.net.URI;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.position.Position;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class URIView extends BaseView {

    private static final Cache<URI, ImageBuffer> decodeCache = CacheBuilder.newBuilder().weakKeys().softValues().build();

    protected URIImageReader reader;
    protected String xml;
    protected Region imageRegion;

    public URIView(DecodeExecutor _executor, APIRequest _request, URI _uri) {
        super(_executor, _request, _uri);
    }

    @Override
    public void decode(Position viewpoint, double pixFactor, double factor) {
        ImageBuffer imageBuffer = decodeCache.getIfPresent(uri);
        if (imageBuffer == null) {
            executor.decode(new URIDecoder(this, viewpoint));
        } else {
            sendDataToHandler(imageBuffer, viewpoint);
        }
    }

    void setDataFromDecoder(ImageBuffer imageBuffer, Position viewpoint) {
        decodeCache.put(uri, imageBuffer);
        sendDataToHandler(imageBuffer, viewpoint);
    }

    private void sendDataToHandler(ImageBuffer imageBuffer, Position viewpoint) {
        ImageData data = new ImageData(imageBuffer, metaData[0], imageRegion, viewpoint);
        EventQueue.invokeLater(() -> { // decouple from ImageLayers.displaySynced
            if (dataHandler != null)
                dataHandler.handleData(data);
        });
    }

    URIImageReader getReader() {
        return reader;
    }

    @Nonnull
    @Override
    public String getXMLMetaData() {
        return xml;
    }

    @Override
    public void abolish() {
        super.abolish();
        decodeCache.invalidate(uri);
    }

}
