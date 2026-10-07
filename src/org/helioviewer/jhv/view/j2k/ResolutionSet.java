package org.helioviewer.jhv.view.j2k;

import java.util.concurrent.atomic.AtomicBoolean;

// A class describing the available resolution levels for a given image
public class ResolutionSet {

    // The indices represent the number of discardLayers
    private final Level[] resolutions;
    private final AtomicBoolean[] complete;
    private final int numLevels;
    final int numComps;

    ResolutionSet(Level[] _resolutions, int _numComps) {
        resolutions = _resolutions;
        numLevels = resolutions.length;
        numComps = _numComps;

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

    // The coarsest level is the first to be complete.
    boolean isDisplayable() {
        return complete[numLevels - 1].get();
    }

    // The finest complete level at this one or coarser; a displayable frame has one.
    Level getCompleteLevel(int level) {
        int idx = Math.min(level, numLevels - 1);
        while (idx < numLevels - 1 && !complete[idx].get())
            idx++;
        return resolutions[idx];
    }

    Level getLevel(int idx) {
        return resolutions[Math.min(idx, numLevels - 1)];
    }

    Level getClosestLevel(int w, int h) {
        Level closest = resolutions[0];
        for (Level res : resolutions) {
            if (Math.abs(res.width - w) + Math.abs(res.height - h) < Math.abs(closest.width - w) + Math.abs(closest.height - h))
                closest = res;
        }
        return closest;
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
            this(_level, _width, _height, _factorX, _factorY, new J2KParams.SubImage(0, 0, _width, _height));
        }
    }

}
