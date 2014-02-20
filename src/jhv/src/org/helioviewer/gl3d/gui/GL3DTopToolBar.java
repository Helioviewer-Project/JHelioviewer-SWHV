package org.helioviewer.gl3d.gui;

import java.awt.DisplayMode;
import java.awt.event.MouseListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JToggleButton;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.actions.View2DAction;
import org.helioviewer.jhv.gui.actions.View3DAction;
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

    // private enum DisplayMode {
    // ICONANDTEXT, ICONONLY, TEXTONLY
    // };

    // private DisplayMode displayMode;

    private JToggleButton panButton;
    private JToggleButton rotateButton;
    private JToggleButton zoomBoxButton;

    private JToggleButton trackSolarRotationButton;
    private JToggleButton coronaVisibilityButton;
    private JToggleButton gridVisibilityButton;
    

    /**
     * Default constructor.
     */
    public GL3DTopToolBar() {
        setRollover(true);

        // try {
        // displayMode =
        // DisplayMode.valueOf(Settings.getSingletonInstance().getProperty("display.toolbar").toUpperCase());
        // } catch (Exception e) {
        // Log.error("Error when reading the display mode of the toolbar", e);
        // displayMode = DisplayMode.ICONANDTEXT;
        // }

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
        removeAll();

        // Zoom
        addButton(new JButton(new GL3DZoomInAction(false)));
        addButton(new JButton(new GL3DZoomOutAction(false)));
        addButton(new JButton(new GL3DZoomFitAction(false)));
        addButton(new JButton(new GL3DResetCameraAction()));
        // addButton(new JButton(new Zoom1to1Action(false)));

        addSeparator();

        // Selection
        ButtonGroup group = new ButtonGroup();

        panButton = new JToggleButton(new GL3DSetPanInteractionAction());
        panButton.setSelected(selectionMode == SelectionMode.PAN);
        panButton.setIcon(IconBank.getIcon(JHVIcon.PAN));
        panButton.setSelectedIcon(IconBank.getIcon(JHVIcon.PAN_SELECTED));
        panButton.setToolTipText("Select Panning");
        group.add(panButton);
        addButton(panButton);

        zoomBoxButton = new JToggleButton(new GL3DSetZoomBoxInteractionAction());
        zoomBoxButton.setSelected(selectionMode == SelectionMode.ZOOMBOX);
        zoomBoxButton.setIcon(IconBank.getIcon(JHVIcon.SELECT));
        zoomBoxButton.setSelectedIcon(IconBank.getIcon(JHVIcon.SELECT_SELECTED));
        zoomBoxButton.setToolTipText("Select Zoom Box");
        group.add(zoomBoxButton);
        addButton(zoomBoxButton);

        rotateButton = new JToggleButton(new GL3DSetRotationInteractionAction());
        rotateButton.setSelected(selectionMode == SelectionMode.ROTATE);
        rotateButton.setIcon(IconBank.getIcon(JHVIcon.ROTATE));
        rotateButton.setSelectedIcon(IconBank.getIcon(JHVIcon.ROTATE_SELECTED));
        rotateButton.setToolTipText("Select Rotating");
        group.add(rotateButton);
        addButton(rotateButton);

        addSeparator();

        boolean solarRotationWasEnabled = (trackSolarRotationButton != null && trackSolarRotationButton.isSelected());
        trackSolarRotationButton = new JToggleButton(new GL3DToggleSolarRotationAction());
        trackSolarRotationButton.setSelected(solarRotationWasEnabled);
        trackSolarRotationButton.setIcon(IconBank.getIcon(JHVIcon.FOCUS));
        trackSolarRotationButton.setSelectedIcon(IconBank.getIcon(JHVIcon.FOCUS_SELECTED));
        trackSolarRotationButton.setToolTipText("Enable Solar Rotation Tracking");
        addButton(trackSolarRotationButton);

        // coronaVisibilityButton =
        coronaVisibilityButton = new JToggleButton(new GL3DToggleCoronaVisibilityAction());
        coronaVisibilityButton.setSelected(false);
        coronaVisibilityButton.setIcon(IconBank.getIcon(JHVIcon.LAYER_IMAGE));
        coronaVisibilityButton.setSelectedIcon(IconBank.getIcon(JHVIcon.LAYER_IMAGE_OFF));
        coronaVisibilityButton.setToolTipText("Toggle Corona Visibility");
        addButton(coronaVisibilityButton);
        
        gridVisibilityButton = new JToggleButton(new GL3DToggleGridVisibilityAction());
        gridVisibilityButton.setSelected(false);
        gridVisibilityButton.setIcon(IconBank.getIcon(JHVIcon.LAYER_IMAGE));
        gridVisibilityButton.setSelectedIcon(IconBank.getIcon(JHVIcon.LAYER_IMAGE_OFF));
        gridVisibilityButton.setToolTipText("Toggle Grid Visibility");
        addButton(gridVisibilityButton);
        // VSO Export - DEACTIVATED FOR NOW
        // addSeparator();
        // addButton(new JButton(new NewQueryAction(true)));

        addSeparator();

        ButtonGroup stateGroup = new ButtonGroup();
        view2d = new JToggleButton(new View2DAction());
        view2d.setIcon(IconBank.getIcon(JHVIcon.MODE_2D));
        view2d.setSelectedIcon(IconBank.getIcon(JHVIcon.MODE_2D_SELECTED));
        view2d.setText("2D");
        stateGroup.add(view2d);

        view3d = new JToggleButton(new View3DAction());
        view3d.setIcon(IconBank.getIcon(JHVIcon.MODE_3D));
        view3d.setSelectedIcon(IconBank.getIcon(JHVIcon.MODE_3D_SELECTED));
        view3d.setText("3D");
        view3d.setSelected(true);
        stateGroup.add(view3d);

        addButton(view2d);
        addButton(view3d);
        addSeparator();

        updateStateButtons();
    }

}
