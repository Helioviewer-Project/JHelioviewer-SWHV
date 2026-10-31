package org.helioviewer.jhv.gui.component;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.helioviewer.jhv.gui.DesktopIntegration;
import org.helioviewer.jhv.ui.Panel;
import org.helioviewer.jhv.ui.Screen;

// A table control. The screen owns rows, order and selection; the sorter keeps the snapshot's order and shows the header's sort column.
@SuppressWarnings("serial")
final class PanelTable extends WrappedTable {

    private final Screen screen;
    private final Consumer<Runnable> act;
    private final int owner;
    private final String[] names;
    private final int[] headerItems;
    private final Model model = new Model();
    private final Sorter sorter;
    private final WrappedTextRenderer wrapped = new WrappedTextRenderer();

    private String[][] cells = {};
    private String[][] urls = {};
    private int[] items = {};
    private boolean[] selected = {};

    PanelTable(Panel p, int control, Screen _screen, Consumer<Runnable> _act) {
        if (!p.has(control, Panel.HEADER_ROW))
            throw new IllegalArgumentException("Swing table needs a header row");
        screen = _screen;
        act = _act;
        owner = p.value(control, Panel.IntField.OWNER);

        int columns = p.value(control, Panel.IntField.COLUMNS);
        names = new String[columns];
        headerItems = new int[columns];
        for (int column = 0; column < columns; column++) {
            int button = control + 2 + column;
            names[column] = p.string(button, Panel.IntField.LABEL);
            headerItems[column] = p.value(button, Panel.IntField.ID);
        }

        setModel(model);
        setIntercellSpacing(new Dimension(0, 0));
        int stretch = p.value(control, Panel.IntField.FLEX_COLUMNS);
        for (int column = 0; column < columns; column++) {
            TableColumn tableColumn = getColumnModel().getColumn(column);
            int width = p.columnPoints(control, column);
            if ((stretch & 1 << column) == 0) {
                tableColumn.setResizable(false);
                tableColumn.setMaxWidth(width);
            }
            tableColumn.setPreferredWidth(width);
        }
        setPreferredScrollableViewportSize(new Dimension(p.value(control, Panel.IntField.WIDTH), p.value(control, Panel.IntField.HEIGHT)));

        sorter = new Sorter();
        setRowSorter(sorter);
        getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                act.accept(this::reportSelection);
        });

        MouseAdapter links = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                setCursor(url(e) == null ? Cursor.getDefaultCursor() : Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setCursor(Cursor.getDefaultCursor());
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                String url = url(e);
                if (url != null)
                    DesktopIntegration.openURL(url);
            }
        };
        addMouseMotionListener(links);
        addMouseListener(links);
    }

    private String url(MouseEvent e) {
        int row = rowAtPoint(e.getPoint());
        int column = columnAtPoint(e.getPoint());
        return row < 0 || column < 0 ? null : urls[row][convertColumnIndexToModel(column)];
    }

    private void reportSelection() {
        for (int row = 0; row < items.length; row++) {
            boolean shown = isRowSelected(row);
            if (shown != selected[row])
                screen.command(owner, items[row], shown ? Panel.Command.SELECT_ADD : Panel.Command.SELECT_REMOVE);
        }
    }

    void update(Panel p, int control) {
        int columns = names.length;
        int row = control + 1;
        List<RowSorter.SortKey> keys = new ArrayList<>(1);
        for (int column = 0; column < columns; column++) {
            int order = p.value(row + 1 + column, Panel.IntField.INDEX);
            if (order != 0)
                keys.add(new RowSorter.SortKey(column, order == 2 ? SortOrder.DESCENDING : SortOrder.ASCENDING));
        }
        row += columns + 2;

        int rows = (p.tableEnd(control) - row) / (columns + 2);
        String[][] newCells = new String[rows][columns];
        String[][] newUrls = new String[rows][columns];
        int[] newItems = new int[rows];
        boolean[] newSelected = new boolean[rows];
        for (int r = 0; r < rows; r++, row += columns + 2) {
            if (p.kind(row) != Panel.Kind.ROW || p.kind(row + columns + 1) != Panel.Kind.ROW_END)
                throw new IllegalArgumentException("Swing table row needs one control per column");
            newItems[r] = p.value(row, Panel.IntField.ID);
            newSelected[r] = p.has(row, Panel.SELECTED);
            for (int column = 0; column < columns; column++) {
                int cell = row + 1 + column;
                String label = p.string(cell, Panel.IntField.LABEL);
                switch (p.kind(cell)) {
                    case LABEL -> newCells[r][column] = label;
                    case LINK -> {
                        newUrls[r][column] = p.string(cell, Panel.IntField.TEXT);
                        newCells[r][column] = "<a href=\"" + newUrls[r][column] + "\">" + label + "</a>";
                    }
                    default -> throw new IllegalArgumentException("Swing table has no " + p.kind(cell));
                }
                if (p.has(cell, Panel.WRAP))
                    getColumnModel().getColumn(column).setCellRenderer(wrapped);
            }
        }

        items = newItems;
        urls = newUrls;
        selected = newSelected;
        if (!keys.equals(sorter.getSortKeys()))
            sorter.setSortKeys(keys);
        if (!Arrays.deepEquals(cells, newCells)) {
            cells = newCells;
            model.fireTableDataChanged();
            updateRowHeights();
        }
        for (int r = 0; r < rows; r++) {
            if (isRowSelected(r) != selected[r]) {
                if (selected[r])
                    addRowSelectionInterval(r, r);
                else
                    removeRowSelectionInterval(r, r);
            }
        }
    }

    private final class Model extends AbstractTableModel {

        @Override
        public int getRowCount() {
            return cells.length;
        }

        @Override
        public int getColumnCount() {
            return names.length;
        }

        @Override
        public String getColumnName(int column) {
            return names[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            return cells[row][column];
        }

    }

    private final class Sorter extends TableRowSorter<TableModel> {

        Sorter() {
            super(model);
            for (int column = 0; column < names.length; column++)
                setComparator(column, (a, b) -> 0);
        }

        @Override
        public void toggleSortOrder(int column) {
            act.accept(() -> screen.command(owner, headerItems[column], Panel.Command.CLICK));
        }

    }

}
