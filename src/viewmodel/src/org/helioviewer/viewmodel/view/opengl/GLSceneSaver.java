package org.helioviewer.viewmodel.view.opengl;

import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.util.Stack;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;

import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLScalePowerOfTwoVertexShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderProgram;

/**
 * Class to render a to a texture and reuse the image at a later point.
 * 
 * <p>
 * Using this class, it is possible to use multiple shaders on the same image.
 * This might be useful, if some shaders are too heavy for the graphics card or
 * if shaders should be used before and after a
 * {@link org.helioviewer.viewmodel.view.LayeredView}.
 * 
 * <p>
 * This class is not used in the current code (March 2010), and during tests it
 * slowed down the rendering performance significant. Thus, using it should be
 * well considered.
 * 
 * @author Markus Langenberg
 */
public class GLSceneSaver {

    private static GLTextureHelper textureHelper = new GLTextureHelper();
    private static GLScalePowerOfTwoVertexShaderProgram scalingShader;

    private static Stack<Integer> sceneTextureStack = new Stack<Integer>();

    /**
     * Saves the current scene, including all attributes and shaders.
     * 
     * The current state is pushed onto a stack and can be restored later using
     * {@link #popScene(GL)}. The design of this function is thought to be
     * similar to usual OpenGL behavior (e.g. glPushMatrix).
     * 
     * @param gl
     *            Valid reference to the current gl object
     */
    public static void pushScene(GL gl) {

        GLFragmentShaderProgram.pushShader(gl);
        GLVertexShaderProgram.pushShader(gl);

        sceneTextureStack.push(textureHelper.copyFrameBufferToTexture(gl));

        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
    }

    /**
     * Restores a saved scene, including all attributes and shaders.
     * 
     * The saved state is taken from a stack, where it was saved before using
     * {@link #popScene(GL)}. The design of this function is thought to be
     * similar to usual OpenGL behavior (e.g. glPuopMatrix).
     * 
     * @param gl
     *            Valid reference to the current gl object
     */
    public static void popScene(GL gl) {

        // Generate scaling shader, if necessary
        if (!GLTextureHelper.textureNonPowerOfTwoAvailable() && scalingShader == null) {
            scalingShader = new GLScalePowerOfTwoVertexShaderProgram();
            scalingShader.buildStandAlone(gl);
        }

        // Save current scene
        int newTexture = textureHelper.copyFrameBufferToTexture(gl);

        // Clear screen
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);

        // calculate region
        boolean isFragmentProgramEnabled = gl.glIsEnabled(GL.GL_FRAGMENT_PROGRAM_ARB);
        boolean isVertexProgramEnabled = gl.glIsEnabled(GL.GL_VERTEX_PROGRAM_ARB);

        DoubleBuffer modelViewMatrix = DoubleBuffer.allocate(16);
        DoubleBuffer projectionMatrix = DoubleBuffer.allocate(16);
        IntBuffer viewport = IntBuffer.allocate(4);

        gl.glGetDoublev(GL.GL_MODELVIEW_MATRIX, modelViewMatrix);
        gl.glGetDoublev(GL.GL_PROJECTION_MATRIX, projectionMatrix);
        gl.glGetIntegerv(GL.GL_VIEWPORT, viewport);

        DoubleBuffer positionBuffer = DoubleBuffer.allocate(3);
        DoubleBuffer sizeBuffer = DoubleBuffer.allocate(3);

        GLU glu = new GLU();
        glu.gluUnProject(0, viewport.get(3), 0, modelViewMatrix, projectionMatrix, viewport, positionBuffer);
        glu.gluUnProject(viewport.get(2), 0, 0, modelViewMatrix, projectionMatrix, viewport, sizeBuffer);

        Vector2dDouble position = new Vector2dDouble(positionBuffer.get(), positionBuffer.get());
        Vector2dDouble size = (new Vector2dDouble(sizeBuffer.get(), sizeBuffer.get())).subtract(position);
        Region region = StaticRegion.createAdaptedRegion(position, size);

        // Set shaders
        gl.glDisable(GL.GL_FRAGMENT_PROGRAM_ARB);
        if (!GLTextureHelper.textureNonPowerOfTwoAvailable()) {
            gl.glEnable(GL.GL_VERTEX_PROGRAM_ARB);
            scalingShader.bind(gl);
        } else {
            gl.glDisable(GL.GL_VERTEX_PROGRAM_ARB);
        }

        // Get saved scene from stack
        int oldTexture = sceneTextureStack.pop();
        textureHelper.bindTexture(gl, oldTexture);

        // draw saved scene
        textureHelper.renderTextureToScreen(gl, region);

        // delete svaed texture (it is not used any more)
        textureHelper.delTextureID(gl, oldTexture);

        // Reset shaders
        if (!GLTextureHelper.textureNonPowerOfTwoAvailable() && !isVertexProgramEnabled) {
            gl.glDisable(GL.GL_VERTEX_PROGRAM_ARB);
        } else if (GLTextureHelper.textureNonPowerOfTwoAvailable() && isVertexProgramEnabled) {
            gl.glEnable(GL.GL_VERTEX_PROGRAM_ARB);
        }

        if (isFragmentProgramEnabled) {
            gl.glEnable(GL.GL_FRAGMENT_PROGRAM_ARB);
        }

        // Load current shaders again
        GLFragmentShaderProgram.popShader(gl);
        GLVertexShaderProgram.popShader(gl);

        // bind current texture
        textureHelper.bindTexture(gl, newTexture);

        // draw current texture on top of old one
        textureHelper.renderTextureToScreen(gl, region);

        // delete current texture (it is not used any more)
        textureHelper.delTextureID(gl, newTexture);
    }

    /**
     * Clears the shader shared all GLSceneSavers
     */
    public static void clearShader() {
        scalingShader = null;
    }
}
