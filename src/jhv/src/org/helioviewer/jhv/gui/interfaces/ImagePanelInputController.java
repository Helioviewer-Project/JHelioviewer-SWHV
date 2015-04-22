package org.helioviewer.jhv.gui.interfaces;

import java.awt.event.MouseWheelListener;

import javax.swing.event.MouseInputListener;

/**
 * Interface representing an input controller for an image panel.
 * 
 * <p>
 * There can only be one input controller attached to an image panel at a time.
 * It receives all mouse event from the panel.
 * 
 * <p>
 * For further informations, see {@link ImagePanelPlugin}
 * {@link org.helioviewer.jhv.gui.components.MainImagePanel}.
 * 
 */
public interface ImagePanelInputController extends MouseInputListener, MouseWheelListener, ImagePanelPlugin {

}
