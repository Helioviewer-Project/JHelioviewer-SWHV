package org.helioviewer.jhv.view.j2k;

// A class describing the available resolution levels for a given image
public class ResolutionSet {

    // The indices represent the number of discardLayers
    private final Level[] resolutions;
    // Native completion snapshot, published by J2KSource.update() to EDT and decode workers.
    private volatile int completeFrom;
    final int numComps;

    ResolutionSet(Level[] _resolutions, int _numComps) {
        resolutions = _resolutions;
        numComps = _numComps;

        completeFrom = resolutions.length;
    }

    void setCompleteLevels(int count) {
        completeFrom = resolutions.length - count;
    }

    boolean getComplete(int level) {
        return Math.min(level, resolutions.length - 1) >= completeFrom;
    }

    // The coarsest level is the first to be complete.
    boolean isDisplayable() {
        return completeFrom < resolutions.length;
    }

    // The finest complete level at this one or coarser; a displayable frame has one.
    Level getCompleteLevel(int level) {
        return getLevel(Math.max(level, completeFrom));
    }

    Level getLevel(int idx) {
        return resolutions[Math.min(idx, resolutions.length - 1)];
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
        for (int i = 1; i < resolutions.length; ++i) {
            if (resolutions[i].width < w || resolutions[i].height < h)
                return resolutions[i - 1];
        }
        return resolutions[resolutions.length - 1];
    }

    public record Level(int level, int width, int height) {}

}
