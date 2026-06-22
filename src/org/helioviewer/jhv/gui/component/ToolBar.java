package org.helioviewer.jhv.gui.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EnumMap;
//import java.util.LinkedHashMap;
//import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import org.helioviewer.jhv.annotation.AnnotationMode;
import org.helioviewer.jhv.annotation.Annotations;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.app.Settings;
import org.helioviewer.jhv.app.state.ViewState;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.display.MapMode;
import org.helioviewer.jhv.display.interaction.Interaction;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.gui.Actions;
import org.helioviewer.jhv.input.InputController;
import org.helioviewer.jhv.io.samp.SampClient;
//import org.helioviewer.jhv.timelines.band.HapiReader;

import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideSplitButton;
import com.jidesoft.swing.JideToggleButton;

@SuppressWarnings("serial")
public final class ToolBar extends JToolBar implements ViewState.ModeListener {

    private static final int ZOOM_HOLD_REPEAT_MS = 33;

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
    private final ButtonText REFRESH = new ButtonText(Buttons.refresh, "Refresh", "Automatic refresh");
    private final ButtonText RESETCAMERA = new ButtonText(Buttons.resetCamera, "Reset View", "Reset view to default");
    private final ButtonText RESETCAMERAAXIS = new ButtonText(Buttons.resetCameraAxis, "Reset Axis", "Reset view axis");
    private final ButtonText ROTATE = new ButtonText(Buttons.rotate, "Rotate", "Rotate");
    private final ButtonText ROTATE90 = new ButtonText(Buttons.rotate90, "Rotate View 90°", "Rotate view 90°");
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

    private static JideSplitButton toolSplitButton(ButtonText text) {
        JideSplitButton b = new JideSplitButton(text.toString());
        b.setToolTipText(text.tip);
        b.setAlwaysDropdown(true);
        return b;
    }

    private static JideToggleButton toolToggleButton(ButtonText text) {
        JideToggleButton b = new JideToggleButton(text.toString());
        b.setToolTipText(text.tip);
        return b;
    }

    public ToolBar() {
        setLayout(new FlowLayout(FlowLayout.LEADING, 1, 3));
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, getBackground().brighter()));
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

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }
        });

        try {
            displayMode = DisplayMode.valueOf(Settings.getProperty("display.toolbar").toUpperCase());
        } catch (Exception ignore) {}
        setDisplayMode(displayMode);
        ViewState.addModeListener(this);

        // Keep the disk radial-range slider's maximum equal to the loaded layers' extent.
        Layers.addListener(new Layers.Listener() {
            @Override
            public void layerAdded(int index, Layer layer) {
                updateDiskRangeMax();
            }

            @Override
            public void layerRemoved(int index, Layer layer) {
                updateDiskRangeMax();
            }

            @Override
            public void layersCleared() {
                updateDiskRangeMax();
            }

            @Override
            public void layerUpdated(Layer layer) {
                updateDiskRangeMax();
            }

            @Override
            public void nameUpdated(Layer layer) {
            }

            @Override
            public void timeUpdated(Layer layer) {
            }
        });
    }

    private JideToggleButton coronaButton;
    private JideToggleButton diffRotationButton;
    private JideToggleButton multiviewButton;
    private final EnumMap<AnnotationMode, JRadioButtonMenuItem> annotationItems = new EnumMap<>(AnnotationMode.class);
    private final EnumMap<MapMode, JRadioButtonMenuItem> projectionItems = new EnumMap<>(MapMode.class);
    private JHVSlider powerDiskSlider; // PowerDisk radial exponent; only meaningful for that mode
    private JHVRangeSlider diskRangeSlider; // disk view radial range (R_sun); both disk modes; max tracks loaded layers
    private JideToggleButton refreshButton;
    private JideToggleButton trackingButton;

    private void createNewToolBar() {
        annotationItems.clear();
        projectionItems.clear();
        if (Platform.isMacOS()) {
            add(Box.createHorizontalStrut(90), 0);
        }

        Interaction.Mode interactionMode = InputController.getMode();
        try {
            interactionMode = Interaction.Mode.valueOf(Settings.getProperty("display.interaction").toUpperCase());
        } catch (Exception ignore) {}

        Dimension dim = new Dimension(32, 32);

        // Zoom
        JideButton zoomIn = toolButton(ZOOMIN);
        zoomIn.addActionListener(new Actions.ZoomIn());
        HoldRepeat.install(zoomIn, ZOOM_HOLD_REPEAT_MS);
        JideButton zoomOut = toolButton(ZOOMOUT);
        zoomOut.addActionListener(new Actions.ZoomOut());
        HoldRepeat.install(zoomOut, ZOOM_HOLD_REPEAT_MS);
        JideButton zoomFit = toolButton(ZOOMFIT);
        zoomFit.addActionListener(new Actions.ZoomFit());
        JideButton zoomOne = toolButton(ZOOMONE);
        zoomOne.addActionListener(new Actions.ZoomOneToOne());
        JideButton resetCamera = toolButton(RESETCAMERA);
        resetCamera.addActionListener(new Actions.ResetCamera());
        JideButton resetCameraAxis = toolButton(RESETCAMERAAXIS);
        resetCameraAxis.addActionListener(new Actions.ResetCameraAxis());

        JideSplitButton rotate90Button = toolSplitButton(ROTATE90);
        rotate90Button.add(new Actions.Rotate90Camera("X Axis", "X"));
        rotate90Button.add(new Actions.Rotate90Camera("Y Axis", "Y"));
        rotate90Button.add(new Actions.Rotate90Camera("Z Axis", "Z"));

        addButton(zoomIn);
        addButton(zoomOut);
        addButton(zoomFit);
        addButton(zoomOne);
        addSeparator(dim);
        addButton(resetCamera);
        addButton(resetCameraAxis);
        addButton(rotate90Button);
        addSeparator(dim);

        // Interaction
        ButtonGroup group = new ButtonGroup();

        JideToggleButton pan = toolToggleButton(PAN);
        pan.addActionListener(e -> InputController.setMode(Interaction.Mode.PAN));
        JideToggleButton rotate = toolToggleButton(ROTATE);
        rotate.addActionListener(e -> InputController.setMode(Interaction.Mode.ROTATE));
        JideToggleButton axis = toolToggleButton(AXIS);
        axis.addActionListener(e -> InputController.setMode(Interaction.Mode.AXIS));

        group.add(pan);
        group.add(rotate);
        group.add(axis);

        addButton(pan);
        addButton(rotate);
        addButton(axis);
        addSeparator(dim);

        switch (interactionMode) {
            case PAN -> pan.setSelected(true);
            case AXIS -> axis.setSelected(true);
            case ROTATE -> rotate.setSelected(true);
        }
        InputController.setMode(interactionMode);

        trackingButton = toolToggleButton(TRACK);
        trackingButton.setSelected(ViewState.isTracking());
        trackingButton.addItemListener(e -> ViewState.setTracking(trackingButton.isSelected()));

        diffRotationButton = toolToggleButton(DIFFROTATION);
        diffRotationButton.setSelected(ViewState.isDifferentialRotation());
        diffRotationButton.addItemListener(e -> ViewState.setDifferentialRotation(diffRotationButton.isSelected()));

        coronaButton = toolToggleButton(OFFDISK);
        coronaButton.setSelected(ViewState.isShowCorona());
        coronaButton.addItemListener(e -> ViewState.setShowCorona(coronaButton.isSelected()));

        multiviewButton = toolToggleButton(MULTIVIEW);
        multiviewButton.setSelected(ViewState.isMultiview());
        multiviewButton.addItemListener(e -> ViewState.setMultiview(multiviewButton.isSelected()));

        addButton(trackingButton);
        addButton(diffRotationButton);
        addButton(coronaButton);
        addButton(multiviewButton);
        addSeparator(dim);

        JideSplitButton projectionButton = toolSplitButton(PROJECTION);
        ButtonGroup projectionGroup = new ButtonGroup();
        for (MapMode el : MapMode.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(el.toString());
            if (el == ViewState.getProjection())
                item.setSelected(true);
            item.addActionListener(e -> ViewState.setProjection(el));
            projectionGroup.add(item);
            projectionButton.add(item);
            projectionItems.put(el, item);
        }
        projectionButton.addSeparator();
        projectionButton.add(createPowerDiskPanel());
        projectionButton.add(createDiskRangePanel());
        powerDiskSlider.setEnabled(ViewState.getProjection() == MapMode.PowerDisk);
        diskRangeSlider.setEnabled(ViewState.getProjection().isDisk());
        addButton(projectionButton);

        JideSplitButton annotationButton = toolSplitButton(ANNOTATION);
        ButtonGroup annotationGroup = new ButtonGroup();
        for (AnnotationMode mode : AnnotationMode.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(mode.toString());
            if (mode == ViewState.getAnnotationMode())
                item.setSelected(true);
            item.addActionListener(e -> ViewState.setAnnotationMode(mode));
            annotationGroup.add(item);
            annotationButton.add(item);
            annotationItems.put(mode, item);
        }
        annotationButton.addSeparator();
        addAnnotationColorItems(annotationButton);
        annotationButton.add(createAnnotationThicknessPanel());
        annotationButton.addSeparator();
        annotationButton.add(new Actions.ClearAnnotations());
        annotationButton.addSeparator();
        annotationButton.add(new Actions.ZoomFOVAnnotation());
        addButton(annotationButton);

        addSeparator(dim);

        refreshButton = toolToggleButton(REFRESH);
        refreshButton.setSelected(ViewState.isRefresh());
        refreshButton.addItemListener(e -> ViewState.setRefresh(refreshButton.isSelected()));
        addButton(refreshButton);

        addSeparator(dim);

        JideButton cutOut = toolButton(CUTOUT);
        cutOut.addActionListener(new Actions.SDOCutOut());
        addButton(cutOut);

        if (Boolean.parseBoolean(Settings.getProperty("startup.sampHub"))) {
            JideButton samp = toolButton(SAMP);
            samp.addActionListener(e -> SampClient.notifyRequestData());
            addButton(samp);
        }

        addSeparator(dim);
/*
        ButtonText hText = new ButtonText("HAPI", "HAPI", "HAPI");
        JideButton hButton = toolButton(hText);
        hButton.addActionListener(e -> HapiReader.requestCatalog());
        addButton(hButton);
*/
/*
        for (Map.Entry<ButtonText, ActionListener> entry : pluginButtons.entrySet()) {
            JideButton b = toolButton(entry.getKey());
            b.addActionListener(entry.getValue());
            addButton(b);
        }
*/
    }

    private void addButton(AbstractButton b) {
        b.setFocusPainted(false);
        add(b);
    }

    private static void addAnnotationColorItems(JideSplitButton annotationButton) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 8, 3, 8));
        ButtonGroup colorGroup = new ButtonGroup();
        for (Colors.NamedColor color : Annotations.BASE_COLORS) {
            JToggleButton button = new JToggleButton(new ColorIcon(color.awtColor()));
            button.setSelected(color == Annotations.getBaseColor());
            button.setToolTipText(color.toString());
            button.setFocusPainted(false);
            button.setPreferredSize(new Dimension(22, 22));
            button.addActionListener(e -> Annotations.setBaseColor(color));
            colorGroup.add(button);
            panel.add(button);
        }
        annotationButton.add(panel);
    }

    // PowerDisk radial exponent p (display radius ~ r^p): slider 0.01..2, default 1.0 (linear).
    // power() is read live every render, so the value takes effect through the scale rebuild.
    private JPanel createPowerDiskPanel() {
        powerDiskSlider = new JHVSlider(-1000, 1000, (int) Math.round(Display.getDiskPower() * 1000));
        powerDiskSlider.setToolTipText("PowerDisk radial exponent p, applied to the corona (r > 1 R☉): p = -1 inverse, p = 0 logarithmic, p = 1 linear");
        powerDiskSlider.setPreferredSize(new Dimension(110, powerDiskSlider.getPreferredSize().height));
        JLabel value = new JLabel(String.format("p %.3f", Display.getDiskPower()), JLabel.RIGHT);
        powerDiskSlider.addChangeListener(e -> {
            Display.setDiskPower(powerDiskSlider.getValue() / 1000.);
            value.setText(String.format("p %.3f", Display.getDiskPower()));
            DisplayController.display();
        });
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        panel.add(value, BorderLayout.LINE_START);
        panel.add(powerDiskSlider, BorderLayout.CENTER);
        return panel;
    }

    // Disk view radial range (R_sun) shown by both disk projections. Low handle = inner
    // radius, high handle = outer radius. The slider maximum is the radial extent of the
    // loaded layers and tracks it as layers are added/removed (see updateDiskRangeMax), so
    // the high handle at the maximum means "fit to the loaded layers".
    private JPanel createDiskRangePanel() {
        int max = diskRangeMaxTicks();
        boolean fit = Display.getDiskRMax() <= 0;
        int lo = Math.min((int) Math.round(Display.getDiskRMin() * 10), max);
        int hi = fit ? max : Math.min((int) Math.round(Display.getDiskRMax() * 10), max);
        diskRangeSlider = new JHVRangeSlider(0, max, lo, hi);
        diskRangeSlider.setToolTipText("Disk radial range shown in R☉ (outer at the maximum fits the loaded layers)");
        diskRangeSlider.setPreferredSize(new Dimension(110, diskRangeSlider.getPreferredSize().height));

        JLabel value = new JLabel(diskRangeLabel(lo, hi, max), JLabel.RIGHT);
        diskRangeSlider.addChangeListener(e -> {
            int l = diskRangeSlider.getLowValue();
            int h = diskRangeSlider.getHighValue();
            Display.setDiskRange(l / 10., h >= diskRangeSlider.getMaximum() ? 0 : h / 10.); // max = fit
            value.setText(diskRangeLabel(l, h, diskRangeSlider.getMaximum()));
            DisplayController.display();
        });

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        panel.add(value, BorderLayout.LINE_START);
        panel.add(diskRangeSlider, BorderLayout.CENTER);
        return panel;
    }

    private static int diskRangeMaxTicks() {
        return Math.clamp((int) Math.ceil(ImageLayers.getLargestDiskRadius() * 10), 10, 5120);
    }

    // Grow/shrink the range slider to the loaded layers' extent; an outer handle that was
    // sitting at "fit" (the old maximum) keeps fitting at the new extent.
    private void updateDiskRangeMax() {
        if (diskRangeSlider == null)
            return;
        int oldMax = diskRangeSlider.getMaximum();
        int newMax = diskRangeMaxTicks();
        if (newMax == oldMax)
            return;
        boolean fit = diskRangeSlider.getHighValue() >= oldMax;
        diskRangeSlider.setMaximum(newMax);
        if (fit)
            diskRangeSlider.setHighValue(newMax);
    }

    private static String diskRangeLabel(int lo, int hi, int max) {
        String top = hi >= max ? "fit" : String.format("%.1f", hi / 10.);
        return String.format("r %.1f–%s", lo / 10., top);
    }

    private static JPanel createAnnotationThicknessPanel() {
        int thickness = Annotations.getThicknessValue();
        JHVSlider slider = new JHVSlider(Annotations.MIN_THICKNESS, Annotations.MAX_THICKNESS, Annotations.DEFAULT_THICKNESS);
        slider.setValue(thickness);
        slider.setMajorTickSpacing(1);
        slider.setSnapToTicks(true);
        slider.setToolTipText("Annotation thickness");
        slider.setPreferredSize(new Dimension(110, slider.getPreferredSize().height));
        slider.addChangeListener(e -> Annotations.setThicknessValue(slider.getValue()));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        panel.add(slider, BorderLayout.CENTER);
        return panel;
    }

    private static final class ColorIcon implements Icon {

        private static final int SIZE = 12;

        private final Color color;

        private ColorIcon(Color _color) {
            color = _color;
        }

        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(color);
            g.fillRect(x, y, SIZE, SIZE);
            g.setColor(Color.DARK_GRAY);
            g.drawRect(x, y, SIZE - 1, SIZE - 1);
        }
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

    @Override
    public void modeStateChanged() {
        trackingButton.setSelected(ViewState.isTracking());
        diffRotationButton.setSelected(ViewState.isDifferentialRotation());
        coronaButton.setSelected(ViewState.isShowCorona());
        multiviewButton.setSelected(ViewState.isMultiview());
        refreshButton.setSelected(ViewState.isRefresh());
        JRadioButtonMenuItem activeProjection = projectionItems.get(ViewState.getProjection());
        if (activeProjection != null)
            activeProjection.setSelected(true);
        if (powerDiskSlider != null)
            powerDiskSlider.setEnabled(ViewState.getProjection() == MapMode.PowerDisk);
        if (diskRangeSlider != null)
            diskRangeSlider.setEnabled(ViewState.getProjection().isDisk());
        JRadioButtonMenuItem activeAnnotationMode = annotationItems.get(ViewState.getAnnotationMode());
        if (activeAnnotationMode != null)
            activeAnnotationMode.setSelected(true);
    }

}
