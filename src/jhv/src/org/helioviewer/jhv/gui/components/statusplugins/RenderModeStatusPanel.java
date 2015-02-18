package org.helioviewer.jhv.gui.components.statusplugins;

import java.awt.EventQueue;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import org.helioviewer.jhv.gui.interfaces.StatusPanelPlugin;
import org.helioviewer.jhv.opengl.GLInfo;

/**
 * Status Panel for displaying the render mode currently used.
 * 
 * <p>
 * If the OpenGL render mode is used, also shows the OpenGL version.
 * 
 * <p>
 * The information of this panel is independent from the active layer.
 * 
 * @author Markus Langenberg
 */
public class RenderModeStatusPanel extends JLabel implements StatusPanelPlugin {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public RenderModeStatusPanel() {
        setBorder(BorderFactory.createEtchedBorder());
        setText("none");
        //validate();
        //setPreferredSize(null);
    }

    /**
     * Updates the status panel.
     * 
     * It reads the current render mode from
     * {@link org.helioviewer.jhv.opengl.GLInfo}.
     */
    private void updateStatus_raw() {
        if (GLInfo.glIsEnabled()) {
            setText("OpenGL " + GLInfo.getVersion());
            //validate();
            //setPreferredSize(null);
        } else {
            setText("Software");
            //validate();
            //setPreferredSize(null);
        }
    }

    public void updateStatus() {
        if (EventQueue.isDispatchThread()) {
            updateStatus_raw();
        } else {
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    updateStatus_raw();
                }
            });
        }
    }

}
