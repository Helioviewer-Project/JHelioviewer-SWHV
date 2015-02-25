package org.helioviewer.viewmodel.view;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

import org.helioviewer.base.math.RectangleDouble;
import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.LayerChangedReason;
import org.helioviewer.viewmodel.changeevent.LayerChangedReason.LayerChangeType;
import org.helioviewer.viewmodel.changeevent.NonConstantMetaDataChangedReason;
import org.helioviewer.viewmodel.changeevent.RegionChangedReason;
import org.helioviewer.viewmodel.changeevent.RegionUpdatedReason;
import org.helioviewer.viewmodel.changeevent.SubImageDataChangedReason;
import org.helioviewer.viewmodel.changeevent.ViewChainChangedReason;
import org.helioviewer.viewmodel.metadata.ImageSizeMetaData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.metadata.PixelBasedMetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.RegionAdapter;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.viewport.Viewport;
import org.helioviewer.viewmodel.viewportimagesize.ViewportImageSize;

/**
 * Abstract base class implementing LayeredView, providing some common
 * functions.
 *
 * <p>
 * This class provides most of the functionality of a LayeredView, since most of
 * its behavior is independent from the render mode. Because of that, the whole
 * management of the stack of layers is centralized in this abstract class.
 * <p>
 * To improve performance, many intermediate results are cached.
 * <p>
 * For further informations about how to use layers, see {@link LayeredView}.
 *
 * @author Ludwig Schmidt
 * @author Markus Langenberg
 *
 */
public abstract class AbstractLayeredView extends AbstractView implements LayeredView, RegionView, ViewportView, MetaDataView, ViewListener {

    protected CopyOnWriteArrayList<View> layers = new CopyOnWriteArrayList<View>();
    protected ReentrantLock layerLock = new ReentrantLock();
    protected HashMap<View, Layer> viewLookup = new HashMap<View, Layer>();

    protected ViewportImageSize viewportImageSize;

    protected Viewport viewport;
    protected Region region;
    protected MetaData metaData;

    private double minimalRegionSize;

    /**
     * Buffer for precomputed values for each layer.
     *
     * <p>
     * This container saves some values per layer, such as its visibility and
     * precomputed view adapters.
     *
     */
    protected class Layer {
        private final View view;

        public RegionView regionView;
        public ViewportView viewportView;
        public MetaDataView metaDataView;

        public Vector2dInt renderOffset;
        public boolean visibility = true;

        /**
         * Default constructor.
         *
         * Computes view adapters for this layer.
         *
         * @param base
         *            layer to save
         */
        public Layer(View base) {
            view = base;
            update();
        }

        /**
         * Recalculates the view adapters, in case the view chain has changed.
         */
        public void update() {
            if (view != null) {
                regionView = view.getAdapter(RegionView.class);
                viewportView = view.getAdapter(ViewportView.class);
                metaDataView = view.getAdapter(MetaDataView.class);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isVisible(View _view) {
        if (viewLookup.get(_view) != null)
            return viewLookup.get(_view).visibility;
        else
            return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfVisibleLayer() {
        int result = 0;
        layerLock.lock();
        try {
            Collection<Layer> values = viewLookup.values();
            for (Layer l : values) {
                if (l.visibility)
                    result++;
            }
        } finally {
            layerLock.unlock();
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void toggleVisibility(View view) {
        LinkedMovieManager.getActiveInstance().pauseLinkedMovies();

        Layer layer = viewLookup.get(view);
        if (layer != null) {
            layer.visibility = !layer.visibility;
            redrawBuffer(new ChangeEvent(new LayerChangedReason(this, LayerChangeType.LAYER_VISIBILITY, view)));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addLayer(View newLayer) {
        addLayer(newLayer, layers.size());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addLayer(View newLayer, int newIndex) {
        if (newLayer == null) {
            return;
        }
        LinkedMovieManager.getActiveInstance().pauseLinkedMovies();

        ChangeEvent changeEvent = new ChangeEvent(new LayerChangedReason(this, LayerChangeType.LAYER_ADDED, newLayer));

        layers.add(newIndex, newLayer);
        newLayer.addViewListener(this);

        viewLookup.put(newLayer, new Layer(newLayer));

        if (layers.size() > 1) {
            recalculateMetaData(false);
            for (Layer layer : viewLookup.values()) {
                MetaData m = layer.metaDataView.getMetaData();
                if (m instanceof PixelBasedMetaData) {
                    PixelBasedMetaData p = (PixelBasedMetaData) m;
                    p.updatePhysicalRegion(metaData.getPhysicalRegion());
                }
            }
        }
        recalculateMetaData();
        region = metaData.getPhysicalRegion();
        if (viewport != null)
            region = ViewHelper.expandRegionToViewportAspectRatio(viewport, region, metaData);
        if (region != null)
            region = new RegionAdapter(new StaticRegion(-0.5 * region.getWidth(), -0.5 * region.getHeight(), region.getSize()));
        recalculateRegionsAndViewports(new ChangeEvent());
        redrawBuffer(changeEvent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View getLayer(int index) {
        if (index >= 0 && index < layers.size()) {
            return layers.get(index);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumLayers() {
        return layers.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLayerLevel(View view) {
        return layers.indexOf(view);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeLayer(View view) {
        removeLayer(view, true);
    }

    @Override
    public void removeLayer(View view, boolean needAbolish) {

        if (view == null) {
            return;
        }

        int index = layers.indexOf(view);
        if (index == -1) {
            return;
        }
        LinkedMovieManager.getActiveInstance().pauseLinkedMovies();
        layerLock.lock();
        try {
            layers.remove(view);
            viewLookup.remove(view);
            view.removeViewListener(this);
        } finally {
            layerLock.unlock();
        }

        JHVJP2View jhvjp2 = view.getAdapter(JHVJP2View.class);
        if (jhvjp2 != null) {
            if (needAbolish) {
                jhvjp2.abolish();
            }
            jhvjp2.removeRenderListener();
        }

        ChangeEvent event = new ChangeEvent(new LayerChangedReason(this, LayerChangeType.LAYER_REMOVED, view, index));

        recalculateMetaData(false);
        if (metaData != null) {
            for (Layer layer : viewLookup.values()) {
                MetaData m = layer.metaDataView.getMetaData();
                if (m instanceof PixelBasedMetaData) {
                    PixelBasedMetaData p = (PixelBasedMetaData) m;
                    p.updatePhysicalRegion(metaData.getPhysicalRegion());
                }
            }
        }
        recalculateMetaData();
        if (metaData != null) {
            Region bound = metaData.getPhysicalRegion();
            double lowerLeftX = Math.max(bound.getCornerX(), region.getCornerX());
            double lowerLeftY = Math.max(bound.getCornerY(), region.getCornerY());
            Region newRegion = ViewHelper.cropInnerRegionToOuterRegion(StaticRegion.createAdaptedRegion(lowerLeftX, lowerLeftY, region.getSize()), bound);
            setRegion(newRegion, event);
        } else {
            recalculateRegionsAndViewports(event);
        }
        redrawBuffer(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeLayer(int index) {
        if (index >= 0 && index < layers.size()) {
            removeLayer(layers.get(index));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAllLayers() {
        ChangeEvent event = new ChangeEvent();
        LinkedMovieManager.getActiveInstance().pauseLinkedMovies();
        layerLock.lock();
        try {
            for (View view : layers) {
                int index = layers.indexOf(view);
                view.removeViewListener(this);
                if (view.getAdapter(JHVJP2View.class) != null) {
                    view.getAdapter(JHVJP2View.class).abolish();
                }
                event.addReason(new LayerChangedReason(this, LayerChangeType.LAYER_REMOVED, view, index));
            }
            layers.clear();
            viewLookup.clear();
        } finally {
            layerLock.unlock();
        }
        recalculateMetaData();
        recalculateRegionsAndViewports(event);
        redrawBuffer(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings(value = { "unchecked" })
    public <T extends View> T getAdapter(Class<T> c) {
        if (c.isInstance(this)) {
            return (T) this;
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Region getRegion() {
        return region;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setRegion(Region r, ChangeEvent event) {

        if (event == null) {
            event = new ChangeEvent(new RegionUpdatedReason(this, r));
        } else {
            event.addReason(new RegionUpdatedReason(this, r));
        }

        // check if region is valid
        if (region == null || r == null || r.getWidth() < minimalRegionSize || r.getHeight() < minimalRegionSize) {
            notifyViewListeners(event);
            return false;
        }
        // check if region to small or viewport
        r = ViewHelper.expandRegionToViewportAspectRatio(viewport, r, metaData);

        // check if region has changed
        if (region.getCornerX() == r.getCornerX() && region.getCornerY() == r.getCornerY() && region.getWidth() == r.getWidth() && region.getHeight() == r.getHeight()) {
            notifyViewListeners(event);
            return false;
        }

        event.addReason(new RegionChangedReason(this, r));

        region = r;
        recalculateRegionsAndViewports(event);
        redrawBuffer(event);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Viewport getViewport() {
        return viewport;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setViewport(Viewport v, ChangeEvent event) {
        // check if viewport has changed
        if (viewport != null && v != null && viewport.getWidth() == v.getWidth() && viewport.getHeight() == v.getHeight())
            return false;

        viewport = v;

        if (!setRegion(region, event)) {
            recalculateRegionsAndViewports(event);
            redrawBuffer(event);
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MetaData getMetaData() {
        return metaData;
    }

    /**
     * Recalculates the regions and viewports of all layers.
     *
     * <p>
     * Sets the regions and viewports of all layers according to the region and
     * viewport of the LayeredView. Also, calculates the offset of the layers to
     * each other.
     *
     * @param event
     *            ChangeEvent to collect history of all following changes
     * @return true, if at least one region or viewport changed
     */
    protected boolean recalculateRegionsAndViewports(ChangeEvent event) {
        return recalculateRegionsAndViewports(event, true);
    }

    /**
     * Recalculates the regions and viewports of all layers.
     *
     * <p>
     * Sets the regions and viewports of all layers according to the region and
     * viewport of the LayeredView. Also, calculates the offset of the layers to
     * each other.
     *
     * @param event
     *            ChangeEvent to collect history of all following changes
     * @return true, if at least one region or viewport changed
     */
    private boolean recalculateRegionsAndViewports(ChangeEvent event, boolean includePixelBasedImages) {
        boolean changed = false;
        if (region == null && metaData != null) {
            region = StaticRegion.createAdaptedRegion(metaData.getPhysicalRectangle());
        }

        if (viewport != null && region != null) {
            ViewportImageSize oldViewportImageSize = viewportImageSize;
            viewportImageSize = ViewHelper.calculateViewportImageSize(viewport, region);
            changed |= viewportImageSize == null ? oldViewportImageSize == null : viewportImageSize.equals(oldViewportImageSize);

            layerLock.lock();
            for (Layer layer : viewLookup.values()) {
                MetaData m = layer.metaDataView.getMetaData();
                if (includePixelBasedImages || !(m instanceof PixelBasedMetaData)) {
                    Region layerRegion = ViewHelper.cropInnerRegionToOuterRegion(m.getPhysicalRegion(), region);
                    Viewport layerViewport = ViewHelper.calculateInnerViewport(layerRegion, region, viewportImageSize);
                    layer.renderOffset = ViewHelper.calculateInnerViewportOffset(layerRegion, region, viewportImageSize);

                    changed |= layer.regionView.setRegion(layerRegion, event);
                    changed |= layer.viewportView.setViewport(layerViewport, event);
                }
            }
            layerLock.unlock();
        }

        return changed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void viewChanged(View sender, ChangeEvent aEvent) {
        if (aEvent.reasonOccurred(ViewChainChangedReason.class)) {
            for (Layer layer : viewLookup.values()) {
                layer.update();
            }
        }
        if (aEvent.reasonOccurred(NonConstantMetaDataChangedReason.class)) {
            recalculateMetaData();
            recalculateRegionsAndViewports(new ChangeEvent(aEvent));
        }

        if ((aEvent.reasonOccurred(RegionChangedReason.class) || aEvent.reasonOccurred(SubImageDataChangedReason.class) || aEvent.reasonOccurred(ViewChainChangedReason.class)) && sender != null) {
            redrawBuffer(aEvent);
        } else {
            notifyViewListeners(aEvent);
        }
    }

    /**
     * Recalculates the meta data of the LayeredView.
     *
     * The region of the LayeredView is set to the bounding box of all layers.
     */
    protected void recalculateMetaData() {
        recalculateMetaData(true);
    }

    /**
     * Recalculates the meta data of the LayeredView.
     *
     * The region of the LayeredView is set to the bounding box of all layers.
     */
    protected void recalculateMetaData(boolean includePixelBasedImages) {
        if (layers.size() == 0) {
            metaData = null;
            return;
        }

        RectangleDouble bounds = null;
        minimalRegionSize = 0.0f;
        layerLock.lock();
        try {
            for (Layer layer : viewLookup.values()) {
                if (layer.metaDataView != null) {
                    if (includePixelBasedImages || !(layer.metaDataView.getMetaData() instanceof PixelBasedMetaData)) {
                        RectangleDouble metaDataRectangle = layer.metaDataView.getMetaData().getPhysicalRectangle();
                        if (bounds == null) {
                            bounds = metaDataRectangle;
                        } else {
                            bounds = bounds.getBoundingRectangle(metaDataRectangle);
                        }

                        double unitsPerPixel = ((ImageSizeMetaData) layer.metaDataView.getMetaData()).getUnitsPerPixel();
                        if (unitsPerPixel > minimalRegionSize) {
                            minimalRegionSize = unitsPerPixel;
                        }
                    }
                }
            }
        } finally {
            layerLock.unlock();
        }
        if (bounds != null)
            metaData = new PixelBasedMetaData(bounds);

        minimalRegionSize *= 2.0f;
    }

    /**
     * Redraws the scene.
     *
     * Calls the implementation specific function redrawBufferImpl(). A
     * SubImageDataChangedReason will be appended to the given ChangeEvent.
     *
     * @param aEvent
     *            ChangeEvent to collect history
     */
    protected void redrawBuffer(ChangeEvent aEvent) {
        redrawBufferImpl();

        // add reason to change event
        if (aEvent == null)
            aEvent = new ChangeEvent();

        aEvent.addReason(new SubImageDataChangedReason(this));
        notifyViewListeners(aEvent);
    }

    /**
     * Implementation specific part of redrawing the scene.
     *
     * Will be called from redrawBuffer.
     */
    protected abstract void redrawBufferImpl();

    /**
     * {@inheritDoc}
     */
    @Override
    public void moveView(View view, int newLevel) {
        ChangeEvent changeEvent = new ChangeEvent(new LayerChangedReason(this, LayerChangeType.LAYER_MOVED, view, newLevel));
        layerLock.lock();
        try {
            if (layers.contains(view)) {
                layers.remove(view);
                layers.add(newLevel, view);
                redrawBuffer(changeEvent);
            }
        } finally {
            layerLock.unlock();
        }
    }

}
