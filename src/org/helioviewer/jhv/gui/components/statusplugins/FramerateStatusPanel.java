package org.helioviewer.jhv.gui.components.statusplugins;

import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.gui.components.StatusPanel;
import org.helioviewer.jhv.gui.interfaces.LazyComponent;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.Movie;

@SuppressWarnings("serial")
public class FramerateStatusPanel extends StatusPanel.StatusPlugin implements LazyComponent {

    private int fps = -1;

    public FramerateStatusPanel() {
        lazyRepaint();
        UITimer.register(this);
    }

    @Override
    public void lazyRepaint() {
        int f = 0;
        ImageLayer layer;
        if (Movie.isPlaying() && (layer = Layers.getActiveImageLayer()) != null) {
            f = layer.getView().getCurrentFramerate();
        }

        if (f != fps) {
            fps = f;
            setText("fps: " + fps);
        }
    }

}
