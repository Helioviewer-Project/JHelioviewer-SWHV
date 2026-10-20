package org.helioviewer.jhv.wcs;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;

public final class ObserverGeometry {

    public static Vec3 helioprojectiveToWorld(Position viewpoint, double longitude, double latitude) {
        return observerRayToWorld(viewpoint, helioprojectiveRay(longitude, latitude));
    }

    // Directed observer-space ray; its length does not affect the intersection.
    public static Vec3 observerRayToWorld(Position viewpoint, Vec3 ray) {
        double observerDistance = viewpoint.distance;
        double a = Vec3.dot(ray, ray);
        double b = observerDistance * ray.z;
        // Same stable intersection as imageGeometry.frag, evaluated in double precision.
        double impactX = observerDistance * ray.x;
        double impactY = observerDistance * ray.y;
        double discriminant = a - (impactX * impactX + impactY * impactY);
        if (discriminant < 0)
            return null;

        double root = Math.sqrt(discriminant);
        double signedRoot = -b - root > 0 ? -root : root;
        double t = (-b + signedRoot) / a;
        if (t <= 0)
            return null;

        // Expand Z to avoid cancelling the observer distance against t * ray.z.
        Vec3 view = new Vec3(t * ray.x, t * ray.y, (impactX * ray.x + impactY * ray.y + signedRoot * ray.z) / a);
        return viewpoint.toQuat().rotateInverseVector(view);
    }

    @Nullable
    public static Vec2 helioprojectiveToHpcPlane(double observerDistance, double longitude, double latitude) {
        Vec3 ray = helioprojectiveRay(longitude, latitude);
        if (ray.z >= 0)
            return null;
        return new Vec2(-observerDistance * ray.x / ray.z, -observerDistance * ray.y / ray.z);
    }

    private static Vec3 helioprojectiveRay(double longitude, double latitude) {
        double cosLon = Math.cos(longitude);
        double cosLat = Math.cos(latitude);
        return new Vec3(Math.sin(longitude) * cosLat, Math.sin(latitude), -cosLon * cosLat);
    }

    private ObserverGeometry() {}
}
