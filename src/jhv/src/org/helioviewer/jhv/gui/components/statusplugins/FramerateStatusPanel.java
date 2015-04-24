package org.helioviewer.jhv.gui.components.statusplugins;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

/**
 * Status panel for displaying the framerate for image series.
 * The information of this panel is always shown for the active layer.
 * This panel is not visible if the active layer is not an image series.
 * 
 * @author Markus Langenberg
 */
public class FramerateStatusPanel extends JLabel {

    private static final FramerateStatusPanel instance = new FramerateStatusPanel();

    private FramerateStatusPanel() {
        setBorder(BorderFactory.createEtchedBorder());
        setPreferredSize(new Dimension(70, 20));
        setText("fps:");
    }

    public static FramerateStatusPanel getSingletonInstance() {
        return instance;
    }

    public void updateFramerate(int fps) {
        setText(String.format("fps: %d", fps));
    }

}
