package org.helioviewer.jhv.gui;

import java.awt.Component;
import java.awt.Container;

import com.jidesoft.swing.JideSwingUtilities;

public class ComponentUtils {

    public static void setEnabled(Component container, boolean enable) {
        if (container instanceof Container) {
            Component[] components = ((Container) container).getComponents();
            for (Component component : components) {
                component.setEnabled(enable);
                if (component instanceof Container) {
                    setEnabled(component, enable);
                }
            }
        }
        container.setEnabled(enable);
    }

    public static void setVisible(Component container, boolean visible) {
        if (container instanceof Container) {
            Component[] components = ((Container) container).getComponents();
            for (Component component : components) {
                component.setVisible(visible);
                if (component instanceof Container) {
                    setVisible(component, visible);
                }
            }
        }
        container.setVisible(visible);
    }

    public static void smallVariant(Component c) {
        JideSwingUtilities.putClientPropertyRecursively(c, "JComponent.sizeVariant", "small");
    }

}
