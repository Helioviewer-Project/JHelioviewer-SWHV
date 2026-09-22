package org.helioviewer.jhv.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

public final class ComponentUtils {

    public static void setEnabled(Component c, boolean enable) {
        com.jidesoft.swing.JideSwingUtilities.setEnabledRecursively(c, enable);
    }

    public static AbstractAction hideAction(Component component) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                component.setVisible(false);
            }
        };
    }

    private ComponentUtils() {}
}
