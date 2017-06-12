package org.helioviewer.jhv.camera.object;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.gui.components.base.JHVTableCellRenderer;
import org.helioviewer.jhv.time.TimeUtils;

@SuppressWarnings("serial")
public class SpaceObjectContainer extends JPanel {

    private static final int ICON_WIDTH = 12;
    private static final int NUMBEROFVISIBLEROWS = 5;

    private static final int SELECTED_COL = 0;
    private static final int OBJECT_COL = 1;
    private static final int STATUS_COL = 2;

    private final boolean exclusive;
    private final ObjectTableModel model = new ObjectTableModel();
    private final JTable grid = new JTable(model);

    private long startTime = TimeUtils.EPOCH.milli;
    private long endTime = TimeUtils.EPOCH.milli;
    private String frame = "HEEQ";

    public SpaceObjectContainer(boolean _exclusive) {
        setLayout(new BorderLayout());

        exclusive = _exclusive;

        grid.setTableHeader(null);
        grid.setShowGrid(false);
        grid.setRowSelectionAllowed(true);
        grid.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        grid.setColumnSelectionAllowed(false);
        grid.setIntercellSpacing(new Dimension(0, 0));

        grid.getColumnModel().getColumn(SELECTED_COL).setCellRenderer(new SelectedRenderer());
        grid.getColumnModel().getColumn(SELECTED_COL).setPreferredWidth(ICON_WIDTH + 8);
        grid.getColumnModel().getColumn(SELECTED_COL).setMaxWidth(ICON_WIDTH + 8);
        grid.getColumnModel().getColumn(OBJECT_COL).setCellRenderer(new ObjectRenderer());
        grid.getColumnModel().getColumn(STATUS_COL).setCellRenderer(new StatusRenderer());

        grid.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point pt = e.getPoint();
                int row = grid.rowAtPoint(pt);
                if (row < 0)
                    return;

                int col = grid.columnAtPoint(pt);
                if (col == SELECTED_COL)
                    selectElement((SpaceObjectElement) grid.getValueAt(row, col));
            }
        });

        JScrollPane jsp = new JScrollPane(grid, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        jsp.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        jsp.getViewport().setBackground(grid.getBackground());
        jsp.setPreferredSize(new Dimension(-1, getGridRowHeight(grid) * NUMBEROFVISIBLEROWS + 1));
        grid.setRowHeight(getGridRowHeight(grid));

        add(jsp);
    }

    public void selectObject(SpaceObject object) {
        selectElement(model.selectionElement(object));
    }

    public void setStartTime(long _startTime) {
        startTime = _startTime;
    }

    public void setEndTime(long _endTime) {
        endTime = _endTime;
    }

    public void setFrame(String _frame) {
        frame = _frame;
    }

    private void selectElement(SpaceObjectElement element) {
        if (exclusive) {
            model.deselectAll();
            element.select(frame, startTime, endTime);
        } else {
            if (element.isSelected())
                element.deselect();
            else
                element.select(frame, startTime, endTime);
        }
        model.fireTableRowsUpdated(0, model.getRowCount());
    }

    private int rowHeight = -1;

    private int getGridRowHeight(JTable grid) {
        if (rowHeight == -1) {
            rowHeight = grid.getRowHeight() + 4;
        }
        return rowHeight;
    }

    private static class ObjectTableModel extends AbstractTableModel {

        private final ArrayList<SpaceObjectElement> elementList = new ArrayList<>();

        ObjectTableModel() {
            for (SpaceObject object : SpaceObject.getObjectList())
                elementList.add(new SpaceObjectElement(object));
        }

        public void deselectAll() {
            for (SpaceObjectElement element : elementList) {
                if (element.isSelected())
                    element.deselect();
            }
        }

        public SpaceObjectElement selectionElement(SpaceObject object) {
            for (SpaceObjectElement element : elementList) {
                if (element.getObject() == object)
                    return element;
            }
            return null;
        }

        @Override
        public int getRowCount() {
            return elementList.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public Object getValueAt(int row, int column) {
            return elementList.get(row);
        }

    }

    private static class ObjectRenderer extends JHVTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value instanceof SpaceObjectElement) {
                SpaceObjectElement element = (SpaceObjectElement) value;
                label.setText(element.toString());
                label.setBorder(((SpaceObjectElement) value).getObject().getBorder());
            }
            return label;
        }
    }

    private static class SelectedRenderer extends JHVTableCellRenderer {

        private final JCheckBox checkBox = new JCheckBox();

        public SelectedRenderer() {
            setHorizontalAlignment(CENTER);
            checkBox.putClientProperty("JComponent.sizeVariant", "small");
            checkBox.setBorderPainted(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof SpaceObjectElement) {
                SpaceObjectElement element = (SpaceObjectElement) value;
                checkBox.setSelected(element.isSelected());
                checkBox.setBorder(element.getObject().getBorder());
            }
            checkBox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return checkBox;
        }

    }

    private static class StatusRenderer extends JHVTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value instanceof SpaceObjectElement) {
                SpaceObjectElement element = (SpaceObjectElement) value;
                label.setText(element.getStatus());
                label.setBorder(((SpaceObjectElement) value).getObject().getBorder());
            }
            return label;
        }
    }

}
