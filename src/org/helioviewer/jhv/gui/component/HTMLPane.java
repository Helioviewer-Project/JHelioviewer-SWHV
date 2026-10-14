package org.helioviewer.jhv.gui.component;

import javax.swing.JTextPane;

@SuppressWarnings("serial")
public final class HTMLPane extends JTextPane {

    public HTMLPane() {
        setContentType("text/html");
        setEditable(false);
        putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, true);
    }

    @Override
    public void setText(String text) {
        super.setText(text == null ? null : "<html><body>" + text.replace("\n", "<br/>") + "</body></html>"); // may receive null
        setCaretPosition(0);
    }

}
