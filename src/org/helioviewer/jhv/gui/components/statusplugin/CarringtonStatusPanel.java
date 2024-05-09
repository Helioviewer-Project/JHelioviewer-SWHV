package org.helioviewer.jhv.gui.components.statusplugin;

import org.helioviewer.jhv.astronomy.Carrington;
import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.gui.components.StatusPanel;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.time.JHVTime;

@SuppressWarnings("serial")
public final class CarringtonStatusPanel extends StatusPanel.StatusPlugin implements Interfaces.LazyComponent {

    private boolean dirty;
    private JHVTime time;

    public CarringtonStatusPanel() {
        update(Movie.getTime());
        UITimer.register(this);
    }

    public void update(JHVTime _time) { // checked for change in the calling ViewpointLayer
        time = _time;
        dirty = true;
    }

    @Override
    public void lazyRepaint() {
        if (dirty) {
            setText(String.format("CR: %.2f", Carrington.time2CR(time)));
            dirty = false;
        }
    }

}
