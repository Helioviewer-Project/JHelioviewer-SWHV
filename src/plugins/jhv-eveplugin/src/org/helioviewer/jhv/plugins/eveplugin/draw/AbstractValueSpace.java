package org.helioviewer.jhv.plugins.eveplugin.draw;

import java.util.HashSet;
import java.util.Set;

import org.helioviewer.jhv.base.Range;

public abstract class AbstractValueSpace implements ValueSpace {

    protected final Set<ValueSpaceListener> listeners;

    public AbstractValueSpace() {
        listeners = new HashSet<ValueSpaceListener>();
    }

    @Override
    public void resetScaledSelectedRange() {
        Range availableRange = getScaledAvailableRange();
        setScaledSelectedRange(new Range(availableRange));
    }

    public void addValueSpaceListener(ValueSpaceListener valueSpaceListener) {
        listeners.add(valueSpaceListener);
    }

}
