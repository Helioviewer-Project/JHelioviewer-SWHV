package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.View;

/**
 * Action to close the active layer.
 * 
 * @author Markus Langenberg
 */
public class ShowMetainfoOfView extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private View view;

    /**
     * Default constructor.
     */
    public ShowMetainfoOfView(View view) {
        super("Show Metainfo", IconBank.getIcon(JHVIcon.INFO));
        this.view = view;
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
        LayersModel.getSingletonInstance().showMetaInfo(view);
    }

}
