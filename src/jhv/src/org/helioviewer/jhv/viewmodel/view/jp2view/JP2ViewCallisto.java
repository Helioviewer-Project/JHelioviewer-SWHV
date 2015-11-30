package org.helioviewer.jhv.viewmodel.view.jp2view;

import java.awt.Rectangle;

import org.helioviewer.jhv.base.Region;

public class JP2ViewCallisto extends JP2View {

    public JP2Image getJP2Image() {
        return _jp2Image;
    }

    public void setViewport(Rectangle v) {
        ((JP2ImageCallisto) _jp2Image).setViewport(v);
    }

    public boolean setRegion(Region r) {
        ((JP2ImageCallisto) _jp2Image).setRegion(r);
        signalRender(_jp2Image, false, 1);
        return true;
    }

    @Override
    public void render(double factor) {
        // should be called only during setJP2Image
    }

}
