package org.helioviewer.swhv.gui.layerpanel;

import javax.swing.JPanel;

import org.helioviewer.swhv.mvc.SWHVPanel;

public abstract class SWHVLayerPanel extends JPanel implements SWHVPanel {
    private static final long serialVersionUID = 1L;

    @Override
    public abstract SWHVLayerController getController();

}
