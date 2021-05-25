package org.helioviewer.jhv.gui;

import java.awt.Component;
import java.awt.Container;

import com.jidesoft.swing.JideSwingUtilities;

public class ComponentUtils {

    public static void setVisible(Component container, boolean visible) {
        if (container instanceof Container) {
            for (Component component : ((Container) container).getComponents()) {
                component.setVisible(visible);
                if (component instanceof Container) {
                    setVisible(component, visible);
                }
            }
        }
        container.setVisible(visible);
    }

    public static void setEnabled(Component c, boolean enable) {
        JideSwingUtilities.setEnabledRecursively(c, enable);
    }

}
