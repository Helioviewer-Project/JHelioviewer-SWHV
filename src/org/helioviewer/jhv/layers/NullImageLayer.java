package org.helioviewer.jhv.layers;

import org.helioviewer.jhv.view.ManyView;

public class NullImageLayer extends ImageLayer {

    public NullImageLayer(ManyView _view) {
        super(null);
        view = _view;
    }

}
