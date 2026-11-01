package org.helioviewer.jhv.plugins.pfss;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import org.helioviewer.jhv.gui.DesktopIntegration;
import org.helioviewer.jhv.gui.component.SwingPanel;

@SuppressWarnings("serial")
class PfssLayerOptions extends JPanel {

    PfssLayerOptions(PfssLayer layer) {
        super(new BorderLayout());
        add(new SwingPanel(new PfssOptions(layer, DesktopIntegration::openURL)), BorderLayout.CENTER);
    }
}
