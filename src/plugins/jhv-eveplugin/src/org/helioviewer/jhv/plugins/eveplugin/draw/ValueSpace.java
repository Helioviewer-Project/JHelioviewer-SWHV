package org.helioviewer.jhv.plugins.eveplugin.draw;

import org.helioviewer.jhv.base.Range;

public interface ValueSpace {

    public abstract void resetScaledSelectedRange();

    public abstract Range getSelectedRange();

    public abstract Range getAvailableRange();

    public abstract void setSelectedRange(Range range);

    public abstract double scale(double min);

    public abstract double invScale(double endValue);

}
