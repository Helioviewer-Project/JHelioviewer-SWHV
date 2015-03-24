package org.helioviewer.jhv.gui.components.statusplugins;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import org.helioviewer.jhv.gui.interfaces.StatusPanelPlugin;

/**
 * Status panel for displaying the framerate for image series.
 * 
 * <p>
 * The information of this panel is always shown for the active layer.
 * 
 * <p>
 * This panel is not visible, if the active layer is not an image series.
 * 
 * @author Markus Langenberg
 */
public class FramerateStatusPanel extends JLabel implements StatusPanelPlugin {

    private static final long serialVersionUID = 1L;
    private static final FramerateStatusPanel instance = new FramerateStatusPanel();

    private FramerateStatusPanel() {
        setBorder(BorderFactory.createEtchedBorder());
        setPreferredSize(new Dimension(70, 20));
        setText("fps:");

        setVisible(true);
    }

    public static FramerateStatusPanel getSingletonInstance() {
        return instance;
    }

    public void updateFramerate(double fps) {
        setText("fps: " + Double.toString(fps));
    }

}
