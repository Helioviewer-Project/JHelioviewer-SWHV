package org.helioviewer.jhv.gui.components.statusplugins;

import org.helioviewer.jhv.base.astronomy.Carrington;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.gui.components.StatusPanel;
import org.helioviewer.jhv.layers.Layers;

@SuppressWarnings("serial")
public class CarringtonStatusPanel extends StatusPanel.StatusPlugin {

    public CarringtonStatusPanel() {
        update(Layers.getLastUpdatedTimestamp());
    }

    public void update(JHVDate time) {
        setText(String.format("CR: %.2f", Carrington.time2CR(time)));
    }

}
