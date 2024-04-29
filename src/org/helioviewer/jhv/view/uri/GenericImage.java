package org.helioviewer.jhv.view.uri;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.Buffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Iterator;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;

import org.helioviewer.jhv.Log;
//import org.helioviewer.jhv.base.XMLUtils;
import org.helioviewer.jhv.imagedata.ImageBuffer;

// essentially static; local or network cache
class GenericImage implements URIImageReader {

    @Override
    public URIImageReader.Image readImage(File file) throws Exception {
        try (ImageInputStream iis = new FileImageInputStream(file)) {
            ImageReader reader = getReader(iis);
            if (reader == null)
                throw new Exception("No image reader found");

            String xml = null;
            // read metadata of first image
            try {
                IIOMetadata metadata = reader.getImageMetadata(0); // random files may have malformed metadata
                IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree("javax_imageio_1.0");
                Object text = root.getElementsByTagName("TextEntry").item(0);
                if (text instanceof IIOMetadataNode) {
                    xml = ((IIOMetadataNode) text).getAttribute("value");
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
            int[] lut = readLUT(image);
            ImageBuffer imageBuffer = readBuffered(image);
            reader.dispose();

            return new URIImageReader.Image(xml, imageBuffer, lut);
        }
    }

    @Override
    public ImageBuffer readImageBuffer(File file) throws Exception {
        try (ImageInputStream iis = new FileImageInputStream(file)) {
            ImageReader reader = getReader(iis);
            if (reader == null)
                throw new Exception("No image reader found");

            ImageBuffer imageBuffer = readBuffered(reader.read(0));
            reader.dispose();
            return imageBuffer;
        }
    }

    @Nullable
    private static ImageReader getReader(ImageInputStream iis) {
        Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
        if (!readers.hasNext())
            return null;

        // pick the first available ImageReader
        ImageReader reader = readers.next();
        // attach source to the reader
        reader.setInput(iis, true);
        return reader;
    }

    private static ImageBuffer readBuffered(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();

        Buffer buffer;
        ImageBuffer.Format format;
        switch (image.getType()) {
            case BufferedImage.TYPE_BYTE_GRAY, BufferedImage.TYPE_BYTE_INDEXED -> {
                buffer = ByteBuffer.wrap(((DataBufferByte) image.getRaster().getDataBuffer()).getData());
                format = ImageBuffer.Format.Gray8;
            }
            case BufferedImage.TYPE_USHORT_GRAY -> {
                buffer = ShortBuffer.wrap(((DataBufferUShort) image.getRaster().getDataBuffer()).getData());
                format = ImageBuffer.Format.Gray16;
            }
            case BufferedImage.TYPE_INT_ARGB_PRE -> {
                buffer = IntBuffer.wrap(((DataBufferInt) image.getRaster().getDataBuffer()).getData());
                format = ImageBuffer.Format.ARGB32;
            }
            default -> {
                BufferedImage conv = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
                conv.getGraphics().drawImage(image, 0, 0, null);
                buffer = IntBuffer.wrap(((DataBufferInt) conv.getRaster().getDataBuffer()).getData());
                format = ImageBuffer.Format.ARGB32;
            }
        }
        return new ImageBuffer(w, h, format, buffer);
    }

    @Nullable
    private static int[] readLUT(BufferedImage image) {
        ColorModel cm = image.getColorModel();
        if (cm instanceof IndexColorModel icm) {
            int num = icm.getMapSize();
            byte[] r = new byte[num];
            byte[] g = new byte[num];
            byte[] b = new byte[num];
            icm.getReds(r);
            icm.getGreens(g);
            icm.getBlues(b);

            int[] lut = new int[num];
            for (int i = 0; i < num; i++) {
                lut[i] = 0xFF000000 | ((r[i] & 0xFF) << 16) | ((g[i] & 0xFF) << 8) | (b[i] & 0xFF);
            }
            return lut;
        }
        return null;
    }

}
