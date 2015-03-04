package org.helioviewer.jhv.gui.components.statusplugins;

import java.awt.Dimension;

import javax.swing.BorderFactory;

import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.jp2view.JP2View;

/**
 * Status panel for displaying quality layer currently used.
 * 
 * <p>
 * The information of this panel is always shown for the active layer.
 * 
 * <p>
 * This panel is not visible, if the active layer is not an JPG2000 image.
 * 
 * @author Markus Langenberg
 */
public class QualityStatusPanel extends ViewStatusPanelPlugin {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public QualityStatusPanel() {
        setPreferredSize(null);
        setText("");
        setBorder(BorderFactory.createEtchedBorder());

        setPreferredSize(new Dimension(100, 20));
        setText("Quality: ");

        LayersModel.getSingletonInstance().addLayersListener(this);
    }

    /**
     * Updates the quality layer currently used.
     */
    private void updateQualityLayers() {
        View view = LayersModel.getSingletonInstance().getActiveView();
        JP2View jp2View = null;

        if (view != null) {
            jp2View = view.getAdapter(JP2View.class);
        }

        if (jp2View != null) {
            int qlayers = jp2View.getCurrentNumQualityLayers();
            int maxQlayers = jp2View.getMaximumNumQualityLayers();
            setVisible(true);
            setText("Quality: " + qlayers + "/" + maxQlayers);
        } else {
            setVisible(false);
        }
    }

    /**
     * {@inheritDoc}
     */

    public void activeLayerChanged(int idx) {
        updateQualityLayers();
    }

    /**
     * {@inheritDoc}
     */

    public void subImageDataChanged() {
        updateQualityLayers();
    }

}
