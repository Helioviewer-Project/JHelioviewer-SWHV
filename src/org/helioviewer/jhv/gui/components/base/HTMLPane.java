package org.helioviewer.jhv.gui.components.base;

import javax.swing.JTextPane;

import org.helioviewer.jhv.gui.UIGlobals;

@SuppressWarnings("serial")
public class HTMLPane extends JTextPane {

    private static final String pre = String.format("<html><span style='font-size:%dpt'><font face='%s'>", UIGlobals.uiFont.getSize(), UIGlobals.uiFont.getFontName());
    private static final String post = "</font></span></html>";

    public HTMLPane() {
        setContentType("text/html");
        setEditable(false);
        putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
    }

    @Override
    public void setText(String text) {
        super.setText(text == null ? null : pre + text.replace("\n", "<br/>") + post); // may receive null
        setCaretPosition(0);
    }

}
