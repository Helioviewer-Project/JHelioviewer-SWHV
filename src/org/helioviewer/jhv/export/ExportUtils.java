package org.helioviewer.jhv.export;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.imagedata.nio.NativeImageFactory;
import org.helioviewer.jhv.opengl.GLInfo;

class ExportUtils {

    private ExportUtils() {
    }

    static BufferedImage scaleImage(BufferedImage img, int newW, int newH, int movieLinePosition) {
        BufferedImage simg = NativeImageFactory.createCompatible(newW, newH, BufferedImage.TYPE_3BYTE_BGR);

        Graphics2D g = simg.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, newW, newH, null);
        if (movieLinePosition != -1) {
            int oldW = img.getWidth(), oldH = img.getHeight();
            g.setColor(UIGlobals.TL_MOVIE_FRAME_COLOR);
            g.setTransform(AffineTransform.getScaleInstance(newW / (double) oldW, newH / (double) oldH));
            int screenMovieLine = (int) (movieLinePosition * GLInfo.pixelScale[0] + .5);
            g.drawLine(screenMovieLine, 0, screenMovieLine, oldH);
        }
        g.dispose();

        return simg;
    }

}
