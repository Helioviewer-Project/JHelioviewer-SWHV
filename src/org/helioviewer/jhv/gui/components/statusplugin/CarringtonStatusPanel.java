package org.helioviewer.jhv.gui.components.statusplugin;

import org.helioviewer.jhv.astronomy.Carrington;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.gui.components.StatusPanel;
import org.helioviewer.jhv.gui.interfaces.LazyComponent;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.time.JHVTime;

@SuppressWarnings("serial")
public class CarringtonStatusPanel extends StatusPanel.StatusPlugin implements LazyComponent {

    private boolean dirty;
    private JHVTime time;

    public CarringtonStatusPanel() {
        update(Movie.getTime());
        UITimer.register(this);
    }

    public void update(JHVTime _time) {
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
