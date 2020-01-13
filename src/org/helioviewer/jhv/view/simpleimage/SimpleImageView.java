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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;

//import org.helioviewer.jhv.base.XMLUtils;
import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.PixelBasedMetaData;
import org.helioviewer.jhv.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.view.BaseView;

public class SimpleImageView extends BaseView {

    private String xml;

    public SimpleImageView(APIRequest _request, URI _uri) throws Exception {
        super(_request, _uri);

        BufferedImage image = null;
        try (NetClient nc = NetClient.of(uri); ImageInputStream iis = ImageIO.createImageInputStream(nc.getStream())) {
            image = readStream(iis);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (image == null)
            throw new Exception("Could not read image: " + uri);

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

        MetaData m;
        try {
            m = new XMLMetaDataContainer(xml).getHVMetaData(0, true);
        } catch (Exception ignore) {
            m = new PixelBasedMetaData(w, h, 0, uri);
            xml = "<meta/>";
        }

        imageData = new ImageData(new ImageBuffer(w, h, format, buffer), m);
        imageData.setRegion(m.getPhysicalRegion());
        metaData[0] = m;
    }

    @Nonnull
    @Override
    public String getXMLMetaData() {
        return xml;
    }

    @Nullable
    private BufferedImage readStream(ImageInputStream iis) throws Exception {
        Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
        if (!readers.hasNext())
            return null;

        // pick the first available ImageReader
        ImageReader reader = readers.next();
        // attach source to the reader
        reader.setInput(iis, true);
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
        // read first image
        return reader.read(0);
    }

}
