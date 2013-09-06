package org.helioviewer.viewmodel.view.bufferedimage;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.helioviewer.viewmodel.imagedata.ARGBInt32ImageData;
import org.helioviewer.viewmodel.imagedata.ColorMask;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.imagedata.JavaBufferedImageData;
import org.helioviewer.viewmodel.view.AbstractLayeredView;
import org.helioviewer.viewmodel.view.SubimageDataView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.viewportimagesize.ViewportImageSize;

/**
 * Implementation of LayeredView for rendering in software mode.
 * 
 * <p>
 * This class merged multiple layers by drawing them into one single image,
 * including scaling and moving them in a correct way.
 * 
 * <p>
 * For further information about the role of the LayeredView within the view
 * chain, see {@link org.helioviewer.viewmodel.view.LayeredView}
 * 
 * @author Ludwig Schmidt
 * @author Markus Langenberg
 */
public class BufferedImageLayeredView extends AbstractLayeredView implements SubimageDataView {

    ARGBInt32ImageData imageData;
    BufferedImage buffer;

    /**
     * {@inheritDoc}
     */
    public ImageData getSubimageData() {
        return imageData;
    }

    /**
     * {@inheritDoc}
     */
    protected void redrawBufferImpl() {
        if ((viewportImageSize == null) || (viewportImageSize.getWidth() <= 0) || (viewportImageSize.getHeight() <= 0)) {

            return;
        }

        layerLock.lock();

        buffer = new BufferedImage(viewportImageSize.getWidth(), viewportImageSize.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = buffer.createGraphics();

        boolean redWasUsed = false;
        boolean greenWasUsed = false;
        boolean blueWasUsed = false;

        try {
            for (View v : layers) {
                Layer layer = viewLookup.get(v);
                if (layer == null || layer.viewportView == null || layer.regionView == null || !layer.visibility) {
                    continue;
                }

                ViewportImageSize s = ViewHelper.calculateViewportImageSize(layer.viewportView.getViewport(), layer.regionView.getRegion());

                if (s == null || !s.hasArea()) {
                    continue;
                }

                JavaBufferedImageData data = ViewHelper.getImageDataAdapter(v, JavaBufferedImageData.class);

                if (data == null) {
                    continue;
                }
                BufferedImage img = data.getBufferedImage();
                ColorMask colorMask = data.getColorMask();
                int intMask = colorMask.getMask();

                if (intMask == 0xFFFFFFFF) {
                    g.drawImage(img, layer.renderOffset.getX(), layer.renderOffset.getY(), s.getWidth(), s.getHeight(), null);
                } else {
                    // Since Color mask are not supported in Java, perform
                    // blending manually :-/
                    // The blending is performed with the SRC_OVER-Rule,
                    // regardless of the current AlphaComposite.

                    BufferedImage scaledImage;

                    if (img.getWidth() == s.getWidth() && img.getHeight() == s.getHeight()) {
                        scaledImage = img;
                    } else {
                        scaledImage = new BufferedImage(s.getWidth(), s.getHeight(), BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g2 = scaledImage.createGraphics();
                        g2.setComposite(AlphaComposite.Src);
                        g2.drawImage(img, 0, 0, s.getWidth(), s.getHeight(), null);
                    }

                    boolean dstAlphaIsPremultiplied = buffer.getColorModel().isAlphaPremultiplied();

                    int[] dstPixels = new int[s.getWidth() * s.getHeight()];
                    buffer.getRGB(layer.renderOffset.getX(), layer.renderOffset.getY(), s.getWidth(), s.getHeight(), dstPixels, 0, s.getWidth());

                    if (!dstAlphaIsPremultiplied) {
                        for (int i = 0; i < s.getWidth() * s.getHeight(); i++) {
                            int dstPixel = dstPixels[i];
                            dstPixels[i] = dstPixel & 0xFF000000;
                            int dstAlpha = dstPixels[i] >>> 24;
                            dstPixels[i] |= (((dstPixel & 0x00FF0000) >>> 16) * dstAlpha / 0xFF) << 16;
                            dstPixels[i] |= (((dstPixel & 0x0000FF00) >>> 8) * dstAlpha / 0xFF) << 8;
                            dstPixels[i] |= (((dstPixel & 0x000000FF)) * dstAlpha / 0xFF);
                        }
                    }

                    int[] srcPixels = new int[s.getWidth() * s.getHeight()];
                    scaledImage.getRGB(0, 0, s.getWidth(), s.getHeight(), srcPixels, 0, s.getWidth());

                    if (!scaledImage.getColorModel().isAlphaPremultiplied()) {
                        for (int i = 0; i < s.getWidth() * s.getHeight(); i++) {
                            int srcPixel = srcPixels[i];
                            srcPixels[i] &= 0xFF000000;
                            int srcAlpha = srcPixels[i] >>> 24;
                            srcPixels[i] |= (((srcPixel & 0x00FF0000) >>> 16) * srcAlpha / 0xFF) << 16;
                            srcPixels[i] |= (((srcPixel & 0x0000FF00) >>> 8) * srcAlpha / 0xFF) << 8;
                            srcPixels[i] |= (((srcPixel & 0x000000FF)) * srcAlpha / 0xFF);
                        }
                    }

                    for (int i = 0; i < s.getWidth() * s.getHeight(); i++) {
                        int srcPixel = srcPixels[i];
                        int srcAlphaRaw = srcPixel & 0xFF000000;
                        int srcAlpha = (srcAlphaRaw >>> 24) & 0xFF;
                        int srcAlphaDiff = 0xFF - srcAlpha;

                        int dstPixel = dstPixels[i];
                        dstPixels[i] = (dstPixel & ~intMask) | (srcAlphaRaw + ((((dstPixel & 0xFF000000) >>> 24) * srcAlphaDiff / 0xFF) << 24));

                        if (colorMask.showRed()) {
                            dstPixels[i] |= ((((dstPixel & 0x00FF0000) >>> 16) * srcAlphaDiff / 0xFF) << 16) + (srcPixel & 0x00FF0000);
                        }
                        if (colorMask.showGreen()) {
                            dstPixels[i] |= ((((dstPixel & 0x0000FF00) >>> 8) * srcAlphaDiff / 0xFF) << 8) + (srcPixel & 0x0000FF00);
                        }
                        if (colorMask.showBlue()) {
                            dstPixels[i] |= ((((dstPixel & 0x000000FF)) * srcAlphaDiff / 0xFF)) + (srcPixel & 0x000000FF);
                        }
                    }

                    if (!dstAlphaIsPremultiplied) {
                        for (int i = 0; i < s.getWidth() * s.getHeight(); i++) {
                            int dstPixel = dstPixels[i];
                            dstPixels[i] = dstPixel & 0xFF000000;
                            int dstAlpha = dstPixels[i] >>> 24;

                            if (dstAlpha == 0) {
                                continue;
                            }

                            dstPixels[i] |= (((dstPixel & 0x00FF0000) >>> 16) * 0xFF / dstAlpha) << 16;
                            dstPixels[i] |= (((dstPixel & 0x0000FF00) >>> 8) * 0xFF / dstAlpha) << 8;
                            dstPixels[i] |= (((dstPixel & 0x000000FF)) * 0xFF / dstAlpha);
                        }
                    }

                    buffer.setRGB(layer.renderOffset.getX(), layer.renderOffset.getY(), s.getWidth(), s.getHeight(), dstPixels, 0, s.getWidth());
                }

            }
        } finally {
            layerLock.unlock();
        }

        imageData = new ARGBInt32ImageData(buffer, new ColorMask(redWasUsed, greenWasUsed, blueWasUsed));
    }
}
