package org.helioviewer.jhv.gui;

import java.awt.Component;
import java.awt.Container;

public class ComponentUtils {

    public static void setVisible(Component container, boolean visible) {
        if (container instanceof Container cont) {
            for (Component component : cont.getComponents()) {
                component.setVisible(visible);
                if (component instanceof Container) {
                    setVisible(component, visible);
                }
            }
        }
        container.setVisible(visible);
    }

    public static void setEnabled(Component c, boolean enable) {
        com.jidesoft.swing.JideSwingUtilities.setEnabledRecursively(c, enable);
    }

}
