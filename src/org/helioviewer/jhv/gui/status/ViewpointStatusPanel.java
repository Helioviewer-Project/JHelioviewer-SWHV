package org.helioviewer.jhv.gui.status;

import org.helioviewer.jhv.astronomy.Carrington;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.gui.component.StatusPanel;
import org.helioviewer.jhv.math.FastFormat;
import org.helioviewer.jhv.opengl.GLRenderer;
import org.helioviewer.jhv.time.JHVTime;

@SuppressWarnings("serial")
public final class ViewpointStatusPanel extends StatusPanel.StatusPlugin implements Interfaces.LazyComponent {

    private final int[] labels = new int[4];
    private double distance;
    private JHVTime time = Sun.StartEarth.time;

    public ViewpointStatusPanel() {
        setLabelRanges(labels);
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
        labels[0] = 0;
        sb.append("CR");
        labels[1] = sb.length();
        sb.append(' ');
        FastFormat.appendFixed2(sb, Carrington.time2CR(time), 0, false);
        sb.append("    ");
        int fieldStart = sb.length();
        FastFormat.appendFixed3(sb, distance * Sun.MeanEarthDistanceInv, 7, false);
        while (sb.charAt(fieldStart) == ' ')
            fieldStart++;
        sb.insert(fieldStart, "D☉ ");
        labels[2] = fieldStart;
        labels[3] = fieldStart + 2;
        sb.append(" au");
        setText(sb.toString());
    }

}
