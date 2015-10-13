package org.helioviewer.jhv.gui;

import java.awt.Component;
import java.awt.Container;

public class ComponentUtils {

    public static void enableComponents(Component container, boolean enable) {
        if (container instanceof Container) {
            Component[] components = ((Container) container).getComponents();
            for (Component component : components) {
                component.setEnabled(enable);
                if (component instanceof Container) {
                    enableComponents(component, enable);
                }
            }
        }
        container.setEnabled(enable);
    }

}
