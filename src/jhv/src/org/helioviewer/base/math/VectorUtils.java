package org.helioviewer.base.math;

import org.helioviewer.base.JavaCompatibility;

public class VectorUtils {

    /**
     * Helper routine needed for testing if a point is inside a triangle
     * 
     * @param a
     *            - first point
     * @param b
     *            - second point
     * @param c
     *            - third point
     * 
     * @return number representing the turn-direction of the three points
     */
    public static double clockwise(Vector2dDouble a, Vector2dDouble b, Vector2dDouble c) {
        return (c.getX() - a.getX()) * (b.getY() - a.getY()) - (b.getX() - a.getX()) * (c.getY() - a.getY());
    }

    /**
     * Check if a given point lies inside the given triangle
     * 
     * @param trianglePointA
     *            - first triangle point
     * @param trianglePointB
     *            - second triangle point
     * @param trianglePointC
     *            - third triangle point
     * @param toCheck
     *            - point in question
     * @return true if the point is located inside the triangle
     */
    public static boolean pointInTriangle(Vector2dDouble trianglePointA, Vector2dDouble trianglePointB, Vector2dDouble trianglePointC, Vector2dDouble toCheck) {
        double cw0 = clockwise(trianglePointA, trianglePointB, toCheck);
        double cw1 = clockwise(trianglePointB, trianglePointC, toCheck);
        double cw2 = clockwise(trianglePointC, trianglePointA, toCheck);

        cw0 = Math.abs(cw0) < JavaCompatibility.DOUBLE_MIN_NORMAL ? 0 : cw0 < 0 ? -1 : 1;
        cw1 = Math.abs(cw1) < JavaCompatibility.DOUBLE_MIN_NORMAL ? 0 : cw1 < 0 ? -1 : 1;
        cw2 = Math.abs(cw2) < JavaCompatibility.DOUBLE_MIN_NORMAL ? 0 : cw2 < 0 ? -1 : 1;

        if (Math.abs(cw0 + cw1 + cw2) >= 2) {
            return true;
        } else {
            return Math.abs(cw0) + Math.abs(cw1) + Math.abs(cw2) <= 1;
        }

    }

    /**
     * Project the given 2d in-plane coordinates back to the 3d space
     * 
     * @param planeCenter
     *            - define the center of the plane
     * @param planeVectorA
     *            - first in-plane direction vector
     * @param planeVectorB
     *            - second in-plane direction vector
     * @param toProject
     *            - point to be projected into the plane
     * @return
     */
    public static Vector3dDouble projectBack(Vector3dDouble planeCenter, Vector3dDouble planeVectorA, Vector3dDouble planeVectorB, Vector2dDouble toProject) {
        Vector3dDouble inPlane = planeCenter.add(planeVectorA.scale(toProject.getX())).add(planeVectorB.scale(toProject.getY()));
        return inPlane;
    }

    /**
     * Get the (projected) in-plane coordinates of the given point
     * 
     * @param planeCenter
     *            - define the center of the plane
     * @param planeVectorA
     *            - first in-plane direction vector
     * @param planeVectorB
     *            - second in-plane direction vector
     * @param toProject
     *            - point to be projected into the plane
     * @return
     */
    public static Vector2dDouble inPlaneCoord(Vector3dDouble planeCenter, Vector3dDouble planeVectorA, Vector3dDouble planeVectorB, Vector3dDouble toProject) {
        Vector3dDouble inPlane = projectToPlane(planeCenter, toProject);
        double x = Vector3dDouble.dot(planeVectorA, inPlane);
        double y = Vector3dDouble.dot(planeVectorB, inPlane);
        return new Vector2dDouble(x, y);
    }

    /**
     * Calculate the in-plane-vector of the given point to the plane with origin
     * planeCenter and normal norm(planeCenter)
     * 
     * @param planeCenter
     *            - the plane's normal vector
     * @param toProject
     *            - point to project
     * 
     * @return the projection of the targetPoint
     */
    public static Vector3dDouble inPlaneShift(Vector3dDouble planeCenter, Vector3dDouble toProject) {
        Vector3dDouble normal = planeCenter.normalize();
        Vector3dDouble inPlaneShift = toProject.subtract(normal.scale(Vector3dDouble.dot(normal, toProject)));
        return inPlaneShift;
    }

    /**
     * Calculate the projection of the given point to the plane with origin
     * planeCenter and normal norm(planeCenter)
     * 
     * @param planeCenter
     * @param toProject
     * @return
     */
    public static Vector3dDouble projectToPlane(Vector3dDouble planeCenter, Vector3dDouble toProject) {
        Vector3dDouble normal = planeCenter.normalize();
        Vector3dDouble inPlaneShift = toProject.subtract(normal.scale(Vector3dDouble.dot(normal, toProject)));
        Vector3dDouble projection = planeCenter.add(inPlaneShift);
        return projection;
    }

}
