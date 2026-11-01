package org.helioviewer.jhv.layers.fov;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.component.SwingPanel;

@SuppressWarnings("serial")
public final class FOVTreePane extends JPanel {

    public FOVTreePane(FOVCatalog catalog) {
        super(new BorderLayout());
        add(new SwingPanel(new FOVOptions(catalog)), BorderLayout.CENTER);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, getBackground().brighter()));
    }

}
