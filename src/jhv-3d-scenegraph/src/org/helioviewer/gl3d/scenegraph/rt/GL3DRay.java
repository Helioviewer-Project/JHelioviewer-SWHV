package org.helioviewer.gl3d.scenegraph.rt;

import javax.media.opengl.GL;

import org.helioviewer.gl3d.scenegraph.GL3DShape;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;

/**
 * A {@link GL3DRay} is used for detecting hit points within the scene graph and
 * stores required attributes for a fast ray casting. {@link GL3DRay}s should be
 * created by the {@link GL3DRayTracer}.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DRay {
    private GL3DVec3d origin;
    private GL3DVec3d direction;
    private GL3DVec3d originOS;
    private GL3DVec3d directionOS;
    private double length;

    private GL3DShape originShape;

    private GL3DVec3d hitPoint;
    private GL3DVec3d hitPointOS;
    private GL3DVec3d hitNormal;

    private GL3DVec3d invDirection;
    private GL3DVec3d invDirectionOS;
    private int[] sign = new int[3];
    private int[] signOS = new int[3];
    private double tmin;
    private double tmax;

    public boolean isOutside;

    public boolean isOnSun = false;

    public static GL3DRay createPrimaryRay(GL3DVec3d origin, GL3DVec3d dir) {
        GL3DRay ray = new GL3DRay();
        ray.origin = origin;
        dir.normalize();
        ray.setDir(dir);
        ray.length = Double.MAX_VALUE;
        ray.isOutside = true;

        // ray.fromVStoWS(state);

        // Log.debug("GL3DRay.createPrimaryRay: Origin: "+ray.origin+" Direction: "+ray.direction);
        return ray;
    }

    private GL3DRay() {
    }

    public void setDirOS(GL3DVec3d dir) {
        this.directionOS = dir;
        invDirectionOS = new GL3DVec3d(dir.x == 0 ? 0 : 1 / dir.x, dir.y == 0 ? 0 : 1 / dir.y, dir.z == 0 ? 0 : 1 / dir.z);
        signOS[0] = invDirectionOS.x < 0 ? 1 : 0;
        signOS[1] = invDirectionOS.y < 0 ? 1 : 0;
        signOS[2] = invDirectionOS.z < 0 ? 1 : 0;
    }

    public void setDir(GL3DVec3d dir) {
        this.direction = dir;
        invDirection = new GL3DVec3d(dir.x == 0 ? 0 : 1 / dir.x, dir.y == 0 ? 0 : 1 / dir.y, dir.z == 0 ? 0 : 1 / dir.z);
        invDirection = new GL3DVec3d(1 / dir.x, 1 / dir.y, 1 / dir.z);
        sign[0] = invDirection.x < 0 ? 1 : 0;
        sign[1] = invDirection.y < 0 ? 1 : 0;
        sign[2] = invDirection.z < 0 ? 1 : 0;
    }

    /*
     * public void fromVStoWS(GL3DState state) { //
     * Log.debug("GL3DRay: VS: Origin: " + this.origin + " Direction: " +
     * this.direction+" InvDir: "+this.invDirection); this.origin =
     * (state.getMVInverse().multiply(this.origin));
     * setDir(state.getMVInverse().mat3().multiply(this.direction)); //
     * Log.debug("GL3DRay: WS: Origin: " + this.origin + " Direction: " +
     * this.direction+" InvDir: "+this.invDirection); }
     */

    public void draw(GL3DState state) {
        GL gl = state.gl;

        this.length = this.length < 1000 ? this.length : 100;

        gl.glDisable(GL.GL_LIGHTING);
        gl.glDisable(GL.GL_TEXTURE_2D);

        state.pushMV();
        // state.loadIdentity();
        gl.glBegin(GL.GL_LINES);

        gl.glColor3d(1, 1, 0);
        gl.glVertex3d(origin.x, origin.y, origin.z);

        gl.glColor3d(1, 0, 0);
        gl.glVertex3d(origin.x + direction.x * length, origin.y + direction.y * length, origin.z + direction.z * length);

        gl.glEnd();
        state.popMV();

        // Log.debug("GL3DRay: DRAW IN WS: Origin: "+this.origin+" Destination: "+GL3DVec3d.add(this.origin,
        // this.direction.copy().multiply(length)));
        gl.glEnable(GL.GL_LIGHTING);
        gl.glEnable(GL.GL_TEXTURE_2D);
    }

    public GL3DVec3d getOrigin() {
        return origin;
    }

    public GL3DVec3d getDirection() {
        return direction;
    }

    public GL3DVec3d getOriginOS() {
        return originOS;
    }

    public GL3DVec3d getDirectionOS() {
        return directionOS;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public GL3DShape getOriginShape() {
        return originShape;
    }

    public void setOriginShape(GL3DShape originShape) {
        this.originShape = originShape;
    }

    public GL3DVec3d getHitPoint() {
        return hitPoint;
    }

    public void setHitPoint(GL3DVec3d hitPoint) {
        this.hitPoint = hitPoint;
    }

    public GL3DVec3d getHitNormal() {
        return hitNormal;
    }

    public void setHitNormal(GL3DVec3d hitNormal) {
        this.hitNormal = hitNormal;
    }

    public GL3DVec3d getInvDirection() {
        return invDirection;
    }

    public GL3DVec3d getInvDirectionOS() {
        return invDirectionOS;
    }

    public int[] getSign() {
        return sign;
    }

    public int[] getSignOS() {
        return signOS;
    }

    public double getTmin() {
        return tmin;
    }

    public void setTmin(double tmin) {
        this.tmin = tmin;
    }

    public double getTmax() {
        return tmax;
    }

    public void setTmax(double tmax) {
        this.tmax = tmax;
    }

    public boolean isOutside() {
        return isOutside;
    }

    public void setOutside(boolean isOutside) {
        this.isOutside = isOutside;
    }

    public void setOriginOS(GL3DVec3d originOS) {
        this.originOS = originOS;
    }

    public void setDirectionOS(GL3DVec3d directionOS) {
        this.directionOS = directionOS;
    }

    public GL3DVec3d getHitPointOS() {
        return hitPointOS;
    }

    public void setHitPointOS(GL3DVec3d hitPointOS) {
        this.hitPointOS = hitPointOS;
    }
}
