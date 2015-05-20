package org.helioviewer.jhv.renderable.gui;

import javax.swing.JLabel;
import javax.swing.JPanel;

@SuppressWarnings({"serial"})
public class RenderablePanel extends JPanel {

    public RenderablePanel(Renderable renderable) {
        add(new JLabel(renderable.getName()));
    }

}
