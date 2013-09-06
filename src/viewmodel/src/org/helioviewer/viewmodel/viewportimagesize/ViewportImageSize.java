package org.helioviewer.viewmodel.viewportimagesize;

/**
 * Extension of {@link BasicViewportImageSize}, representing a region.
 * 
 * It might be useful to get the basic information of a viewport image size in
 * another way. The methods provide a mapping of the basic values in different
 * formats.
 * 
 * @author Ludwig Schmidt
 * */
public interface ViewportImageSize extends BasicViewportImageSize {

    /**
     * Returns the width of the image inside the viewport.
     * 
     * @return width of the image inside the viewport.
     * */
    public int getWidth();

    /**
     * Returns the height of the image inside the viewport.
     * 
     * @return height of the image inside the viewport.
     * */
    public int getHeight();

    /**
     * Checks if the image inside the viewport has a valid size.
     * 
     * @return true if width and height of the image inside the viewport has at
     *         least one pixel; false otherwise.
     * */
    public boolean hasArea();

}
