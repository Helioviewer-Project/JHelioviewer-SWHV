package org.helioviewer.jhv.layers;

import org.helioviewer.jhv.view.View;

/**
 * Interface for GUI objects to react to changes of layers
 * The callbacks are invoked on the Event Dispatch Thread
 */
public interface LayersListener {
    /**
     * Gets fired if another layer has become the new active layer
     *
     * @param view
     *            - view of the new active layer, null if none
     */
    void activeLayerChanged(View view);

}
