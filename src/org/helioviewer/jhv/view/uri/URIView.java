package org.helioviewer.jhv.view.uri;

import java.awt.EventQueue;
import java.io.File;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.imagedata.ImageBufferCache;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.imagedata.ImageFilter;
import org.helioviewer.jhv.io.DataUri;
import org.helioviewer.jhv.metadata.BasicMetaData;
import org.helioviewer.jhv.metadata.FitsMetaData;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.view.BaseView;
import org.helioviewer.jhv.view.DecodeCallback;
import org.helioviewer.jhv.view.DecodeExecutor;

public final class URIView extends BaseView {

    private record DecodeKey(DataUri uri, ImageFilter.Type filter) {}

    static void clearURICache() {
        ImageBufferCache.invalidateIf(key -> key instanceof DecodeKey);
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
            ImageBufferCache.put(new DecodeKey(dataUri, ImageFilter.Type.None), buffer);

            LUT lut = image.lut();
            if (lut != null)
                builtinLUT = lut;
        } catch (Exception e) {
            throw new Exception(e.getMessage() + ": " + dataUri, e);
        }
    }

    @Override
    public void decode(Position viewpoint, double pixFactor, float factor) {
        DecodeKey key = new DecodeKey(dataUri, filterType);
        ImageBuffer imageBuffer = ImageBufferCache.get(key);
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
            ImageBuffer imageBuffer = reader.readImageBuffer(file, type);
            if (imageBuffer == null) // e.g. FITS
                throw new Exception("Could not read: " + file);
            return imageBuffer;
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

            ImageBufferCache.put(key, result);
            sendDataToHandler(result, viewpoint);
        }

    }

    private void sendDataToHandler(ImageBuffer imageBuffer, Position viewpoint) {
        imageBuffer.protectFromExplicitFree();
        ImageData data = new ImageData(imageBuffer, metaData[0], imageRegion, viewpoint);
        EventQueue.invokeLater(() -> { // decouple from ImageLayers.displaySynced
            if (dataHandler != null) {
                dataHandler.handleData(data);
            } else {
                // Free eagerly unsent buffers.
                imageBuffer.allowExplicitFree();
            }
        });
    }

    @Nonnull
    @Override
    public String getXMLMetaData() {
        return xml;
    }

    @Override
    public void abolish() {
        ImageBufferCache.invalidateIf(key -> key instanceof DecodeKey k && k.uri().equals(dataUri));
    }

    @Override
    public void clearCache() {
        abolish();
    }

}
