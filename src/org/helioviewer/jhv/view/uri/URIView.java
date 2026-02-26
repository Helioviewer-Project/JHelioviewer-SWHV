package org.helioviewer.jhv.view.uri;

import java.awt.EventQueue;
import java.io.File;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.imagedata.ImageFilter;
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

    private record DecodeKey(DataUri uri, ImageFilter.Type filter) {
    }

    private static final Cache<DecodeKey, ImageBuffer> decodeCache = Caffeine.newBuilder().softValues().build();

    static void clearURICache() {
        decodeCache.invalidateAll();
    }

    private final URIImageReader reader;
    private final String xml;
    private final Region imageRegion;

    public URIView(DecodeExecutor _executor, DataUri _dataUri) throws Exception {
        super(_executor, _dataUri);

        reader = dataUri.format() == DataUri.Format.Image.FITS ? new FITSImage() : new GenericImage();

        try {
            MetaData m;
            URIImageReader.Image image = reader.readImage(dataUri.file());
            ImageBuffer buffer = image.buffer();
            String readXml = image.xml();
            if (readXml == null) {
                xml = EMPTY_METAXML;
                m = new PixelBasedMetaData(buffer.width, buffer.height, dataUri.baseName());
            } else {
                xml = readXml;
                m = new XMLMetaDataContainer(xml).getHVMetaData();
            }

            imageRegion = m.roiToRegion(0, 0, m.getPixelWidth(), m.getPixelHeight(), 1, 1);
            metaData[0] = m;
            decodeCache.put(new DecodeKey(dataUri, ImageFilter.Type.None), buffer);

            int[] lut = image.lut();
            if (lut != null)
                builtinLUT = new LUT("built-in", lut);
        } catch (Exception e) {
            throw new Exception(e.getMessage() + ": " + dataUri, e);
        }
    }

    @Override
    public void decode(Position viewpoint, double pixFactor, float factor) {
        DecodeKey key = new DecodeKey(dataUri, filterType);
        ImageBuffer imageBuffer = decodeCache.getIfPresent(key);
        if (imageBuffer != null) {
            sendDataToHandler(imageBuffer, viewpoint);
            return;
        }
        executor.decode(new Decoder(dataUri.file(), reader, filterType), new Callback(key, viewpoint));
    }

    private record Decoder(File file, URIImageReader reader, ImageFilter.Type type) implements Callable<ImageBuffer> {
        @Nonnull
        @Override
        public ImageBuffer call() throws Exception {
            ImageBuffer imageBuffer = reader.readImageBuffer(file);
            if (imageBuffer == null) // e.g. FITS
                throw new Exception("Could not read: " + file);
            return ImageBuffer.filter(imageBuffer, type);
        }
    }

    private class Callback extends DecodeCallback {

        private final DecodeKey key;
        private final Position viewpoint;

        Callback(DecodeKey _key, Position _viewpoint) {
            key = _key;
            viewpoint = _viewpoint;
        }

        @Override
        public void onSuccess(ImageBuffer result) {
            if (key.filter() != filterType) return; // filter changed in-flight

            decodeCache.put(key, result);
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
        decodeCache.asMap().keySet().removeIf(k -> k.uri().equals(dataUri));
    }

    @Override
    public void clearCache() {
        decodeCache.invalidateAll();
    }

}
