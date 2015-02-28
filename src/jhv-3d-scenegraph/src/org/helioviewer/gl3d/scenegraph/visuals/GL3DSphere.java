package org.helioviewer.gl3d.scenegraph.visuals;

import java.util.List;

import org.helioviewer.gl3d.scenegraph.GL3DMesh;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec2d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4f;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRay;

public class GL3DSphere extends GL3DMesh {
    private final int resolutionX;
    private final int resolutionY;
    private final double radius;

    private GL3DVec3d center;
    private final GL3DVec3d centerOS = new GL3DVec3d(0, 0, 0);

    public GL3DSphere(double radius, int resolutionX, int resolutionY, GL3DVec4f color) {
        this("Sphere", radius, resolutionX, resolutionY, color);
    }

    public GL3DSphere(String name, double radius, int resolutionX, int resolutionY, GL3DVec4f color) {
        super(name, color);
        this.radius = radius;
        this.resolutionX = resolutionX;
        this.resolutionY = resolutionY;
    }

    @Override
    public GL3DMeshPrimitive createMesh(GL3DState state, List<GL3DVec3d> positions, List<GL3DVec3d> normals, List<GL3DVec2d> textCoords, List<Integer> indices, List<GL3DVec4d> colors) {

        for (int latNumber = 0; latNumber <= this.resolutionX; latNumber++) {
            double theta = latNumber * Math.PI / resolutionX;
            double sinTheta = Math.sin(theta);
            double cosTheta = Math.cos(theta);

            for (int longNumber = 0; longNumber <= resolutionY; longNumber++) {
                double phi = longNumber * 2 * Math.PI / resolutionY;
                double sinPhi = Math.sin(phi);
                double cosPhi = Math.cos(phi);

                double x = cosPhi * sinTheta;
                double y = cosTheta;
                double z = sinPhi * sinTheta;

                positions.add(new GL3DVec3d(radius * x, radius * y, radius * z));
                normals.add(new GL3DVec3d(x, y, z));
            }
        }

        for (int latNumber = 0; latNumber < this.resolutionX; latNumber++) {
            for (int longNumber = 0; longNumber < resolutionY; longNumber++) {
                int first = (latNumber * (resolutionY + 1)) + longNumber;
                int second = first + resolutionY + 1;

                indices.add(first);
                indices.add(first + 1);
                indices.add(second + 1);
                indices.add(second);

            }
        }

        return GL3DMeshPrimitive.QUADS;
    }

    @Override
    public void shapeInit(GL3DState state) {
        super.shapeInit(state);
        this.center = this.wm.multiply(this.centerOS);
    }

    @Override
    public void shapeUpdate(GL3DState state) {
        this.center = this.wm.multiply(this.centerOS);
    }

    @Override
    public boolean shapeHit(GL3DRay ray) {
        // if(super.shapeHit(ray)) {
        // this.lastHitPoint = this.wmI.multiply(ray.getHitPoint());
        // // Log.debug("GL3DShape.shapeHit: LastHitPoint="+this.lastHitPoint);
        // return true;
        // }
        // return false;
        // Log.debug("GL3DSphere.shapeHit: Dir="+ray.getDirection()+" Origin="+ray.getOrigin()+" Center="+this.center);
        GL3DVec3d l = this.center.copy().subtract(ray.getOrigin());
        GL3DVec3d rayDirCopy = ray.getDirection().copy();
        rayDirCopy.normalize();
        double s = l.dot(rayDirCopy);
        double l2 = l.length2();
        double r2 = this.radius * this.radius;
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
        // ray.setHitPoint(this.wmI.multiply(ray.getHitPoint()));
        ray.isOutside = false;
        ray.setOriginShape(this);
        // Log.debug("GL3DShape.shapeHit: Hit at Distance: "+t+" HitPoint: "+ray.getHitPoint());
        return true;
    }

    public GL3DVec3d getCenter() {
        return center;
    }

}
