package org.helioviewer.jhv.layers;

import org.helioviewer.jhv.view.View;

public class NullImageLayer extends ImageLayer {

    public NullImageLayer(View _view) {
        super(_view);
    }

    @Override
    public void setView(View _view) {
        view = _view;
    }

}
