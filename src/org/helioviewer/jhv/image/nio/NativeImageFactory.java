package org.helioviewer.jhv.image.nio;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class NativeImageFactory {

    private static final AbstractOwnedDataBuffer.BackendKind BACKEND_KIND = AbstractOwnedDataBuffer.BackendKind.NATIVE;

    private NativeImageFactory() {}

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
        return CompatibleImageUtils.createCompatibleImage(
                width, height, type,
                (dataType, size, numBanks) -> AbstractOwnedDataBuffer.create(dataType, size, numBanks, BACKEND_KIND, BufferBacking::allocate));
    }

    public static BufferedImage createRGBAPremultipliedImage(int width, int height) {
        return CompatibleImageUtils.createRGBAPremultipliedImage(
                width, height,
                (dataType, size, numBanks) -> AbstractOwnedDataBuffer.create(dataType, size, numBanks, BACKEND_KIND, BufferBacking::allocate));
    }

    public static BufferedImage createRGBImage(int width, int height) {
        return CompatibleImageUtils.createRGBImage(
                width, height,
                (dataType, size, numBanks) -> AbstractOwnedDataBuffer.create(dataType, size, numBanks, BACKEND_KIND, BufferBacking::allocate));
    }

    public static ByteBuffer getByteBuffer(BufferedImage bi) {
        return AbstractOwnedDataBuffer.getByteBuffer(bi, BACKEND_KIND);
    }

    public static ShortBuffer getShortBuffer(BufferedImage bi) {
        return AbstractOwnedDataBuffer.getShortBuffer(bi, BACKEND_KIND);
    }

    public static IntBuffer getIntBuffer(BufferedImage bi) {
        return AbstractOwnedDataBuffer.getIntBuffer(bi, BACKEND_KIND);
    }

    public static void free(BufferedImage bi) {
        AbstractOwnedDataBuffer.free(bi, BACKEND_KIND);
    }

}
