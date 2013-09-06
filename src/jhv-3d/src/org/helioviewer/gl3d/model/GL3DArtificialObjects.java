package org.helioviewer.gl3d.model;

import org.helioviewer.gl3d.scenegraph.GL3DGroup;
import org.helioviewer.gl3d.scenegraph.GL3DState;

/**
 * Grouping Object for all artificial objects, that is visual assistance objects
 * that do not represent any real data.
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DArtificialObjects extends GL3DGroup {

    public GL3DArtificialObjects() {
        super("Artificial Objects");
    }

    public void shapeDraw(GL3DState state) {
        // state.gl.glDisable(GL.GL_LIGHTING);
        // state.gl.glEnable(GL.GL_BLEND);
        // state.gl.glDisable(GL.GL_DEPTH_TEST);
        // state.gl.glDepthMask(false);
        super.shapeDraw(state);
        // state.gl.glDepthMask(true);
        // state.gl.glEnable(GL.GL_LIGHTING);
        // state.gl.glDisable(GL.GL_BLEND);
        // state.gl.glEnable(GL.GL_DEPTH_TEST);
    }
}
