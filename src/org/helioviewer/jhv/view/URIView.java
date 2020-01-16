package org.helioviewer.jhv.view;

import java.awt.EventQueue;
import java.net.URI;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.PixelBasedMetaData;
import org.helioviewer.jhv.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.position.Position;
import org.helioviewer.jhv.view.fits.FITSImage;
import org.helioviewer.jhv.view.simpleimage.SimpleImage;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class URIView extends BaseView {

    private static final Cache<URI, ImageBuffer> decodeCache = CacheBuilder.newBuilder().weakKeys().softValues().build();

    public enum URIType {

        FITS {
            @Override
            protected URIImageReader getReader() {
                return new FITSImage();
            }
        },
        SIMPLE {
            @Override
            protected URIImageReader getReader() {
                return new SimpleImage();
            }
        };

        protected abstract URIImageReader getReader();

    }

    private final URIImageReader reader;
    private final String xml;
    private final Region imageRegion;

    public URIView(DecodeExecutor _executor, APIRequest _request, URI _uri, URIType type) throws Exception {
        super(_executor, _request, _uri);

        reader = type.getReader();

        MetaData m;
        String readXml = reader.readXML(uri);
        if (readXml == null) {
            xml = "<meta/>";
            m = new PixelBasedMetaData(100, 100, 0, uri);
        } else {
            xml = readXml;
            m = new XMLMetaDataContainer(xml).getHVMetaData(0, true);
        }

        imageRegion = m.roiToRegion(0, 0, m.getPixelWidth(), m.getPixelHeight(), 1, 1);
        metaData[0] = m;
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
        decodeCache.invalidate(uri);
    }

}
