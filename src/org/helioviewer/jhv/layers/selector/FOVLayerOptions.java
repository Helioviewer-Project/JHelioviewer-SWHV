package org.helioviewer.jhv.layers.selector;

import org.helioviewer.jhv.layers.FOVLayer;
import org.helioviewer.jhv.layers.fov.FOVTreePane;

@SuppressWarnings("serial")
final class FOVLayerOptions extends FOVTreePane implements LayerOptions.OptionsPanel {

    FOVLayerOptions(FOVLayer layer) {
        super(layer.getCatalog());
    }

}
