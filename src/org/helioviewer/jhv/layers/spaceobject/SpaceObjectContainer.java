package org.helioviewer.jhv.layers.spaceobject;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.astronomy.Frame;
import org.helioviewer.jhv.astronomy.PositionLoad;
import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.gui.components.base.TableValue;
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
        grid.setShowHorizontalLines(true);
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
                Display.getCamera().refresh(); // full camera refresh to update viewpoint for relative longitude
            }
        });

        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Colors.lightGray));

        setViewportView(grid);
        getViewport().setBackground(grid.getBackground());
        setPreferredSize(new Dimension(-1, grid.getRowHeight() * NUMBEROFVISIBLEROWS + 1));

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

    @Nullable
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

    public JSONArray toJson() {
        JSONArray ja = new JSONArray();
        model.getSelected().forEach(ja::put);
        return ja;
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
