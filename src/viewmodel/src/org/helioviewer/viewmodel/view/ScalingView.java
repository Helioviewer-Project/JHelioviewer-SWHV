package org.helioviewer.viewmodel.view;

/**
 * View to scale the image.
 * 
 * <p>
 * This view manages the scaling behavior of views, which scale the image.
 * 
 * @author Ludwig Schmidt
 * 
 */
public interface ScalingView extends View {

    /**
     * Interpolation Mode
     */
    public enum InterpolationMode {
        NEAREST_NEIGHBOR, BILINEAR, BICUBIC, LANCZOS
    }

    /**
     * Returns the current interpolation mode.
     * 
     * @return Current interpolation mode.
     * @see #setInterpolationMode(InterpolationMode)
     */
    public InterpolationMode getInterpolationMode();

    /**
     * Sets the interpolation mode.
     * 
     * @param newInterpolationMode
     *            New interpolation mode
     * @see #getInterpolationMode()
     */
    public void setInterpolationMode(InterpolationMode newInterpolationMode);

}
