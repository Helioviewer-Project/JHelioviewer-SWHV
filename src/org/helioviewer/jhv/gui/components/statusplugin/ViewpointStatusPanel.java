package org.helioviewer.jhv.gui.components.statusplugin;

import org.helioviewer.jhv.astronomy.Carrington;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.gui.components.StatusPanel;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeUtils;

@SuppressWarnings("serial")
public final class ViewpointStatusPanel extends StatusPanel.StatusPlugin implements Interfaces.LazyComponent {

    private double distance;
    private JHVTime time = TimeUtils.START;

    public ViewpointStatusPanel() {
        lazyRepaint();
        UITimer.register(this);
    }

    @Override
    public void lazyRepaint() {
        Position viewpoint = Display.getCamera().getViewpoint();
        if (distance == viewpoint.distance && time.milli == viewpoint.time.milli)
            return;

        distance = viewpoint.distance;
        time = viewpoint.time;
        setText(String.format("CR: %.2f D\u2609: %7.3fau |", Carrington.time2CR(time), distance * Sun.MeanEarthDistanceInv));
    }

}
