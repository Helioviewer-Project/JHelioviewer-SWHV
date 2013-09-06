package org.helioviewer.viewmodel.view.opengl;

import javax.media.opengl.GL;

import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.AbstractLayeredView;
import org.helioviewer.viewmodel.view.SubimageDataView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewHelper;
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

    private GLTextureHelper textureHelper = new GLTextureHelper();

    /**
     * {@inheritDoc}
     */
    public void addLayer(View newLayer, int newIndex) {
        if (newLayer == null) {
            return;
        }

        if (!GLTextureHelper.textureNonPowerOfTwoAvailable() && newLayer.getAdapter(GLScalePowerOfTwoView.class) == null) {
            GLScalePowerOfTwoView scaleView = new GLScalePowerOfTwoView();
            scaleView.setView(newLayer);
            newLayer = scaleView;
        }

        // Add filter for dynamic view angle dependant opacity
        /*
         * GLFilterView filterView = new GLFilterView(); OpacityFilter filter =
         * new OpacityFilter(0.5f); filterView.setFilter(filter);
         * 
         * GLHelioviewerGeometryView geomView =
         * newLayer.getAdapter(GLHelioviewerGeometryView.class); if(geomView !=
         * null) { View firstFilter = geomView.getView();
         * filterView.setView(firstFilter); geomView.setView(filterView); }
         */

        super.addLayer(newLayer, newIndex);
    }

    /**
     * {@inheritDoc}
     */
    protected boolean recalculateRegionsAndViewports(ChangeEvent event) {

        boolean changed = false;

        // check region and viewport
        if (region == null && metaData != null)
            region = StaticRegion.createAdaptedRegion(metaData.getPhysicalRectangle());

        if (viewport != null && region != null) {
            viewportImageSize = ViewHelper.calculateViewportImageSize(viewport, region);

            for (Layer layer : viewLookup.values()) {
                MetaData m = layer.metaDataView.getMetaData();

                Region layerRegion = ViewHelper.cropInnerRegionToOuterRegion(m.getPhysicalRegion(), region);
                changed |= layer.regionView.setRegion(layerRegion, event);
                changed |= layer.viewportView.setViewport(ViewHelper.calculateInnerViewport(layerRegion, region, viewportImageSize), event);
            }
        }

        return changed;
    }

    /**
     * {@inheritDoc}
     */
    public void renderGL(GL gl) {

        layerLock.lock();

        try {
            gl.glPushMatrix();

            for (View v : layers) {
                Layer layer = viewLookup.get(v);

                // If invisible, skip layer
                if (!layer.visibility) {
                    continue;
                }

                gl.glColor3f(1.0f, 1.0f, 1.0f);

                // if layer is GLView, go on, otherwise render now
                if (v instanceof GLView) {
                    ((GLView) v).renderGL(gl);
                } else {
                    textureHelper.renderImageDataToScreen(gl, layer.regionView.getRegion(), v.getAdapter(SubimageDataView.class).getSubimageData());
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
     * takes place in {@link #renderGL(GL)}.
     */

    protected void redrawBufferImpl() {
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * In this case, creates a new shader for every layer and initializes it
     * with the least necessary commands.
     */
    public GLShaderBuilder buildFragmentShader(GLShaderBuilder shaderBuilder) {

        layerLock.lock();

        try {
            for (View v : layers) {

                GLFragmentShaderView fragmentView = v.getAdapter(GLFragmentShaderView.class);
                if (fragmentView != null) {
                    // create new shader builder
                    GLShaderBuilder newShaderBuilder = new GLShaderBuilder(shaderBuilder.getGL(), GL.GL_FRAGMENT_PROGRAM_ARB);

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
    public GLShaderBuilder buildVertexShader(GLShaderBuilder shaderBuilder) {

        layerLock.lock();

        try {
            for (View v : layers) {
                GLVertexShaderView vertexView = v.getAdapter(GLVertexShaderView.class);
                if (vertexView != null) {
                    // create new shader builder
                    GLShaderBuilder newShaderBuilder = new GLShaderBuilder(shaderBuilder.getGL(), GL.GL_VERTEX_PROGRAM_ARB);

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
