package org.helioviewer.gl3d.plugin.vectors.data.filter;

import org.helioviewer.gl3d.plugin.vectors.data.VectorField;

public abstract class VectorFieldFilter {
    protected VectorFieldFilter next;

    public void filter(VectorField vectorField) {
        this.applyFilter(vectorField);

        if (this.next != null) {
            next.filter(vectorField);
        }
    }

    protected abstract void applyFilter(VectorField vectorField);

    public void setNext(VectorFieldFilter next) {
        this.next = next;
    }
}
