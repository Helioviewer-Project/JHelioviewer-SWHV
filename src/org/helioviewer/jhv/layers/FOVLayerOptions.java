package org.helioviewer.jhv.layers;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.components.base.JHVTableCellRenderer;
import org.helioviewer.jhv.gui.components.base.TerminatedFormatterFactory;
import org.helioviewer.jhv.gui.components.base.WheelSupport;

@SuppressWarnings("serial")
class FOVLayerOptions extends JPanel {

    private static final int ICON_WIDTH = 12;
    private static final int NUMBEROFVISIBLEROWS = 5;

    //private static final int SELECTED_COL = 0;
    private static final int FOV_COL = 0;//1;
    private static final int OFF1_COL = 1;//2;
    private static final int OFF2_COL = 2;//2;

    FOVLayerOptions(Object[][] fovData, double customAngle) {
        double fovMin = 0, fovMax = 180;
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(Double.valueOf(customAngle), Double.valueOf(fovMin), Double.valueOf(fovMax), Double.valueOf(0.01)));
        spinner.setMaximumSize(new Dimension(6, 22));
        spinner.addChangeListener(e -> FOVLayer.setCustomAngle((Double) spinner.getValue()));
        JFormattedTextField f = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
        f.setFormatterFactory(new TerminatedFormatterFactory("%.2f", "\u00B0", fovMin, fovMax));
        WheelSupport.installMouseWheelSupport(spinner);

        JCheckBox customCheckBox = new JCheckBox("Custom angle", false);
        customCheckBox.addChangeListener(e -> FOVLayer.setCustomEnabled(customCheckBox.isEnabled()));

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

        DefaultTableModel model = new DefaultTableModel();
        model.setDataVector(fovData, new Object[]{"FOV", "JSlider", "JSlider"});

//        FOVModel model = new FOVModel();
        JTable grid = new JTable(model);
        grid.setTableHeader(null);
        grid.setShowGrid(false);
        grid.setRowSelectionAllowed(true);
        grid.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        grid.setColumnSelectionAllowed(false);
        grid.setIntercellSpacing(new Dimension(0, 0));

        //grid.getColumnModel().getColumn(SELECTED_COL).setCellRenderer(new SelectedRenderer());
        //grid.getColumnModel().getColumn(SELECTED_COL).setPreferredWidth(ICON_WIDTH + 8);
        //grid.getColumnModel().getColumn(SELECTED_COL).setMaxWidth(ICON_WIDTH + 8);
        //grid.getColumnModel().getColumn(FOV_COL).setCellRenderer(new FOVRenderer());
        grid.getColumnModel().getColumn(OFF1_COL).setCellEditor(new FOVLayerOptions.OffEditor());
        grid.getColumnModel().getColumn(OFF1_COL).setCellRenderer(new FOVLayerOptions.OffRenderer());
        grid.getColumnModel().getColumn(OFF2_COL).setCellEditor(new FOVLayerOptions.OffEditor());
        grid.getColumnModel().getColumn(OFF2_COL).setCellRenderer(new FOVLayerOptions.OffRenderer());
/*
        grid.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!grid.isEnabled())
                    return;
                TableValue v = TableValue.tableValueAtPoint(grid, e.getPoint());
                if (v == null)
                    return;

                FOV fov = (FOV) v.value;
                if (v.col == SELECTED_COL) {
                    fov.select();
                    model.fireTableRowsUpdated(v.row, v.row);
                    MovieDisplay.display();
                } else if (e.getClickCount() == 2) {
                    fov.zoom(Display.getCamera());
                    MovieDisplay.render(1);
                }
            }
        });
*/
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

    static class OffControl extends JPanel {

        private final JSlider slider;
        private final JLabel label;

        OffControl() {
            setLayout(new BorderLayout());
            setBorder(JHVTableCellRenderer.cellBorder);

            slider = new JSlider(JSlider.HORIZONTAL, -100, 100, 0);
            WheelSupport.installMouseWheelSupport(slider);

            label = new JLabel(slider.getValue() + "\u2033", JLabel.RIGHT);
            slider.addChangeListener(e -> label.setText(slider.getValue() + "\u2033"));

            add(slider, BorderLayout.LINE_START);
            add(label, BorderLayout.LINE_END);

            ComponentUtils.smallVariant(this);
        }

        @Override
        public void setForeground(Color color) {
            super.setForeground(color);
            if (slider != null) slider.setForeground(color);
            if (label != null) label.setForeground(color);
        }

        @Override
        public void setBackground(Color color) {
            super.setBackground(color);
            if (slider != null) slider.setBackground(color);
            if (label != null) label.setBackground(color);
        }

        int getValue() {
            return slider.getValue();
        }

        void setValue(int v) {
            slider.setValue(v);
        }

        void addChangeListener(ChangeListener l) {
            slider.addChangeListener(l);
        }

    }

    static class OffRenderer extends OffControl implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());

            if (value instanceof Integer) {
                int v = (Integer) value;
                setValue(v);
            }
            return this;
        }
    }

    static class OffEditor extends DefaultCellEditor {

        private OffControl offControl;

        OffEditor() {
            super(new JCheckBox());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            offControl = FOVLayer.getOffControl(row, column - 1);
            offControl.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            offControl.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());

            if (value instanceof Integer) {
                int v = (Integer) value;
                offControl.setValue(v);
            }
            return offControl;
        }

        @Override
        public Object getCellEditorValue() {
            return offControl.getValue();
        }

    }

    static class SelectedRenderer extends JHVTableCellRenderer {

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
                checkBox.setSelected(fov.isSelected());
                checkBox.setBorder(JHVTableCellRenderer.cellBorder);
            }
            checkBox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return checkBox;
        }

    }

    static class FOVRenderer extends JHVTableCellRenderer {
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
