package org.helioviewer.jhv.view.j2k;

import java.util.concurrent.atomic.AtomicBoolean;

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

    public record Level(int level, int width, int height, double factorX, double factorY, J2KParams.SubImage subImage) {
        Level(int _level, int _width, int _height, double _factorX, double _factorY) {
            this(_level, _width, _height, _factorX, _factorY, new J2KParams.SubImage(0, 0, _width, _height, _width, _height));
        }
    }

}
