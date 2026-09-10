package org.helioviewer.jhv.view.uri;

import java.io.File;
import java.nio.Buffer;

import javax.annotation.Nullable;

import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.view.ClipSet;

import com.google.common.escape.Escaper;
import com.google.common.xml.XmlEscapers;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.ImageHDU;
import nom.tam.fits.header.Standard;
import nom.tam.image.compression.hdu.CompressedImageHDU;
import nom.tam.util.Cursor;

public final class FITSImage implements URIImageReader {

    private final FITSViewState.Data state;

    public FITSImage(FITSViewState.Data _state) {
        state = _state;
    }

    @Override
    public URIImageReader.Info readInfo(File file) throws Exception {
        FITSData data = readData(file);
        return new URIImageReader.Info(getHeaderAsXML(data.header()), data.width(), data.height(), null, data.calculateClipSet());
    }

    @Override
    public ImageBuffer decode(File file, ImageFilter filter, @Nullable ClipSet clipSet) throws Exception {
        return readData(file).decode(filter, state, clipSet);
    }

    private static FITSData readData(File file) throws Exception {
        try (Fits f = new Fits(file)) {
            BasicHDU<?> hdu = findHDU(f);
            Header header = imageHeader(hdu);
            int[] axes = imageAxes(header);
            Object pixels = readFlatPixels(hdu, axes);
            boolean hasBlank = header.containsKey(Standard.BLANK);
            long blank = hasBlank ? header.getLongValue(Standard.BLANK) : 0;
            double bzero = header.getDoubleValue(Standard.BZERO, 0);
            double bscale = header.getDoubleValue(Standard.BSCALE, 1);
            if (!(pixels instanceof byte[]) && (!Double.isFinite(bzero) || !Double.isFinite(bscale)))
                throw new Exception("Invalid FITS BZERO/BSCALE");
            float min = header.getFloatValue("HV_DMIN", Float.MAX_VALUE);
            float max = header.getFloatValue("HV_DMAX", Float.MAX_VALUE);
            ClipSet.Range headerRange = min == Float.MAX_VALUE || max == Float.MAX_VALUE ? null : new ClipSet.Range(min, max);
            return new FITSData(header, pixels, axes[1], axes[0], hasBlank, blank, bzero, bscale, headerRange);
        }
    }

    private static BasicHDU<?> findHDU(Fits fits) throws Exception {
        BasicHDU<?>[] hdus = fits.read();
        // this is cumbersome
        for (BasicHDU<?> hdu : hdus) {
            if (hdu instanceof CompressedImageHDU) {
                return hdu;
            }
        }
        for (BasicHDU<?> hdu : hdus) {
            if (hdu instanceof ImageHDU ihdu && ihdu.getAxes() != null /* might be an extension */) {
                return ihdu;
            }
        }
        throw new Exception("No image found");
    }

    private static Header imageHeader(BasicHDU<?> hdu) throws Exception {
        if (hdu instanceof CompressedImageHDU chdu) {
            return chdu.getImageHeader();
        } else {
            return hdu.getHeader();
        }
    }

    private static int[] imageAxes(Header header) throws Exception {
        int nAxis = header.getIntValue("NAXIS", 0);
        if (nAxis != 2)
            throw new Exception("Only 2D FITS files supported");
        int[] axes = {header.getIntValue("NAXIS2", 0), header.getIntValue("NAXIS1", 0)};
        if (axes[0] <= 0 || axes[1] <= 0)
            throw new Exception("Only 2D FITS files supported");
        return axes;
    }

    @SuppressWarnings("deprecation")
    private static Object readFlatPixels(BasicHDU<?> hdu, int[] axes) throws Exception {
        if (hdu instanceof CompressedImageHDU chdu) {
            return unwrapPixelBuffer(chdu.getUncompressedData(), axes[0] * axes[1]);
        } else if (hdu instanceof ImageHDU ihdu) {
            return ihdu.getData().getTiler().getTile(new int[]{0, 0}, axes);
        } else {
            throw new Exception("Unsupported FITS HDU: " + hdu.getClass().getSimpleName());
        }
    }

    private static Object unwrapPixelBuffer(Buffer buffer, int expectedPixels) throws Exception {
        if (!buffer.hasArray() || buffer.arrayOffset() != 0 || buffer.position() != 0 || buffer.remaining() < expectedPixels) {
            throw new Exception("Unsupported compressed FITS pixel buffer: " + buffer.getClass().getSimpleName());
        }
        return buffer.array();
    }

    private static final String nl = System.lineSeparator();
    private static final Escaper XML_CONTENT_ESCAPER = XmlEscapers.xmlContentEscaper();
    private static final Escaper XML_ATTRIBUTE_ESCAPER = XmlEscapers.xmlAttributeEscaper();

    private static String getHeaderAsXML(Header header) {
        StringBuilder builder = new StringBuilder("<meta>" + nl + "<fits>" + nl);

        for (Cursor<String, HeaderCard> iter = header.iterator(); iter.hasNext(); ) {
            HeaderCard headerCard = iter.next();
            String key = headerCard.getKey().trim();
            if ("END".equals(key))
                continue;
            key = key.isEmpty() ? "COMMENT" : key.replace("$", "-"); // allow illegal keyword character in FITS saved by IDL

            String value = headerCard.getValue();
            String val = value == null ? "" : XML_CONTENT_ESCAPER.escape(value);
            String comment = headerCard.getComment();
            String com = comment == null ? "" : " comment=\"" + XML_ATTRIBUTE_ESCAPER.escape(comment) + "\"";

            builder.append('<').append(key).append(com).append('>').append(val).append("</").append(key).append('>').append(nl);
        }
        builder.append("</fits>").append(nl).append("</meta>");
        return builder.toString();
    }
}
