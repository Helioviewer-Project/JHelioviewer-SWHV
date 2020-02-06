package org.helioviewer.jhv.layers.fov;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTree;
import javax.swing.SpinnerNumberModel;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.components.base.TerminatedFormatterFactory;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.layers.MovieDisplay;

@SuppressWarnings("serial")
class FOVTreeRenderer extends DefaultTreeCellRenderer {

    private static final double min = -60;
    private static final double max = 60;

    private final JSpinner spinnerX;
    private final JSpinner spinnerY;

    FOVTreeRenderer() {
        spinnerX = new JSpinner(new SpinnerNumberModel(0, min, max, 0.1));
        JFormattedTextField fX = ((JSpinner.DefaultEditor) spinnerX.getEditor()).getTextField();
        fX.setFormatterFactory(new TerminatedFormatterFactory("%.2f", "\u2032", min, max));
        WheelSupport.installMouseWheelSupport(spinnerX);

        spinnerY = new JSpinner(new SpinnerNumberModel(0, min, max, 0.1));
        JFormattedTextField fY = ((JSpinner.DefaultEditor) spinnerY.getEditor()).getTextField();
        fY.setFormatterFactory(new TerminatedFormatterFactory("%.2f", "\u2032", min, max));
        WheelSupport.installMouseWheelSupport(spinnerY);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object obj, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if (obj instanceof FOVTreeElement) {
            return createLeaf((FOVTreeElement) obj, tree.getBackground());
        } else if (obj instanceof FOVTreeRoot) {
            return createRoot((FOVTreeRoot) obj, tree.getBackground());
        } else {
            return super.getTreeCellRendererComponent(tree, obj, selected, expanded, leaf, row, hasFocus);
        }
    }

    private static JCheckBox createLeaf(FOVTreeElement f, Color back) {
        JCheckBox checkBox = new JCheckBox(f.toString(), f.isEnabled());
        checkBox.setBackground(back);
        checkBox.addActionListener(e -> {
            f.toggle();
            MovieDisplay.display();
        });
        checkBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3 && checkBox.isSelected()) {
                    f.zoom();
                    MovieDisplay.render(1);
                }
            }
        });
        ComponentUtils.smallVariant(checkBox);
        return checkBox;
    }

    private JPanel createRoot(FOVTreeRoot r, Color back) {
        spinnerX.addChangeListener(e -> r.setCenterX((Double) spinnerX.getValue()));
        spinnerY.addChangeListener(e -> r.setCenterY((Double) spinnerY.getValue()));

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        panel.setOpaque(false);
        panel.add(new JLabel(r.toString()));
        panel.add(new JLabel("  Offpoint X: ", JLabel.RIGHT));
        panel.add(spinnerX);
        panel.add(new JLabel("  Offpoint Y: ", JLabel.RIGHT));
        panel.add(spinnerY);
        ComponentUtils.smallVariant(panel);
        return panel;
    }

}
