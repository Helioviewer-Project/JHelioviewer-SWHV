package org.helioviewer.jhv.gui.components.base;

import org.helioviewer.jhv.gui.UIGlobals;

import com.jidesoft.swing.JideToggleButton;

@SuppressWarnings("serial")
public class JHVToggleButton extends JideToggleButton {

    public JHVToggleButton(String text) {
        super(text);
        setForeground(UIGlobals.foreColor);
    }

    public JHVToggleButton(String text, boolean selected) {
        super(text, selected);
        setForeground(UIGlobals.foreColor);
    }

}
