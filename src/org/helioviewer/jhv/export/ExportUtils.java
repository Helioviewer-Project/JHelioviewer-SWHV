package org.helioviewer.jhv.export;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import org.helioviewer.jhv.base.image.MappedImageFactory;
import org.helioviewer.jhv.opengl.GLInfo;

class ExportUtils {

    private static void flipVertically(BufferedImage img, int h) {
        int w = 3 * img.getWidth(); // assume bgr
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

    static void pasteCanvases(BufferedImage im1, int frameH, BufferedImage im2, int movieLinePosition, int finalH) {
        flipVertically(im1, frameH);
        if (im2 == null)
            return;

        Graphics2D g = im1.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(im2, 0, frameH, im1.getWidth(), finalH - frameH, null);

        if (movieLinePosition != -1) {
            g.setColor(Color.BLACK);

            double scaleY = (finalH - frameH) / (double) im2.getHeight();
            double scaleX = im1.getWidth() / (double) im2.getWidth();

            AffineTransform at = AffineTransform.getTranslateInstance(0, frameH);
            at.concatenate(AffineTransform.getScaleInstance(scaleX, scaleY));
            g.setTransform(at);
            g.drawLine(movieLinePosition * GLInfo.pixelScale[0], 0, movieLinePosition * GLInfo.pixelScale[0], im2.getHeight());
        }

        g.dispose();
    }

}
