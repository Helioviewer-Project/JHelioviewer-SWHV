package org.helioviewer.jhv.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.LayoutManager;

import javax.swing.JComponent;
import javax.swing.JPanel;

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

    public static void setClientProperty(Component container, String property, String value) {
        if (container instanceof Container) {
            Component[] components = ((Container) container).getComponents();
            for (Component component : components) {
                if (component instanceof JComponent)
                    ((JComponent) component).putClientProperty(property, value);
                if (component instanceof Container) {
                    setClientProperty(component, property, value);
                }
            }
        }

        if (container instanceof JComponent)
            ((JComponent) container).putClientProperty(property, value);
    }

    @SuppressWarnings("serial")
    public static class SmallPanel extends JPanel {

        public SmallPanel() {
            super();
        }

        public SmallPanel(LayoutManager layout) {
            super(layout);
        }

        public void setSmall() {
            setClientProperty(this, "JComponent.sizeVariant", "small");
        }
    }

}
