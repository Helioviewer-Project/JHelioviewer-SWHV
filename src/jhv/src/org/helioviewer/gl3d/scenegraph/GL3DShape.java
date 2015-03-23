package org.helioviewer.gl3d.scenegraph;

import org.helioviewer.gl3d.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;

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

    public GL3DShape(String name) {

        super(name);

        this.m = GL3DMat4d.identity();
        this.wm = GL3DMat4d.identity();
    }

    @Override
    public void init(GL3DState state) {
        state.pushMV();
        this.wm = state.multiplyMV(this.m);

        this.shapeInit(state);
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
            state.pushMV();
            state.multiplyMV(this.m);
            this.shapeDraw(state);
            state.popMV();
        }
    }

    @Override
    public void delete(GL3DState state) {
        shapeDelete(state);
    }

    public abstract void shapeDelete(GL3DState state);

    public abstract void shapeInit(GL3DState state);

    public abstract void shapeDraw(GL3DState state);

    public abstract void shapeUpdate(GL3DState state);

    public GL3DMat4d modelView() {
        return this.m;
    }

}
