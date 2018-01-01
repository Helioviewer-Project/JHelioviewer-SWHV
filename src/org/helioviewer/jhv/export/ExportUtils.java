package org.helioviewer.jhv.export;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

import org.helioviewer.jhv.opengl.GLInfo;

class ExportUtils {

    private static void flipVertically(BufferedImage img, int h) {
        int w = img.getWidth();
        Object scanline1 = null, scanline2 = null;
        WritableRaster raster = img.getRaster();

        for (int i = 0; i < h / 2; i++) {
            scanline1 = raster.getDataElements(0, i, w, 1, scanline1);
            scanline2 = raster.getDataElements(0, h - i - 1, w, 1, scanline2);
            raster.setDataElements(0, i, w, 1, scanline2);
            raster.setDataElements(0, h - i - 1, w, 1, scanline1);
        }
    }

    static BufferedImage pasteCanvases(BufferedImage im1, int frameH, BufferedImage im2, int movieLinePosition, int finalH) {
        flipVertically(im1, frameH);
        if (im2 == null)
            return im1;

        AffineTransformOp op = new AffineTransformOp(AffineTransform.getScaleInstance(1, 1), AffineTransformOp.TYPE_BILINEAR);
        im2 = op.filter(im2, null);

        Graphics2D g2 = im1.createGraphics();
        g2.drawImage(im2, 0, frameH, im1.getWidth(), finalH - frameH, null);

        if (ExportMovie.EVEMovieLinePosition != -1) {
            g2.setColor(Color.BLACK);

            double scaleY = (finalH - frameH) / (double) im2.getHeight();
            double scaleX = im1.getWidth() / (double) im2.getWidth();

            AffineTransform at = AffineTransform.getTranslateInstance(0, frameH);
            at.concatenate(AffineTransform.getScaleInstance(scaleX, scaleY));
            g2.setTransform(at);
            g2.drawLine(movieLinePosition * GLInfo.pixelScale[0], 0, movieLinePosition * GLInfo.pixelScale[0], im2.getHeight());
        }

        g2.dispose();

        return im1;
    }

}
