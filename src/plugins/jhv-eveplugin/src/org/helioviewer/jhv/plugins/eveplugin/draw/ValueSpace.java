package org.helioviewer.jhv.plugins.eveplugin.draw;

import org.helioviewer.jhv.base.Range;

public interface ValueSpace {

    public abstract Range getScaledSelectedRange();

    public abstract Range getScaledAvailableRange();

    public abstract void setScaledSelectedRange(Range newScaledSelectedRange);

    public abstract void resetScaledSelectedRange();

}
