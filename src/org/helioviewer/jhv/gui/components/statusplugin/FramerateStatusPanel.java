package org.helioviewer.jhv.gui.components.statusplugin;

import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.gui.components.StatusPanel;
import org.helioviewer.jhv.gui.interfaces.LazyComponent;
import org.helioviewer.jhv.opengl.GLListener;

@SuppressWarnings("serial")
public class FramerateStatusPanel extends StatusPanel.StatusPlugin implements LazyComponent {

    private int fps = -1;

    public FramerateStatusPanel() {
        lazyRepaint();
        UITimer.register(this);
    }

    @Override
    public void lazyRepaint() {
        int f = GLListener.getFramerate();
        if (f != fps) {
            fps = f;
            setText("fps: " + fps);
        }
    }

}
