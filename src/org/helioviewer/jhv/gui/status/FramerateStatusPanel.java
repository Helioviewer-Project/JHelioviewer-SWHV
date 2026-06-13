package org.helioviewer.jhv.gui.status;

import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.gui.component.StatusPanel;
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
        int f = MainFrame.getFramerate();
        if (f != fps) {
            fps = f;
            setText(FastFormat.appendInteger(new StringBuilder("FPS: "), fps, 2, false).toString());
        }
    }

}
