package org.helioviewer.gl3d.scenegraph;

/**
 * A {@link GL3DShape} is a {@link GL3DNode} that does have a position and a
 * bounding box within the scene graph. In practice, almost every
 * {@link GL3DNode} is also a {@link GL3DShape}.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public abstract class GL3DShape extends GL3DNode {

    public GL3DShape(String name) {
        super(name);
    }

}
