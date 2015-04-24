package org.helioviewer.viewmodel.metadata;

import org.helioviewer.base.math.Vector2dInt;

/**
 * Meta data providing information about the resolution.
 * 
 * <p>
 * This interface provides informations about the relation between physical size
 * and pixel size. For images without informations about their physical size,
 * {@link #getUnitsPerPixel()} always returns 1, meaning that the physical size
 * is the pixel size by default.
 * 
 * @author Ludwig Schmidt
 * 
 */
public interface ImageSizeMetaData extends MetaData {
    /**
     * Returns the pixel resolution of the image.
     * 
     * @return Pixel resolution of the image
     */
    public Vector2dInt getResolution();

    /**
     * Returns the conversion factor from pixels to a physical unit.
     * 
     * @return conversion factor from pixels to a physical unit.
     */
    public double getUnitsPerPixel();

}
