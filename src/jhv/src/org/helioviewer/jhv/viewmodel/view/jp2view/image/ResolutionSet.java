package org.helioviewer.jhv.viewmodel.view.jp2view.image;

/**
 * A class describing the available resolution levels for a given image. It
 * supplies several simple methods to aid in selecting appropriate zoom levels.
 *
 * @author caplins
 */
public class ResolutionSet {

    /**
     * An array containing the available resolutions. The index's represent the
     * num of discardLayers
     */
    private final ResolutionLevel[] resolutions;

    /**
     * Constructor. Takes the number of resolution levels in the associated
     * image.
     *
     * @param _numResolutions
     */
    public ResolutionSet(int numResolutions) {
        resolutions = new ResolutionLevel[numResolutions];
    }

    /**
     * Adds a resolution level. Used while setting the object up. This method is
     * really the only place a ResolutionLevel object is ever created. NOTE:
     * Normally the resolution levels will always have an origin @ (0,0) and
     * hence a Dimension object would be enough to describe the layer bounds. In
     * some situations this might not be true though, and for extendability I
     * use a rectangle object instead of a Dimension object (even though I don't
     * think I ever explicitly use the origin).
     *
     * @param _discardLayer
     * @param _dims
     */
    public void addResolutionLevel(int discardLayer, int width, int height, double scaleX, double scaleY) {
        resolutions[discardLayer] = new ResolutionLevel(discardLayer, width, height, scaleX, scaleY);
    }

    public ResolutionLevel getResolutionLevel(int index) {
        return resolutions[index];
    }

    public ResolutionLevel getPreviousResolutionLevel(int w, int h) {
        int idx = 0;
        for (int i = 0; i < resolutions.length; i++) {
            idx = i;
            if (resolutions[i].width <= w && resolutions[i].height <= h)
                break;
        }
        return resolutions[idx];
    }

    public ResolutionLevel getNextResolutionLevel(int w, int h) {
        for (int i = 1; i < resolutions.length; ++i) {
            if (resolutions[i].width < w || resolutions[i].height < h)
                return resolutions[i - 1];
        }
        return resolutions[resolutions.length - 1];
    }

    public int getMaxResolutionLevels() {
        return resolutions.length - 1;
    }

    /**
     * A simple class describing a Resolution level for a given image. Note
     * though this class is public it's constructor is private. The
     * ResolutionSet object can be considered to be the ResolutionLevel
     * 'factory'. The ResolutionLevel object is also immutable.
     *
     * @author caplins
     *
     */
    public static class ResolutionLevel {

        public final int discardLayers;

        public final float scaleLevel;

        public final int width;
        public final int height;

        public final double scaleX;
        public final double scaleY;

        /** Private constructor. */
        private ResolutionLevel(int _discardLayers, int _width, int _height, double _scaleX, double _scaleY) {
            discardLayers = _discardLayers;
            scaleLevel = 1f / (1 << discardLayers);
            width = _width;
            height = _height;
            scaleX = _scaleX;
            scaleY = _scaleY;
        }

        /**
         * The equals method. Since these objects are immutable, can only be
         * instantiated in this class, and are not cloneable you can almost
         * certain do reference comparisons using the ==, but since I am
         * paranoid I overridden the equals method.
         */
        @Override
        public boolean equals(Object o) {
            if (o instanceof ResolutionLevel) {
                ResolutionLevel r = (ResolutionLevel) o;
                return discardLayers == r.discardLayers && width == r.width && height == r.height && scaleX == r.scaleX && scaleY == r.scaleY;
            }
            return false;
        }

        /**
         * Clone is NOT supported.
         *
         * @throws CloneNotSupportedException
         */
        @Override
        public Object clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException();
        }

        @Override
        public String toString() {
            return "[[Discard=" + discardLayers + "][ScaleLevel=" + scaleLevel + "][Scale=" + scaleX + "," + scaleY + "][ZoomDims=" + width + "," + height + "]]";
        }

    }

}
