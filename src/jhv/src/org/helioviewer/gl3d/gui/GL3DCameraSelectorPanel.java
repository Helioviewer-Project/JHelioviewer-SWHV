package org.helioviewer.gl3d.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.helioviewer.gl3d.camera.GL3DCamera;

/**
 * Currently not in use.
 * 
 * GUI Element that lets the user change the currently active {@link GL3DCamera}
 * . However it should not be used as the camera concept should be hidden from
 * the client, because the 3D solar rotation tracking is implemented by using a
 * special camera.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DCameraSelectorPanel extends JPanel {
    private static final long serialVersionUID = -8144305016632959898L;

    private JComboBox cameraComboBox;

    private GL3DCameraSelectorModel cameraSelectorModel;

    public GL3DCameraSelectorPanel() {
        init();
    }

    private void init() {
        setLayout(new BorderLayout());
        cameraSelectorModel = GL3DCameraSelectorModel.getInstance();
        this.cameraComboBox = new JComboBox(cameraSelectorModel);
        add(this.cameraComboBox, BorderLayout.CENTER);

        JButton resetCameraButton = new JButton("Reset");
        add(resetCameraButton, BorderLayout.EAST);
        resetCameraButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                // cameraSelectorModel.getSelectedItem().resetCamera();
            }
        });
    }
}
