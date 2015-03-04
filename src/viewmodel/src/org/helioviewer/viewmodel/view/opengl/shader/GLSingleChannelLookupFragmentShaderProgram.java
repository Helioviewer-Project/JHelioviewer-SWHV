package org.helioviewer.viewmodel.view.opengl.shader;

import javax.media.opengl.GL2;

import org.helioviewer.jhv.shaderfactory.ShaderFactory;

/**
 * Specialized implementation of GLFragmentShaderProgram for filters based on
 * lookup tables.
 *
 * <p>
 * To use this program, it is not necessary to derive another class from this
 * one. A one-dimensional texture is used as the lookup table. To set the lookup
 * table, call {@link #activateLutTexture(GL2)}. This binds the texture id used
 * for the lookup table. After that, the texture can be filled with the lookup
 * data. The rest is done by the class itself.
 *
 * @author Markus Langenberg
 */
public class GLSingleChannelLookupFragmentShaderProgram extends GLFragmentShaderProgram {

    private static int lutID = 0;
    private GLShaderBuilder builder;

    @Override
    public void bind(GL2 gl) {
        super.bind(gl);
        gl.glBindProgramARB(target, ShaderFactory.getFragmentId());
    }

}
