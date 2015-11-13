package org.helioviewer.jhv.gui.components.statusplugins;

import org.helioviewer.jhv.gui.components.StatusPanel;

@SuppressWarnings("serial")
public class FramerateStatusPanel extends StatusPanel.StatusPlugin {

    public FramerateStatusPanel() {
        updateFramerate(0);
    }

    public void updateFramerate(float fps) {
        setText(String.format("fps: % 2d", Math.round(fps * 100) / 100));
    }

}
