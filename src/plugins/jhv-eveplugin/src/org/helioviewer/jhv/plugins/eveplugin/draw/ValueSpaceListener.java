package org.helioviewer.jhv.plugins.eveplugin.draw;

import org.helioviewer.jhv.base.Range;

public interface ValueSpaceListener {

    public abstract void valueSpaceChanged(Range availableRange, Range selectedRange);

}
