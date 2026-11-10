package org.helioviewer.jhv.source;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.image.ClipSet;
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.image.ImageProcessingSettings;
import org.helioviewer.jhv.image.lut.LUT;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.wcs.ImageGrid;

import com.google.common.escape.Escaper;
import com.google.common.xml.XmlEscapers;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.ImageHDU;
import nom.tam.image.compression.hdu.CompressedImageHDU;
import nom.tam.util.Cursor;

// NTF reads headers once; CFITSIO reads pixels from Java-owned input storage.
public final class FITSSource implements Source {

    public record Image(int hdu, @Nullable String name, int planes) {}

    private final List<Image> images;
    private final ImageProcessingSettings.FITSSelection selection;
    private final Path path;
    private final boolean temporary;
    private final int bitpix;
    private final int tileRows;
    private final FITSData data;
    private final String xml;
    private final @Nullable String planeName;
    private final ResolutionSet levels;
    private final @Nullable ClipSet clipSet;
    private boolean closed;

    public FITSSource(File file) throws Exception {
        this(file, null);
    }

    public FITSSource(File file, @Nullable ImageProcessingSettings.FITSSelection requested) throws Exception {
        try (InputStream input = Files.newInputStream(file.toPath())) {
            temporary = input.read() == 0x1f && input.read() == 0x8b;
        }
        path = temporary ? inflate(file.toPath()) : file.toPath();
        try {
            Header header;
            int selectedTileRows;
            try (Fits fits = new Fits(path.toFile())) {
                BasicHDU<?>[] hdus = fits.read(); // File-backed payloads stay deferred.
                List<Image> available = new ArrayList<>();
                for (int i = 0; i < hdus.length; i++) {
                    if (!(hdus[i] instanceof ImageHDU) && !(hdus[i] instanceof CompressedImageHDU))
                        continue;
                    Header image = hdus[i] instanceof CompressedImageHDU compressed
                            ? compressed.getImageHeader() : hdus[i].getHeader();
                    int axes = image.getIntValue("NAXIS");
                    if ((axes == 2 || axes == 3) && image.getIntValue("NAXIS1") > 0 && image.getIntValue("NAXIS2") > 0
                            && (axes == 2 || image.getIntValue("NAXIS3") > 0)) {
                        String name = hdus[i].getHeader().getStringValue("EXTNAME");
                        available.add(new Image(i, name, axes == 3 ? image.getIntValue("NAXIS3") : 1));
                    }
                }
                images = List.copyOf(available);
                int selected = requested == null ? findHDU(hdus, images) : requested.hdu();
                Image image = images.stream().filter(candidate -> candidate.hdu() == selected).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("No supported image at FITS HDU " + selected));
                int plane = requested == null ? 0 : requested.plane();
                if (plane < 0 || plane >= image.planes())
                    throw new IllegalArgumentException("Invalid plane " + (plane + 1) + " for FITS HDU " + selected);
                selection = new ImageProcessingSettings.FITSSelection(selected, plane);
                if (hdus[selected] instanceof CompressedImageHDU compressed) {
                    header = compressed.getImageHeader();
                    selectedTileRows = compressed.getHeader().getIntValue("ZTILE2", 1);
                } else {
                    header = hdus[selected].getHeader();
                    selectedTileRows = header.getIntValue("NAXIS2");
                }
            }
            if (header.getIntValue("NAXIS") == 3) {
                for (String key : new String[]{"PC1_3", "PC2_3", "CD1_3", "CD2_3"}) {
                    if (header.getDoubleValue(key, 0) != 0)
                        throw new IllegalArgumentException("Spatially coupled FITS cubes are unsupported: " + key);
                }
            }
            int width = header.getIntValue("NAXIS1", 0);
            int height = header.getIntValue("NAXIS2", 0);
            Math.multiplyExact(width, height); // Java primitive-array limit.
            bitpix = header.getIntValue("BITPIX");
            if (bitpix != 8 && bitpix != 16 && bitpix != 32 && bitpix != 64 && bitpix != -32 && bitpix != -64)
                throw new IllegalArgumentException("Unsupported FITS BITPIX: " + bitpix);
            tileRows = Math.clamp(selectedTileRows, 1, height);
            float min = header.getFloatValue("HV_DMIN", Float.MAX_VALUE);
            float max = header.getFloatValue("HV_DMAX", Float.MAX_VALUE);
            ClipSet.Range headerRange = min == Float.MAX_VALUE || max == Float.MAX_VALUE ? null : new ClipSet.Range(min, max);
            data = new FITSData(width, height, bitpix == 8, headerRange);
            planeName = header.getStringValue("OBSLAYR" + (selection.plane() + 1));
            xml = getHeaderAsXML(header);
            levels = new ResolutionSet(new ImageGrid[]{new ImageGrid(0, width, height)}, 1);
            clipSet = bitpix == 8 || headerRange != null ? null : data.calculateSampledClipSet(
                    readPixels(Math.max((int) (4L * width / 1024), 1), Math.max((int) (4L * height / 1024), 1)));
        } catch (Throwable error) {
            if (temporary) {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception cleanup) {
                    error.addSuppressed(cleanup);
                }
            }
            throw error;
        }
    }

    public List<Image> images() {
        return images;
    }

    public @Nullable String planeName() {
        return planeName;
    }

    public ImageProcessingSettings.FITSSelection selection() {
        return selection;
    }

    private static Path inflate(Path compressed) throws Exception {
        Path result = Files.createTempFile(Directories.fileCacheDir.toPath(), "fits-", ".fits");
        try (InputStream raw = Files.newInputStream(compressed); InputStream input = new GZIPInputStream(raw)) {
            Files.copy(input, result, StandardCopyOption.REPLACE_EXISTING);
            return result;
        } catch (Throwable error) {
            try {
                Files.deleteIfExists(result);
            } catch (Exception cleanup) {
                error.addSuppressed(cleanup);
            }
            throw error;
        }
    }

    @Override
    public int frames() {
        return 1;
    }

    @Override
    public String xml(int frame) {
        return xml;
    }

    @Override
    public ResolutionSet levels(int frame) {
        return levels;
    }

    @Nullable
    @Override
    public LUT lut() {
        return null;
    }

    @Nullable
    @Override
    public ClipSet clipSet() {
        return clipSet;
    }

    @Override
    public boolean usesFITSParameters() {
        return true;
    }

    @Override
    public ImageBuffer decode(int frame, int level, ImageFilter filter,
                              @Nullable ImageProcessingSettings.FITSParameters fits, @Nullable ClipSet.Range clip) throws Exception {
        float[] pixels;
        synchronized (this) {
            if (closed)
                throw new IllegalStateException("FITS source is closed");
            pixels = readPixels(1, 1);
        }
        return data.decode(pixels, filter, fits, clip);
    }

    @Override
    public synchronized void close() {
        if (closed)
            return;
        closed = true;
        if (temporary) {
            try {
                Files.deleteIfExists(path);
            } catch (Exception e) {
                Log.error("Cannot remove staged FITS " + path, e);
            }
        }
    }

    private float[] readPixels(int stepX, int stepY) throws Exception {
        return FITSReader.read(path, selection.hdu() + 1, bitpix, data.width(), data.height(), selection.plane(), tileRows, stepX, stepY);
    }

    private static int findHDU(BasicHDU<?>[] hdus, List<Image> images) throws Exception {
        for (Image image : images) {
            if (hdus[image.hdu()] instanceof CompressedImageHDU)
                return image.hdu();
        }
        if (images.isEmpty())
            throw new Exception("No supported 2D image or 3D cube found");
        return images.getFirst().hdu();
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
