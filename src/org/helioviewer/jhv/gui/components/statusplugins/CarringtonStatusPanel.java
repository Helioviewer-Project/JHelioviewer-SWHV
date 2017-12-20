package org.helioviewer.jhv.gui.components.statusplugins;

import org.helioviewer.jhv.astronomy.Carrington;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.gui.components.StatusPanel;
import org.helioviewer.jhv.gui.interfaces.LazyComponent;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.time.JHVDate;

@SuppressWarnings("serial")
public class CarringtonStatusPanel extends StatusPanel.StatusPlugin implements LazyComponent {

    private boolean dirty;
    private JHVDate time;

    public CarringtonStatusPanel() {
        update(Layers.getLastUpdatedTimestamp());
        UITimer.register(this);
    }

    public void update(JHVDate _time) {
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
