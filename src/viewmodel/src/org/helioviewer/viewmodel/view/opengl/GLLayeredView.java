package org.helioviewer.viewmodel.view.opengl;

import javax.media.opengl.GL2;

import org.helioviewer.viewmodel.view.AbstractLayeredView;
import org.helioviewer.viewmodel.view.SubimageDataView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderView;
import org.helioviewer.viewmodel.view.opengl.shader.GLMinimalFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLMinimalVertexShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderView;

/**
 * Implementation of LayeredView for rendering in OpenGL mode.
 *
 * <p>
 * This class manages different layers in OpenGL by branching the renderGL calls
 * as well as the calls for building shaders.
 *
 * <p>
 * For further information about the role of the LayeredView within the view
 * chain, see {@link org.helioviewer.viewmodel.view.LayeredView}
 *
 * @author Markus Langenberg
 *
 */
public class GLLayeredView extends AbstractLayeredView implements GLFragmentShaderView, GLVertexShaderView {

    /**
     * {@inheritDoc}
     */
    @Override
    public void renderGL(GL2 gl, boolean nextView) {
        layerLock.lock();
        try {
            gl.glPushMatrix();
            for (View v : layers) {
                if (!isVisible(v)) {
                    continue;
                }
                // if layer is GLView, go on, otherwise render now
                if (v instanceof GLView) {
                    ((GLView) v).renderGL(gl, true);
                } else {
                    GLTextureHelper.renderImageDataToScreen(gl, v.getAdapter(SubimageDataView.class).getSubimageData(), v.getAdapter(JHVJP2View.class));
                }
            }
            gl.glPopMatrix();
        } finally {
            layerLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * In this case, it does nothing, since for OpenGL views, the rendering
     * takes place in {@link #renderGL(GL2)}.
     */

    @Override
    protected void redrawBufferImpl() {
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * In this case, creates a new shader for every layer and initializes it
     * with the least necessary commands.
     */
    @Override
    public GLShaderBuilder buildFragmentShader(GLShaderBuilder shaderBuilder) {
        layerLock.lock();
        try {
            for (View v : layers) {
                GLFragmentShaderView fragmentView = v.getAdapter(GLFragmentShaderView.class);
                if (fragmentView != null) {
                    // create new shader builder
                    GLShaderBuilder newShaderBuilder = new GLShaderBuilder(shaderBuilder.getGL(), GL2.GL_FRAGMENT_PROGRAM_ARB);

                    // fill with standard values
                    GLMinimalFragmentShaderProgram minimalProgram = new GLMinimalFragmentShaderProgram();
                    minimalProgram.build(newShaderBuilder);

                    // fill with other filters and compile
                    fragmentView.buildFragmentShader(newShaderBuilder).compile();
                }
            }
        } finally {
            layerLock.unlock();
        }

        return shaderBuilder;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * In this case, creates a new shader for every layer and initializes it
     * with the least necessary commands.
     */
    @Override
    public GLShaderBuilder buildVertexShader(GLShaderBuilder shaderBuilder) {
        layerLock.lock();
        try {
            for (View v : layers) {
                GLVertexShaderView vertexView = v.getAdapter(GLVertexShaderView.class);
                if (vertexView != null) {
                    // create new shader builder
                    GLShaderBuilder newShaderBuilder = new GLShaderBuilder(shaderBuilder.getGL(), GL2.GL_VERTEX_PROGRAM_ARB);

                    // fill with standard values
                    GLMinimalVertexShaderProgram minimalProgram = new GLMinimalVertexShaderProgram();
                    minimalProgram.build(newShaderBuilder);

                    // fill with other filters and compile
                    vertexView.buildVertexShader(newShaderBuilder).compile();
                }
            }
        } finally {
            layerLock.unlock();
        }

        return shaderBuilder;
    }

}
