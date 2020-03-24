package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.layers.ImageLayers;

@SuppressWarnings("serial")
public class SDOCutOutAction extends AbstractAction {

    private static final String baseURL = "https://www.lmsal.com/get_aia_data/?";

    public SDOCutOutAction() {
        super("SDO Cut-out");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JHVGlobals.openURL(baseURL + ImageLayers.getSDOCutoutString());
    }

}
