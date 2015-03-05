package org.helioviewer.viewmodel.factory;

import java.util.LinkedList;

import org.helioviewer.base.logging.Log;
import org.helioviewer.viewmodel.filter.Filter;
import org.helioviewer.viewmodel.renderer.screen.ScreenRenderer;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.FilterView;
import org.helioviewer.viewmodel.view.ImageInfoView;
import org.helioviewer.viewmodel.view.JHVSimpleImageView;
import org.helioviewer.viewmodel.view.OverlayView;
import org.helioviewer.viewmodel.view.ScalingView;
import org.helioviewer.viewmodel.view.StandardFilterView;
import org.helioviewer.viewmodel.view.StandardSolarRotationTrackingView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.fitsview.JHVFITSView;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;

/**
 * Abstract implementation of interface ViewFactory for independent views.
 * 
 * <p>
 * This class produced views, which are independent from the type of the used
 * view chain and which can be used in every type. Since the set of views
 * created by this factory is incomplete, it does not make sense to use it
 * itself, thus it is abstract.
 * <p>
 * Apart from that, it provides some basic internal functionality.
 * <p>
 * For further details on how to use view factories, see {@link ViewFactory}.
 * 
 * @author Markus Langenberg
 */
public abstract class StandardViewFactory implements ViewFactory {

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T extends View> T createNewView(Class<T> pattern) {

        // FilterView
        if (pattern.isAssignableFrom(FilterView.class)) {
            return (T) new StandardFilterView();
            // SolarRotationTrackingView
        } else if (pattern.isAssignableFrom(StandardSolarRotationTrackingView.class)) {
            return (T) new StandardSolarRotationTrackingView();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T extends View> T createViewFromSource(T source, boolean keepSource) {

        // ComponentView
        if (source instanceof ComponentView) {
            ComponentView sourceComponent = (ComponentView) source;
            ComponentView newComponent = (ComponentView) createViewFromSourceImpl(source);

            LinkedList<ScreenRenderer> renderers = new LinkedList<ScreenRenderer>(sourceComponent.getAllPostRenderer());

            for (ScreenRenderer r : renderers) {
                if (!keepSource) {
                    sourceComponent.removePostRenderer(r);
                }
                newComponent.addPostRenderer(r);
            }

            return (T) newComponent;

            // OverlayView
        } else if (source instanceof OverlayView) {
            OverlayView newOverlay = (OverlayView) createViewFromSourceImpl(source);

            return (T) newOverlay;

            // FilterView
        } else if (source instanceof FilterView) {
            FilterView sourceFilter = (FilterView) source;
            FilterView newFilter = (FilterView) createViewFromSourceImpl(source);

            Filter filter = sourceFilter.getFilter();
            if (filter != null)
                filter.forceRefilter();
            if (!keepSource) {
                sourceFilter.setFilter(null);
            }
            newFilter.setFilter(filter);

            return (T) newFilter;

            // ScalingView
        } else if (source instanceof ScalingView) {
            ScalingView sourceScaling = (ScalingView) source;
            ScalingView newScaling = (ScalingView) createViewFromSourceImpl(source);

            if (newScaling == null) {
                return null;
            }

            newScaling.setInterpolationMode(sourceScaling.getInterpolationMode());

            return (T) newScaling;

            // ImageInfoView
        } else if (source instanceof ImageInfoView) {
            if (source instanceof JHVJPXView) {
                JHVJPXView sourceJPX = (JHVJPXView) source;
                JHVJPXView newJPX = new JHVJPXView(false, sourceJPX.getDateRange(), true);

                newJPX.setJP2Image(sourceJPX.getJP2Image());

                return (T) newJPX;

            }
            if (source instanceof JHVJP2View) {
                JHVJP2View sourceJP2 = (JHVJP2View) source;
                JHVJP2View newJP2 = new JHVJP2View(false, sourceJP2.getDateRange());

                newJP2.setJP2Image(sourceJP2.getJP2Image());

                return (T) newJP2;

            } else if (source instanceof JHVFITSView) {
                JHVFITSView sourceFITS = (JHVFITSView) source;
                JHVFITSView newFITS = new JHVFITSView(sourceFITS.getFITSImage(), sourceFITS.getUri(), sourceFITS.getDateRange());

                return (T) newFITS;

            } else if (source instanceof JHVSimpleImageView) {
                JHVSimpleImageView sourceSimple = (JHVSimpleImageView) source;
                JHVSimpleImageView newSimple = new JHVSimpleImageView(sourceSimple.getSimpleImage(), sourceSimple.getUri(), null);

                return (T) newSimple;
            } else {
                Log.error("Copying of this ImageInfoView type is not implemented yet!");
                return null;
            }

        } else {
            return createViewFromSourceImpl(source);
        }
    }

    /**
     * Internal function to create views based on pattern implementation.
     * 
     * This function returns creates view from a pattern. The only task it has
     * to fulfill is to return a blank class, fitting to the pattern. Copying
     * the functional members takes place in the caller, which is
     * createViewFromSource.
     * 
     * @param <T>
     *            interface type of new view
     * @param source
     *            pattern to create new view
     * @return implementation equivalent to source
     */
    protected abstract <T extends View> T createViewFromSourceImpl(T source);

    /**
     * Internal function to create independent views based on a pattern
     * implementation.
     * 
     * This function may be called by subclasses of StandardFactory, when they
     * do not support the given pattern.
     * 
     * @param <T>
     *            interface type of new view
     * @param source
     *            pattern to create new view
     * @return implementation equivalent to source
     */
    @SuppressWarnings("unchecked")
    protected <T extends View> T createStandardViewFromSource(T source) {
        // FilterView
        if (source instanceof FilterView) {
            return (T) new StandardFilterView();
            // SolarRotationTrackingView
        } else if (source instanceof StandardSolarRotationTrackingView) {
            return (T) new StandardSolarRotationTrackingView();
        } else {
            return null;
        }
    }
}
