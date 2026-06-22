package org.helioviewer.jhv.view.uri;

import java.awt.EventQueue;
import java.io.File;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.image.DecodedImage;
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageBufferCache;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.image.lut.LUT;
import org.helioviewer.jhv.io.DataUri;
import org.helioviewer.jhv.metadata.BasicMetaData;
import org.helioviewer.jhv.metadata.FitsMetaData;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.Region;
import org.helioviewer.jhv.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.thread.LatestWorker;
import org.helioviewer.jhv.view.BaseView;
import org.helioviewer.jhv.view.View;

public final class URIView extends BaseView {

    static void clearURICache() {
        ImageBufferCache.invalidateIf(key -> key instanceof URIDecodeKey);
    }

    private final URIImageReader reader;
    private final String xml;
    private final Region imageRegion;

    public URIView(LatestWorker<DecodedImage> _executor, DataUri _dataUri) throws Exception {
        super(_executor, _dataUri);

        reader = dataUri.format() == DataUri.Format.Image.FITS ? new FITSImage() : new GenericImage();

        try {
            MetaData m;
            URIImageReader.Image image = reader.readImage(dataUri.file());
            ImageBuffer buffer = image.buffer();

            String readXml = image.xml();
            try {
                if (readXml == null)
                    throw new Exception("Missing XML metadata");
                m = new FitsMetaData(new XMLMetaDataContainer(readXml));
            } catch (Exception e) {
                readXml = EMPTY_METAXML;
                m = new BasicMetaData(buffer.width, buffer.height, dataUri.baseName());
                Log.warn("Helioviewer metadata missing for " + dataUri.baseName(), e);
            }
            xml = readXml;

            imageRegion = m.roiToRegion(0, 0, buffer.width, buffer.height, 1, 1);
            metaData[0] = m;
            ImageBufferCache.put(decodeKey(ImageFilter.Type.None), new DecodedImage(buffer, imageRegion));

            LUT lut = image.lut();
            if (lut != null)
                builtinLUT = lut;
        } catch (Exception e) {
            throw new Exception(e.getMessage() + ": " + dataUri, e);
        }
    }

    @Override
    public void decode(Position viewpoint, double pixFactor, float factor) {
        URIDecodeKey key = decodeKey(filterType);
        DecodedImage image = ImageBufferCache.get(key);
        if (image != null) {
            // Mark running decodes stale before publishing this cached result.
            executor.cancel();
            sendDataToHandler(image, viewpoint);
            return;
        }
        executor.submit(new Decoder(dataUri.file(), reader, createFilter(filterType), imageRegion, fixedRange), new Callback(key, viewpoint));
    }

    private ImageFilter createFilter(ImageFilter.Type type) {
        return ImageFilter.of(type, imageRegion, metaData[0]);
    }

    private ImageFilter.Type decodeKeyFilter;
    private URIDecodeKey decodeKey;

    private URIDecodeKey decodeKey(ImageFilter.Type filter) {
        if (decodeKey == null || decodeKeyFilter != filter) {
            decodeKeyFilter = filter;
            decodeKey = new URIDecodeKey(dataUri, filter);
        }
        return decodeKey;
    }

    // Optional shared [min, max] display range for this layer's FITS frames. When set, every frame
    // normalizes to it instead of per-frame auto, so a multi-frame layer does not strobe.
    private volatile float[] fixedRange;

    @Override
    public void setRange(double min, double max) {
        fixedRange = new float[]{(float) min, (float) max};
        // drop cached decodes for this file so they re-decode with the new range (caller re-renders)
        ImageBufferCache.invalidateIf(k -> k instanceof URIDecodeKey key && key.uri() == dataUri);
    }

    private record Decoder(File file, URIImageReader reader, ImageFilter filter, Region imageRegion, float[] clip) implements Callable<DecodedImage> {
        @Nonnull
        @Override
        public DecodedImage call() throws Exception {
            ImageBuffer imageBuffer = reader.readImageBuffer(file, filter, clip);
            if (imageBuffer == null) // e.g. FITS
                throw new Exception("Could not read: " + file);
            return new DecodedImage(imageBuffer, imageRegion);
        }
    }

    private class Callback implements LatestWorker.Callback<DecodedImage> {

        private final URIDecodeKey key;
        private final Position viewpoint;

        Callback(URIDecodeKey _key, Position _viewpoint) {
            key = _key;
            viewpoint = _viewpoint;
        }

        @Override
        public void onSuccess(DecodedImage result, boolean fresh) {
            if (key.filter() != filterType) return; // filter changed in-flight

            ImageBufferCache.put(key, result);
            // This decode was superseded after it started; do not publish it to the layer.
            if (!fresh) return;
            sendDataToHandler(result, viewpoint);
        }

        @Override
        public void onFailure(@Nonnull Throwable t, boolean fresh) {
            Log.errorStack(t);
        }

    }

    private void sendDataToHandler(DecodedImage image, Position viewpoint) {
        image.imageBuffer().protectFromExplicitFree();
        View.ImageData data = new View.ImageData(image.imageBuffer(), metaData[0], image.region(), viewpoint);
        EventQueue.invokeLater(() -> {
            if (dataHandler != null)
                dataHandler.handleData(data);
            else
                image.imageBuffer().allowExplicitFree();
        });
    }

    @Nonnull
    @Override
    public String getXMLMetaData() {
        return xml;
    }

    @Override
    public void abolish() {
        ImageBufferCache.invalidateIf(key -> key instanceof URIDecodeKey k && k.uri() == dataUri);
    }

    @Override
    public void clearCache() {
        abolish();
    }

}
