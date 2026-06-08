package org.helioviewer.jhv.gui.components.statusplugin;

import org.helioviewer.jhv.astronomy.Carrington;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.gui.components.StatusPanel;
import org.helioviewer.jhv.math.FastFormat;
import org.helioviewer.jhv.opengl.GLRenderer;
import org.helioviewer.jhv.time.JHVTime;

@SuppressWarnings("serial")
public final class ViewpointStatusPanel extends StatusPanel.StatusPlugin implements Interfaces.LazyComponent {

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

        StringBuilder sb = new StringBuilder(64);
        sb.append("CR: ");
        FastFormat.appendFixed2(sb, Carrington.time2CR(time), 0, false);
        sb.append(" D☉: ");
        FastFormat.appendFixed3(sb, distance * Sun.MeanEarthDistanceInv, 7, false);
        sb.append("au |");
        setText(sb.toString());
    }

}
