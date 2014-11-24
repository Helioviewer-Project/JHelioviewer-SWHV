package org.helioviewer.gl3d.scenegraph;

import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRay;

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

    public boolean intersects(GL3DRay ray) {
        GL3DVec3d e1, e2; // edge 1 and 2
        GL3DVec3d AO, K, Q;

        // find vectors for two edges sharing the triangle vertex A
        e1 = GL3DVec3d.subtract(this.b, this.a);
        e2 = GL3DVec3d.subtract(this.c, this.a);

        // begin calculating determinant - also used to calculate U parameter
        K = GL3DVec3d.cross(ray.getDirectionOS(), e2);

        // if determinant is near zero, ray lies in plane of triangle
        final double det = e1.dot(K);

        double inv_det, t, u, v;

        if (det < EPSILON && det > -EPSILON)
            return false;

        inv_det = 1.0f / det;

        // calculate distance from A to ray origin
        AO = GL3DVec3d.subtract(ray.getOriginOS(), a);

        // Calculate barycentric coordinates: u>0 && v>0 && u+v<=1
        u = AO.dot(K) * inv_det;
        if (u < 0.0f || u > 1.0)
            return false;

        // prepare to test v parameter
        Q = GL3DVec3d.cross(AO, e1);

        // calculate v parameter and test bounds
        v = Q.dot(ray.getDirectionOS()) * inv_det;
        if (v < 0.0f || u + v > 1.0f)
            return false;

        // calculate t, ray intersects triangle
        t = e2.dot(Q) * inv_det;

        // if intersection is closer replace ray intersection parameters
        if (t > ray.getLength() || t < 0.0f)
            return false;

        ray.setLength(t);

        return true;
    }

    public boolean intersects(GL3DRay ray, GL3DMat4d extraRot) {
        GL3DVec3d e1, e2; // edge 1 and 2
        GL3DVec3d AO, K, Q;

        // find vectors for two edges sharing the triangle vertex A
        e1 = GL3DVec3d.subtract(extraRot.multiply(this.b), extraRot.multiply(this.a));
        e2 = GL3DVec3d.subtract(extraRot.multiply(this.c), extraRot.multiply(this.a));

        // begin calculating determinant - also used to calculate U parameter
        K = GL3DVec3d.cross(ray.getDirectionOS(), e2);

        // if determinant is near zero, ray lies in plane of triangle
        final double det = e1.dot(K);

        double inv_det, t, u, v;

        if (det < EPSILON && det > -EPSILON)
            return false;

        inv_det = 1.0f / det;

        // calculate distance from A to ray origin
        AO = GL3DVec3d.subtract(ray.getOriginOS(), extraRot.multiply(this.a));

        // Calculate barycentric coordinates: u>0 && v>0 && u+v<=1
        u = AO.dot(K) * inv_det;
        if (u < 0.0f || u > 1.0)
            return false;

        // prepare to test v parameter
        Q = GL3DVec3d.cross(AO, e1);

        // calculate v parameter and test bounds
        v = Q.dot(ray.getDirectionOS()) * inv_det;
        if (v < 0.0f || u + v > 1.0f)
            return false;

        // calculate t, ray intersects triangle
        t = e2.dot(Q) * inv_det;

        // if intersection is closer replace ray intersection parameters
        if (t > ray.getLength() || t < 0.0f)
            return false;

        ray.setLength(t);

        return true;
    }

    public boolean intersectsPlanar(GL3DRay ray) {
        GL3DVec3d e1, e2; // edge 1 and 2
        GL3DVec3d AO, K, Q;

        // find vectors for two edges sharing the triangle vertex A
        e1 = GL3DVec3d.subtract(this.b, this.a);
        e2 = GL3DVec3d.subtract(this.c, this.a);

        // begin calculating determinant - also used to calculate U parameter
        K = GL3DVec3d.cross(ray.getDirectionOS(), e2);

        // if determinant is near zero, ray lies in plane of triangle
        final double det = e1.dot(K);

        double inv_det, t, u, v;

        inv_det = 1.0f / det;

        // calculate distance from A to ray origin
        AO = GL3DVec3d.subtract(ray.getOriginOS(), a);

        // prepare to test v parameter
        Q = GL3DVec3d.cross(AO, e1);

        // calculate t, ray intersects triangle
        t = e2.dot(Q) * inv_det;

        ray.setLength(t);

        return true;
    }
}
