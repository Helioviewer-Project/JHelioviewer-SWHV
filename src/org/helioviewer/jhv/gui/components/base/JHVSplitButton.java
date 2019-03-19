package org.helioviewer.jhv.gui.components.base;

import org.helioviewer.jhv.gui.UIGlobals;

import com.jidesoft.swing.JideSplitButton;

@SuppressWarnings("serial")
public class JHVSplitButton extends JideSplitButton {

    public JHVSplitButton(String text) {
        super(text);
        setForeground(UIGlobals.foreColor);
    }

}
