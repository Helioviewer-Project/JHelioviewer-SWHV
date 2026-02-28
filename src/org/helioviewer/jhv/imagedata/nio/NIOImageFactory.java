package org.helioviewer.jhv.imagedata.nio;

//import java.awt.GraphicsConfiguration;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.nio.ByteBuffer;

public class NIOImageFactory {

    private NIOImageFactory() {
    }

    public static BufferedImage copyImage(BufferedImage bi) {
        BufferedImage ret = createCompatible(bi.getWidth(), bi.getHeight(), bi.getType());
        try {
            bi.copyData(ret.getRaster());
            return ret;
        } catch (RuntimeException | Error e) {
            free(ret);
            throw e;
        }
    }

    public static BufferedImage createCompatible(int width, int height, int type) {
        BufferedImage temp = new BufferedImage(1, 1, type);
        return createCompatible(width, height, temp.getSampleModel().createCompatibleSampleModel(width, height), temp.getColorModel());
    }

    /*
        public static BufferedImage createCompatible(int width, int height, GraphicsConfiguration configuration, int transparency) {
            return createCompatible(width, height, configuration.getColorModel(transparency));
        }

        private static BufferedImage createCompatible(int width, int height, ColorModel cm) {
            return createCompatible(width, height, cm.createCompatibleSampleModel(width, height), cm);
        }
    */
    private static BufferedImage createCompatible(int width, int height, SampleModel sm, ColorModel cm) {
        DataBuffer buffer = NIODataBuffer.create(sm.getTransferType(), width * height * sm.getNumDataElements(), 1);
        return new BufferedImage(cm, RasterFactory.factory.createRaster(sm, buffer, new Point()), cm.isAlphaPremultiplied(), null);
    }

    public static ByteBuffer getByteBuffer(BufferedImage img) {
        DataBuffer buffer = img.getRaster().getDataBuffer();
        if (buffer instanceof NIODataBuffer.DataBufferByte dbb)
            return (ByteBuffer) dbb.getBuffer();
        else
            throw new IncompatibleClassChangeError("Not a NIODataBuffer byte backed image");
    }

    public static void free(BufferedImage img) {
        if (img == null)
            return;
        DataBuffer buffer = img.getRaster().getDataBuffer();
        if (buffer instanceof NIODataBuffer ndb) {
            ndb.free();
        }
    }

}
