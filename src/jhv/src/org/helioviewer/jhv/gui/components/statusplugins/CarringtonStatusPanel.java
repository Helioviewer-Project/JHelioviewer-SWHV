package org.helioviewer.jhv.gui.components.statusplugins;

import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.StatusPanel;
import org.helioviewer.jhv.layers.Layers;

@SuppressWarnings("serial")
public class CarringtonStatusPanel extends StatusPanel.StatusPlugin {

    public CarringtonStatusPanel() {
        update(Layers.getLastUpdatedTimestamp(), Displayer.getCamera().getViewpoint().distance);
    }

    public void update(JHVDate time, double d) {
        setText(String.format("CR: %.2f | D\u2299: %5.2fau", Sun.getCarringtonRotation(time), d / Sun.MeanEarthDistance));
    }

}
