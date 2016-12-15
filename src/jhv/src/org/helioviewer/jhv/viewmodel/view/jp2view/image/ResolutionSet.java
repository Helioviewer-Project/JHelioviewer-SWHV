package org.helioviewer.jhv.viewmodel.view.jp2view.image;

// A class describing the available resolution levels for a given image
public class ResolutionSet {

    // The indices represent the number of discardLayers
    private final ResolutionLevel[] resolutions;
    private final boolean[] complete;
    private final int numLevels;
    public final int numComps;

    public ResolutionSet(int _numLevels, int _numComps) {
        numLevels = _numLevels;
        numComps = _numComps;
        resolutions = new ResolutionLevel[numLevels];
        complete = new boolean[numLevels];
    }

    public void setComplete(int idx) {
        for (int i = idx; i < numLevels; i++)
            complete[i] = true;
    }

    public boolean getComplete(int idx) {
        return complete[idx];
    }

    public void addResolutionLevel(int discardLayer, int width, int height, double scaleX, double scaleY) {
        resolutions[discardLayer] = new ResolutionLevel(discardLayer, width, height, scaleX, scaleY);
    }

    public ResolutionLevel getResolutionLevel(int idx) {
        return resolutions[idx];
    }

    public ResolutionLevel getPreviousResolutionLevel(int w, int h) {
        int idx = 0;
        for (int i = 0; i < numLevels; i++) {
            idx = i;
            if (resolutions[i].width <= w && resolutions[i].height <= h)
                break;
        }
        return resolutions[idx];
    }

    public ResolutionLevel getNextResolutionLevel(int w, int h) {
        for (int i = 1; i < numLevels; ++i) {
            if (resolutions[i].width < w || resolutions[i].height < h)
                return resolutions[i - 1];
        }
        return resolutions[numLevels - 1];
    }

    public static class ResolutionLevel {

        public final int level;
        public final float scaleLevel;

        public final int width;
        public final int height;

        public final double factorX;
        public final double factorY;

        // Private constructor
        private ResolutionLevel(int _level, int _width, int _height, double _factorX, double _factorY) {
            level = _level;
            scaleLevel = 1f / (1 << level);
            width = _width;
            height = _height;
            factorX = _factorX;
            factorY = _factorY;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ResolutionLevel))
                return false;
            ResolutionLevel r = (ResolutionLevel) o;
            return level == r.level && width == r.width && height == r.height && factorX == r.factorX && factorY == r.factorY;
        }

        @Override
        public int hashCode() {
            assert false : "hashCode not designed";
            return 42;
        }

        @Override
        public String toString() {
            return "[[Discard=" + level + "][ScaleFactor=" + factorX + "," + factorY + "][ZoomDims=" + width + "," + height + "]]";
        }

    }

}
