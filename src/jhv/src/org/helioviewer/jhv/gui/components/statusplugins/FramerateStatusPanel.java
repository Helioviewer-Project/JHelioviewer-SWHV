package org.helioviewer.jhv.gui.components.statusplugins;

import org.helioviewer.jhv.gui.components.StatusPanel;

@SuppressWarnings("serial")
public class FramerateStatusPanel extends StatusPanel.StatusPlugin {

    public FramerateStatusPanel() {
        update(0);
    }

    public void update(int fps) {
        setText(String.format("fps: %d", fps));
    }

}
