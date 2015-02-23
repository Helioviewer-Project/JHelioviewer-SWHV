package org.helioviewer.viewmodel.view.opengl;

import javax.media.opengl.GL2;

import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.FilterChangedReason;
import org.helioviewer.viewmodel.changeevent.RegionChangedReason;
import org.helioviewer.viewmodel.changeevent.SubImageDataChangedReason;
import org.helioviewer.viewmodel.changeevent.ViewChainChangedReason;
import org.helioviewer.viewmodel.filter.Filter;
import org.helioviewer.viewmodel.filter.FrameFilter;
import org.helioviewer.viewmodel.filter.GLFilter;
import org.helioviewer.viewmodel.filter.GLFragmentShaderFilter;
import org.helioviewer.viewmodel.filter.GLImageSizeFilter;
import org.helioviewer.viewmodel.filter.GLPostFilter;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.view.StandardFilterView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.view.ViewportView;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderView;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.viewport.Viewport;

/**
 * Implementation of FilterView for rendering in OpenGL mode.
 *
 * <p>
 * Since filters in OpenGL are implemented as shaders, it is not possible to use
 * every filter in OpenGL mode. In particular, only GLFilters should be used.
 * Nevertheless, the view chain still works, when a filter does not support
 * OpenGL, but OpenGL will not be used to accelerate views beneath that filter.
 * Instead, it switches to standard mode for the remaining views. This behavior
 * is implemented in this class.
 *
 * <p>
 * For now it also switch to software mode beneath it, when a time machine is
 * used. TODO
 *
 * <p>
 * For further information on how to use filters, see
 * {@link org.helioviewer.viewmodel.filter} and
 * {@link org.helioviewer.viewmodel.view.StandardFilterView}
 *
 * <p>
 * For further information about how to build shaders, see
 * {@link org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder} as well
 * as the Cg User Manual.
 *
 * @author Markus Langenberg
 *
 */
public class GLFilterView extends StandardFilterView implements GLFragmentShaderView {

    protected ViewportView viewportView;

    protected boolean filteredDataIsUpToDate = false;
    private JHVJPXView jpxView;

    /**
     * {@inheritDoc} This function also sets the image size.
     */
    @Override
    protected void refilterPrepare() {
        if (filter instanceof FrameFilter) {
            updatePrecomputedViews();
            ((FrameFilter) filter).setJP2View(jpxView);
        }
        super.refilterPrepare();
        if (filter instanceof GLImageSizeFilter && viewportView != null) {
            Viewport viewport = viewportView.getViewport();
            if (viewport != null) {
                ((GLImageSizeFilter) filter).setImageSize(viewport.getWidth(), viewport.getHeight());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void renderGL(GL2 gl, boolean nextView) {
        updatePrecomputedViews();
        if (filter instanceof GLFilter) {
            refilterPrepare();

            if (filter instanceof GLFragmentShaderFilter) {
                gl.glEnable(GL2.GL_FRAGMENT_PROGRAM_ARB);
            }

            ((GLFilter) filter).applyGL(gl);
            if (view instanceof GLView) {
                ((GLView) view).renderGL(gl, true);
            } else {
                if (subimageDataView != null) {
                    GLTextureHelper.renderImageDataToScreen(gl, subimageDataView.getSubimageData(), view.getAdapter(JHVJP2View.class));
                }
            }

            if (filter instanceof GLPostFilter) {
                ((GLPostFilter) filter).postApplyGL(gl);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageData getSubimageData() {
        if (!filteredDataIsUpToDate) {
            refilter();
        }

        return super.getSubimageData();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void viewChanged(View sender, ChangeEvent aEvent) {
        if (!(filter instanceof GLFilter)) {
            super.viewChanged(sender, aEvent);
        } else {
            if (aEvent.reasonOccurred(ViewChainChangedReason.class)) {
                updatePrecomputedViews();
                refilter();
            }

            if (aEvent.reasonOccurred(RegionChangedReason.class) || aEvent.reasonOccurred(SubImageDataChangedReason.class)) {
                filteredDataIsUpToDate = false;
            }

            notifyViewListeners(aEvent);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void filterChanged(Filter f) {
        if (!(filter instanceof GLFilter)) {
            super.filterChanged(f);
        } else {
            filteredDataIsUpToDate = false;

            ChangeEvent event = new ChangeEvent();

            event.addReason(new FilterChangedReason(this, filter));
            event.addReason(new SubImageDataChangedReason(this));

            notifyViewListeners(event);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GLShaderBuilder buildFragmentShader(GLShaderBuilder shaderBuilder) {
        if (!(filter instanceof GLFilter)) {
            return shaderBuilder;
        }

        GLFragmentShaderView nextView = view.getAdapter(GLFragmentShaderView.class);
        if (nextView != null) {
            shaderBuilder = nextView.buildFragmentShader(shaderBuilder);
        }

        if (filter instanceof GLFragmentShaderFilter) {
            shaderBuilder = ((GLFragmentShaderFilter) filter).buildFragmentShader(shaderBuilder);
        }

        return shaderBuilder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updatePrecomputedViews() {
        super.updatePrecomputedViews();
        viewportView = ViewHelper.getViewAdapter(view, ViewportView.class);
        jpxView = ViewHelper.getViewAdapter(view, JHVJPXView.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void refilter() {
        super.refilter();
        filteredDataIsUpToDate = (filteredData != null);
    }

}
