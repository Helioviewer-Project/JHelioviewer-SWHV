package org.helioviewer.gl3d.scenegraph;

import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.gl3d.scenegraph.math.GL3DMat3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRay;
import org.helioviewer.gl3d.wcs.CoordinateSystem;

/**
 * A {@link GL3DShape} is a {@link GL3DNode} that does have a position and a
 * bounding box within the scene graph. In practice, almost every
 * {@link GL3DNode} is also a {@link GL3DShape}.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public abstract class GL3DShape extends GL3DNode {
    // Model Matrix
    protected GL3DMat4d m;

    // World Matrix
    protected GL3DMat4d wm;
    protected GL3DMat4d wmI;
    protected GL3DMat3d wmN;

    protected GL3DAABBox aabb;

    // The coordinate system in which this Shape is defined in
    protected CoordinateSystem coordinateSystem;

    public GL3DShape(String name) {
        this(name, null);
    }

    public GL3DShape(String name, CoordinateSystem coordinateSystem) {
        super(name);
        this.coordinateSystem = coordinateSystem;

        this.m = GL3DMat4d.identity();
        this.wm = GL3DMat4d.identity();
        this.aabb = new GL3DAABBox();
    }

    @Override
    public void init(GL3DState state) {
        state.pushMV();
        this.wm = state.multiplyMV(this.m);
        state.buildInverseAndNormalMatrix();
        this.wmI = new GL3DMat4d(state.getMVInverse());
        this.wmN = new GL3DMat3d(state.normalMatrix);

        this.shapeInit(state);
        this.buildAABB();
        this.isInitialised = true;
        state.popMV();
    }

    @Override
    public void update(GL3DState state) {
        if (!this.isInitialised) {
            this.init(state);
        }
        if (this.hasChanged()) {
            state.pushMV();
            // this.updateMatrix(state);
            this.wm = state.multiplyMV(this.m);
            state.buildInverseAndNormalMatrix();
            this.wmI = new GL3DMat4d(state.getMVInverse());
            this.wmN = new GL3DMat3d(state.normalMatrix);
            this.shapeUpdate(state);
            this.setUnchanged();
            //this.buildAABB();
            state.popMV();
        }
    }

    public void updateMatrix(GL3DState state) {

    }

    @Override
    public void draw(GL3DState state) {
        if (!isDrawBitOn(Bit.Hidden)) {
            // Log.debug("GL3DShape: Drawing '"+getName()+"'");
            state.pushMV();
            state.multiplyMV(this.m);
            state.buildInverseAndNormalMatrix();
            // this.wmI = new GL3DMat4d(state.getMVInverse());
            // this.wmN = new GL3DMat3d(state.normalMatrix);
            this.shapeDraw(state);

            if (isDrawBitOn(Bit.BoundingBox)) {
                if (GL3DGroup.class.isAssignableFrom(this.getClass())) {
                    // Is it the root?
                    if (this.parent == null) {
                        state.gl.glLineWidth(4.0f);
                        this.aabb.drawOS(state, new GL3DVec4d(0, 1, 0, 1));
                        state.gl.glLineWidth(1.0f);
                    } else {
                        this.aabb.drawOS(state, new GL3DVec4d(0, 1, 1, 1));
                    }
                } else {
                    this.aabb.drawOS(state, new GL3DVec4d(1, 0, 0, 1));
                }
            } else if (isDrawBitOn(Bit.Selected)) {
                state.gl.glLineWidth(2.0f);
                this.aabb.drawOS(state, new GL3DVec4d(0, 0.0, 1, 1));
                state.gl.glLineWidth(1.0f);
            }

            state.popMV();
        }
    }

    @Override
    public boolean hit(GL3DRay ray) {
        // if its hidden, it can't be hit
        if (isDrawBitOn(Bit.Hidden)) {
            return false;
        }

        // First check if bounding Box is hit
        if (!this.aabb.isHitInWS(ray)) {
            return false;
        }
        // Log.debug("GL3DShape.hit: AABB is Hit!");

        // Transform ray to object space for non-groups
        if (!getClass().isAssignableFrom(GL3DGroup.class)) {
            ray.setOriginOS(this.wmI.multiply(ray.getOrigin()));
            ray.setDirOS(this.wmI.mat3().multiply(ray.getDirection()));
        }
        return this.shapeHit(ray);
    }

    @Override
    public void delete(GL3DState state) {
        if (parent != null && (parent instanceof GL3DGroup)) {
            parent.removeNode(this);
        }
        parent = null;
        shapeDelete(state);
    }

    public abstract void shapeDelete(GL3DState state);

    public abstract boolean shapeHit(GL3DRay ray);

    public abstract void shapeInit(GL3DState state);

    public abstract void shapeDraw(GL3DState state);

    public abstract void shapeUpdate(GL3DState state);

    public GL3DMat4d modelView() {
        return this.m;
    }

    public GL3DAABBox getAABBox() {
        return this.aabb;
    }

}
