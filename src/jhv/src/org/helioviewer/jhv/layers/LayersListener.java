package org.helioviewer.jhv.layers;

import org.helioviewer.jhv.viewmodel.view.View;

/**
 * Interface for GUI objects to react to changes of layers
 * The callbacks are invoked on the Event Dispatch Thread
 */
public interface LayersListener {

    /**
     * Gets fired if a new layer has been added.
     *
     * @param idx
     *            - view of the added layer
     */
    public void layerAdded(View view);

    /**
     * Gets fired if the active layer has changed (meaning, a new layer has
     * become the new active layer).
     *
     * @param view
     *            - view of the new active layer, null if none
     */
    public void activeLayerChanged(View view);

}
