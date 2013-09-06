package org.helioviewer.gl3d.wcs;

import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;

public interface MatrixCoordinateConversion extends CoordinateConversion {
    public GL3DMat4d getConversionMatrix();
}
