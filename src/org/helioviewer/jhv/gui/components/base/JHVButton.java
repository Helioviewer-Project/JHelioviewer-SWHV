package org.helioviewer.jhv.gui.components.base;

import org.helioviewer.jhv.gui.UIGlobals;

import com.jidesoft.swing.JideButton;

@SuppressWarnings("serial")
public class JHVButton extends JideButton {

    public JHVButton() {
        setForeground(UIGlobals.foreColor);
    }

    public JHVButton(String text) {
        super(text);
        setForeground(UIGlobals.foreColor);
    }

}
