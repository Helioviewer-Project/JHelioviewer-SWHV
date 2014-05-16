package org.helioviewer.viewmodel.view;

import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.HashMap;

import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.LayerChangedReason;
import org.helioviewer.viewmodel.changeevent.LayerChangedReason.LayerChangeType;
import org.helioviewer.viewmodel.changeevent.RegionChangedReason;
import org.helioviewer.viewmodel.changeevent.SubImageDataChangedReason;
import org.helioviewer.viewmodel.changeevent.SynchronizedROIChangedReason;
import org.helioviewer.viewmodel.factory.BufferedImageViewFactory;

/**
 * Implementation of SynchronizeView for use in an overview view chain.
 *
 * <p>
 * This class implements a SynchronizeView to use it in an overview view chain.
 * An overview view chain copies the behavior of the main view chain, but it is
 * allowed to opt out unnecessary views.
 *
 * @author Stephan Pagel
 */
public class SynchronizeOverviewChainView extends AbstractSynchronizeChainView {

    // /////////////////////////////////////////////////////////////////////////
    // Definitions
    // /////////////////////////////////////////////////////////////////////////

    // The hashmap memorizes the relation between the sub views of the different
    // layer views.
    private AbstractMap<ImageInfoView, ImageInfoView> viewRelations;

    // /////////////////////////////////////////////////////////////////////////
    // Methods
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Default constructor
     */
    public SynchronizeOverviewChainView() {
        super();
        viewRelations = new HashMap<ImageInfoView, ImageInfoView>();
    }

    @Override
    public ImageInfoView getCorrespondingView(ImageInfoView aView) {
        return viewRelations.get(aView);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setViewMapping(AbstractMap<ImageInfoView, ImageInfoView> map) {
        viewRelations = map;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractMap<ImageInfoView, ImageInfoView> getViewMapping() {
        return viewRelations;
    }

    /**
     * {@inheritDoc}
     */

    @Override
    protected void analyzeObservedView(View sender, ChangeEvent aEvent) {

        // check if a layer in main chain changed -> synchronize the layers
        LayerChangedReason layerReason = aEvent.getLastChangedReasonByType(LayerChangedReason.class);
        if (layerReason != null) {
            layerChanged(layerReason);
        }

        // check if main image data changed -> get information from sender about
        // region
        SubImageDataChangedReason imageDataReason = aEvent.getLastChangedReasonByType(SubImageDataChangedReason.class);
        if (imageDataReason != null || aEvent.getLastChangedReasonByType(RegionChangedReason.class) != null) {
            mainImageDataChanged(sender);
        }
    }

    /**
     * Handles the synchronization of layer related things.
     *
     * <p>
     * In particular, this function handles adding, removing and changing the
     * visibility of layers in the main view chain.
     *
     * @param aLayerChangedReason
     *            observed ChangedReason
     */
    private void layerChanged(LayerChangedReason aLayerChangedReason) {
        if (aLayerChangedReason.getLayerChangeType() == LayerChangeType.LAYER_ADDED) {
            // add layer

            // create sub view chain of layer for overview
            addLayer(aLayerChangedReason.getSubView(), null);

        } else if (aLayerChangedReason.getLayerChangeType() == LayerChangeType.LAYER_REMOVED) {
            // remove a layer
            ImageInfoView imageInfoView = ViewHelper.getViewAdapter(aLayerChangedReason.getSubView(), ImageInfoView.class);
            View overviewImageView = viewRelations.get(imageInfoView);
            if (overviewImageView != null) {
                View subView = findLayerSubView(overviewImageView);
                removeLayer(imageInfoView, subView);
            }
        } else if (aLayerChangedReason.getLayerChangeType() == LayerChangeType.LAYER_VISIBILITY) {
            // change visibility of a layer
            ImageInfoView imageInfoView = ViewHelper.getViewAdapter(aLayerChangedReason.getSubView(), ImageInfoView.class);
            View overviewImageView = viewRelations.get(imageInfoView);

            if (overviewImageView != null) {
                changeLayerVisibility(overviewImageView);
            }
        } else if (aLayerChangedReason.getLayerChangeType() == LayerChangeType.LAYER_MOVED) {
            // move layer
            ImageInfoView iiv = ViewHelper.getViewAdapter(aLayerChangedReason.getSubView(), ImageInfoView.class);
            ImageInfoView overviewImageView = viewRelations.get(iiv);
            LayeredView layeredView = getAdapter(LayeredView.class);
            if (overviewImageView != null) {
                for (int i = 0; i < layeredView.getNumLayers(); ++i) {
                    View layer = layeredView.getLayer(i);
                    if (layer != null && layer.getAdapter(ImageInfoView.class) == overviewImageView) {
                        layeredView.moveView(layer, aLayerChangedReason.getLayerIndex());
                        break;
                    }
                }
            }
        }
    }

    /**
     * Handles the synchronization of region changes related to ROI (e.g. for
     * rectangle)
     *
     * <p>
     * In particular, this function fires a ChangeEvent to notify other views,
     * that the region of the main view chain has changed.
     *
     * @param sender
     *            Origin of the change in the main view chain
     */
    private void mainImageDataChanged(View sender) {

        // get region from other view chain and create corresponding change
        // reason for own view chain
        SynchronizedROIChangedReason reason = new SynchronizedROIChangedReason(this, sender.getAdapter(RegionView.class).getRegion());

        // inform listeners about the changed region in other view chain
        notifyViewListeners(new ChangeEvent(reason));
    }

    /**
     * Adds a sub view chain for a new layer in the main view chain to the
     * overview view chain.
     *
     * All corresponding filter views and the image view will be created.
     *
     * @param view
     *            First view in sub view chain of the corresponding layer from
     *            observed view chain.
     * @return The new image view of the overview layer or null if it could not
     *         be created.
     */
    @Override
    public ImageInfoView addLayer(View view, ImageInfoView observerImageView) {

        ImageInfoView sourceImageView = ViewHelper.getViewAdapter(view, ImageInfoView.class);

        // check if an image view is in sub view chain
        if (sourceImageView == null)
            return null;

        // initialize local variables
        BufferedImageViewFactory viewFactory = new BufferedImageViewFactory();
        ModifiableInnerViewView first = null;
        ModifiableInnerViewView last = null;

        // go through passed sub view chain
        while (view != sourceImageView) {

            // transfer major filter views only
            if (view instanceof FilterView && (((FilterView) view).getFilter().isMajorFilter())) {
                FilterView newFilterView = (FilterView) viewFactory.createViewFromSource(view, true);

                if (first == null) {
                    first = newFilterView;
                    last = newFilterView;
                } else {
                    last.setView(newFilterView);
                    last = newFilterView;
                }
            } else if (view instanceof HelioviewerGeometryView) {
                HelioviewerGeometryView geometryView = (HelioviewerGeometryView) viewFactory.createViewFromSource(view, true);

                if (first == null) {
                    first = geometryView;
                    last = geometryView;
                } else {
                    last.setView(geometryView);
                    last = geometryView;
                }
            }

            // get next sub view
            view = ((ModifiableInnerViewView) view).getView();
        }

        // create new image view and add it to new sub view chain
        if (observerImageView == null) {
            observerImageView = viewFactory.createViewFromSource(sourceImageView, true);
        }

        // if no major filter view found
        if (first == null) {
            getAdapter(LayeredView.class).addLayer(observerImageView);
        } else {
            last.setView(observerImageView);

            // set new sub view chain to corresponding layer
            getAdapter(LayeredView.class).addLayer(first);
        }

        if (observerImageView != null) {
            viewRelations.put(sourceImageView, observerImageView);
        }

        // return
        return observerImageView;
    }

    /**
     * Removes a image view from the overview with all associated views.
     *
     * @param aView
     *            ImageInfoView of the main view chain which is removed
     * */
    @Override
    public void removeLayer(ImageInfoView imageInfoView, View subView) {
        getAdapter(LayeredView.class).removeLayer(subView, false);
        viewRelations.remove(imageInfoView);
    }

    /**
     * Changes the visibility of a layer view chain observed in the main view
     * chain.
     *
     * @param aView
     *            View of the sub image view chain. The visibility of the
     *            related image will be toggled.
     */
    private void changeLayerVisibility(View aView) {
        View subView = findLayerSubView(aView);
        getAdapter(LayeredView.class).toggleVisibility(subView);
    }

    /**
     * Searches for the layered view in a sub tree chain by checking the
     * listening views. The method returns the first sub view of the layered
     * view which belongs to the passed view.
     *
     * @param aView
     *            View from where to search for a layered view.
     * @return The first sub view of the layered view which belongs to the
     *         passed view or null if no layered view could be found.
     * */
    private View findLayerSubView(View aView) {

        AbstractList<ViewListener> viewListeners = aView.getAllViewListener();

        for (ViewListener v : viewListeners) {

            if (v instanceof LayeredView)
                return aView;
            else {
                if (v instanceof View) {
                    View result = findLayerSubView((View) v);

                    if (result != null)
                        return result;
                }
            }
        }

        return null;
    }
}
