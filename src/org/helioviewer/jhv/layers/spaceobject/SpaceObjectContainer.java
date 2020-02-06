package org.helioviewer.jhv.layers.spaceobject;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import org.helioviewer.jhv.astronomy.Frame;
import org.helioviewer.jhv.astronomy.PositionLoad;
import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.gui.components.base.JHVTableCellRenderer;
import org.helioviewer.jhv.gui.components.base.TableValue;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.json.JSONArray;

@SuppressWarnings("serial")
public class SpaceObjectContainer extends JScrollPane {

    private static final int ICON_WIDTH = 12;
    private static final int NUMBEROFVISIBLEROWS = 5;

    private static final int SELECTED_COL = 0;
    private static final int OBJECT_COL = 1;
    private static final int STATUS_COL = 2;

    private final boolean exclusive;
    private final UpdateViewpoint uv;
    private final SpaceObject observer;
    private final SpaceObjectModel model;

    private SpaceObjectElement highlighted;
    private Frame frame;
    private long startTime;
    private long endTime;

    public SpaceObjectContainer(JSONArray ja, boolean _exclusive, UpdateViewpoint _uv, SpaceObject _observer, Frame _frame, long _startTime, long _endTime) {
        exclusive = _exclusive;
        uv = _uv;
        observer = _observer;
        frame = _frame;
        startTime = _startTime;
        endTime = _endTime;

        model = new SpaceObjectModel(observer);

        JTable grid = new JTable(model);
        grid.setTableHeader(null);
        grid.setShowGrid(false);
        grid.setRowSelectionAllowed(true);
        grid.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        grid.setColumnSelectionAllowed(false);
        grid.setIntercellSpacing(new Dimension(0, 0));

        if (exclusive)
            grid.getColumnModel().getColumn(SELECTED_COL).setCellRenderer(new SelectedExclusiveRenderer());
        else
            grid.getColumnModel().getColumn(SELECTED_COL).setCellRenderer(new SelectedRenderer());
        grid.getColumnModel().getColumn(SELECTED_COL).setPreferredWidth(ICON_WIDTH + 8);
        grid.getColumnModel().getColumn(SELECTED_COL).setMaxWidth(ICON_WIDTH + 8);
        grid.getColumnModel().getColumn(OBJECT_COL).setCellRenderer(new ObjectRenderer());
        grid.getColumnModel().getColumn(STATUS_COL).setCellRenderer(new StatusRenderer());

        grid.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                TableValue v = TableValue.tableValueAtPoint(grid, e.getPoint());
                if (v == null || !(v.value instanceof SpaceObjectElement))
                    return;

                highlighted = (SpaceObjectElement) v.value;
                if (v.col == SELECTED_COL)
                    selectElement(highlighted);
                MovieDisplay.display();
            }
        });

        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));

        setViewportView(grid);
        getViewport().setBackground(grid.getBackground());
        setPreferredSize(new Dimension(-1, getGridRowHeight(grid) * NUMBEROFVISIBLEROWS + 1));
        grid.setRowHeight(getGridRowHeight(grid));

        PositionLoad.removeAll(uv);

        ListSelectionModel selectionModel = grid.getSelectionModel();
        int len = ja.length();
        for (int i = 0; i < len; i++)
            selectTarget(SpaceObject.get(ja.optString(i, "Earth")), selectionModel);
    }

    private void selectTarget(SpaceObject target, ListSelectionModel selectionModel) {
        int idx = model.indexOf(target);
        if (idx != -1) { // found
            selectionModel.setSelectionInterval(idx, idx); // highlight in table
            SpaceObjectElement element = (SpaceObjectElement) model.getValueAt(idx, 0);
            selectElement(element);
            highlighted = element;
        }
    }

    public PositionLoad getHighlightedLoad() {
        return highlighted == null ? null : highlighted.getLoad(uv);
    }

    public void setFrame(Frame _frame) {
        if (frame == _frame)
            return;

        frame = _frame;
        model.getSelected().forEach(element -> element.load(uv, observer, frame, startTime, endTime));
    }

    public void setTime(long _startTime, long _endTime) {
        if (startTime == _startTime && endTime == _endTime)
            return;

        startTime = _startTime;
        endTime = _endTime;
        model.getSelected().forEach(element -> element.load(uv, observer, frame, startTime, endTime));
    }

    private void selectElement(SpaceObjectElement element) {
        if (exclusive) {
            model.getSelected().forEach(e -> e.unload(uv));
            element.load(uv, observer, frame, startTime, endTime);
        } else {
            if (element.isSelected())
                element.unload(uv);
            else
                element.load(uv, observer, frame, startTime, endTime);
        }
    }

    public boolean isDownloading() {
        for (SpaceObjectElement element : model.getSelected()) {
            if (element.isDownloading())
                return true;
        }
        return false;
    }

    private int rowHeight = -1;

    private int getGridRowHeight(JTable grid) {
        if (rowHeight == -1) {
            rowHeight = grid.getRowHeight() + 4;
        }
        return rowHeight;
    }

    public JSONArray toJson() {
        JSONArray ja = new JSONArray();
        model.getSelected().forEach(ja::put);
        return ja;
    }

    private static class ObjectRenderer extends JHVTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value instanceof SpaceObjectElement) {
                SpaceObjectElement element = (SpaceObjectElement) value;
                label.setText(element.toString());
                label.setBorder(element.getBorder());
                label.setToolTipText("Select for spiral");
            }
            return label;
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
            if (value instanceof SpaceObjectElement) {
                SpaceObjectElement element = (SpaceObjectElement) value;
                checkBox.setSelected(element.isSelected());
                checkBox.setBorder(element.getBorder());
            }
            checkBox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            checkBox.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
            return checkBox;
        }

    }

    private static class SelectedExclusiveRenderer extends JHVTableCellRenderer {

        private final JRadioButton radio = new JRadioButton();

        SelectedExclusiveRenderer() {
            setHorizontalAlignment(CENTER);
            radio.putClientProperty("JComponent.sizeVariant", "small");
            radio.setBorderPainted(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof SpaceObjectElement) {
                SpaceObjectElement element = (SpaceObjectElement) value;
                radio.setSelected(element.isSelected());
                radio.setBorder(element.getBorder());
            }
            radio.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            radio.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
            return radio;
        }

    }

    private static class StatusRenderer extends JHVTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value instanceof SpaceObjectElement) {
                SpaceObjectElement element = (SpaceObjectElement) value;
                String status = element.getStatus();
                label.setText(status);
                label.setToolTipText(status);
                label.setBorder(element.getBorder());
            }
            return label;
        }
    }

}
