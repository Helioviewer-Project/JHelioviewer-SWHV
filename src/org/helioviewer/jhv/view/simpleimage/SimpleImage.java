package org.helioviewer.jhv.view.simpleimage;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.net.URI;
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
import javax.imageio.stream.ImageInputStream;

//import org.helioviewer.jhv.base.XMLUtils;
import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.view.URIImageReader;

// essentially static; local or network cache
class SimpleImage implements URIImageReader {

    @Nullable
    public String readXML(URI uri) throws Exception {
        try (NetClient nc = NetClient.of(uri); ImageInputStream iis = ImageIO.createImageInputStream(nc.getStream())) {
            ImageReader reader = getReader(iis);
            if (reader == null)
                throw new Exception("No image reader found for: " + uri);

            String xml = null;
            // read metadata of first image
            IIOMetadata metadata = reader.getImageMetadata(0);
            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree("javax_imageio_1.0");
            Object text = root.getElementsByTagName("TextEntry").item(0);
            if (text instanceof IIOMetadataNode) {
                xml = ((IIOMetadataNode) text).getAttribute("value");
                if (xml != null) // coverity
                    xml = xml.trim().replace("&", "&amp;");
            }
            /*
            String[] names = metadata.getMetadataFormatNames();
            int length = names.length;
            for (int i = 0; i < length; i++) {
                System.out.println("Format name: " + names[i]);
                XMLUtils.displayNode(metadata.getAsTree(names[i]), 0);
            }
            */
            reader.dispose();
            return xml;
        }
    }

    public ImageBuffer readImageBuffer(URI uri) throws Exception {
        try (NetClient nc = NetClient.of(uri); ImageInputStream iis = ImageIO.createImageInputStream(nc.getStream())) {
            ImageReader reader = getReader(iis);
            if (reader == null)
                throw new Exception("No image reader found for: " + uri);

            ImageBuffer imageBuffer = buffered2ImageBuffer(reader.read(0));
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

    private static ImageBuffer buffered2ImageBuffer(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();

        Buffer buffer;
        ImageBuffer.Format format;
        switch (image.getType()) {
            case BufferedImage.TYPE_BYTE_GRAY:
                buffer = ByteBuffer.wrap(((DataBufferByte) image.getRaster().getDataBuffer()).getData());
                format = ImageBuffer.Format.Gray8;
                break;
            case BufferedImage.TYPE_USHORT_GRAY:
                buffer = ShortBuffer.wrap(((DataBufferUShort) image.getRaster().getDataBuffer()).getData());
                format = ImageBuffer.Format.Gray16;
                break;
            case BufferedImage.TYPE_INT_ARGB_PRE:
                buffer = IntBuffer.wrap(((DataBufferInt) image.getRaster().getDataBuffer()).getData());
                format = ImageBuffer.Format.ARGB32;
                break;
            default:
                BufferedImage conv = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
                conv.getGraphics().drawImage(image, 0, 0, null);
                buffer = IntBuffer.wrap(((DataBufferInt) conv.getRaster().getDataBuffer()).getData());
                format = ImageBuffer.Format.ARGB32;
        }
        return new ImageBuffer(w, h, format, buffer);
    }

}