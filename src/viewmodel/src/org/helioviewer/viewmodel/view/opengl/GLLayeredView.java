package org.helioviewer.viewmodel.view.opengl;

import java.io.IOException;
import java.io.InputStream;

import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;

import org.helioviewer.base.FileUtils;
import org.helioviewer.base.logging.Log;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.AbstractLayeredView;
import org.helioviewer.viewmodel.view.SubimageDataView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderView;
import org.helioviewer.viewmodel.view.opengl.shader.GLMinimalFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLMinimalVertexShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderHelper;
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
    private int shaderID = -1;

    /**
     * {@inheritDoc}
     */
    @Override
    public void renderGL(GL2 gl, boolean nextView) {
        layerLock.lock();
        {
            for (View v : layers) {
                Layer layer = viewLookup.get(v);
                if (!layer.visibility) {
                    continue;
                }
                // if layer is GLView, go on, otherwise render now
                if (v instanceof GLView) {
                    ((GLView) v).renderGL(gl, true);
                } else {
                    GLTextureHelper.renderImageDataToScreen(gl, layer.regionView.getRegion(),
                                                                layer.subimageDataView.getSubimageData(),
                                                                v.getAdapter(JHVJP2View.class).tex);
                }
            }
        }
        layerLock.unlock();
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
     */
    @Override
    protected boolean recalculateRegionsAndViewports(ChangeEvent event) {
        boolean changed = false;
        if (region == null && metaData != null) {
            region = StaticRegion.createAdaptedRegion(metaData.getPhysicalRectangle());
        }

        if (viewport != null && region != null) {
            viewportImageSize = ViewHelper.calculateViewportImageSize(viewport, region);

            layerLock.lock();
            for (Layer layer : viewLookup.values()) {
                Region layerRegion = ViewHelper.cropInnerRegionToOuterRegion(layer.metaDataView.getMetaData().getPhysicalRegion(), region);
                changed |= layer.regionView.setRegion(layerRegion, event);
                changed |= layer.viewportView.setViewport(ViewHelper.calculateInnerViewport(layerRegion, region, viewportImageSize), event);
            }
            layerLock.unlock();
        }

        return changed;
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
                    InputStream fragmentStream = FileUtils.getResourceInputStream("/data/fragment3d.cg");
                    String fragmentText = FileUtils.convertStreamToString(fragmentStream);
                    try {
                        fragmentStream.close();
                    } catch (IOException e) {
                        Log.debug("Stream refuses to close" + e);
                    }
                    // fill with other filters and compile
                    fragmentView.buildFragmentShader(newShaderBuilder).compile();

                    GL2 gl = (GL2) GLU.getCurrentGL();
                    if (shaderID == -1) {
                        int[] tmp = new int[1];
                        gl.glGenProgramsARB(1, tmp, 0);
                        shaderID = tmp[0];
                    }
                    GLShaderHelper shaderHelper = new GLShaderHelper();
                    shaderHelper.compileProgram(gl, GL2.GL_FRAGMENT_PROGRAM_ARB, fragmentText, shaderID);

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
