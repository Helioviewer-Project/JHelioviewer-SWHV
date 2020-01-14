package org.helioviewer.jhv.view.fits;

import java.awt.EventQueue;
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

    private static final Cache<URI, ImageBuffer> decodeCache = CacheBuilder.newBuilder().softValues().build();

    private final String xml;

    public FITSView(APIRequest _request, URI _uri) throws Exception {
        super(_request, _uri);

        xml = FITSImage.getHeader(uri);
        if (xml == null)
            throw new Exception("Could not read FITS: " + uri);
        metaData[0] = new XMLMetaDataContainer(xml).getHVMetaData(0, false);
    }

    @Override
    public void decode(Position viewpoint, double pixFactor, double factor) {
        if (dataHandler != null) {
            ImageBuffer imageBuffer = decodeCache.getIfPresent(uri);
            if (imageBuffer == null) { // first read or was evicted
                runDecode(viewpoint);
            } else {
                setDataFromDecoder(imageBuffer, viewpoint);
            }
        }
    }

    private void setDataFromDecoder(ImageBuffer imageBuffer, Position viewpoint) {
        ImageData data = new ImageData(imageBuffer, metaData[0], metaData[0].getPhysicalRegion(), viewpoint);
        if (dataHandler != null)
            dataHandler.handleData(data);
    }

    private void runDecode(Position viewpoint) {
        new Thread(() -> {
            try {
                ImageBuffer imageBuffer = FITSImage.getHDU(uri);
                if (imageBuffer == null)
                    throw new Exception("Could not read FITS: " + uri);

                decodeCache.put(uri, imageBuffer);
                EventQueue.invokeLater(() -> setDataFromDecoder(imageBuffer, viewpoint));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void abolish() {
        decodeCache.invalidate(uri);
    }

    @Nonnull
    @Override
    public String getXMLMetaData() {
        return xml;
    }

}
