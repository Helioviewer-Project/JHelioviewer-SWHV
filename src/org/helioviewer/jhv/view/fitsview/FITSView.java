package org.helioviewer.jhv.view.fitsview;

import java.net.URI;

import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.imagedata.SubImage;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.metadata.HelioviewerMetaData;
import org.helioviewer.jhv.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.view.AbstractView;

public class FITSView extends AbstractView {

    private final String xml;
    private final ImageBuffer imageBuffer;

    public FITSView(URI _uri, APIRequest _request) throws Exception {
        super(_uri, _request);

        FITSImage fits = new FITSImage(uri);
        xml = fits.xml;
        HelioviewerMetaData m = new XMLMetaDataContainer(xml).getHVMetaData(0, false);
        metaData[0] = m;

        int w = m.getPixelWidth();
        int h = m.getPixelHeight();
        imageBuffer = fits.imageBuffer;
        imageData = new ImageData(imageBuffer);
        imageData.setRegion(m.roiToRegion(new SubImage(0, 0, w, h, w, h), 1, 1));
        imageData.setMetaData(m);
    }

    @Override
    public String getXMLMetaData() {
        return xml;
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public void abolish() {
        imageBuffer.delete();
    }

}
