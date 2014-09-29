package org.helioviewer.gl3d.camera;

import java.awt.GraphicsEnvironment;

import javax.swing.JComboBox;

public class FontComboBox extends JComboBox {
    private static final long serialVersionUID = 1L;

    public FontComboBox() {
        this.setEditable(true);
        String fonts[] = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();

        for (int i = 0; i < fonts.length; i++) {
            this.addItem(fonts[i]);
        }
    }

}
