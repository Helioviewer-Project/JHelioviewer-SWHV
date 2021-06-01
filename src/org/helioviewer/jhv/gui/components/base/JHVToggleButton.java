package org.helioviewer.jhv.gui.components.base;

import com.jidesoft.swing.JideToggleButton;

@SuppressWarnings("serial")
public class JHVToggleButton extends JideToggleButton {

    public JHVToggleButton(String text) {
        super(text);
    }

    public JHVToggleButton(String text, boolean selected) {
        super(text, selected);
    }

}
