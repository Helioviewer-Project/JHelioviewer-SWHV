package org.helioviewer.gl3d.scenegraph;

import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;

/**
 * A {@link GL3DModel} is a node within the Scene graph that can be turned on
 * and off by the user.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DModel extends GL3DGroup {
    private String description;

    public GL3DModel(String name, String description) {
        super(name);
        this.description = description;
    }

    public boolean isActive() {
        return !this.isDrawBitOn(Bit.Hidden);
    }

    public void setActive(boolean value) {
        this.getDrawBits().set(Bit.Hidden, !value);
    }

    public String getDescription() {
        return this.description;
    }
}
