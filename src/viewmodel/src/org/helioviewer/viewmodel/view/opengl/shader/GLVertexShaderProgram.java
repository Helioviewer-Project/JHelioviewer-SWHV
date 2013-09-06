package org.helioviewer.viewmodel.view.opengl.shader;

import java.util.Stack;

import javax.media.opengl.GL;

/**
 * Abstract class to build customized vertex shaders.
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
public abstract class GLVertexShaderProgram {

    protected static final int target = GL.GL_VERTEX_PROGRAM_ARB;

    private static Stack<Integer> shaderStack = new Stack<Integer>();
    private static int shaderCurrentlyUsed = -1;
    private int shaderID;

    /**
     * Build the shader.
     * 
     * This function is called during the building process of all shaders,
     * providing a shader builder object. That object may already contain code
     * from other shader blocks. This functions calls
     * {@link #buildImpl(GLShaderBuilder)} and remembers the shader if the
     * shader, to be able to bind it later.
     * 
     * @param shaderBuilder
     *            ShaderBuilder to append customized code
     */
    public final void build(GLShaderBuilder shaderBuilder) {
        buildImpl(shaderBuilder);
        shaderID = shaderBuilder.getShaderID();
        shaderCurrentlyUsed = -1;
    }

    /**
     * Build customized part of the shader.
     * 
     * This function is called during the building process of all shaders,
     * providing a shader builder object. That object may already contain code
     * from other shader blocks. Just append the additional code within this
     * function.
     * 
     * @param shaderBuilder
     *            ShaderBuilder to append customized code
     */
    protected abstract void buildImpl(GLShaderBuilder shaderBuilder);

    /**
     * Binds (= activates it) the shader, if it is not active so far.
     * 
     * @param gl
     *            Valid reference to the current gl object
     */
    public final void bind(GL gl) {
        bind(gl, shaderID);
    }

    /**
     * Pushes the shader currently in use onto a stack.
     * 
     * This is useful to load another shader but still being able to restore the
     * old one, similar to the very common pushMatrix() in OpenGL.
     * 
     * @param gl
     *            Valid reference to the current gl object
     * @see #popShader(GL)
     */
    public static void pushShader(GL gl) {
        shaderStack.push(shaderCurrentlyUsed);
        // Log.debug("GL3DVertexShaderProgram: pushShader, current="+shaderCurrentlyUsed);
    }

    /**
     * Takes the top of from the shader stack and binds it.
     * 
     * This restores a shader pushed onto the stack earlier, similar to the very
     * common popMatrix() in OpenGL.
     * 
     * @param gl
     *            Valid reference to the current gl object
     * @see #pushShader(GL)
     */
    public static void popShader(GL gl) {
        Integer restoreShaderObject = shaderStack.pop();
        int restoreShader = restoreShaderObject == 0 ? 0 : restoreShaderObject.intValue();
        bind(gl, restoreShader);
        // Log.debug("GL3DVertexShaderProgram:  popShader, current="+shaderCurrentlyUsed);
    }

    /**
     * Binds (= activates it) the given shader, if it is not active so far.
     * 
     * @param gl
     *            Valid reference to the current gl object
     */
    private static void bind(GL gl, int shader) {
        if (shader != shaderCurrentlyUsed) {
            shaderCurrentlyUsed = shader;
            // Log.debug("GLVertexShaderProgram.bind shader="+shader);
            gl.glBindProgramARB(target, shader);
        }
    }

    public int getId() {
        return this.shaderID;
    }
}
