package org.helioviewer.jhv.layers;

import org.helioviewer.viewmodel.view.View;

/**
 * Interface for GUI objects to react to changes of layers
 * <p>
 * The events come usually from the loading thread so the event calls are likely
 * to occur so the listener are responsible doing the gui work in the
 * EventThread
 * http://download.oracle.com/javase/6/docs/api/javax/swing/package-summary
 * .html#package_description}
 * 
 * @author Malte Nuhn
 */
public interface LayersListener {

    /**
     * Gets fired if a new layer has been added.
     * 
     * @param idx
     *            - index of the new layer
     */
    public void layerAdded(int idx);

    /**
     * Gets fired if a layer has been removed.
     * 
     * @param oldIdx
     *            - (old) index of the layer that has been removed
     */
    public void layerRemoved(View oldView, int oldIdx);

    /**
     * Gets fired if a layer has changed.
     * 
     * @param idx
     *            - index of the layer that changed
     */
    public void layerChanged(int idx);

    /**
     * Gets fired if the active layer has changed (meaning, a new layer has
     * become the new active layer).
     * 
     * @param view
     *            - view of the new active layer, null if none
     */
    public void activeLayerChanged(View view);

}
