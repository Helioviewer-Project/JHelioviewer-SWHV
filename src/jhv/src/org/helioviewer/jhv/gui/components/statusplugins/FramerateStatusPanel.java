package org.helioviewer.jhv.gui.components.statusplugins;

import javax.swing.JLabel;

@SuppressWarnings({"serial"})
public class FramerateStatusPanel extends JLabel {

    public FramerateStatusPanel() {
        updateFramerate(0);
    }

    public void updateFramerate(float fps) {
        setText(String.format("fps: % 2d", Math.round(fps * 100) / 100));
    }

}
