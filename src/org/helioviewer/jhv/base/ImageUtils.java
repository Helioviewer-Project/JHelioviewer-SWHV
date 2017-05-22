package org.helioviewer.jhv.base;

import java.io.File;
import java.io.IOException;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;

public class ImageUtils {

   public static BufferedImage deepCopy(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);

        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    public static void writePNG(BufferedImage image, String name) throws IOException {
        Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("png");
        ImageWriter writer = iter.next();

        ImageWriteParam iwp = writer.getDefaultWriteParam();
        // iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        // iwp.setCompressionQuality(0);

        FileImageOutputStream output = new FileImageOutputStream(new File(name));
        writer.setOutput(output);
        IIOImage outimage = new IIOImage(image, null, null);
        writer.write(null, outimage, iwp);
        writer.dispose();

        // ImageIO.write(image, "png", new File(name));
    }

}
