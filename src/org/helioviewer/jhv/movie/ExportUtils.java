package org.helioviewer.jhv.movie;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import org.helioviewer.jhv.image.nio.NativeImageFactory;

class ExportUtils {

    static BufferedImage scaleImage(BufferedImage img, int newW, int newH, int movieLinePosition) {
        BufferedImage simg = NativeImageFactory.createRGBImage(newW, newH);

        Graphics2D g = simg.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, newW, newH, null);
        if (movieLinePosition != -1) {
            int oldW = img.getWidth(), oldH = img.getHeight();
            g.setColor(ExportMovie.movieLineColor);
            g.setTransform(AffineTransform.getScaleInstance(newW / (double) oldW, newH / (double) oldH));
            g.drawLine(movieLinePosition, 0, movieLinePosition, oldH);
        }
        g.dispose();

        return simg;
    }

    private ExportUtils() {}
}
