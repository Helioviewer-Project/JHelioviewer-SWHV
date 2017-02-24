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
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.camera.Camera;
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

    private enum InteractionMode {
        PAN, ROTATE, ANNOTATE
    }

    private InteractionMode interactionMode;

    private final JideToggleButton panButton;
    private final JideToggleButton rotateButton;
    private final JideToggleButton annotateButton;

    public TopToolBar() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
        setRollover(true);

        try {
            interactionMode = InteractionMode.valueOf(Settings.getSingletonInstance().getProperty("display.interaction").toUpperCase());
        } catch (Exception e) {
            Log.error("Error when reading the interaction mode", e);
            interactionMode = InteractionMode.ROTATE;
        }

        Dimension dim = new Dimension(32, 32);

        // Zoom
        JideButton zoomIn = new JideButton(Buttons.zoomIn);
        zoomIn.addActionListener(new ZoomInAction());
        zoomIn.setToolTipText("Zoom in");
        addButton(zoomIn);

        JideButton zoomOut = new JideButton(Buttons.zoomOut);
        zoomOut.addActionListener(new ZoomOutAction());
        zoomOut.setToolTipText("Zoom out");
        addButton(zoomOut);

        JideButton zoomFit = new JideButton(Buttons.zoomFit);
        zoomFit.addActionListener(new ZoomFitAction());
        zoomFit.setToolTipText("Zoom to fit");
        addButton(zoomFit);

        JideButton zoomOne = new JideButton(Buttons.zoomOne);
        zoomOne.addActionListener(new ZoomOneToOneAction());
        zoomOne.setToolTipText("Zoom to native resolution");
        addButton(zoomOne);

        JideButton resetCamera = new JideButton(Buttons.resetCamera);
        resetCamera.addActionListener(new ResetCameraAction());
        resetCamera.setToolTipText("Reset camera position to default");
        addButton(resetCamera);

        add(new JToolBar.Separator(dim));

        // Interaction
        ButtonGroup group = new ButtonGroup();

        panButton = new JideToggleButton(Buttons.pan);
        panButton.addActionListener(e -> setActiveInteractionMode(InteractionMode.PAN));
        panButton.setToolTipText("Pan");
        group.add(panButton);
        addButton(panButton);

        rotateButton = new JideToggleButton(Buttons.rotate);
        rotateButton.addActionListener(e -> setActiveInteractionMode(InteractionMode.ROTATE));
        rotateButton.setToolTipText("Rotate");
        group.add(rotateButton);
        addButton(rotateButton);

        annotateButton = new JideToggleButton(Buttons.annotate);
        annotateButton.addActionListener(e -> setActiveInteractionMode(InteractionMode.ANNOTATE));
        annotateButton.setToolTipText("Annotate");
        group.add(annotateButton);
        addButton(annotateButton);

        setActiveInteractionMode(interactionMode);

        JPopupMenu annotatePopup = new JPopupMenu();
        ButtonGroup annotateGroup = new ButtonGroup();

        JRadioButtonMenuItem rectangleItem = new JRadioButtonMenuItem("Rectangle");
        rectangleItem.addActionListener(e -> ImageViewerGui.getAnnotateInteraction().setMode(AnnotationMode.RECTANGLE));
        annotatePopup.add(rectangleItem);
        annotateGroup.add(rectangleItem);
        rectangleItem.setSelected(true);

        JRadioButtonMenuItem circleItem = new JRadioButtonMenuItem("Circle");
        circleItem.addActionListener(e -> ImageViewerGui.getAnnotateInteraction().setMode(AnnotationMode.CIRCLE));
        annotatePopup.add(circleItem);
        annotateGroup.add(circleItem);

        JRadioButtonMenuItem crossItem = new JRadioButtonMenuItem("Cross");
        crossItem.addActionListener(e -> ImageViewerGui.getAnnotateInteraction().setMode(AnnotationMode.CROSS));
        annotatePopup.add(crossItem);
        annotateGroup.add(crossItem);

        annotatePopup.addSeparator();
        annotatePopup.add(new ClearAnnotationsAction());

        annotateButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                annotatePopup.show(e.getComponent(), 0, e.getComponent().getHeight());
            }
        });

        add(new JToolBar.Separator(dim));

        JideToggleButton trackSolarRotationButton = new JideToggleButton(Buttons.track);
        trackSolarRotationButton.addActionListener(e -> {
            Camera camera = Displayer.getCamera();
            camera.setTrackingMode(!camera.getTrackingMode());
        });
        trackSolarRotationButton.setSelected(false);
        trackSolarRotationButton.setToolTipText("Track solar rotation");
        addButton(trackSolarRotationButton);

        JideToggleButton coronaVisibilityButton = new JideToggleButton(Buttons.offDisk);
        coronaVisibilityButton.addActionListener(e -> {
            Displayer.toggleShowCorona();
            Displayer.display();
        });
        coronaVisibilityButton.setSelected(false);
        coronaVisibilityButton.setToolTipText("Toggle off-disk corona");
        addButton(coronaVisibilityButton);

        add(new JToolBar.Separator(dim));

        JideButton projectionButton = new JideButton(Buttons.projection);
        projectionButton.setToolTipText("Projection");
        addButton(projectionButton);

        JPopupMenu projectionPopup = new JPopupMenu();
        ButtonGroup projectionGroup = new ButtonGroup();
        for (Displayer.DisplayMode el : Displayer.DisplayMode.values()) {
            JRadioButtonMenuItem projectionItem = new JRadioButtonMenuItem(el.toString());
            projectionItem.addActionListener(e -> {
                Displayer.setMode(el);
                el.setGridScale();
            });
            projectionPopup.add(projectionItem);
            projectionGroup.add(projectionItem);
            if (el == Displayer.DisplayMode.ORTHO) {
                projectionItem.setSelected(true);
            }
        }

        projectionButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                projectionPopup.show(e.getComponent(), 0, e.getComponent().getHeight());
            }
        });

        add(new JToolBar.Separator(dim));

        JideButton cutOut = new JideButton(Buttons.cutOut);
        cutOut.addActionListener(new SDOCutOutAction());
        cutOut.setToolTipText("SDO cut-out service");
        addButton(cutOut);
    }

    private void setActiveInteractionMode(InteractionMode mode) {
        interactionMode = mode;
        Settings.getSingletonInstance().setProperty("display.interaction", mode.toString().toLowerCase());
        Settings.getSingletonInstance().save("display.interaction");

        switch (mode) {
        case PAN:
            ImageViewerGui.setCurrentInteraction(ImageViewerGui.getPanInteraction());
            panButton.setSelected(true);
            break;
        case ROTATE:
            ImageViewerGui.setCurrentInteraction(ImageViewerGui.getRotateInteraction());
            rotateButton.setSelected(true);
            break;
        case ANNOTATE:
            ImageViewerGui.setCurrentInteraction(ImageViewerGui.getAnnotateInteraction());
            annotateButton.setSelected(true);
            break;
        }
    }

    private void addButton(JideButton b) {
        b.setFocusPainted(false);
        add(b);
    }

}
