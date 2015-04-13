package org.helioviewer.gl3d.camera;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.math.GL3DMat4d;
import org.helioviewer.gl3d.math.GL3DVec3d;
import org.helioviewer.gl3d.math.GL3DVec4d;
import org.helioviewer.gl3d.scenegraph.GL3DState;

public class RayTracer {
    public enum HITPOINT {
        SPHERICAL, PLANAR;
    }

    private final GL3DCamera camera;
    private final Sphere sphere;
    private final Plane plane;

    public RayTracer(GL3DCamera camera) {
        this.camera = camera;
        sphere = new Sphere(new GL3DVec3d(0, 0, 0), Constants.SunRadius);
        plane = new Plane(new GL3DVec3d(0, 0, 1), 0);
    }

    public Ray cast(double x, double y) {
        GL3DState state = GL3DState.get();
        GL3DVec4d centeredViewportCoordinates1 = new GL3DVec4d(2. * (x / state.getViewportWidth() - 0.5), -2. * (y / state.getViewportHeight() - 0.5), 1., 1.);
        GL3DVec4d centeredViewportCoordinates2 = new GL3DVec4d(2. * (x / state.getViewportWidth() - 0.5), -2. * (y / state.getViewportHeight() - 0.5), -1., 1.);

        GL3DMat4d roti = camera.getCameraTransformation().inverse();
        GL3DMat4d vpmi = camera.orthoMatrixInverse;
        GL3DVec4d up2 = roti.multiply(vpmi.multiply(centeredViewportCoordinates1));
        GL3DVec4d up1 = roti.multiply(vpmi.multiply(centeredViewportCoordinates2));
        GL3DVec4d linevec = GL3DVec4d.subtract(up1, up2);

        GL3DVec3d direction = new GL3DVec3d(linevec.x, linevec.y, linevec.z);
        direction.normalize();
        direction.negate();
        GL3DVec3d origin = new GL3DVec3d(up1.x, up1.y, up1.z);
        Ray ray = new Ray(origin, direction);

        return intersect(ray);
    }

    private Ray intersect(Ray ray) {
        double tSphere = sphere.intersect(ray);
        if (tSphere > 0) {
            ray.hitpointType = HITPOINT.SPHERICAL;
            ray.t = tSphere;
        } else {
            ray.hitpointType = HITPOINT.PLANAR;
            double tPlane = plane.intersect(ray);
            ray.t = tPlane;
        }
        return ray;
    }

    public class Ray {
        public GL3DVec3d origin;
        public GL3DVec3d direction;
        public double t = -1;
        public HITPOINT hitpointType;

        public Ray(GL3DVec3d origin, GL3DVec3d direction) {
            this.origin = origin;
            this.direction = direction;
        }

        public GL3DVec3d getHitpoint() {
            return GL3DVec3d.add(this.origin, GL3DVec3d.multiply(this.direction, this.t));
        }
    }

    private class Sphere {
        public GL3DVec3d center;
        public double radius;

        public Sphere(GL3DVec3d center, double radius) {
            this.center = center;
            this.radius = radius;
        }

        public double intersect(Ray ray) {
            double t = -1;
            GL3DVec3d L = GL3DVec3d.subtract(this.center, ray.origin);
            double tca = GL3DVec3d.dot(L, ray.direction);
            if (tca < 0) {
                return t;
            }
            double dsq = GL3DVec3d.dot(L, L) - tca * tca;
            double diff = this.radius * this.radius - dsq;
            if (diff < 0) {
                return t;
            }
            t = (tca - Math.sqrt(diff));
            return t;
        }
    }

    private class Plane {
        public GL3DVec3d normal;
        public double distance;

        public Plane(GL3DVec3d normal, double distance) {
            this.normal = normal;
            this.distance = distance;
        }

        public double intersect(Ray ray) {
            GL3DVec3d altnormal = camera.getLocalRotation().toMatrix().multiply(this.normal);
            return -(this.distance + ray.origin.dot(altnormal)) / ray.direction.dot(altnormal);
        }
    }

}
