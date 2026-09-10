package org.helioviewer.jhv.view.uri;

import java.io.File;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
import org.helioviewer.jhv.view.ClipSet;

public final class URIView extends BaseView {

    private final FITSViewState fitsViewState;
    private final @Nullable ClipSet clipSet;
    private @Nullable ClipSet.Range clipRange;
    private final String xml;
    private final Region imageRegion;

    public URIView(LatestWorker<DecodedImage> _executor, DataUri _dataUri, FITSViewState _fitsViewState) throws Exception {
        super(_executor, _dataUri);

        fitsViewState = _fitsViewState;

        try {
            MetaData m;
            FITSViewState.Data data = hasFITS() ? fitsViewState.data() : null;
            URIImageReader.Info info = reader(data).readInfo(dataUri.file());
            clipSet = info.clipSet();

            String readXml = info.xml();
            try {
                if (readXml == null)
                    throw new Exception("Missing XML metadata");
                m = new FitsMetaData(new XMLMetaDataContainer(readXml), dataUri.sourceUri());
            } catch (Exception e) {
                readXml = EMPTY_METAXML;
                m = new BasicMetaData(info.width(), info.height(), dataUri.baseName(), dataUri.sourceUri());
                Log.warn("Helioviewer metadata missing for " + dataUri.baseName(), e);
            }
            xml = readXml;

            imageRegion = m.roiToRegion(0, 0, info.width(), info.height(), 1, 1);
            metaData[0] = m;

            LUT lut = info.lut();
            if (lut != null)
                builtinLUT = lut;
        } catch (Exception e) {
            throw new Exception(e.getMessage() + ": " + dataUri, e);
        }
    }

    @Override
    public void decode(Position viewpoint, double pixFactor, float factor, @Nullable ClipSet.Range range) {
        clipRange = hasFITS() ? range : null;
        URIDecodeKey key = decodeKey(filterType);
        DecodedImage image = ImageBufferCache.get(key);
        if (image != null) {
            // Mark running decodes stale before publishing this cached result.
            executor.cancel();
            sendDataToHandler(0, viewpoint, image, () -> key.equals(decodeKey(filterType)));
            return;
        }
        executor.submit(new Decoder(dataUri.file(), reader(key.fitsData()), createFilter(filterType), imageRegion, key.clipRange()), new Callback(key, viewpoint));
    }

    private ImageFilter createFilter(ImageFilter.Type type) {
        return ImageFilter.of(type, imageRegion, metaData[0]);
    }

    @Nullable
    @Override
    public ClipSet getClipSet() {
        return clipSet;
    }

    @Override
    public boolean hasFITS() {
        return dataUri.format() == DataUri.Format.FITS;
    }

    private URIDecodeKey decodeKey(ImageFilter.Type filter) {
        FITSViewState.Data data = hasFITS() ? fitsViewState.data() : null;
        return new URIDecodeKey(dataUri, filter, data, clipRange);
    }

    private static URIImageReader reader(@Nullable FITSViewState.Data data) {
        return data == null ? new GenericImage() : new FITSImage(data);
    }

    private record Decoder(File file, URIImageReader reader, ImageFilter filter, Region imageRegion, @Nullable ClipSet.Range clipRange) implements Callable<DecodedImage> {
        @Nonnull
        @Override
        public DecodedImage call() throws Exception {
            ImageBuffer imageBuffer = reader.decode(file, filter, clipRange);
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
            if (!key.equals(decodeKey(filterType))) return; // settings or filter changed in-flight

            ImageBufferCache.put(key, result);
            // This decode was superseded after it started; do not publish it to the layer.
            if (!fresh) return;
            sendDataToHandler(0, viewpoint, result, () -> key.equals(decodeKey(filterType)));
        }

        @Override
        public void onFailure(@Nonnull Throwable t, boolean fresh) {
            Log.errorStack(t);
        }

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
