package org.helioviewer.jhv.view.uri;

import java.awt.EventQueue;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.io.DataUri;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.PixelBasedMetaData;
import org.helioviewer.jhv.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.view.BaseView;
import org.helioviewer.jhv.view.DecodeCallback;
import org.helioviewer.jhv.view.DecodeExecutor;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public class URIView extends BaseView {

    private static final Cache<DataUri, ImageBuffer> decodeCache = Caffeine.newBuilder().softValues().build();

    static void clearURICache() {
        decodeCache.invalidateAll();
    }

    private final URIImageReader reader;
    private final String xml;
    private final Region imageRegion;

    public URIView(DecodeExecutor _executor, DataUri _dataUri) throws Exception {
        super(_executor, null, _dataUri);

        reader = dataUri.format() == DataUri.Format.FITS ? new FITSImage() : new GenericImage();

        try {
            MetaData m;
            URIImageReader.Image image = reader.readImage(dataUri.file());
            String readXml = image.xml();
            if (readXml == null) {
                xml = EMPTY_METAXML;
                m = new PixelBasedMetaData(100, 100, dataUri.baseName());
            } else {
                xml = readXml;
                m = new XMLMetaDataContainer(xml).getHVMetaData();
            }

            imageRegion = m.roiToRegion(0, 0, m.getPixelWidth(), m.getPixelHeight(), 1, 1);
            metaData[0] = m;
            decodeCache.put(dataUri, image.buffer());
        } catch (Exception e) {
            throw new Exception(e.getMessage() + ": " + dataUri, e);
        }
    }

    @Override
    public void decode(Position viewpoint, double pixFactor, float factor) {
        ImageBuffer imageBuffer = decodeCache.getIfPresent(dataUri);
        if (imageBuffer == null) {
            executor.decode(new URIDecoder(dataUri.file(), reader, mgn), new Callback(viewpoint));
        } else {
            sendDataToHandler(imageBuffer, viewpoint);
        }
    }

    private class Callback extends DecodeCallback {

        private final Position viewpoint;

        Callback(Position _viewpoint) {
            viewpoint = _viewpoint;
        }

        @Override
        public void onSuccess(ImageBuffer result) {
            decodeCache.put(dataUri, result);
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
        decodeCache.invalidate(dataUri);
    }

    @Override
    public void clearCache() {
        decodeCache.invalidateAll();
    }

}
