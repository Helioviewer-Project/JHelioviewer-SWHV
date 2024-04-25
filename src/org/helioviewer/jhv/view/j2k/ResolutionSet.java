package org.helioviewer.jhv.view.j2k;

import java.util.concurrent.atomic.AtomicBoolean;

import org.helioviewer.jhv.view.j2k.image.SubImage;

// A class describing the available resolution levels for a given image
public class ResolutionSet {

    // The indices represent the number of discardLayers
    private final Level[] resolutions;
    private final AtomicBoolean[] complete;
    private final int numLevels;
    final int numComps;

    ResolutionSet(int _numLevels, int _numComps) {
        numLevels = _numLevels;
        numComps = _numComps;
        resolutions = new Level[numLevels];

        complete = new AtomicBoolean[numLevels];
        for (int i = 0; i < numLevels; i++)
            complete[i] = new AtomicBoolean();
    }

    void setComplete(int level) {
        for (int i = level; i < numLevels; i++)
            complete[i].set(true);
    }

    AtomicBoolean getComplete(int level) {
        return complete[Math.min(level, numLevels - 1)];
    }

    void addLevel(int discardLayer, int width, int height, double scaleX, double scaleY) {
        resolutions[discardLayer] = new Level(discardLayer, width, height, scaleX, scaleY);
    }

    Level getLevel(int idx) {
        return resolutions[idx];
    }

    Level getPreviousLevel(int w, int h) {
        int idx = 0;
        for (int i = 0; i < numLevels; i++) {
            idx = i;
            if (resolutions[i].width <= w && resolutions[i].height <= h)
                break;
        }
        return resolutions[idx];
    }

    Level getNextLevel(int w, int h) {
        for (int i = 1; i < numLevels; ++i) {
            if (resolutions[i].width < w || resolutions[i].height < h)
                return resolutions[i - 1];
        }
        return resolutions[numLevels - 1];
    }

    public static class Level {

        final int level;

        public final int width;
        public final int height;
        final SubImage subImage;

        final double factorX;
        final double factorY;

        Level(int _level, int _width, int _height, double _factorX, double _factorY) {
            level = _level;
            width = _width;
            height = _height;
            subImage = new SubImage(0, 0, width, height, width, height);

            factorX = _factorX;
            factorY = _factorY;
        }

        @Override
        public String toString() {
            return "[Discard=" + level + " ScaleFactor=" + factorX + ',' + factorY + " ZoomDims=" + width + ',' + height + ']';
        }

    }

}
