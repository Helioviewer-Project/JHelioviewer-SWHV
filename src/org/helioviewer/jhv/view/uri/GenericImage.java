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
import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.image.lut.LUT;
import org.helioviewer.jhv.image.nio.NativeImageFactory;
import org.helioviewer.jhv.view.ClipSet;

final class GenericImage implements URIImageReader {

    private interface ReaderAction<T> {
        T run(ImageReader reader) throws Exception;
    }

    private static <T> T withReader(File file, ReaderAction<T> action) throws Exception {
        try (ImageInputStream iis = new FileImageInputStream(file)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext())
                throw new Exception("No image reader found");
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true);
                return action.run(reader);
            } finally {
                reader.dispose();
            }
        }
    }

    @Override
    public URIImageReader.Info readInfo(File file) throws Exception {
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
                Log.error(file.toString(), e);
            }
            LUT lut = readLUT(reader.getImageTypes(0).next().getColorModel());
            return new URIImageReader.Info(xml, reader.getWidth(0), reader.getHeight(0), lut, null);
        });
    }

    @Override
    public ImageBuffer decode(File file, ImageFilter filter, @Nullable ClipSet clipSet) throws Exception {
        return withReader(file, reader -> convertImage(reader.read(0), filter));
    }

    private static ImageBuffer convertImage(BufferedImage image, ImageFilter filter) {
        int w = image.getWidth();
        int h = image.getHeight();

        switch (image.getType()) {
            case BufferedImage.TYPE_BYTE_GRAY, BufferedImage.TYPE_BYTE_INDEXED -> {
                return ImageBuffer.fromBytes(w, h, ImageBuffer.Format.Gray8,
                        ((DataBufferByte) image.getRaster().getDataBuffer()).getData(), filter);
            }
            case BufferedImage.TYPE_USHORT_GRAY -> {
                return ImageBuffer.fromShorts(w, h, ImageBuffer.Format.Gray16F,
                        toHalfFloatInPlace(((DataBufferUShort) image.getRaster().getDataBuffer()).getData()), filter);
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
                    ImageBuffer.WriteBuffer output = ImageBuffer.createWriteBuffer(w, h, ImageBuffer.Format.RGBA32, filter);
                    BufferUtils.putRemaining(output.byteBuffer(), NativeImageFactory.getByteBuffer(conv)).flip();
                    return output.finish();
                } finally {
                    NativeImageFactory.free(conv);
                }
            }
        }
    }

    private static short[] toHalfFloatInPlace(short[] data) {
        for (int i = 0; i < data.length; i++)
            data[i] = Float.floatToFloat16((data[i] & 0xFFFF) / 65535f);
        return data;
    }

    @Nullable
    private static LUT readLUT(ColorModel cm) {
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
