package org.helioviewer.jhv.gui.components.statusplugins;

import javax.swing.JLabel;

public class FramerateStatusPanel extends JLabel {

    private static final FramerateStatusPanel instance = new FramerateStatusPanel();

    private FramerateStatusPanel() {
        updateFramerate(0);
    }

    public static FramerateStatusPanel getSingletonInstance() {
        return instance;
    }

    public void updateFramerate(int fps) {
        setText(String.format("fps: % 2d", fps));
    }

}
