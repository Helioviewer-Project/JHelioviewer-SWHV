package org.helioviewer.jhv.export;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

import com.jogamp.opengl.util.awt.ImageUtil;

public class ExportUtils {

    public static BufferedImage pasteCanvases(BufferedImage im1, BufferedImage im2, int height) {
        ImageUtil.flipImageVertically(im1);

        if (im2 == null)
            return im1;

        BufferedImage ret = new BufferedImage(im1.getWidth(), height, im1.getType());
        Graphics2D g2 = ret.createGraphics();
        g2.drawImage(im1, null, 0, 0);

        AffineTransformOp op = new AffineTransformOp(AffineTransform.getScaleInstance(1, 1), AffineTransformOp.TYPE_BILINEAR);
        im2 = op.filter(im2, null);
        g2.drawImage(im2, 0, im1.getHeight(), im1.getWidth(), ret.getHeight() - im1.getHeight(), null);
        g2.dispose();

        return ret;
    }

}
