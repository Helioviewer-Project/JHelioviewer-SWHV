package org.helioviewer.jhv.gui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
//import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
//import java.util.LinkedHashMap;
//import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToolBar;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.camera.Interaction;
import org.helioviewer.jhv.camera.InteractionAnnotate.AnnotationMode;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.actions.ClearAnnotationsAction;
import org.helioviewer.jhv.gui.actions.ResetCameraAction;
import org.helioviewer.jhv.gui.actions.SDOCutOutAction;
import org.helioviewer.jhv.gui.actions.ZoomFOVAnnotationAction;
import org.helioviewer.jhv.gui.actions.ZoomFitAction;
import org.helioviewer.jhv.gui.actions.ZoomInAction;
import org.helioviewer.jhv.gui.actions.ZoomOneToOneAction;
import org.helioviewer.jhv.gui.actions.ZoomOutAction;
import org.helioviewer.jhv.gui.components.base.JHVButton;
import org.helioviewer.jhv.gui.components.base.JHVToggleButton;
import org.helioviewer.jhv.io.SampClient;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.layers.MovieDisplay;

import com.jidesoft.swing.JideButton;

@SuppressWarnings("serial")
public class ToolBar extends JToolBar {

    private static DisplayMode displayMode = DisplayMode.ICONANDTEXT;

    private enum DisplayMode {
        ICONANDTEXT, ICONONLY
    }

    static class ButtonText {

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

    private final ButtonText SAMP = new ButtonText(Buttons.samp, "SAMP", "Send SAMP message");
    private final ButtonText CUTOUT = new ButtonText(Buttons.cutOut, "SDO Cut-out", "SDO cut-out service");
    private final ButtonText ANNOTATE = new ButtonText(Buttons.annotate, "Annotate", "Annotate");
    private final ButtonText PROJECTION = new ButtonText(Buttons.projection, "Projection", "Projection");
    private final ButtonText OFFDISK = new ButtonText(Buttons.offDisk, "Corona", "Toggle off-disk corona");
    private final ButtonText TRACK = new ButtonText(Buttons.track, "Track", "Track solar rotation");
    private final ButtonText AXIS = new ButtonText(Buttons.axis, "Axis", "Axis");
    private final ButtonText ROTATE = new ButtonText(Buttons.rotate, "Rotate", "Rotate");
    private final ButtonText PAN = new ButtonText(Buttons.pan, "Pan", "Pan");
    private final ButtonText MULTIVIEW = new ButtonText(Buttons.multiview, "Multiview", "Multiview");
    private final ButtonText RESETCAMERA = new ButtonText(Buttons.resetCamera, "Reset Camera", "Reset camera position to default");
    private final ButtonText ZOOMONE = new ButtonText(Buttons.zoomOne, "Actual Size", "Zoom to native resolution");
    private final ButtonText ZOOMFIT = new ButtonText(Buttons.zoomFit, "Zoom to Fit", "Zoom to fit");
    private final ButtonText ZOOMOUT = new ButtonText(Buttons.zoomOut, "Zoom Out", "Zoom out");
    private final ButtonText ZOOMIN = new ButtonText(Buttons.zoomIn, "Zoom In", "Zoom in");

    private enum InteractionMode {
        PAN(JHVFrame.getPanInteraction()), ROTATE(JHVFrame.getRotateInteraction()), AXIS(JHVFrame.getAxisInteraction());

        final Interaction interaction;

        InteractionMode(Interaction _interaction) {
            interaction = _interaction;
        }
    }

//  private final LinkedHashMap<ButtonText, ActionListener> pluginButtons = new LinkedHashMap<>();

    private static JHVButton toolButton(ButtonText text) {
        JHVButton b = new JHVButton(text.toString());
        b.setToolTipText(text.tip);
        return b;
    }

    private static JHVToggleButton toolToggleButton(ButtonText text) {
        JHVToggleButton b = new JHVToggleButton(text.toString());
        b.setToolTipText(text.tip);
        return b;
    }

    public ToolBar() {
        setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
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
            displayMode = DisplayMode.valueOf(Settings.getProperty("display.toolbar").toUpperCase());
        } catch (Exception ignore) {
        }
        setDisplayMode(displayMode);
    }

    private JHVToggleButton multiviewButton;
    private JHVToggleButton coronaButton;
    private JHVToggleButton trackingButton;

    public JHVToggleButton getMultiviewButton() {
        return multiviewButton;
    }

    public JHVToggleButton getShowCoronaButton() {
        return coronaButton;
    }

    public JHVToggleButton getTrackingButton() {
        return trackingButton;
    }

    private void createNewToolBar() {
        InteractionMode interactionMode = InteractionMode.ROTATE;
        try {
            interactionMode = InteractionMode.valueOf(Settings.getProperty("display.interaction").toUpperCase());
        } catch (Exception ignore) {
        }

        Dimension dim = new Dimension(32, 32);

        // Zoom
        JHVButton zoomIn = toolButton(ZOOMIN);
        zoomIn.addActionListener(new ZoomInAction());
        JHVButton zoomOut = toolButton(ZOOMOUT);
        zoomOut.addActionListener(new ZoomOutAction());
        JHVButton zoomFit = toolButton(ZOOMFIT);
        zoomFit.addActionListener(new ZoomFitAction());
        JHVButton zoomOne = toolButton(ZOOMONE);
        zoomOne.addActionListener(new ZoomOneToOneAction());
        JHVButton resetCamera = toolButton(RESETCAMERA);
        resetCamera.addActionListener(new ResetCameraAction());

        addButton(zoomIn);
        addButton(zoomOut);
        addButton(zoomFit);
        addButton(zoomOne);
        addButton(resetCamera);

        add(new JToolBar.Separator(dim));

        // Interaction
        ButtonGroup group = new ButtonGroup();

        JHVToggleButton pan = toolToggleButton(PAN);
        pan.addActionListener(e -> setActiveInteractionMode(InteractionMode.PAN));
        JHVToggleButton rotate = toolToggleButton(ROTATE);
        rotate.addActionListener(e -> setActiveInteractionMode(InteractionMode.ROTATE));
        JHVToggleButton axis = toolToggleButton(AXIS);
        axis.addActionListener(e -> setActiveInteractionMode(InteractionMode.AXIS));

        group.add(pan);
        group.add(rotate);
        group.add(axis);
        addButton(pan);
        addButton(rotate);
        addButton(axis);

        switch (interactionMode) {
            case PAN:
                pan.setSelected(true);
                break;
            case AXIS:
                axis.setSelected(true);
                break;
            default:
                rotate.setSelected(true);
        }
        setActiveInteractionMode(interactionMode);

        add(new JToolBar.Separator(dim));

        trackingButton = toolToggleButton(TRACK);
        trackingButton.setSelected(Display.getCamera().getTrackingMode());
        trackingButton.addItemListener(e -> Display.getCamera().setTrackingMode(trackingButton.isSelected()));
        addButton(trackingButton);

        coronaButton = toolToggleButton(OFFDISK);
        coronaButton.setSelected(Display.getShowCorona());
        coronaButton.addItemListener(e -> {
            Display.setShowCorona(coronaButton.isSelected());
            MovieDisplay.display();
        });
        addButton(coronaButton);

        add(new JToolBar.Separator(dim));

        multiviewButton = toolToggleButton(MULTIVIEW);
        multiviewButton.setSelected(Display.multiview);
        multiviewButton.addItemListener(e -> {
            Display.multiview = multiviewButton.isSelected();
            ImageLayers.arrangeMultiView(Display.multiview);
        });
        addButton(multiviewButton);

        JHVButton projectionButton = toolButton(PROJECTION);
        addButton(projectionButton);

        JPopupMenu projectionPopup = new JPopupMenu();
        ButtonGroup projectionGroup = new ButtonGroup();
        for (Display.DisplayMode el : Display.DisplayMode.values()) {
            projectionPopup.add(el.radio);
            projectionGroup.add(el.radio);
        }
        Display.DisplayMode.Orthographic.radio.setSelected(true);

        projectionButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                projectionPopup.show(e.getComponent(), 0, e.getComponent().getHeight());
            }
        });

        JHVButton anotateButton = toolButton(ANNOTATE);
        addButton(anotateButton);

        JPopupMenu annotatePopup = new JPopupMenu();
        ButtonGroup annotateGroup = new ButtonGroup();
        for (AnnotationMode mode : AnnotationMode.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(mode.toString());
            if (mode == AnnotationMode.Rectangle)
                item.setSelected(true);
            item.addActionListener(e -> JHVFrame.getAnnotateInteraction().setMode(mode));
            annotateGroup.add(item);
            annotatePopup.add(item);
        }

        annotatePopup.addSeparator();
        annotatePopup.add(new ClearAnnotationsAction());
        annotatePopup.addSeparator();
        annotatePopup.add(new ZoomFOVAnnotationAction());

        anotateButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                annotatePopup.show(e.getComponent(), 0, e.getComponent().getHeight());
            }
        });

        add(new JToolBar.Separator(dim));

        JHVButton cutOut = toolButton(CUTOUT);
        cutOut.addActionListener(new SDOCutOutAction());
        addButton(cutOut);

        if (Boolean.parseBoolean(Settings.getProperty("startup.sampHub"))) {
            JHVButton samp = toolButton(SAMP);
            samp.addActionListener(e -> SampClient.notifyRequestData());
            addButton(samp);
        }

        add(new JToolBar.Separator(dim));

/*
        for (Map.Entry<ButtonText, ActionListener> entry : pluginButtons.entrySet()) {
            JHVButton b = toolButton(entry.getKey());
            b.addActionListener(entry.getValue());
            addButton(b);
        }
*/  }

    private static void setActiveInteractionMode(InteractionMode mode) {
        Settings.setProperty("display.interaction", mode.toString());
        JHVFrame.setCurrentInteraction(mode.interaction);
    }

    private void addButton(JideButton b) {
        b.setFocusPainted(false);
        add(b);
    }

    private void setDisplayMode(DisplayMode mode) {
        displayMode = mode;
        Settings.setProperty("display.toolbar", mode.toString().toLowerCase());
        recreate();
    }

    private void recreate() {
        removeAll();
        createNewToolBar();
        revalidate();
        repaint();
    }
/*
    public void addPluginButton(ButtonText text, ActionListener a) {
        pluginButtons.put(text, a);
        recreate();
    }

    public void removePluginButton(ButtonText text) {
        pluginButtons.remove(text);
        recreate();
    }
*/
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
