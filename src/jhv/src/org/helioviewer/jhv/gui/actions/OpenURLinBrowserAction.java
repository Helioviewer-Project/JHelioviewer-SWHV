package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.JHVGlobals;

@SuppressWarnings("serial")
public class OpenURLinBrowserAction extends AbstractAction {

    private final String urlToOpen;

    public OpenURLinBrowserAction(String name, String url) {
        super(name);
        urlToOpen = url;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JHVGlobals.openURL(urlToOpen);
    }

}
