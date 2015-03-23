package org.helioviewer.gl3d.scenegraph;

import org.helioviewer.gl3d.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;

/**
 * A {@link GL3DNode} is the base class for all nodes within the scene graph. It
 * defines the structure and basic attributes of every node.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public abstract class GL3DNode {

    private final String name;

    protected GL3DNode next;
    protected GL3DNode previous;

    protected int depth;

    // Flag whether this node has Changed
    private boolean hasChanged;

    // Flag wheter this node has already been initialised
    protected boolean isInitialised;

    protected GL3DDrawBits drawBits;

    // Model Matrix
    protected GL3DMat4d m;

    // World Matrix
    protected GL3DMat4d wm;

    public GL3DNode(String name) {
        this.name = name;
        this.drawBits = new GL3DDrawBits();

        this.m = GL3DMat4d.identity();
        this.wm = GL3DMat4d.identity();
    }

    @Override
    public String toString() {
        return this.name;
    }

    public String getName() {
        return this.name;
    }

    public GL3DNode getNext() {
        return this.next;
    }

    public void markAsChanged() {
        this.hasChanged = true;
    }

    public boolean hasChanged() {
        return this.hasChanged;
    }

    public void setUnchanged() {
        this.hasChanged = false;
    }

    public GL3DDrawBits getDrawBits() {
        return this.drawBits;
    }

    public boolean isDrawBitOn(Bit bit) {
        return this.drawBits.get(bit);// || (this.parent != null && this.parent instanceof GL3DShape && ((GL3DShape) this.parent).isDrawBitOn(bit));
    }

    public void clearDrawBit(Bit bit) {
        this.drawBits.off(bit);
    }

    public void init(GL3DState state) {
        state.pushMV();
        this.wm = state.multiplyMV(this.m);

        this.shapeInit(state);
        this.isInitialised = true;
        state.popMV();
    }

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

    public void draw(GL3DState state) {
        if (!isDrawBitOn(Bit.Hidden)) {
            state.pushMV();
            state.multiplyMV(this.m);
            this.shapeDraw(state);
            state.popMV();
        }
    }

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
