package org.helioviewer.jhv.layers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.components.base.JHVTableCellRenderer;
import org.helioviewer.jhv.gui.components.base.TableValue;
import org.helioviewer.jhv.gui.components.base.TerminatedFormatterFactory;
import org.helioviewer.jhv.gui.components.base.WheelSupport;

@SuppressWarnings("serial")
class FOVLayerOptions extends JPanel {

    private static final int ICON_WIDTH = 12;
    private static final int NUMBEROFVISIBLEROWS = 5;

    static final int NUMBEROFCOLUMNS = 4;
    static final int SELECTED_COL = 0;
    static final int FOV_COL = 1;
    static final int OFF1_COL = 2;
    private static final int OFF2_COL = 3;

    FOVLayerOptions(AbstractTableModel model, double customAngle) {
        double fovMin = 0, fovMax = 180;
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(Double.valueOf(customAngle), Double.valueOf(fovMin), Double.valueOf(fovMax), Double.valueOf(0.01)));
        spinner.addChangeListener(e -> FOVLayer.setCustomAngle((Double) spinner.getValue()));
        JFormattedTextField f = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
        f.setFormatterFactory(new TerminatedFormatterFactory("%.2f", "\u00B0", fovMin, fovMax));
        WheelSupport.installMouseWheelSupport(spinner);

        JCheckBox customCheckBox = new JCheckBox("Custom angle", false);
        customCheckBox.addChangeListener(e -> FOVLayer.setCustomEnabled(customCheckBox.isSelected()));

        JPanel customPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c0 = new GridBagConstraints();
        c0.anchor = GridBagConstraints.LINE_END;
        c0.weightx = 1.;
        c0.weighty = 1.;
        c0.gridy = 0;
        c0.gridx = 0;
        customPanel.add(customCheckBox, c0);
        c0.anchor = GridBagConstraints.LINE_START;
        c0.gridx = 1;
        customPanel.add(spinner, c0);

        JTable grid = new JTable(model);
        grid.setTableHeader(null);
        grid.setShowGrid(false);
        grid.setRowSelectionAllowed(true);
        grid.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        grid.setColumnSelectionAllowed(false);
        grid.setIntercellSpacing(new Dimension(0, 0));

        grid.getColumnModel().getColumn(SELECTED_COL).setCellRenderer(new SelectedRenderer());
        grid.getColumnModel().getColumn(SELECTED_COL).setPreferredWidth(ICON_WIDTH + 8);
        grid.getColumnModel().getColumn(SELECTED_COL).setMaxWidth(ICON_WIDTH + 8);
        grid.getColumnModel().getColumn(FOV_COL).setCellRenderer(new FOVRenderer());
        grid.getColumnModel().getColumn(OFF1_COL).setCellEditor(new FOVLayerOptions.OffEditor());
        grid.getColumnModel().getColumn(OFF1_COL).setCellRenderer(new FOVLayerOptions.OffRenderer());
        grid.getColumnModel().getColumn(OFF2_COL).setCellEditor(new FOVLayerOptions.OffEditor());
        grid.getColumnModel().getColumn(OFF2_COL).setCellRenderer(new FOVLayerOptions.OffRenderer());

        grid.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!grid.isEnabled())
                    return;
                TableValue v = TableValue.tableValueAtPoint(grid, e.getPoint());
                if (v == null)
                    return;

                if (v.value instanceof FOVLayer.FOV) {
                    FOVLayer.FOV fov = (FOVLayer.FOV) v.value;
                    if (v.col == SELECTED_COL) {
                        fov.toggle();
                        model.fireTableRowsUpdated(v.row, v.row);
                        MovieDisplay.display();
                    } else if (e.getClickCount() == 2 && fov.isEnabled()) {
                        fov.zoom(Display.getCamera());
                        MovieDisplay.render(1);
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane();
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));

        scroll.setViewportView(grid);
        scroll.getViewport().setBackground(grid.getBackground());
        scroll.setPreferredSize(new Dimension(-1, getGridRowHeight(grid) * NUMBEROFVISIBLEROWS + 1));
        grid.setRowHeight(getGridRowHeight(grid));

        setLayout(new GridBagLayout());
        GridBagConstraints c1 = new GridBagConstraints();
        c1.fill = GridBagConstraints.BOTH;
        c1.weightx = 1;
        c1.gridx = 0;

        c1.weighty = 0;
        c1.gridy = 0;
        add(customPanel, c1);

        c1.weighty = 1;
        c1.gridy = 1;
        add(scroll, c1);

        ComponentUtils.smallVariant(this);
    }

    private int rowHeight = -1;

    private int getGridRowHeight(JTable grid) {
        if (rowHeight == -1) {
            rowHeight = grid.getRowHeight() + 4;
        }
        return rowHeight;
    }

    static class OffControl extends JSpinner {

        private static final double min = -60;
        private static final double max = 60;

        OffControl() {
            setModel(new SpinnerNumberModel(0, min, max, 0.01));
            JFormattedTextField f = ((JSpinner.DefaultEditor) getEditor()).getTextField();
            f.setFormatterFactory(new TerminatedFormatterFactory("%.2f", "\u2032", min, max));
            WheelSupport.installMouseWheelSupport(this);

            setBorder(JHVTableCellRenderer.cellBorder);
            putClientProperty("JComponent.sizeVariant", "small");
        }

    }

    private static class OffRenderer extends JHVTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value instanceof Double) {
                label.setText(String.format("%.2f\u2032", value));
                label.setBorder(JHVTableCellRenderer.cellBorder);
                label.setHorizontalAlignment(JLabel.RIGHT);
            }
            return label;
        }
    }

    private static class OffEditor extends DefaultCellEditor {

        private OffControl offControl;

        OffEditor() {
            super(new JCheckBox());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            offControl = FOVLayer.getOffControl(row, column - OFF1_COL);
            if (value instanceof Double) {
                offControl.setValue(value);
            }
            return offControl;
        }

        @Override
        public Object getCellEditorValue() {
            return offControl.getValue();
        }

    }

    private static class SelectedRenderer extends JHVTableCellRenderer {

        private final JCheckBox checkBox = new JCheckBox();

        SelectedRenderer() {
            setHorizontalAlignment(CENTER);
            checkBox.putClientProperty("JComponent.sizeVariant", "small");
            checkBox.setBorderPainted(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof FOVLayer.FOV) {
                FOVLayer.FOV fov = (FOVLayer.FOV) value;
                checkBox.setSelected(fov.isEnabled());
                checkBox.setBorder(JHVTableCellRenderer.cellBorder);
            }
            checkBox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            checkBox.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
            return checkBox;
        }

    }

    private static class FOVRenderer extends JHVTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value instanceof FOVLayer.FOV) {
                FOVLayer.FOV fov = (FOVLayer.FOV) value;
                label.setText(fov.toString());
                label.setBorder(JHVTableCellRenderer.cellBorder);
                label.setToolTipText("Double-click to fit FOV");
            }
            return label;
        }
    }

}
