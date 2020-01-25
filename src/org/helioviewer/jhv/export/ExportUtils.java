package org.helioviewer.jhv.export;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import org.helioviewer.jhv.base.image.MappedImageFactory;
import org.helioviewer.jhv.base.image.NIOImageFactory;
import org.helioviewer.jhv.opengl.GLInfo;

class ExportUtils {

    static void flipVertically(BufferedImage img) {
        int w = 3 * img.getWidth(); // assume bgr
        int h = img.getHeight();
        byte[] line1 = new byte[w];
        byte[] line2 = new byte[w];
        ByteBuffer data = MappedImageFactory.getByteBuffer(img);

        for (int i = 0; i < h / 2; i++) {
            data.position(w * i);
            data.get(line1);
            data.position(w * (h - i - 1));
            data.get(line2);

            data.position(w * (h - i - 1));
            data.put(line1);
            data.position(w * i);
            data.put(line2);
        }
    }

    static BufferedImage scaleImage(BufferedImage img, int newW, int newH, int movieLinePosition) {
        int oldW = img.getWidth(), oldH = img.getHeight();
        Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        BufferedImage simg = NIOImageFactory.createCompatible(newW, newH, BufferedImage.TYPE_3BYTE_BGR);

        Graphics2D g = simg.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(tmp, 0, 0, null);

        if (movieLinePosition != -1) {
            g.setColor(Color.BLACK);
            g.setTransform(AffineTransform.getScaleInstance(newW / (double) oldW, newH / (double) oldH));
            int screenMovieLine = (int) (movieLinePosition * GLInfo.pixelScale[0] + .5);
            g.drawLine(screenMovieLine, 0, screenMovieLine, oldH);
        }
        g.dispose();

        return simg;
    }

}
