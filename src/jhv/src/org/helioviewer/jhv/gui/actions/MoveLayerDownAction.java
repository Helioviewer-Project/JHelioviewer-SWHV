package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.View;

/**
 * Action for a specific layer to move down a given layer.
 * 
 * @author Malte Nuhn
 */
public class MoveLayerDownAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    /**
     * Controlled layer by this action.
     */
    private View view;

    /**
     * Creates a action to move a layer down
     * 
     * @param view
     *            Layer to move down
     */
    public MoveLayerDownAction(View view) {
        super("Move Layer Down", IconBank.getIcon(JHVIcon.DOWN));
        this.view = view;
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
        LayersModel.getSingletonInstance().moveLayerDown(view);
    }

}
