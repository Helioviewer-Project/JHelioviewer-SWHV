package org.helioviewer.jhv.layers.selector;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.component.Buttons;
import org.helioviewer.jhv.gui.dialog.TextDialog;
import org.helioviewer.jhv.layers.ViewpointLayer;
import org.helioviewer.jhv.layers.ViewpointLayerOptions;
import org.helioviewer.jhv.layers.spaceobject.SpaceObjectElement;

import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideToggleButton;

@SuppressWarnings("serial")
final class ViewpointLayerOptionsPanel extends JPanel {

    private static final String explanation = """
            <b>Observer</b>: view from the active image observer.
            <b>Observer at 1au</b>: view from the active observer, but at 1au fixed distance.
            <b>Location</b>: view from selected object.
            <b>Heliosphere</b>: view onto the solar equatorial plane.

            If "Use movie time interval" is unselected, the viewpoint time is interpolated in the configured time interval.""";

    private final ViewpointLayer layer;
    private final ViewpointLayerOptionsExpertPanel locationPanel;
    private final ViewpointLayerOptionsExpertPanel equatorialPanel;
    private final JComboBox<ViewpointLayerOptions.CameraMode> modeBox = new JComboBox<>(ViewpointLayerOptions.CameraMode.values());
    private final JideToggleButton expandButton = new JideToggleButton(Buttons.chevronDown + "Viewpoint");
    private boolean syncing;

    ViewpointLayerOptionsPanel(ViewpointLayer _layer) {
        layer = _layer;
        locationPanel = new ViewpointLayerOptionsExpertPanel(layer.getOptions().getLocationOptions(), modeBox::repaint);
        equatorialPanel = new ViewpointLayerOptionsExpertPanel(layer.getOptions().getEquatorialOptions(), modeBox::repaint);
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        JPanel modePanel = new JPanel(new BorderLayout(5, 0));
        expandButton.setSelected(true);
        expandButton.addActionListener(e -> refreshMode());
        modePanel.add(expandButton, BorderLayout.LINE_START);
        modeBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focused) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
                if (index == -1 && value == ViewpointLayerOptions.CameraMode.Location) {
                    for (SpaceObjectElement element : layer.getOptions().getLocationOptions().getContainer().getElements()) {
                        if (element.isSelected()) {
                            label.setText("Location: " + element);
                            break;
                        }
                    }
                }
                return label;
            }
        });
        modeBox.setSelectedItem(layer.getCameraMode());
        modeBox.addActionListener(e -> {
            if (!syncing && modeBox.getSelectedItem() instanceof ViewpointLayerOptions.CameraMode mode) {
                expandButton.setSelected(true);
                if (layer.getCameraMode() == mode)
                    refreshMode();
                else
                    layer.setCameraMode(mode);
            }
        });
        modePanel.add(modeBox, BorderLayout.CENTER);

        JideButton info = new JideButton(Buttons.info);
        info.setToolTipText("Show viewpoint info");
        info.addActionListener(e -> new TextDialog("Viewpoint Options Information", explanation, false).showDialog());
        modePanel.add(info, BorderLayout.LINE_END);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        add(modePanel, c);
        c.gridy = 1;
        add(locationPanel, c);
        add(equatorialPanel, c);
        refreshMode();
    }

    void refreshMode() {
        ViewpointLayerOptions.CameraMode mode = layer.getCameraMode();
        boolean hasOptions = mode == ViewpointLayerOptions.CameraMode.Location || mode == ViewpointLayerOptions.CameraMode.Heliosphere;
        if (!hasOptions || modeBox.getSelectedItem() != mode)
            expandButton.setSelected(hasOptions);
        expandButton.setEnabled(hasOptions);
        syncing = true;
        try {
            modeBox.setSelectedItem(mode);
        } finally {
            syncing = false;
        }
        expandButton.setText((expandButton.isSelected() ? Buttons.chevronDown : Buttons.chevronRight) + "Viewpoint");
        boolean expanded = expandButton.isSelected();
        locationPanel.setVisible(expanded && mode == ViewpointLayerOptions.CameraMode.Location);
        equatorialPanel.setVisible(expanded && mode == ViewpointLayerOptions.CameraMode.Heliosphere);
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        // Reserve the width of every mode without activating it or changing the camera.
        size.width = Math.max(size.width, Math.max(locationPanel.getPreferredSize().width, equatorialPanel.getPreferredSize().width));
        return size;
    }

}
