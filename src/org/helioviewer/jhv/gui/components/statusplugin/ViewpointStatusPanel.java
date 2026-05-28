package org.helioviewer.jhv.gui.components.statusplugin;

import java.text.DecimalFormat;

import org.helioviewer.jhv.astronomy.Carrington;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.gui.components.StatusPanel;
import org.helioviewer.jhv.opengl.GLRenderer;
import org.helioviewer.jhv.time.JHVTime;

@SuppressWarnings("serial")
public final class ViewpointStatusPanel extends StatusPanel.StatusPlugin implements Interfaces.LazyComponent {

    private final DecimalFormat carringtonFormat = new DecimalFormat("0.00");
    private final DecimalFormat distanceFormat = new DecimalFormat("0.000");

    private double distance;
    private JHVTime time = Sun.StartEarth.time;

    public ViewpointStatusPanel() {
        lazyRepaint();
        UITimer.register(this);
    }

    @Override
    public void lazyRepaint() {
        Position viewpoint = GLRenderer.getDisplayedViewpoint();
        if (distance == viewpoint.distance && time.milli == viewpoint.time.milli)
            return;

        distance = viewpoint.distance;
        time = viewpoint.time;
        setText("CR: " + carringtonFormat.format(Carrington.time2CR(time)) + " D\u2609: " + distanceFormat.format(distance * Sun.MeanEarthDistanceInv) + "au |");
    }

}
