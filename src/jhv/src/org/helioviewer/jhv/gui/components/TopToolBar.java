package org.helioviewer.jhv.gui.components;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.InteractionAnnotate.AnnotationMode;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.actions.ResetCameraAction;
import org.helioviewer.jhv.gui.actions.ZoomFitAction;
import org.helioviewer.jhv.gui.actions.ZoomInAction;
import org.helioviewer.jhv.gui.actions.ZoomOneToOneAction;
import org.helioviewer.jhv.gui.actions.ZoomOutAction;
import org.helioviewer.jhv.renderable.components.RenderableImageLayer;

/**
 * Toolbar containing the most common actions.
 *
 * <p>
 * The toolbar provides a context menu to change its appearance.
 *
 * @author Markus Langenberg
 * @author Andre Dau
 */
@SuppressWarnings("serial")
public class TopToolBar extends JToolBar implements MouseListener {

    private enum InteractionMode {
        PAN, ROTATE, ANNOTATE
    };

    private static InteractionMode interactionMode;
    private static DisplayMode displayMode;

    private enum DisplayMode {
        ICONANDTEXT, ICONONLY, TEXTONLY
    };

    private JToggleButton panButton;
    private JToggleButton rotateButton;
    private JToggleButton annotateButton;

    private JToggleButton trackSolarRotationButton;
    private JToggleButton coronaVisibilityButton;

    protected ArrayList<JToggleButton> pluginList = new ArrayList<JToggleButton>();

    public TopToolBar() {
        setRollover(true);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

        try {
            displayMode = DisplayMode.valueOf(Settings.getSingletonInstance().getProperty("display.toolbar").toUpperCase());
        } catch (Exception e) {
            Log.error("Error when reading the display mode of the toolbar", e);
            displayMode = DisplayMode.ICONANDTEXT;
        }

        try {
            interactionMode = InteractionMode.valueOf(Settings.getSingletonInstance().getProperty("display.interaction").toUpperCase());
        } catch (Exception e) {
            Log.error("Error when reading the interaction mode", e);
            interactionMode = InteractionMode.ROTATE;
        }

        createNewToolBar();
        addMouseListener(this);
    }

    private void setActiveInteractionMode(InteractionMode mode) {
        interactionMode = mode;
        Settings.getSingletonInstance().setProperty("display.interaction", mode.toString().toLowerCase());
        Settings.getSingletonInstance().save();

        Camera camera = Displayer.getViewport().getCamera();
        switch (mode) {
            case PAN:
                camera.setCurrentInteraction(camera.getPanInteraction());
                panButton.setSelected(true);
                break;
            case ROTATE:
                camera.setCurrentInteraction(camera.getRotateInteraction());
                rotateButton.setSelected(true);
                break;
            case ANNOTATE:
                camera.setCurrentInteraction(camera.getAnnotateInteraction());
                annotateButton.setSelected(true);
                break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        maybeShowPopup(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseEntered(MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseExited(MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mousePressed(MouseEvent e) {
        maybeShowPopup(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseReleased(MouseEvent e) {
    }

    /**
     * (Re)creates the toolbar.
     *
     * This function is called during the construction of this panel as well as
     * after the display mode has changed.
     *
     * @param InteractionMode
     *            Current interaction mode
     * @see #setDisplayMode(DisplayMode)
     */
    protected void createNewToolBar() {
        removeAll();

        // Zoom
        addButton(new JButton(new ZoomInAction(false, true)));
        addButton(new JButton(new ZoomOutAction(false, true)));
        addButton(new JButton(new ZoomFitAction(false, true)));
        addButton(new JButton(new ZoomOneToOneAction(false, true)));
        addButton(new JButton(new ResetCameraAction(false, true)));

        addSeparator();

        // Interaction
        ButtonGroup group = new ButtonGroup();

        panButton = new JToggleButton(new AbstractAction("Pan") {
            @Override
            public void actionPerformed(ActionEvent e) {
                setActiveInteractionMode(InteractionMode.PAN);
            }
        });
        panButton.setIcon(IconBank.getIcon(JHVIcon.PAN));
        panButton.setSelectedIcon(IconBank.getIcon(JHVIcon.PAN_SELECTED));
        panButton.setToolTipText("Pan");
        group.add(panButton);
        addButton(panButton);

        rotateButton = new JToggleButton(new AbstractAction("Rotate") {
            @Override
            public void actionPerformed(ActionEvent e) {
                setActiveInteractionMode(InteractionMode.ROTATE);
            }
        });
        rotateButton.setIcon(IconBank.getIcon(JHVIcon.ROTATE));
        rotateButton.setSelectedIcon(IconBank.getIcon(JHVIcon.ROTATE_SELECTED));
        rotateButton.setToolTipText("Rotate");
        group.add(rotateButton);
        addButton(rotateButton);

        annotateButton = new JToggleButton(new AbstractAction("Annotate") {
            @Override
            public void actionPerformed(ActionEvent e) {
                setActiveInteractionMode(InteractionMode.ANNOTATE);
            }
        });
        annotateButton.setIcon(IconBank.getIcon(JHVIcon.SELECT));
        annotateButton.setSelectedIcon(IconBank.getIcon(JHVIcon.SELECT_SELECTED));
        annotateButton.setToolTipText("Annotate");
        group.add(annotateButton);
        addButton(annotateButton);

        setActiveInteractionMode(interactionMode);

        final JPopupMenu annotatePopup = new JPopupMenu();
        ButtonGroup annotateGroup = new ButtonGroup();

        JRadioButtonMenuItem rectangleItem = new JRadioButtonMenuItem(new AbstractAction("Rectangle") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Displayer.getViewport().getCamera().getAnnotateInteraction().setMode(AnnotationMode.RECTANGLE);
            }
        });
        annotatePopup.add(rectangleItem);
        annotateGroup.add(rectangleItem);
        rectangleItem.setSelected(true);

        JRadioButtonMenuItem circleItem = new JRadioButtonMenuItem(new AbstractAction("Circle") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Displayer.getViewport().getCamera().getAnnotateInteraction().setMode(AnnotationMode.CIRCLE);
            }
        });
        annotatePopup.add(circleItem);
        annotateGroup.add(circleItem);

        JRadioButtonMenuItem crossItem = new JRadioButtonMenuItem(new AbstractAction("Cross") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Displayer.getViewport().getCamera().getAnnotateInteraction().setMode(AnnotationMode.CROSS);
            }
        });
        annotatePopup.add(crossItem);
        annotateGroup.add(crossItem);

        annotateButton.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                annotatePopup.show(e.getComponent(), 0, e.getComponent().getHeight());
            }
        });

        addSeparator();

        trackSolarRotationButton = new JToggleButton(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Camera camera = Displayer.getViewport().getCamera();
                camera.setTrackingMode(!camera.getTrackingMode());
            }
        });
        trackSolarRotationButton.setSelected(false);
        trackSolarRotationButton.setIcon(IconBank.getIcon(JHVIcon.FOCUS));
        trackSolarRotationButton.setSelectedIcon(IconBank.getIcon(JHVIcon.FOCUS_SELECTED));
        trackSolarRotationButton.setToolTipText("Track solar rotation");
        addButton(trackSolarRotationButton);

        coronaVisibilityButton = new JToggleButton(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RenderableImageLayer.toggleCorona();
                Displayer.display();
            }
        });
        coronaVisibilityButton.setSelected(false);
        coronaVisibilityButton.setIcon(IconBank.getIcon(JHVIcon.LAYER_IMAGE));
        coronaVisibilityButton.setSelectedIcon(IconBank.getIcon(JHVIcon.LAYER_IMAGE_OFF));
        coronaVisibilityButton.setToolTipText("Toggle off-disk corona");
        addButton(coronaVisibilityButton);

        addSeparator();

        for (JToggleButton button : this.pluginList) {
            if (displayMode == DisplayMode.ICONANDTEXT)
                this.add(button);
            else if (displayMode == DisplayMode.TEXTONLY)
                this.add(new JToggleButton(button.getText()));
            else
                this.add(new JToggleButton(button.getIcon()));
        }
    }

    /**
     * Adds a given button to the toolbar.
     *
     * This function sets some standard values of the button regarding the
     * appearance. The current display mode is taken into account.
     *
     * @param button
     *            Button to add
     */
    public void addButton(AbstractButton button) {
        // button.setMargin(buttonMargin);
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.addMouseListener(this);

        switch (displayMode) {
        case TEXTONLY:
            button.setIcon(null);
            break;
        case ICONONLY:
            button.setText(null);
            break;
        }

        add(button);
    }

    /**
     * Sets the current display mode.
     *
     * This changes the way the toolbar is display.
     *
     * @param mode
     *            Display mode can be either ICONANDTEXT, ICONONLY or TEXTONLY.
     */
    public void setDisplayMode(DisplayMode mode) {
        DisplayMode oldDisplayMode = displayMode;
        if (mode != null) {
            displayMode = mode;
            Settings.getSingletonInstance().setProperty("display.toolbar", mode.toString().toLowerCase());
            Settings.getSingletonInstance().save();
        }

        createNewToolBar();
        firePropertyChange("displayMode", oldDisplayMode, displayMode);
        revalidate();
    }

    public void addToolbarPlugin(JToggleButton button) {
        pluginList.add(button);
    }

    public void removeToolbarPlugin(AbstractButton button) {
        pluginList.remove(button);
    }

    /**
     * Shows the popup if the correct mouse button was pressed.
     *
     * @param e
     *            MouseEvent that triggered the event
     */
    protected void maybeShowPopup(MouseEvent e) {
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu popUpMenu = new JPopupMenu();
            ButtonGroup group = new ButtonGroup();

            JRadioButtonMenuItem iconAndText = new JRadioButtonMenuItem("Icon and Text", displayMode == DisplayMode.ICONANDTEXT);
            iconAndText.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setDisplayMode(DisplayMode.ICONANDTEXT);
                }
            });
            group.add(iconAndText);
            popUpMenu.add(iconAndText);

            JRadioButtonMenuItem iconOnly = new JRadioButtonMenuItem("Icon Only", displayMode == DisplayMode.ICONONLY);
            iconOnly.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setDisplayMode(DisplayMode.ICONONLY);
                }
            });
            group.add(iconOnly);
            popUpMenu.add(iconOnly);

            JRadioButtonMenuItem textOnly = new JRadioButtonMenuItem("Text Only", displayMode == DisplayMode.TEXTONLY);
            textOnly.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setDisplayMode(DisplayMode.TEXTONLY);
                }
            });
            group.add(textOnly);
            popUpMenu.add(textOnly);

            popUpMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

}
