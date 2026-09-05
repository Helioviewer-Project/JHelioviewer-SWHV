package org.helioviewer.jhv.gui.component;

/*
 * @(#)SpinnerWheelSupport.java 7/28/2006
 *
 * Copyright 2002 - 2006 JIDE Software Inc. All rights reserved.
 */

import java.awt.event.ActionEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.JSpinner;

/**
 * A helper class to add mouse wheel support to JSpinner. You can call
 * {@link #installMouseWheelSupport(JSpinner)} to add the support
 * and {@link #uninstallMouseWheelSupport(JComponent)} to remove the support.
 */
final class WheelSupport {

    private static final String CLIENT_PROPERTY_MOUSE_WHEEL_LISTENER = "mouseWheelListener";
    private static final String SPINNER_ACTION_NAME_INCREMENT = "increment";
    private static final String SPINNER_ACTION_NAME_DECREMENT = "decrement";
    private static final String SLIDER_ACTION_NAME_INCREMENT = "positiveUnitIncrement";
    private static final String SLIDER_ACTION_NAME_DECREMENT = "negativeUnitIncrement";

    static void installMouseWheelSupport(JSpinner spinner) {
        install(spinner, SPINNER_ACTION_NAME_INCREMENT, SPINNER_ACTION_NAME_DECREMENT);
    }

    static void installMouseWheelSupport(JSlider slider) {
        install(slider, SLIDER_ACTION_NAME_INCREMENT, SLIDER_ACTION_NAME_DECREMENT);
    }

    private static void install(JComponent component, String increment, String decrement) {
        MouseWheelListener l = e -> {
            if (!component.isEnabled()) {
                return;
            }

            int rotation = e.getWheelRotation();
            if (rotation == 0) {
                return;
            }
            String actionName = rotation < 0 ? increment : decrement;
            Action action = component.getActionMap().get(actionName);
            if (action != null) {
                action.actionPerformed(new ActionEvent(e.getSource(), 0, actionName));
            }
        };
        component.addMouseWheelListener(l);
        component.putClientProperty(CLIENT_PROPERTY_MOUSE_WHEEL_LISTENER, l);
    }

    static <T extends JComponent> void uninstallMouseWheelSupport(T component) {
        MouseWheelListener l = (MouseWheelListener) component.getClientProperty(CLIENT_PROPERTY_MOUSE_WHEEL_LISTENER);
        if (l != null) {
            component.removeMouseWheelListener(l);
        }
    }

    private WheelSupport() {}
}
