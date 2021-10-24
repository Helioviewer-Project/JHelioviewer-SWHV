package org.helioviewer.jhv.layers;

import org.helioviewer.jhv.view.NullView;

public class NullImageLayer extends ImageLayer {

    public NullImageLayer(long start, long end, int cadence) {
        super(null);
        view = NullView.create(start, end, cadence);
    }

}
