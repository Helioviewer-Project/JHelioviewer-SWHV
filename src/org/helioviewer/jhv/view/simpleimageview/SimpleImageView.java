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

import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.imagedata.ImageData.ImageFormat;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.metadata.PixelBasedMetaData;
import org.helioviewer.jhv.view.AbstractView;

public class SimpleImageView extends AbstractView {

    private final ImageBuffer imageBuffer;

    public SimpleImageView(URI _uri, APIRequest _request) throws Exception {
        super(_uri, _request);

        BufferedImage image = ImageIO.read(uri.toURL());
        if (image == null)
            throw new Exception("Could not read image: " + uri);

        int w = image.getWidth();
        int h = image.getHeight();
        metaData[0] = new PixelBasedMetaData(w, h, 0);

        ImageFormat format;
        Buffer buffer;
        switch (image.getType()) {
            case BufferedImage.TYPE_BYTE_GRAY:
                format = ImageFormat.Gray8;
                buffer = ByteBuffer.wrap(((DataBufferByte) image.getRaster().getDataBuffer()).getData());
                break;
            case BufferedImage.TYPE_USHORT_GRAY:
                format = ImageFormat.Gray16;
                buffer = ShortBuffer.wrap(((DataBufferUShort) image.getRaster().getDataBuffer()).getData());
                break;
            case BufferedImage.TYPE_INT_ARGB_PRE:
                format = ImageFormat.ARGB32;
                buffer = IntBuffer.wrap(((DataBufferInt) image.getRaster().getDataBuffer()).getData());
                break;
            default:
                format = ImageFormat.ARGB32;
                BufferedImage conv = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
                conv.getGraphics().drawImage(image, 0, 0, null);
                buffer = IntBuffer.wrap(((DataBufferInt) conv.getRaster().getDataBuffer()).getData());
        }
        imageBuffer = new ImageBuffer(w, h, format, buffer);
        imageData = new ImageData(imageBuffer);
        imageData.setRegion(metaData[0].getPhysicalRegion());
        imageData.setMetaData(metaData[0]);
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
