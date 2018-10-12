package org.helioviewer.jhv.gui.components.base;

import javax.swing.JTextPane;

import org.helioviewer.jhv.gui.UIGlobals;

@SuppressWarnings("serial")
public class HTMLPane extends JTextPane {

    private static final String pre = String.format("<html><span style='font-size:%dpt'><font face='%s'>", UIGlobals.UIFont.getSize(), UIGlobals.UIFont.getFontName());

    public HTMLPane() {
        setContentType("text/html");
        setEditable(false);
    }

    @Override
    public void setText(String text) {
        super.setText(pre + text.replace("\n", "<br/>"));
    }

}
