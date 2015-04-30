package org.helioviewer.jhv.gui.components.statusplugins;

import javax.swing.JLabel;

import org.helioviewer.jhv.gui.components.StatusPanel;

/**
 * Status panel for displaying the framerate for image series. The information
 * of this panel is always shown for the active layer. This panel is not visible
 * if the active layer is not an image series.
 *
 * @author Markus Langenberg
 */
public class FramerateStatusPanel extends JLabel {

    private static final FramerateStatusPanel instance = new FramerateStatusPanel();

    private FramerateStatusPanel() {
        setBorder(StatusPanel.paddingBorder);
        getPreferredSize().height = StatusPanel.HEIGHT;
        updateFramerate(0);
    }

    public static FramerateStatusPanel getSingletonInstance() {
        return instance;
    }

    public void updateFramerate(int fps) {
        setText(String.format("fps: %02d", fps));
    }

}
