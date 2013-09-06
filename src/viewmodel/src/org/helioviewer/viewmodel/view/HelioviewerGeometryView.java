package org.helioviewer.viewmodel.view;

/**
 * View specially designed to improve the appearance of solar images.
 * 
 * <p>
 * For many kinds of solar images, not every part of the image rectangle
 * contains valid data, since the sun is round. This view is supposed to cut
 * away the unnecessary parts of such solar images (e.g. MDI and LASCO), to
 * improve the overall look of the entire image. This is done by applying a mask
 * the images, which defines the areas which should turn transparent.
 * 
 * <p>
 * Currently, all implementations support to kinds of masks: Disc images, such
 * as MDI and occulter images, such as LASCO.
 * 
 * <p>
 * The information on which parts of the image can be cut be away is read from
 * the image meta data. Apart from that, the borders are moved a little bit to
 * get a clearer image, on cost of cutting away some of the actual image data.
 * This is behavior is desired, since the border areas of such images do not
 * contain any important data.
 * 
 * @author Markus Langenberg
 * 
 */
public interface HelioviewerGeometryView extends ModifiableInnerViewView, ViewListener {

    /**
     * Factor to use on the sun radius for MDI masks.
     */
    public final static float discFactor = 0.997f;

    /**
     * Factor to use on the inner radius for occulter masks.
     */
    public final static float roccInnerFactor = 1.05f;

    /**
     * Factor to use on the outer radius for occulter masks.
     */
    public final static float roccOuterFactor = 0.9625f;

    /**
     * Factor to determine the range for fading from the disc to the corona.
     */
    public final static float discFadingFactor = 1.05f;
}
