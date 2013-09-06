package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.View;

/**
 * Action to center the active layer.
 * 
 * @author Markus Langenberg
 */
public class CenterImageAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public CenterImageAction() {
        super("Center Image");
        putValue(SHORT_DESCRIPTION, "Center the image");
        putValue(MNEMONIC_KEY, KeyEvent.VK_C);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.ALT_MASK));
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
        View view = LayersModel.getSingletonInstance().getActiveView();

        if (view != null) {

            // ViewHelper.getViewAdapter(ImageViewerGui.getSingletonInstance().getImageSelectorPanel().getActiveImageInfoView(),
            // MetaDataView.class);
            MetaDataView baseView = view.getAdapter(MetaDataView.class);
            if (baseView == null) {
                return;
            }

            RegionView targetView = ImageViewerGui.getSingletonInstance().getMainView().getAdapter(RegionView.class);

            Region baseRegion = baseView.getMetaData().getPhysicalRegion();
            Region targetRegion = targetView.getRegion();

            Vector2dDouble newLowerLeftCorner = baseRegion.getLowerLeftCorner().add(baseRegion.getSize().subtract(targetRegion.getSize()).scale(0.5));

            targetView.setRegion(StaticRegion.createAdaptedRegion(newLowerLeftCorner, targetRegion.getSize()), new ChangeEvent());

        }

    }

}
