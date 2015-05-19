package org.helioviewer.jhv.plugins.eveplugin.lines.model;

import org.helioviewer.jhv.plugins.eveplugin.base.Range;

public interface EVEValueRangeModelListener {
    public abstract void availableRangeChanged(Range availableRange);

    public abstract void selectedRangeChanged(Range selectedRange);
}
