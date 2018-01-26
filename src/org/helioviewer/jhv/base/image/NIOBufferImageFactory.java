package org.helioviewer.jhv.base.image;

//import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.image.*;
import java.nio.Buffer;

public class NIOBufferImageFactory {

    private NIOBufferImageFactory() {
    }

    public static BufferedImage copyImage(BufferedImage bi) {
        BufferedImage ret = createCompatible(bi.getWidth(), bi.getHeight(), bi.getType());
        bi.copyData(ret.getRaster());
        return ret;
    }

    public static BufferedImage createIndexed(Buffer buffer, int width, int height, IndexColorModel cm) {
        BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_INDEXED, cm);
        SampleModel sm = temp.getSampleModel().createCompatibleSampleModel(width, height);
        return new BufferedImage(cm, RasterFactory.factory.createRaster(sm, NIODataBuffer.create(buffer), new Point()), false, null);
    }

    private static BufferedImage createCompatible(int width, int height, int type) {
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

}
