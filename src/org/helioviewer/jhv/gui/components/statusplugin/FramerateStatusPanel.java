package org.helioviewer.jhv.gui.components.statusplugin;

import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.gui.components.StatusPanel;
import org.helioviewer.jhv.math.FastFormat;

@SuppressWarnings("serial")
public final class FramerateStatusPanel extends StatusPanel.StatusPlugin implements Interfaces.LazyComponent {

    private int fps = -1;

    public FramerateStatusPanel() {
        lazyRepaint();
        UITimer.register(this);
    }

    @Override
    public void lazyRepaint() {
        int f = JHVFrame.getFramerate();
        if (f != fps) {
            fps = f;
            setText("FPS: " + FastFormat.integer(fps, 2, false));
        }
    }

}
