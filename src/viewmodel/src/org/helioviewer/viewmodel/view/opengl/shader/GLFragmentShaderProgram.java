package org.helioviewer.viewmodel.view.opengl.shader;

import javax.media.opengl.GL2;

/**
 * Abstract class to build customized fragment shaders.
 *
 * <p>
 * To use this class, implement it and put the generation of the shader in the
 * buildImpl-function. That function will be called during the rebuild of all
 * shaders.
 *
 * <p>
 * Every implementation of this class represents one block of the final shader
 * code.
 *
 * <p>
 * For further information about how to build shaders, see
 * {@link GLShaderBuilder} as well as the Cg User Manual.
 *
 * @author Markus Langenberg
 */
public abstract class GLFragmentShaderProgram {

    protected static final int target = GL2.GL_FRAGMENT_PROGRAM_ARB;

    /**
     * Binds (= activates it) the shader, if it is not active so far.
     *
     * @param gl
     *            Valid reference to the current gl object
     */
    public void bind(GL2 gl) {
    }

}
