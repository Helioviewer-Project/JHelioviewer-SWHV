package org.helioviewer.jhv.view.simpleimageview;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.Buffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import javax.imageio.ImageIO;

import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.imagedata.ImageData.ImageFormat;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.metadata.PixelBasedMetaData;
import org.helioviewer.jhv.view.AbstractView;

public class SimpleImageView extends AbstractView {

    public SimpleImageView(URI _uri, APIRequest _request) throws Exception {
        super(_uri, _request);

        BufferedImage image = ImageIO.read(uri.toURL());
        if (image == null)
            throw new Exception("Could not read image: " + uri);

        int w = image.getWidth();
        int h = image.getHeight();
        metaData[0] = new PixelBasedMetaData(w, h, 0);

        Buffer buffer;
        ImageFormat format;
        switch (image.getType()) {
            case BufferedImage.TYPE_BYTE_GRAY:
                buffer = ByteBuffer.wrap(((DataBufferByte) image.getRaster().getDataBuffer()).getData());
                format = ImageFormat.Gray8;
                break;
            case BufferedImage.TYPE_USHORT_GRAY:
                buffer = ShortBuffer.wrap(((DataBufferUShort) image.getRaster().getDataBuffer()).getData());
                format = ImageFormat.Gray16;
                break;
            case BufferedImage.TYPE_INT_ARGB_PRE:
                buffer = IntBuffer.wrap(((DataBufferInt) image.getRaster().getDataBuffer()).getData());
                format = ImageFormat.ARGB32;
                break;
            default:
                BufferedImage conv = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
                conv.getGraphics().drawImage(image, 0, 0, null);
                buffer = IntBuffer.wrap(((DataBufferInt) conv.getRaster().getDataBuffer()).getData());
                format = ImageFormat.ARGB32;
        }
        imageData = new ImageData(w, h, format, buffer);
        imageData.setRegion(metaData[0].getPhysicalRegion());
        imageData.setMetaData(metaData[0]);
    }

    @Override
    public boolean isComplete() {
        return true;
    }

}
