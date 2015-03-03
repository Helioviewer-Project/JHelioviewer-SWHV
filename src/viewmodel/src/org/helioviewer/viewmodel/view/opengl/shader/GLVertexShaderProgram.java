package org.helioviewer.viewmodel.view.opengl.shader;

import java.util.Stack;

import javax.media.opengl.GL2;

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

    protected static final int target = GL2.GL_VERTEX_PROGRAM_ARB;

    private static Stack<Integer> shaderStack = new Stack<Integer>();
    protected static int shaderCurrentlyUsed = -1;
    protected int shaderID;
    protected double xOffset = 0.0;
    protected double yOffset = 0.0;
    protected double xScale = 1.0;
    protected double yScale = 1.0;
    protected double xTextureScale = 1;
    protected double yTextureScale = 1;
    protected double defaultXOffset = 0;
    protected double defaultYOffset = 0;

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
    public void bind(GL2 gl) {
        bind(gl, shaderID, xOffset, yOffset, xScale, yScale, xTextureScale, yTextureScale, defaultXOffset, defaultYOffset);
    }

    /**
     * Pushes the shader currently in use onto a stack.
     * 
     * This is useful to load another shader but still being able to restore the
     * old one, similar to the very common pushMatrix() in OpenGL2.
     * 
     * @param gl
     *            Valid reference to the current gl object
     * @see #popShader(GL)
     */
    public static void pushShader(GL2 gl) {
        shaderStack.push(shaderCurrentlyUsed);
        // Log.debug("GL3DVertexShaderProgram: pushShader, current="+shaderCurrentlyUsed);
    }

    /**
     * Takes the top of from the shader stack and binds it.
     * 
     * This restores a shader pushed onto the stack earlier, similar to the very
     * common popMatrix() in OpenGL2.
     * 
     * @param gl
     *            Valid reference to the current gl object
     * @see #pushShader(GL)
     */
    public static void popShader(GL2 gl) {
        Integer restoreShaderObject = shaderStack.pop();
        int restoreShader = restoreShaderObject == 0 ? 0 : restoreShaderObject.intValue();
        bind(gl, restoreShader, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f);
        // Log.debug("GL3DVertexShaderProgram:  popShader, current="+shaderCurrentlyUsed);
    }

    /**
     * Binds (= activates it) the given shader, if it is not active so far.
     * 
     * @param gl
     *            Valid reference to the current gl object
     */
    private static void bind(GL2 gl, int shader, double xOffset, double yOffset, double xScale, double yScale, double xTextureScale, double yTextureScale, double defaultXOffset, double defaultYOffset) {
        if (shader != shaderCurrentlyUsed) {
            shaderCurrentlyUsed = shader;
            // Log.debug("GLVertexShaderProgram.bind shader="+shader);
            gl.glBindProgramARB(target, shader);
            gl.glProgramLocalParameter4dARB(target, 0, xOffset, yOffset, xScale, yScale);
            gl.glProgramLocalParameter4dARB(target, 1, xTextureScale, yTextureScale, 0, 0);
            gl.glProgramLocalParameter4dARB(target, 2, defaultXOffset, defaultYOffset, 0, 0);

        }
    }

    public int getId() {
        return this.shaderID;
    }

}
