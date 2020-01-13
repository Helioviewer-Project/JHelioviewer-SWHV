package org.helioviewer.jhv.view.fits;

import java.net.URI;

import javax.annotation.Nonnull;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.ImageHDU;
import nom.tam.image.compression.hdu.CompressedImageHDU;
import nom.tam.util.Cursor;

import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.metadata.HelioviewerMetaData;
import org.helioviewer.jhv.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.view.BaseView;

public class FITSView extends BaseView {

    private final String xml;

    public FITSView(APIRequest _request, URI _uri) throws Exception {
        super(_request, _uri);

        xml = get();

        HelioviewerMetaData m = new XMLMetaDataContainer(xml).getHVMetaData(0, false);
        imageData.setRegion(m.getPhysicalRegion());
        imageData.setMetaData(m);
        metaData[0] = m;
    }

    @Nonnull
    @Override
    public String getXMLMetaData() {
        return xml;
    }

    private String get() throws Exception {
        try (NetClient nc = NetClient.of(uri); Fits f = new Fits(nc.getStream())) {
            BasicHDU<?>[] hdus = f.read();
            // this is cumbersome
            for (BasicHDU<?> hdu : hdus) {
                if (hdu instanceof CompressedImageHDU) {
                    String meta = getHeaderAsXML(hdu.getHeader());
                    imageData = FITSImage.readHDU(((CompressedImageHDU) hdu).asImageHDU());
                    return meta;
                }
            }
            for (BasicHDU<?> hdu : hdus) {
                if (hdu instanceof ImageHDU) {
                    String meta = getHeaderAsXML(hdu.getHeader());
                    imageData = FITSImage.readHDU(hdu);
                    return meta;
                }
            }
        }
        return "<meta/>";
    }

    private static String getHeaderAsXML(Header header) {
        String nl = System.getProperty("line.separator");
        StringBuilder builder = new StringBuilder("<meta>").append(nl).append("<fits>").append(nl);

        for (Cursor<String, HeaderCard> iter = header.iterator(); iter.hasNext(); ) {
            HeaderCard headerCard = iter.next();
            if (headerCard.getValue() != null) {
                builder.append('<').append(headerCard.getKey()).append('>').append(headerCard.getValue()).append("</").append(headerCard.getKey()).append('>').append(nl);
            }
        }
        builder.append("</fits>").append(nl).append("</meta>");
        return builder.toString().replace("&", "&amp;");
    }

}
