package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.View;

/**
 * Action for a specific layer to move up a given layer.
 * 
 * @author Malte Nuhn
 */
public class MoveLayerUpAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    /**
     * Controlled layer by this action.
     */
    private View view;

    /**
     * Creates a action to move a layer up
     * 
     * @param view
     *            Layer to move up
     */
    public MoveLayerUpAction(View view) {
        super("Move Layer Up", IconBank.getIcon(JHVIcon.UP));
        this.view = view;
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
        LayersModel.getSingletonInstance().moveLayerUp(view);
    }

}
