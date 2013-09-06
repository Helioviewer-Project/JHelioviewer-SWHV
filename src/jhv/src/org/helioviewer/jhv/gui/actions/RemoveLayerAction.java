package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.View;

/**
 * Action to remove the given layer.
 * 
 * @author Malte Nuhn
 */
public class RemoveLayerAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    /**
     * Controlled layer by this action.
     */
    private View view;

    /**
     * Creates a action to remove a given layer
     * 
     * @param view
     *            Layer to control
     */
    public RemoveLayerAction(View view) {
        super("Close Layer", IconBank.getIcon(JHVIcon.REMOVE_LAYER));
        this.view = view;
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
        LayersModel.getSingletonInstance().removeLayer(view);
    }

}
