package org.helioviewer.jhv.gui.components.statusplugins;

import java.awt.Dimension;

import javax.swing.BorderFactory;

import org.helioviewer.jhv.layers.LayersModel;

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
public class FramerateStatusPanel extends ViewStatusPanelPlugin {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public FramerateStatusPanel() {
        setBorder(BorderFactory.createEtchedBorder());

        setPreferredSize(new Dimension(70, 20));
        setText("fps:");

        setVisible(true);
        LayersModel.getSingletonInstance().addLayersListener(this);
    }

    private void updateFramerate() {
        int idx = LayersModel.getSingletonInstance().getActiveLayer();
        if (LayersModel.getSingletonInstance().isValidIndex(idx)) {
            double fps = LayersModel.getSingletonInstance().getFPS(idx);
            String fpsString = Double.toString(fps);

            setVisible(true);
            setText("fps: " + fpsString);
        } else {
            setVisible(false);
        }
    }

    public void activeLayerChanged(int idx) {
        updateFramerate();
    }

}
