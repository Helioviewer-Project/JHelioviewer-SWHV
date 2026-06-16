package org.helioviewer.jhv.view.uri;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.util.Iterator;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.image.lut.LUT;
import org.helioviewer.jhv.image.nio.NativeImageFactory;
import org.helioviewer.jhv.metadata.Region;
//import org.helioviewer.jhv.io.XMLUtils;

// essentially static; local or network cache
final class GenericImage implements URIImageReader {

    private interface ReaderAction<T> {
        T run(ImageReader reader) throws Exception;
    }

    private static <T> T withReader(File file, ReaderAction<T> action) throws Exception {
        try (ImageInputStream iis = new FileImageInputStream(file)) {
            ImageReader reader = getReader(iis);
            if (reader == null)
                throw new Exception("No image reader found");
            try {
                return action.run(reader);
            } finally {
                reader.dispose();
            }
        }
    }

    @Override
    public URIImageReader.Image readImage(File file) throws Exception {
        return withReader(file, reader -> {
            String xml = null;
            // read metadata of first image
            try {
                IIOMetadata metadata = reader.getImageMetadata(0); // random files may have malformed metadata
                IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree("javax_imageio_1.0");
                Object text = root.getElementsByTagName("TextEntry").item(0);
                if (text instanceof IIOMetadataNode mn) {
                    xml = mn.getAttribute("value");
                }
            } catch (Exception e) {
                Log.error(file.toString(), e); // tbd
            }
            /*
            String[] names = metadata.getMetadataFormatNames();
            int length = names.length;
            for (int i = 0; i < length; i++) {
                System.out.println("Format name: " + names[i]);
                XMLUtils.displayNode(metadata.getAsTree(names[i]), 0);
            }
            */
            BufferedImage image = reader.read(0);
            LUT lut = readLUT(image);
            ImageBuffer imageBuffer = readBuffered(image, ImageFilter.Type.None, null);

            return new URIImageReader.Image(xml, imageBuffer, lut);
        });
    }

    @Override
    public ImageBuffer readImageBuffer(File file, ImageFilter.Type filterType, @Nullable Region region) throws Exception {
        return withReader(file, reader -> readBuffered(reader.read(0), filterType, region));
    }

    @Nullable
    private static ImageReader getReader(ImageInputStream iis) {
        Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
        if (!readers.hasNext())
            return null;

        // pick the first available ImageReader
        ImageReader reader = readers.next();
        try {
            // attach source to the reader
            reader.setInput(iis, true);
            return reader;
        } catch (RuntimeException | Error e) {
            reader.dispose();
            throw e;
        }
    }

    private static ImageBuffer readBuffered(BufferedImage image, ImageFilter.Type filterType, @Nullable Region region) {
        int w = image.getWidth();
        int h = image.getHeight();

        switch (image.getType()) {
            case BufferedImage.TYPE_BYTE_GRAY, BufferedImage.TYPE_BYTE_INDEXED -> {
                return ImageBuffer.fromBytes(w, h, ImageBuffer.Format.Gray8,
                        ((DataBufferByte) image.getRaster().getDataBuffer()).getData(), filterType, region);
            }
            case BufferedImage.TYPE_USHORT_GRAY -> {
                return ImageBuffer.fromShorts(w, h, ImageBuffer.Format.Gray16F,
                        halfFloat(((DataBufferUShort) image.getRaster().getDataBuffer()).getData()), filterType, region);
            }
            default -> {
                BufferedImage conv = NativeImageFactory.createRGBAPremultipliedImage(w, h);
                try {
                    Graphics g = conv.getGraphics();
                    try {
                        g.drawImage(image, 0, 0, null);
                    } finally {
                        g.dispose();
                    }
                    // avoidable native -> heap -> native copy.
                    byte[] buffer = new byte[w * h * 4];
                    NativeImageFactory.getByteBuffer(conv).get(buffer);
                    return ImageBuffer.fromBytes(w, h, ImageBuffer.Format.RGBA32, buffer);
                } finally {
                    NativeImageFactory.free(conv);
                }
            }
        }
    }

    private static short[] halfFloat(short[] data) {
        short[] halfFloat = new short[data.length];
        for (int i = 0; i < data.length; i++)
            halfFloat[i] = Float.floatToFloat16((data[i] & 0xFFFF) / 65535f);
        return halfFloat;
    }

    @Nullable
    private static LUT readLUT(BufferedImage image) {
        ColorModel cm = image.getColorModel();
        if (cm instanceof IndexColorModel icm) {
            int num = icm.getMapSize();
            byte[] r = new byte[num];
            byte[] g = new byte[num];
            byte[] b = new byte[num];
            icm.getReds(r);
            icm.getGreens(g);
            icm.getBlues(b);
            return LUT.fromOpaqueRgb("built-in", r, g, b);
        }
        return null;
    }

}
