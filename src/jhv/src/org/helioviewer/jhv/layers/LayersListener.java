package org.helioviewer.jhv.layers;

import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;

/**
 * Interface for GUI objects to react to changes of layers The events come on
 * the Event Dispatch Thread (EventQueue)
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
    public void layerRemoved(int oldIdx);

    /**
     * Gets fired if the active layer has changed (meaning, a new layer has
     * become the new active layer).
     *
     * @param view
     *            - view of the new active layer, null if none
     */
    public void activeLayerChanged(JHVJP2View view);

}
