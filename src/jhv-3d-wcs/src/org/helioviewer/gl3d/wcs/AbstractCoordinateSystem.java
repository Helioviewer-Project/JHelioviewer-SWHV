package org.helioviewer.gl3d.wcs;

import java.util.ArrayList;
import java.util.List;

/**
 * Basic implementation of a {@link CoordinateSystem}.
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 * 
 */
public abstract class AbstractCoordinateSystem implements CoordinateSystem {

    private List<CoordinateSystemChangeListener> listeners;

    public AbstractCoordinateSystem() {
        this.listeners = new ArrayList<CoordinateSystemChangeListener>();
    }

    public CoordinateVector createCoordinateVector(double... value) {
        if (value.length != getDimensions()) {
            throw new IllegalArgumentException("Need " + getDimensions() + " values to create a Vector in the CoordinateSystem " + this);
        }
        return new CoordinateVector(this, value);
    }

    public String toString() {
        return this.getClass().getSimpleName();
    }

    public CoordinateConversion getConversion(CoordinateSystem coordinateSystem) {
        if (coordinateSystem.getClass().equals(this.getClass())) {
            return new IdentityMatrixConversion(this, coordinateSystem);
        }
        throw new IllegalArgumentException("No Conversion available from " + this + " to " + coordinateSystem);
    }

    public void addListener(CoordinateSystemChangeListener listener) {
        this.listeners.add(listener);
    }

    public void fireCoordinateSystemChanged() {
        for (CoordinateSystemChangeListener listener : this.listeners) {
            listener.coordinateSystemChanged(this);
        }
    }
}
