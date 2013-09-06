package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.controller.MainImagePanelMouseZoomBoxController;

/**
 * Action to switch to zoom box selection mode.
 * 
 * <p>
 * In zoom box selection mode, the user can specify a region of interest by
 * selecting it using a zoom box.
 * 
 * <p>
 * For further information, see
 * {@link org.helioviewer.jhv.gui.controller.MainImagePanelMouseZoomBoxController}.
 * 
 * @author Markus Langenberg
 */
public class SetZoomBoxSelectionAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public SetZoomBoxSelectionAction() {
        super("Zoom Box");
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
        ImageViewerGui ivg = ImageViewerGui.getSingletonInstance();
        ivg.getMainImagePanel().getInputController().detach();
        ivg.getMainImagePanel().setInputController(new MainImagePanelMouseZoomBoxController());
    }

}