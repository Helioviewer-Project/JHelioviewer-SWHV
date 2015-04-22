package org.helioviewer.gl3d.gui;

import java.awt.DisplayMode;
import java.awt.event.MouseListener;

import javax.swing.JToggleButton;

import org.helioviewer.jhv.gui.components.TopToolBar;

/**
 * The ToolBar that is used for the 3D Mode.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 */
public class GL3DTopToolBar extends TopToolBar implements MouseListener {

    private static final long serialVersionUID = 1L;

    public enum SelectionMode {
        PAN, ZOOMBOX, ROTATE
    };

    private JToggleButton panButton;
    private JToggleButton rotateButton;
    private JToggleButton zoomBoxButton;

    /**
     * Default constructor.
     */
    public GL3DTopToolBar() {
        setRollover(true);

        createNewToolBar(SelectionMode.ROTATE);

        addMouseListener(this);
    }

    /**
     * Sets the active selection mode.
     *
     * @param mode
     *            Selection mode, can be either PAN, ZOOMBOX or FOCUS.
     */
    public void setActiveSelectionMode(SelectionMode mode) {
        switch (mode) {
        case PAN:
            panButton.doClick();
            break;
        case ROTATE:
            rotateButton.doClick();
            break;
        case ZOOMBOX:
            zoomBoxButton.doClick();
            break;
        }
    }

    /**
     * (Re)creates the toolbar.
     *
     * This function is called during the construction of this panel as well as
     * after the display mode has changed.
     *
     * @param selectionMode
     *            Current selection mode, to select the correct button.
     * @see #setDisplayMode(DisplayMode)
     */
    protected void createNewToolBar(SelectionMode selectionMode) {
    }

}
