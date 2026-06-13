package org.helioviewer.jhv.layers;

import org.helioviewer.jhv.view.View;

class NullImageLayer extends ImageLayer {

    NullImageLayer(View _view) {
        super(_view);
    }

    @Override
    void setView(View _view) {
        view = _view;
    }

}
