package org.helioviewer.jhv.base;

import java.io.File;
import java.io.IOException;

import java.awt.Color;
import java.awt.Graphics2D;
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

    public static BufferedImage stackImages(BufferedImage[] bufImgs, double horizontal, double vertical) {
        if (bufImgs.length == 0 || bufImgs[0] == null) {
            return null;
        }

        // the first layer is strictly copied
        BufferedImage result = deepCopy(bufImgs[0]);
        int width = result.getWidth();
        int height = result.getHeight();

        Graphics2D g = result.createGraphics();
        for (int i = 1; i < bufImgs.length; i++) {
            BufferedImage currentImg = bufImgs[i];
            int offsetX = (int) (horizontal * (width - currentImg.getWidth()));
            int offsetY = (int) (vertical * (height - currentImg.getHeight()));
            g.drawImage(currentImg, null, offsetX, offsetY);
        }
        g.dispose();

        return result;
    }

    public static Color getAverageColor(BufferedImage bufImg) {
        double sumRed = 0;
        double sumGreen = 0;
        double sumBlue = 0;

        for (int x = 0; x < bufImg.getWidth(); x++) {
            for (int y = 0; y < bufImg.getHeight(); y++) {
                Color curColor = new Color(bufImg.getRGB(x, y), true);
                sumRed += curColor.getRed();
                sumGreen += curColor.getGreen();
                sumBlue += curColor.getBlue();
            }
        }

        int totalPixels = bufImg.getWidth() * bufImg.getHeight();
        float red = (float) (sumRed / totalPixels / 255);
        float green = (float) (sumGreen / totalPixels / 255);
        float blue = (float) (sumBlue / totalPixels / 255);

        return new Color(red, green, blue);
    }

}
