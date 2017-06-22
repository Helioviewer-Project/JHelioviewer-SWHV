package org.helioviewer.jhv.gui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToolBar;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.camera.Interaction;
import org.helioviewer.jhv.camera.InteractionAnnotate.AnnotationMode;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.actions.ClearAnnotationsAction;
import org.helioviewer.jhv.gui.actions.ResetCameraAction;
import org.helioviewer.jhv.gui.actions.SDOCutOutAction;
import org.helioviewer.jhv.gui.actions.ZoomFitAction;
import org.helioviewer.jhv.gui.actions.ZoomInAction;
import org.helioviewer.jhv.gui.actions.ZoomOneToOneAction;
import org.helioviewer.jhv.gui.actions.ZoomOutAction;

import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideToggleButton;

@SuppressWarnings("serial")
public class TopToolBar extends JToolBar {

    private static DisplayMode displayMode = DisplayMode.ICONANDTEXT;

    private enum DisplayMode {
        ICONANDTEXT, ICONONLY
    }

    private enum ButtonText {
        CUTOUT(Buttons.cutOut, "SDO Cut-out", "SDO cut-out service"),
        PROJECTION(Buttons.projection, "Projection", "Projection"),
        OFFDISK(Buttons.offDisk, "Corona", "Toggle off-disk corona"), TRACK(Buttons.track, "Track", "Track solar rotation"),
        ANNOTATE(Buttons.annotate, "Annotate", "Annotate"), ROTATE(Buttons.rotate, "Rotate", "Rotate"), PAN(Buttons.pan, "Pan", "Pan"),
        RESETCAMERA(Buttons.resetCamera, "Reset Camera", "Reset camera position to default"),
        ZOOMONE(Buttons.zoomOne, "Actual Size", "Zoom to native resolution"), ZOOMFIT(Buttons.zoomFit, "Zoom to Fit", "Zoom to fit"),
        ZOOMOUT(Buttons.zoomOut, "Zoom Out", "Zoom out"), ZOOMIN(Buttons.zoomIn, "Zoom In", "Zoom in");

        private final String icon;
        private final String text;
        final String tip;

        ButtonText(String _icon, String _text, String _tip) {
            icon = _icon;
            text = _text;
            tip = _tip;
        }

        @Override
        public String toString() {
            return displayMode == DisplayMode.ICONONLY ? icon : icon + "<br/>" + text;
        }
    }

    private enum InteractionMode {
        PAN(ImageViewerGui.getPanInteraction()), ROTATE(ImageViewerGui.getRotateInteraction()), ANNOTATE(ImageViewerGui.getAnnotateInteraction());

        final Interaction interaction;

        InteractionMode(Interaction _interaction) {
            interaction = _interaction;
        }
    }

    private static JideButton toolButton(ButtonText text) {
        JideButton b = new JideButton(text.toString());
        b.setToolTipText(text.tip);
        return b;
    }

    private static JideToggleButton toolToggleButton(ButtonText text) {
        JideToggleButton b = new JideToggleButton(text.toString());
        b.setToolTipText(text.tip);
        return b;
    }

    public TopToolBar() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
        setRollover(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }
        });

        try {
            displayMode = DisplayMode.valueOf(Settings.getSingletonInstance().getProperty("display.toolbar").toUpperCase());
        } catch (Exception ignore) {
        }
        setDisplayMode(displayMode);
    }

    private JideToggleButton coronaButton;
    private JideToggleButton trackingButton;

    public JideToggleButton getShowCoronaButton() {
        return coronaButton;
    }

    public JideToggleButton getTrackingButton() {
        return trackingButton;
    }

    private void createNewToolBar() {
        InteractionMode interactionMode = InteractionMode.ROTATE;
        try {
            interactionMode = InteractionMode.valueOf(Settings.getSingletonInstance().getProperty("display.interaction").toUpperCase());
        } catch (Exception ignore) {
        }

        Dimension dim = new Dimension(32, 32);

        // Zoom
        JideButton zoomIn = toolButton(ButtonText.ZOOMIN);
        zoomIn.addActionListener(new ZoomInAction());
        JideButton zoomOut = toolButton(ButtonText.ZOOMOUT);
        zoomOut.addActionListener(new ZoomOutAction());
        JideButton zoomFit = toolButton(ButtonText.ZOOMFIT);
        zoomFit.addActionListener(new ZoomFitAction());
        JideButton zoomOne = toolButton(ButtonText.ZOOMONE);
        zoomOne.addActionListener(new ZoomOneToOneAction());
        JideButton resetCamera = toolButton(ButtonText.RESETCAMERA);
        resetCamera.addActionListener(new ResetCameraAction());

        addButton(zoomIn);
        addButton(zoomOut);
        addButton(zoomFit);
        addButton(zoomOne);
        addButton(resetCamera);

        add(new JToolBar.Separator(dim));

        // Interaction
        ButtonGroup group = new ButtonGroup();

        JideToggleButton pan = toolToggleButton(ButtonText.PAN);
        pan.addActionListener(e -> setActiveInteractionMode(InteractionMode.PAN));
        JideToggleButton rotate = toolToggleButton(ButtonText.ROTATE);
        rotate.addActionListener(e -> setActiveInteractionMode(InteractionMode.ROTATE));
        JideToggleButton annotate = toolToggleButton(ButtonText.ANNOTATE);
        annotate.addActionListener(e -> setActiveInteractionMode(InteractionMode.ANNOTATE));

        group.add(pan);
        group.add(rotate);
        group.add(annotate);
        addButton(pan);
        addButton(rotate);
        addButton(annotate);

        switch (interactionMode) {
            case PAN:
                pan.setSelected(true);
                break;
            case ANNOTATE:
                annotate.setSelected(true);
                break;
            default:
                rotate.setSelected(true);
        }
        setActiveInteractionMode(interactionMode);

        JPopupMenu annotatePopup = new JPopupMenu();
        ButtonGroup annotateGroup = new ButtonGroup();

        for (AnnotationMode mode : AnnotationMode.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(mode.toString());
            if (mode == AnnotationMode.Rectangle)
                item.setSelected(true);
            item.addActionListener(e -> ImageViewerGui.getAnnotateInteraction().setMode(mode));
            annotateGroup.add(item);
            annotatePopup.add(item);
        }

        annotatePopup.addSeparator();
        annotatePopup.add(new ClearAnnotationsAction());

        annotate.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                annotatePopup.show(e.getComponent(), 0, e.getComponent().getHeight());
            }
        });

        add(new JToolBar.Separator(dim));

        trackingButton = toolToggleButton(ButtonText.TRACK);
        trackingButton.setSelected(Displayer.getCamera().getTrackingMode());
        trackingButton.addItemListener(e -> Displayer.getCamera().setTrackingMode(trackingButton.isSelected()));
        addButton(trackingButton);

        coronaButton = toolToggleButton(ButtonText.OFFDISK);
        coronaButton.setSelected(Displayer.getShowCorona());
        coronaButton.addItemListener(e -> {
            Displayer.setShowCorona(coronaButton.isSelected());
            Displayer.display();
        });
        addButton(coronaButton);

        add(new JToolBar.Separator(dim));

        JideButton projectionButton = toolButton(ButtonText.PROJECTION);
        addButton(projectionButton);

        JPopupMenu projectionPopup = new JPopupMenu();
        ButtonGroup projectionGroup = new ButtonGroup();
        for (Displayer.DisplayMode el : Displayer.DisplayMode.values()) {
            if (el == Displayer.DisplayMode.Orthographic)
                el.radio.setSelected(true);
            projectionPopup.add(el.radio);
            projectionGroup.add(el.radio);
        }

        projectionButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                projectionPopup.show(e.getComponent(), 0, e.getComponent().getHeight());
            }
        });

        add(new JToolBar.Separator(dim));

        JideButton cutOut = toolButton(ButtonText.CUTOUT);
        cutOut.addActionListener(new SDOCutOutAction());
        addButton(cutOut);
    }

    private static void setActiveInteractionMode(InteractionMode mode) {
        Settings.getSingletonInstance().setProperty("display.interaction", mode.toString());
        Settings.getSingletonInstance().save("display.interaction");
        ImageViewerGui.setCurrentInteraction(mode.interaction);
    }

    private void addButton(JideButton b) {
        b.setFocusPainted(false);
        add(b);
    }

    private void setDisplayMode(DisplayMode mode) {
        displayMode = mode;
        Settings.getSingletonInstance().setProperty("display.toolbar", mode.toString().toLowerCase());
        Settings.getSingletonInstance().save("display.toolbar");

        removeAll();
        createNewToolBar();
        revalidate();
    }

    private void maybeShowPopup(MouseEvent me) {
        if (me.isPopupTrigger() || me.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu popUpMenu = new JPopupMenu();
            ButtonGroup group = new ButtonGroup();

            JRadioButtonMenuItem iconAndText = new JRadioButtonMenuItem("Icon and Text", displayMode == DisplayMode.ICONANDTEXT);
            iconAndText.addActionListener(e -> setDisplayMode(DisplayMode.ICONANDTEXT));
            group.add(iconAndText);
            popUpMenu.add(iconAndText);

            JRadioButtonMenuItem iconOnly = new JRadioButtonMenuItem("Icon Only", displayMode == DisplayMode.ICONONLY);
            iconOnly.addActionListener(e -> setDisplayMode(DisplayMode.ICONONLY));
            group.add(iconOnly);
            popUpMenu.add(iconOnly);

            popUpMenu.show(me.getComponent(), me.getX(), me.getY());
        }
    }

}
