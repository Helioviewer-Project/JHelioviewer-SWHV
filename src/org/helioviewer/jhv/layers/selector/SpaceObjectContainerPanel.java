package org.helioviewer.jhv.layers.selector;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.gui.components.base.TableValue;
import org.helioviewer.jhv.layers.ViewpointLayerOptionsExpert;
import org.helioviewer.jhv.layers.spaceobject.SpaceObjectContainer;
import org.helioviewer.jhv.layers.spaceobject.SpaceObjectElement;

@SuppressWarnings("serial")
final class SpaceObjectContainerPanel extends JScrollPane {

    private static final int ICON_WIDTH = 12;
    private static final int NUMBEROFVISIBLEROWS = 5;

    private static final int SELECTED_COL = 0;
    private static final int OBJECT_COL = 1;
    private static final int STATUS_COL = 2;

    private final JTable grid;

    SpaceObjectContainerPanel(ViewpointLayerOptionsExpert options) {
        SpaceObjectContainer container = options.getContainer();
        grid = new JTable(new SpaceObjectTableModel(container));
        grid.setTableHeader(null);
        grid.setShowHorizontalLines(true);
        grid.setRowSelectionAllowed(true);
        grid.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        grid.setColumnSelectionAllowed(false);
        grid.setIntercellSpacing(new Dimension(0, 0));

        if (container.isExclusive())
            grid.getColumnModel().getColumn(SELECTED_COL).setCellRenderer(new SelectedExclusiveRenderer());
        else
            grid.getColumnModel().getColumn(SELECTED_COL).setCellRenderer(new SelectedRenderer());
        grid.getColumnModel().getColumn(SELECTED_COL).setPreferredWidth(ICON_WIDTH + 8);
        grid.getColumnModel().getColumn(SELECTED_COL).setMaxWidth(ICON_WIDTH + 8);
        grid.getColumnModel().getColumn(OBJECT_COL).setCellRenderer(new ObjectRenderer());
        grid.getColumnModel().getColumn(STATUS_COL).setCellRenderer(new StatusRenderer());

        int row = container.getHighlightedIndex();
        if (row != -1)
            grid.getSelectionModel().setSelectionInterval(row, row);

        grid.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!isEnabled())
                    return;
                TableValue v = TableValue.tableValueAtPoint(grid, e.getPoint());
                if (v == null || !(v.value instanceof SpaceObjectElement element))
                    return;

                if (v.col == SELECTED_COL)
                    options.selectElement(element);
                else
                    options.setHighlightedElement(element);
                DisplayController.refreshCamera();
            }
        });

        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, getBackground().brighter()));

        setViewportView(grid);
        getViewport().setBackground(grid.getBackground());
        setPreferredSize(new Dimension(-1, grid.getRowHeight() * NUMBEROFVISIBLEROWS + 1));
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        grid.setEnabled(enabled);
        setWheelScrollingEnabled(enabled);
        getHorizontalScrollBar().setEnabled(enabled);
        getVerticalScrollBar().setEnabled(enabled);
    }

    private static class ObjectRenderer extends DefaultTableCellRenderer {
        @Override
        public void setValue(Object value) {
            if (value instanceof SpaceObjectElement element) {
                setText(element.toString());
                setToolTipText("Select for spiral");
            }
        }
    }

    private static class SelectedRenderer extends DefaultTableCellRenderer {

        private final JCheckBox checkBox = new JCheckBox();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof SpaceObjectElement element) {
                checkBox.setSelected(element.isSelected());
            }
            checkBox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return checkBox;
        }

    }

    private static class SelectedExclusiveRenderer extends DefaultTableCellRenderer {

        private final JRadioButton radio = new JRadioButton();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof SpaceObjectElement element) {
                radio.setSelected(element.isSelected());
            }
            radio.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return radio;
        }

    }

    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public void setValue(Object value) {
            if (value instanceof SpaceObjectElement element) {
                String status = element.getStatus();
                setText(status);
                setToolTipText(status);
            }
        }
    }

}
