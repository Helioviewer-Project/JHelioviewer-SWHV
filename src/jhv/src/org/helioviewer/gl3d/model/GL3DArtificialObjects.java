package org.helioviewer.gl3d.model;

import org.helioviewer.gl3d.scenegraph.GL3DGroup;
import org.helioviewer.gl3d.scenegraph.GL3DState;

/**
 * Grouping Object for all artificial objects, that is visual assistance objects
 * that do not represent any real data.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DArtificialObjects extends GL3DGroup {

    public GL3DArtificialObjects() {
        super("Artificial Objects");
    }

    @Override
    public void shapeDraw(GL3DState state) {
        super.shapeDraw(state);
    }
}