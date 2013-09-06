package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.controller.MainImagePanelMousePanController;

/**
 * Action to switch to pan selection mode.
 * 
 * <p>
 * In pan selection mode, the image can be dragged around.
 * 
 * <p>
 * For further information, see
 * {@link org.helioviewer.jhv.gui.controller.MainImagePanelMousePanController}.
 * 
 * @author Markus Langenberg
 */
public class SetPanSelectionAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public SetPanSelectionAction() {
        super("Pan");
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
        ImageViewerGui ivg = ImageViewerGui.getSingletonInstance();
        ivg.getMainImagePanel().getInputController().detach();
        ivg.getMainImagePanel().setInputController(new MainImagePanelMousePanController());
    }

}
