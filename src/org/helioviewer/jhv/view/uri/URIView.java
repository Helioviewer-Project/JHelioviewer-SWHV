package org.helioviewer.jhv.view.uri;

import java.awt.EventQueue;
import java.net.URI;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.PixelBasedMetaData;
import org.helioviewer.jhv.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.view.BaseView;
import org.helioviewer.jhv.view.DecodeCallback;
import org.helioviewer.jhv.view.DecodeExecutor;
import org.helioviewer.jhv.view.uri.fits.FITSImage;
import org.helioviewer.jhv.view.uri.generic.GenericImage;

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
        GENERIC {
            @Override
            protected URIImageReader getReader() {
                return new GenericImage();
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

        try {
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
        } catch (Exception e) {
            throw new Exception(e.getMessage() + ": " + uri, e);
        }
    }

    @Override
    public void decode(Position viewpoint, double pixFactor, float factor) {
        ImageBuffer imageBuffer = decodeCache.getIfPresent(uri);
        if (imageBuffer == null) {
            executor.decode(new URIDecoder(uri, reader), new URICallback(viewpoint));
        } else {
            sendDataToHandler(imageBuffer, viewpoint);
        }
    }

    private class URICallback extends DecodeCallback {

        private final Position viewpoint;

        URICallback(Position _viewpoint) {
            viewpoint = _viewpoint;
        }

        @Override
        public void onSuccess(ImageBuffer result) {
            decodeCache.put(uri, result);
            sendDataToHandler(result, viewpoint);
        }

    }

    private void sendDataToHandler(ImageBuffer imageBuffer, Position viewpoint) {
        ImageData data = new ImageData(imageBuffer, metaData[0], imageRegion, viewpoint);
        EventQueue.invokeLater(() -> { // decouple from ImageLayers.displaySynced
            if (dataHandler != null)
                dataHandler.handleData(data);
        });
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
