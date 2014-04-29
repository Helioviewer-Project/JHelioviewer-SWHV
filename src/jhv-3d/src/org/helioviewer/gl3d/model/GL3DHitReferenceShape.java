package org.helioviewer.gl3d.model;

import java.util.List;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.model.image.GL3DImageMesh;
import org.helioviewer.gl3d.scenegraph.GL3DAABBox;
import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.gl3d.scenegraph.GL3DMesh;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec2d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRay;

/**
 * The {@link GL3DHitReferenceShape} unifies all possible Image Layers (
 * {@link GL3DImageMesh} nodes in the Scene Graph) in a single mesh node. This
 * node offers a mathematically simpler representation for faster hit point
 * detection when used for determining the region of interest on the image
 * meshes.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DHitReferenceShape extends GL3DMesh {
    private static final double extremeValue = Constants.SunMeanDistanceToEarth * 10;

    private boolean allowBacksideHits;
    private double angle = 0.0;
    private GL3DMat4d phiRotation;

    public GL3DHitReferenceShape() {
        this(false);
    }

    public GL3DHitReferenceShape(boolean allowBacksideHits) {
        super("Hit Reference Shape");
        this.allowBacksideHits = allowBacksideHits;
    }

    public GL3DHitReferenceShape(boolean allowBacksideHits, double angle) {
        super("Hit Reference Shape");
        this.allowBacksideHits = allowBacksideHits;
        this.angle = angle;
    }

    public void shapeDraw(GL3DState state) {
        return;
    }

    public GL3DMeshPrimitive createMesh(GL3DState state, List<GL3DVec3d> positions, List<GL3DVec3d> normals, List<GL3DVec2d> textCoords, List<Integer> indices, List<GL3DVec4d> colors) {
        this.phiRotation = GL3DMat4d.rotation(angle, new GL3DVec3d(0, 1, 0));
        GL3DVec3d ll = createVertex(-extremeValue, -extremeValue, 0);
        GL3DVec3d lr = createVertex(extremeValue, -extremeValue, 0);
        GL3DVec3d tr = createVertex(extremeValue, extremeValue, 0);
        GL3DVec3d tl = createVertex(-extremeValue, extremeValue, 0);

        positions.add(ll);// normals.add(new GL3DVec3d(0,0,1));//colors.add(new
                          // GL3DVec4d(0, 0, 1, 0.0));
        positions.add(lr);// normals.add(new GL3DVec3d(0,0,1));//colors.add(new
                          // GL3DVec4d(0, 0, 1, 0.0));
        positions.add(tr);// normals.add(new GL3DVec3d(0,0,1));//colors.add(new
                          // GL3DVec4d(0, 0, 1, 0.0));
        positions.add(tl);// normals.add(new GL3DVec3d(0,0,1));//colors.add(new
                          // GL3DVec4d(0, 0, 1, 0.0));

        indices.add(0);
        indices.add(1);
        indices.add(2);

        indices.add(0);
        indices.add(2);
        indices.add(3);

        return GL3DMeshPrimitive.TRIANGLES;
    }

    private GL3DVec3d createVertex(double x, double y, double z) {
        double cx = x * phiRotation.m[0] + y * phiRotation.m[4] + z * phiRotation.m[8] + phiRotation.m[12];
        double cy = x * phiRotation.m[1] + y * phiRotation.m[5] + z * phiRotation.m[9] + phiRotation.m[13];
        double cz = x * phiRotation.m[2] + y * phiRotation.m[6] + z * phiRotation.m[10] + phiRotation.m[14];
        return new GL3DVec3d(cx, cy, cz);
    }

    public boolean hit(GL3DRay ray) {
        // if its hidden, it can't be hit
        if (isDrawBitOn(Bit.Hidden) || this.wmI == null) {
            return false;

        }

        // Transform ray to object space for non-groups
        ray.setOriginOS(this.wmI.multiply(ray.getOrigin()));
        GL3DVec3d helpingDir = this.wmI.multiply(ray.getDirection());
        helpingDir.normalize();
        ray.setDirOS(helpingDir);
        return this.shapeHit(ray);
    }

    public boolean shapeHit(GL3DRay ray) {
        // Hit detection happens in Object-Space
        boolean isSphereHit = isSphereHit(ray);
        // boolean isSphereHit = isSphereHitInOS(ray);
        if (isSphereHit) {
            // GL3DVec3d hitPoint = this.wmI.multiply(ray.getHitPoint()).;
            GL3DVec3d hitPoint = ray.getHitPoint();
            GL3DVec3d projectionPlaneNormal = new GL3DVec3d(0, 0, 1);
            GL3DVec3d pointOnSphere = this.wmI.multiply(ray.getHitPoint());
            ray.setHitPointOS(pointOnSphere);
            pointOnSphere.normalize();
            double cos = pointOnSphere.dot(projectionPlaneNormal);

            boolean pointOnSphereBackside = cos < 0;
            // boolean pointOnSphereBackside = pointOnSphere.z<0;

            if (pointOnSphereBackside) {
                // Hit the backside of the sphere, ray must have hit the plane
                // first
                isSphereHit = this.allowBacksideHits;
                // Log.debug("GL3DHitReferenceShape: Viewing Plane from Behind! "+pointOnSphere+
                // " Projection Plane: "+ projectionPlaneNormal);
            }
        }

        if (isSphereHit) {
            ray.isOnSun = true;
            // ray.setHitPoint(this.wmI.multiply(ray.getHitPoint()));
            return true;
        } else {
            super.shapeHit(ray);
            if (ray.getHitPoint() != null)
                ray.setHitPointOS(this.wmI.multiply(ray.getHitPoint()));
        }

        return true;
    }

    private boolean isSphereHit(GL3DRay ray) {
        GL3DVec3d l = new GL3DVec3d(0, 0, 0).subtract(ray.getOrigin());
        GL3DVec3d rayDirCopy = ray.getDirection().copy();
        rayDirCopy.normalize();
        double s = l.dot(rayDirCopy);
        double l2 = l.length2();
        double r2 = Constants.SunRadius2;
        if (s < 0 && l2 > r2) {
            return false;
        }

        double s2 = s * s;
        double m2 = l2 - s2;
        if (m2 > r2) {
            return false;
        }

        double q = Math.sqrt(r2 - m2);
        double t;
        if (l2 > r2) {
            t = s - q;
        } else {
            t = s + q;
        }
        ray.setLength(t);
        GL3DVec3d rayCopy2 = ray.getDirection().copy();
        rayCopy2.normalize();
        rayCopy2.multiply(t);

        GL3DVec3d rayCopy = ray.getOrigin().copy();
        rayCopy.add(rayCopy2);
        ray.setHitPoint(rayCopy);
        ray.isOutside = false;
        ray.setOriginShape(this);
        // Log.debug("GL3DShape.shapeHit: Hit at Distance: "+t+" HitPoint: "+ray.getHitPoint());
        return true;
    }

    public GL3DAABBox buildAABB() {
        this.aabb.fromOStoWS(new GL3DVec3d(-extremeValue, -extremeValue, -extremeValue), new GL3DVec3d(extremeValue, extremeValue, extremeValue), this.wm);

        return this.aabb;
    }
}
