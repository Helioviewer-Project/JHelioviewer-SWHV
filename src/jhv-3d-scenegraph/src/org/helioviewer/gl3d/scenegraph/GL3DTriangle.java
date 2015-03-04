package org.helioviewer.gl3d.scenegraph;

import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;

/**
 * A representation of a triangle, basically used as the most basic primitive
 * when calculating hit points of a {@link GL3DMesh}. Every Mesh is also stored
 * as a set of {@link GL3DTriangle}s.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DTriangle {
    protected GL3DVec3d a;
    protected GL3DVec3d b;
    protected GL3DVec3d c;

    protected GL3DVec3d center;

    public GL3DTriangle() {

    }

    public GL3DTriangle(GL3DVec3d a, GL3DVec3d b, GL3DVec3d c) {
        this.a = a;
        this.b = b;
        this.c = c;

        this.center = this.a.copy();
        this.center.add(c);
        this.center.add(b);
        this.center.divide(3);
    }

    private final static double EPSILON = 0.0001;

}
