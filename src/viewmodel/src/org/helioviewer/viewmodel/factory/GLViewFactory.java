package org.helioviewer.viewmodel.factory;

import org.helioviewer.viewmodel.view.FilterView;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.HelioviewerGeometryView;
import org.helioviewer.viewmodel.view.LayeredView;
import org.helioviewer.viewmodel.view.OverlayView;
import org.helioviewer.viewmodel.view.StandardSolarRotationTrackingView;
import org.helioviewer.viewmodel.view.SynchronizeView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.opengl.GLHelioviewerGeometryView;
import org.helioviewer.viewmodel.view.opengl.GLOverlayView;
import org.helioviewer.viewmodel.view.opengl.GLFilterView;
import org.helioviewer.viewmodel.view.opengl.GLComponentView;
import org.helioviewer.viewmodel.view.opengl.GLLayeredView;
import org.helioviewer.viewmodel.view.opengl.GLSolarRotationTrackingView;
import org.helioviewer.viewmodel.view.opengl.GLSynchronizeOverviewChainView;

/**
 * Implementation of interface ViewFactory for OpenGL views.
 * 
 * <p>
 * This class implements the interface ViewFactory in such a way, that it
 * returns a OpenGL specific implementation or a independent standard
 * implementation, if no specific one is available.
 * <p>
 * For further details on how to use view factories, see {@link ViewFactory}.
 * 
 * @author Markus Langenberg
 * 
 */
public class GLViewFactory extends StandardViewFactory {

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T extends View> T createNewView(Class<T> pattern) {

        // ComponentView
        if (pattern.isAssignableFrom(ComponentView.class)) {
            return (T) new GLComponentView();
            // EventView
        } else if (pattern.isAssignableFrom(OverlayView.class)) {
            return (T) new GLOverlayView();
            // LayeredView
        } else if (pattern.isAssignableFrom(LayeredView.class)) {
            return (T) new GLLayeredView();
            // FilterView
        } else if (pattern.isAssignableFrom(FilterView.class)) {
            return (T) new GLFilterView();
            // SynchronizedView
        } else if (pattern.isAssignableFrom(SynchronizeView.class)) {
            return (T) new GLSynchronizeOverviewChainView();
            // HelioviewerGeometryView
        } else if (pattern.isAssignableFrom(HelioviewerGeometryView.class)) {
            return (T) new GLHelioviewerGeometryView();
            // SolarRotationTrackingView
        } else if (pattern.isAssignableFrom(StandardSolarRotationTrackingView.class)) {
            return (T) new GLSolarRotationTrackingView();
        } else {
            return super.createNewView(pattern);
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    protected <T extends View> T createViewFromSourceImpl(T source) {

        // ComponentView
        if (source instanceof ComponentView) {
            return (T) new GLComponentView();
            // OverlayView
        } else if (source instanceof OverlayView) {
            return (T) new GLOverlayView();
            // LayeredView
        } else if (source instanceof LayeredView) {
            return (T) new GLLayeredView();
            // FilterView
        } else if (source instanceof FilterView) {
            return (T) new GLFilterView();
            // SynchronizedView
        } else if (source instanceof SynchronizeView) {
            return (T) new GLSynchronizeOverviewChainView();
            // HelioviewerGeometryView
        } else if (source instanceof HelioviewerGeometryView) {
            return (T) new GLHelioviewerGeometryView();
            // SolarRotationTrackingView
        } else if (source instanceof StandardSolarRotationTrackingView) {
            return (T) new GLSolarRotationTrackingView();
        } else {
            return createStandardViewFromSource(source);
        }
    }

}
