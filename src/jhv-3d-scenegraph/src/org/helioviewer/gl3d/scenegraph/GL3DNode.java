package org.helioviewer.gl3d.scenegraph;

import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRay;

/**
 * A {@link GL3DNode} is the base class for all nodes within the scene graph. It
 * defines the structure and basic attributes of every node.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public abstract class GL3DNode {
    private String name;

    protected GL3DNode next;
    protected GL3DNode previous;
    protected GL3DGroup parent;

    protected int depth;

    // Flag whether this node has Changed
    private boolean hasChanged;

    // Flag wheter this node has already been initialised
    protected boolean isInitialised;

    protected GL3DDrawBits drawBits;

    public GL3DNode(String name) {
        this.name = name;
        this.drawBits = new GL3DDrawBits();
    }

    public abstract void init(GL3DState state);

    public abstract void draw(GL3DState state);

    public abstract boolean hit(GL3DRay ray);

    public abstract void update(GL3DState state);

    public abstract void delete(GL3DState state);

    public String toString() {
        return this.name;
    }

    public String getName() {
        return this.name;
    }

    public GL3DNode getNext() {
        return this.next;
    }

    public GL3DGroup getParent() {
        return this.parent;
    }

    public void markAsChanged() {
        this.hasChanged = true;
        if (this.parent != null) {
            this.parent.markAsChanged();
        }
    }

    public abstract GL3DAABBox buildAABB();

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
        return this.drawBits.get(bit) || (this.parent != null && this.parent instanceof GL3DShape && ((GL3DShape) this.parent).isDrawBitOn(bit));
    }

    public void clearDrawBit(Bit bit) {
        this.drawBits.off(bit);
    }
}
