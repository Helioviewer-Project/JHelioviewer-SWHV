package org.helioviewer.viewmodel.view.bufferedimage;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.RegionChangedReason;
import org.helioviewer.viewmodel.changeevent.SubImageDataChangedReason;
import org.helioviewer.viewmodel.changeevent.ViewChainChangedReason;
import org.helioviewer.viewmodel.imagedata.ARGBInt32ImageData;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.imagedata.JavaBufferedImageData;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.metadata.HelioviewerOcculterMetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.AbstractBasicView;
import org.helioviewer.viewmodel.view.HelioviewerGeometryView;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.SubimageDataView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.viewportimagesize.StaticViewportImageSize;
import org.helioviewer.viewmodel.viewportimagesize.ViewportImageSize;

/**
 * Implementation of HelioviewGeometryView for rendering in OpenGL mode.
 * 
 * <p>
 * This class provides the capability to cut out the invalid areas in solar
 * images. It does so by calculating the distance from the center for every
 * single pixel in the image. If the distance is outside the valid area of that
 * specific image, its alpha value is set to zero, otherwise it remains
 * untouched.
 * 
 * <p>
 * Technically, it uses the Java AlphaComposite to mask invalid areas.
 * 
 * <p>
 * For further information about the role of the HelioviewerGeometryView within
 * the view chain, see
 * {@link org.helioviewer.viewmodel.view.HelioviewerGeometryView}
 * 
 * @author Markus Langenberg
 */
public class BufferedImageHelioviewerGeometryView extends AbstractBasicView implements SubimageDataView, HelioviewerGeometryView {

    private RegionView regionView;
    private SubimageDataView subimageDataView;
    private ImageData imageData;
    private BufferedImage maskImage;
    private HelioviewerMetaData metaData;

    /**
     * {@inheritDoc}
     */
    protected void setViewSpecificImplementation(View newView, ChangeEvent changeEvent) {
        updatePrecomputed();

        if (subimageDataView != null) {
            redraw();
            changeEvent.addReason(new SubImageDataChangedReason(this));
        }
    }

    /**
     * {@inheritDoc}
     */
    public ImageData getSubimageData() {
        return imageData;
    }

    /**
     * {@inheritDoc}
     */
    public void viewChanged(View sender, ChangeEvent aEvent) {

        if (aEvent.reasonOccurred(ViewChainChangedReason.class)) {
            updatePrecomputed();
        }

        if (aEvent.reasonOccurred(RegionChangedReason.class) || aEvent.reasonOccurred(SubImageDataChangedReason.class)) {
            redraw();
        }

        notifyViewListeners(aEvent);
    }

    /**
     * Updates the precomputed view adapters.
     */
    private void updatePrecomputed() {
        regionView = ViewHelper.getViewAdapter(view, RegionView.class);
        subimageDataView = ViewHelper.getViewAdapter(view, SubimageDataView.class);

        MetaDataView metaDataView = ViewHelper.getViewAdapter(view, MetaDataView.class);
        if (metaDataView != null && metaDataView.getMetaData() != null && metaDataView.getMetaData() instanceof HelioviewerMetaData) {
            metaData = (HelioviewerMetaData) metaDataView.getMetaData();
        }

        redrawMask();
    }

    /**
     * Redraws the mask used for alpha masking.
     * 
     * The mask is an image containing transparent areas where the actual image
     * contains invalid areas. The two images are merged using an
     * AlphaComposite.
     */
    private void redrawMask() {
        if (subimageDataView != null && regionView != null && regionView.getRegion() != null) {
            ImageData data = subimageDataView.getSubimageData();

            if (data instanceof JavaBufferedImageData && metaData != null) {
                maskImage = new BufferedImage(data.getWidth(), data.getHeight(), BufferedImage.TYPE_INT_ARGB);

                ViewportImageSize viewportImageSize = StaticViewportImageSize.createAdaptedViewportImageSize(data.getWidth(), data.getHeight());

                Vector2dInt offset = ViewHelper.convertImageToScreenDisplacement(-regionView.getRegion().getUpperLeftCorner().getX(), regionView.getRegion().getUpperLeftCorner().getY(), regionView.getRegion(), viewportImageSize);

                Vector2dInt radius;
                Region region = regionView.getRegion();

                if (metaData instanceof HelioviewerOcculterMetaData) {
                    radius = ViewHelper.convertImageToScreenDisplacement(((HelioviewerOcculterMetaData) metaData).getInnerPhysicalOcculterRadius() * roccInnerFactor, ((HelioviewerOcculterMetaData) metaData).getOuterPhysicalOcculterRadius() * roccOuterFactor, region, viewportImageSize);
                } else if (metaData.getInstrument().equalsIgnoreCase("LASCO")) {
                    radius = ViewHelper.convertImageToScreenDisplacement(Constants.SunRadius * discFadingFactor, Constants.SunRadius, region, viewportImageSize);
                } else {
                    radius = ViewHelper.convertImageToScreenDisplacement(0, Constants.SunRadius * discFactor, region, viewportImageSize);
                }

                Graphics2D g = maskImage.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (metaData.getInstrument().equalsIgnoreCase("MDI") || metaData.getInstrument().equalsIgnoreCase("HMI") || metaData.getInstrument().equalsIgnoreCase("LASCO")) {
                    g.setComposite(AlphaComposite.Clear);
                    g.fillRect(0, 0, data.getWidth(), data.getHeight());
                    g.setComposite(AlphaComposite.Src);
                    g.fillOval(offset.getX() - radius.getY(), offset.getY() - radius.getY(), radius.getY() * 2, radius.getY() * 2);

                    if (radius.getX() != 0) {
                        g.setComposite(AlphaComposite.Clear);
                        g.fillOval(offset.getX() - radius.getX(), offset.getY() - radius.getX(), radius.getX() * 2, radius.getX() * 2);

                        HelioviewerOcculterMetaData occulterMetaData = (HelioviewerOcculterMetaData) metaData;
                        int flatSize = ViewHelper.convertImageToScreenDisplacement(occulterMetaData.getPhysicalFlatOcculterSize(), 0, region, viewportImageSize).getX();
                        double rotation = occulterMetaData.getMaskRotation();

                        if (Math.abs(rotation) > 1e-3) {
                            g.rotate(-rotation, offset.getX(), offset.getY());
                            g.fillRect(offset.getX() - 2 * flatSize, offset.getY() - flatSize, flatSize, 2 * flatSize);
                            g.fillRect(offset.getX() - flatSize, offset.getY() - 2 * flatSize, 2 * flatSize, flatSize);
                            g.fillRect(offset.getX() + flatSize, offset.getY() - flatSize, flatSize, 2 * flatSize);
                            g.fillRect(offset.getX() - flatSize, offset.getY() + flatSize, 2 * flatSize, flatSize);
                        }
                    }

                } else { // EIT and AIA
                    int[] sourcePixels = new int[data.getWidth() * data.getHeight()];
                    ((JavaBufferedImageData) data).getBufferedImage().getRGB(0, 0, data.getWidth(), data.getHeight(), sourcePixels, 0, data.getWidth());

                    for (int i = 0; i < data.getWidth() * data.getHeight(); i++) {
                        int posX = i % data.getWidth();
                        int posY = i / data.getWidth();

                        double currentRadius = Math.sqrt(Math.pow(offset.getX() - posX, 2) + Math.pow(offset.getY() - posY, 2));

                        if (currentRadius < radius.getY()) {
                            sourcePixels[i] = 0xFFFFFFFF;
                        } else {
                            int pixel = sourcePixels[i] & 0x00FFFFFF;
                            int maxPixelValue = Math.max(Math.max((pixel & 0x00FF0000) >> 16, (pixel & 0x0000FF00) >> 8), (pixel & 0x000000FF));
                            int alphaModification = ((int) (255 * Math.pow(maxPixelValue / 255.0f, 1.0f - ((sourcePixels[i] >> 24) & 0xFF) / 255.0f)));

                            if (currentRadius > radius.getX()) {
                                sourcePixels[i] = 0x00FFFFFF | (alphaModification << 24);
                            } else {

                                float fadeDisc = (float) ((radius.getX() - currentRadius) / (radius.getX() - radius.getY()));
                                fadeDisc = Math.max(Math.min(fadeDisc, 1.0f), 0.0f);
                                fadeDisc = fadeDisc * fadeDisc * (3.0f - 2.0f * fadeDisc);

                                sourcePixels[i] = 0x00FFFFFF | ((int) (0xFF * fadeDisc + alphaModification * (1 - fadeDisc)) << 24);
                            }
                        }
                    }

                    maskImage.setRGB(0, 0, data.getWidth(), data.getHeight(), sourcePixels, 0, data.getWidth());
                }

                g.dispose();
            }
        }
    }

    /**
     * Redraws the masked image.
     * 
     * To turn the invalid areas transparent, the image is masked with a
     * precalculated mask using AlphaComposite.
     */
    private void redraw() {

        ImageData data = subimageDataView.getSubimageData();

        if (data instanceof JavaBufferedImageData && metaData != null) {
            BufferedImage source = ((JavaBufferedImageData) data).getBufferedImage();
            BufferedImage target = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);

            redrawMask();

            Graphics2D g = target.createGraphics();
            g.setComposite(AlphaComposite.Clear);
            g.fillRect(0, 0, data.getWidth(), data.getHeight());
            g.setComposite(AlphaComposite.Src);
            g.drawImage(source, 0, 0, null);
            g.setComposite(AlphaComposite.DstIn);
            g.drawImage(maskImage, 0, 0, null);
            g.dispose();

            imageData = new ARGBInt32ImageData(data, target);
        } else {
            imageData = data;
        }
    }
}
