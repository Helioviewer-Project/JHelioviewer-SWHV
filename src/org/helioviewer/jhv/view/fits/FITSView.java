package org.helioviewer.jhv.view.fits;

import java.awt.EventQueue;
import java.lang.ref.Cleaner;
import java.net.URI;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.position.Position;
import org.helioviewer.jhv.view.BaseView;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class FITSView extends BaseView {

    private static final Cache<FITSView, ImageBuffer> decodeCache = CacheBuilder.newBuilder().weakKeys().softValues().build();
    private static final Cleaner reaper = Cleaner.create();

    private final Cleaner.Cleanable abolishable;
    private final FITSDecodeExecutor executor;
    private final String xml;

    public FITSView(APIRequest _request, URI _uri, FITSDecodeExecutor _executor) throws Exception {
        super(_request, _uri);
        executor = _executor == null ? new FITSDecodeExecutor() : _executor;

        xml = FITSImage.getHeader(uri);
        if (xml == null)
            throw new Exception("Could not read FITS: " + uri);
        metaData[0] = new XMLMetaDataContainer(xml).getHVMetaData(0, false);
        abolishable = reaper.register(this, new Abolisher(executor));
    }

    @Override
    public void decode(Position viewpoint, double pixFactor, double factor) {
        ImageBuffer imageBuffer = decodeCache.getIfPresent(this);
        if (imageBuffer == null) {
            executor.decode(this, viewpoint);
        } else {
            ImageData data = new ImageData(imageBuffer, metaData[0], metaData[0].getPhysicalRegion(), viewpoint);
            if (dataHandler != null)
                dataHandler.handleData(data);
        }
    }

    void setDataFromDecoder(ImageBuffer imageBuffer, Position viewpoint) {
        decodeCache.put(this, imageBuffer);
        ImageData data = new ImageData(imageBuffer, metaData[0], metaData[0].getPhysicalRegion(), viewpoint);
        EventQueue.invokeLater(() -> {
            if (dataHandler != null)
                dataHandler.handleData(data);
        });
    }

    @Nonnull
    @Override
    public String getXMLMetaData() {
        return xml;
    }

    // if instance was built before cancelling
    private static class Abolisher implements Runnable {

        private final FITSDecodeExecutor aExecutor;

        Abolisher(FITSDecodeExecutor _executor) {
            aExecutor = _executor;
        }

        @Override
        public void run() {
            // executor abolish may take too long in stressed conditions
            new Thread(aExecutor::abolish).start();
        }

    }

    @Override
    public void abolish() {
        decodeCache.invalidate(this);
        abolishable.clean();
    }

}
