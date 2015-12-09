package org.helioviewer.jhv.gui.components.statusplugins;

import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.gui.components.StatusPanel;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.TimeListener;

@SuppressWarnings("serial")
public class CarringtonStatusPanel extends StatusPanel.StatusPlugin implements TimeListener {

    public CarringtonStatusPanel() {
        updateCarrington(Layers.addTimeListener(this));
    }

    @Override
    public void timeChanged(JHVDate time) {
        updateCarrington(time);
    }

    private void updateCarrington(JHVDate time) {
        setText(String.format("CR: %.2f", Sun.getCarringtonSynodic(time)));
    }

}
