package org.helioviewer.jhv.gui.components;

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
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Interaction;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.actions.ClearAnnotationsAction;
import org.helioviewer.jhv.gui.actions.ResetCameraAction;
import org.helioviewer.jhv.gui.actions.ResetCameraAxisAction;
import org.helioviewer.jhv.gui.actions.SDOCutOutAction;
import org.helioviewer.jhv.gui.actions.ZoomFOVAnnotationAction;
import org.helioviewer.jhv.gui.actions.ZoomFitAction;
import org.helioviewer.jhv.gui.actions.ZoomInAction;
import org.helioviewer.jhv.gui.actions.ZoomOneToOneAction;
import org.helioviewer.jhv.gui.actions.ZoomOutAction;
import org.helioviewer.jhv.io.SampClient;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.layers.MovieDisplay;

import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideToggleButton;

@SuppressWarnings("serial")
public class ToolBar extends JToolBar {

    private static DisplayMode displayMode = DisplayMode.ICONANDTEXT;

    private enum DisplayMode {
        ICONANDTEXT, ICONONLY
    }

    private record ButtonText(String icon, String text, String tip) {
        @Override
        public String toString() {
            return displayMode == DisplayMode.ICONONLY ? icon : icon + "<br/>" + text;
        }
    }

    private final ButtonText ANNOTATION = new ButtonText(Buttons.annotate, "Annotation", "Annotation (Press Shift to draw)");
    private final ButtonText AXIS = new ButtonText(Buttons.axis, "Axis", "Axis");
    private final ButtonText CUTOUT = new ButtonText(Buttons.cutOut, "SDO Cut-out", "Send layers to SDO cut-out service");
    private final ButtonText DIFFROTATION = new ButtonText(Buttons.diffRotation, "Differential", "Toggle differential rotation");
    private final ButtonText MULTIVIEW = new ButtonText(Buttons.multiview, "Multiview", "Multiview");
    private final ButtonText OFFDISK = new ButtonText(Buttons.offDisk, "Corona", "Toggle off-disk corona");
    private final ButtonText PAN = new ButtonText(Buttons.pan, "Pan", "Pan");
    private final ButtonText PROJECTION = new ButtonText(Buttons.projection, "Projection", "Projection");
    private final ButtonText RESETCAMERA = new ButtonText(Buttons.resetCamera, "Reset Camera", "Reset camera position to default");
    private final ButtonText RESETCAMERAAXIS = new ButtonText(Buttons.resetCameraAxis, "Reset Camera Axis", "Reset camera axis");
    private final ButtonText ROTATE = new ButtonText(Buttons.rotate, "Rotate", "Rotate");
    private final ButtonText SAMP = new ButtonText(Buttons.samp, "SAMP", "Send SAMP message");
    private final ButtonText TRACK = new ButtonText(Buttons.track, "Track", "Track solar rotation");
    private final ButtonText ZOOMFIT = new ButtonText(Buttons.zoomFit, "Zoom-Fit", "Zoom to fit");
    private final ButtonText ZOOMIN = new ButtonText(Buttons.zoomIn, "Zoom In", "Zoom in");
    private final ButtonText ZOOMONE = new ButtonText(Buttons.zoomOne, "Actual Size", "Zoom to native resolution");
    private final ButtonText ZOOMOUT = new ButtonText(Buttons.zoomOut, "Zoom Out", "Zoom out");

//  private final LinkedHashMap<ButtonText, ActionListener> pluginButtons = new LinkedHashMap<>();

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

    public ToolBar() {
        setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Colors.lightGray));
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

    private JideToggleButton coronaButton;
    private JideToggleButton diffRotationButton;
    private JideToggleButton multiviewButton;
    private JideToggleButton trackingButton;

    public JideToggleButton getShowCoronaButton() {
        return coronaButton;
    }

    public JideToggleButton getDiffRotationButton() {
        return diffRotationButton;
    }

    public JideToggleButton getMultiviewButton() {
        return multiviewButton;
    }

    public JideToggleButton getTrackingButton() {
        return trackingButton;
    }

    private void createNewToolBar() {
        Interaction.Mode interactionMode = JHVFrame.getInteraction().getMode();
        try {
            interactionMode = Interaction.Mode.valueOf(Settings.getProperty("display.interaction").toUpperCase());
        } catch (Exception ignore) {
        }

        Dimension dim = new Dimension(32, 32);

        // Zoom
        JideButton zoomIn = toolButton(ZOOMIN);
        zoomIn.addActionListener(new ZoomInAction());
        JideButton zoomOut = toolButton(ZOOMOUT);
        zoomOut.addActionListener(new ZoomOutAction());
        JideButton zoomFit = toolButton(ZOOMFIT);
        zoomFit.addActionListener(new ZoomFitAction());
        JideButton zoomOne = toolButton(ZOOMONE);
        zoomOne.addActionListener(new ZoomOneToOneAction());
        JideButton resetCamera = toolButton(RESETCAMERA);
        resetCamera.addActionListener(new ResetCameraAction());
        JideButton resetCameraAxis = toolButton(RESETCAMERAAXIS);
        resetCameraAxis.addActionListener(new ResetCameraAxisAction());

        addButton(zoomIn);
        addButton(zoomOut);
        addButton(zoomFit);
        addButton(zoomOne);
        add(new JToolBar.Separator(dim));
        addButton(resetCameraAxis);
        addButton(resetCamera);
        add(new JToolBar.Separator(dim));

        // Interaction
        ButtonGroup group = new ButtonGroup();

        JideToggleButton pan = toolToggleButton(PAN);
        pan.addActionListener(e -> JHVFrame.getInteraction().setMode(Interaction.Mode.PAN));
        JideToggleButton rotate = toolToggleButton(ROTATE);
        rotate.addActionListener(e -> JHVFrame.getInteraction().setMode(Interaction.Mode.ROTATE));
        JideToggleButton axis = toolToggleButton(AXIS);
        axis.addActionListener(e -> JHVFrame.getInteraction().setMode(Interaction.Mode.AXIS));

        group.add(pan);
        group.add(rotate);
        group.add(axis);

        addButton(pan);
        addButton(rotate);
        addButton(axis);
        add(new JToolBar.Separator(dim));

        switch (interactionMode) {
            case PAN -> pan.setSelected(true);
            case AXIS -> axis.setSelected(true);
            case ROTATE -> rotate.setSelected(true);
        }
        JHVFrame.getInteraction().setMode(interactionMode);

        trackingButton = toolToggleButton(TRACK);
        trackingButton.setSelected(Display.getCamera().getTrackingMode());
        trackingButton.addItemListener(e -> Display.getCamera().setTrackingMode(trackingButton.isSelected()));

        diffRotationButton = toolToggleButton(DIFFROTATION);
        diffRotationButton.setSelected(ImageLayers.getDiffRotationMode());
        diffRotationButton.addItemListener(e -> {
            ImageLayers.setDiffRotationMode(diffRotationButton.isSelected());
            MovieDisplay.display();
        });

        coronaButton = toolToggleButton(OFFDISK);
        coronaButton.setSelected(Display.getShowCorona());
        coronaButton.addItemListener(e -> {
            Display.setShowCorona(coronaButton.isSelected());
            MovieDisplay.display();
        });

        multiviewButton = toolToggleButton(MULTIVIEW);
        multiviewButton.setSelected(Display.multiview);
        multiviewButton.addItemListener(e -> {
            Display.multiview = multiviewButton.isSelected();
            ImageLayers.arrangeMultiView(Display.multiview);
        });

        addButton(trackingButton);
        addButton(diffRotationButton);
        addButton(coronaButton);
        addButton(multiviewButton);
        add(new JToolBar.Separator(dim));

        JideButton projectionButton = toolButton(PROJECTION);
        addButton(projectionButton);

        JPopupMenu projectionPopup = new JPopupMenu();
        ButtonGroup projectionGroup = new ButtonGroup();
        for (Display.ProjectionMode el : Display.ProjectionMode.values()) {
            projectionPopup.add(el.radio);
            projectionGroup.add(el.radio);
        }
        Display.ProjectionMode.Orthographic.radio.setSelected(true);

        projectionButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                projectionPopup.show(e.getComponent(), 0, e.getComponent().getHeight());
            }
        });

        JideButton anotationButton = toolButton(ANNOTATION);
        addButton(anotationButton);

        JPopupMenu annotationPopup = new JPopupMenu();
        ButtonGroup annotationGroup = new ButtonGroup();
        for (Interaction.AnnotationMode mode : Interaction.AnnotationMode.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(mode.toString());
            if (mode == JHVFrame.getInteraction().getAnnotationMode())
                item.setSelected(true);
            item.addActionListener(e -> JHVFrame.getInteraction().setAnnotationMode(mode));
            annotationGroup.add(item);
            annotationPopup.add(item);
        }

        annotationPopup.addSeparator();
        annotationPopup.add(new ClearAnnotationsAction());
        annotationPopup.addSeparator();
        annotationPopup.add(new ZoomFOVAnnotationAction());

        anotationButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                annotationPopup.show(e.getComponent(), 0, e.getComponent().getHeight());
            }
        });

        add(new JToolBar.Separator(dim));

        JideButton cutOut = toolButton(CUTOUT);
        cutOut.addActionListener(new SDOCutOutAction());
        addButton(cutOut);

        if (Boolean.parseBoolean(Settings.getProperty("startup.sampHub"))) {
            JideButton samp = toolButton(SAMP);
            samp.addActionListener(e -> SampClient.notifyRequestData());
            addButton(samp);
        }

        add(new JToolBar.Separator(dim));

/*
        for (Map.Entry<ButtonText, ActionListener> entry : pluginButtons.entrySet()) {
            JideButton b = toolButton(entry.getKey());
            b.addActionListener(entry.getValue());
            addButton(b);
        }
*/
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
