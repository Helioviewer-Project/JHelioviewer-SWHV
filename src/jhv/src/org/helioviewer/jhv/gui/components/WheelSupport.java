package org.helioviewer.jhv.gui.components;

/*
 * @(#)SpinnerWheelSupport.java 7/28/2006
 *
 * Copyright 2002 - 2006 JIDE Software Inc. All rights reserved.
 */

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * A helper class to add mouse wheel support to JSpinner. You can call
 * {@link #installMouseWheelSupport(javax.swing.JSpinner)} to add the support
 * and {@link #uninstallMouseWheelSupport(JComponent)} to remove the support.
 */
public class WheelSupport {

    public static final String CLIENT_PROPERTY_MOUSE_WHEEL_LISTENER = "mouseWheelListener";
    protected static final String SPINNER_ACTION_NAME_INCREMENT = "increment";
    protected static final String SPINNER_ACTION_NAME_DECREMENT = "decrement";
    protected static final String SLIDER_ACTION_NAME_INCREMENT = "positiveUnitIncrement";
    protected static final String SLIDER_ACTION_NAME_DECREMENT = "negativeUnitIncrement";

    public static void installMouseWheelSupport(final JSpinner spinner) {
        MouseWheelListener l = new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (!spinner.isEnabled()) {
                    return;
                }

                int rotation = e.getWheelRotation();
                if (rotation < 0) {
                    Action action = spinner.getActionMap().get(SPINNER_ACTION_NAME_INCREMENT);
                    if (action != null) {
                        action.actionPerformed(new ActionEvent(e.getSource(), 0, SPINNER_ACTION_NAME_INCREMENT));
                    }
                } else if (rotation > 0) {
                    Action action = spinner.getActionMap().get(SPINNER_ACTION_NAME_DECREMENT);
                    if (action != null) {
                        action.actionPerformed(new ActionEvent(e.getSource(), 0, SPINNER_ACTION_NAME_DECREMENT));
                    }
                }
            }
        };
        spinner.addMouseWheelListener(l);
        spinner.putClientProperty(CLIENT_PROPERTY_MOUSE_WHEEL_LISTENER, l);
    }

    public static void installMouseWheelSupport(final JSlider slider) {
        MouseWheelListener l = new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (!slider.isEnabled()) {
                    return;
                }

                int rotation = e.getWheelRotation();
                if (rotation < 0) {
                    Action action = slider.getActionMap().get(SLIDER_ACTION_NAME_INCREMENT);
                    if (action != null) {
                        action.actionPerformed(new ActionEvent(e.getSource(), 0, SLIDER_ACTION_NAME_INCREMENT));
                    }
                } else if (rotation > 0) {
                    Action action = slider.getActionMap().get(SLIDER_ACTION_NAME_DECREMENT);
                    if (action != null) {
                        action.actionPerformed(new ActionEvent(e.getSource(), 0, SLIDER_ACTION_NAME_DECREMENT));
                    }
                }
            }
        };
        slider.addMouseWheelListener(l);
        slider.putClientProperty(CLIENT_PROPERTY_MOUSE_WHEEL_LISTENER, l);
    }

    public static <T extends JComponent> void uninstallMouseWheelSupport(final T component) {
        MouseWheelListener l = (MouseWheelListener) component.getClientProperty(CLIENT_PROPERTY_MOUSE_WHEEL_LISTENER);
        if (l != null) {
            component.removeMouseWheelListener(l);
        }
    }
}
