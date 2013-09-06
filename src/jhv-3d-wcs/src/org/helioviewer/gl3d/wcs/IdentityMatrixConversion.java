package org.helioviewer.gl3d.wcs;

import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;

public class IdentityMatrixConversion implements MatrixCoordinateConversion {
    private static GL3DMat4d identity = GL3DMat4d.identity();

    private CoordinateSystem source;
    private CoordinateSystem target;

    public IdentityMatrixConversion(CoordinateSystem source, CoordinateSystem target) {
        this.source = source;
        this.target = target;
    }

    public CoordinateSystem getSourceCoordinateSystem() {
        return this.source;
    }

    public CoordinateSystem getTargetCoordinateSystem() {
        return this.target;
    }

    public CoordinateVector convert(CoordinateVector vector) {
        return vector;
    }

    public GL3DMat4d getConversionMatrix() {
        return identity;
    }
}
