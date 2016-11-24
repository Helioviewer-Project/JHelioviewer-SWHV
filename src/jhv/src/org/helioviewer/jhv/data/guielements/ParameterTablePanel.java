package org.helioviewer.jhv.data.guielements;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.regex.Matcher;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.Regex;
import org.helioviewer.jhv.data.datatype.event.JHVEventParameter;
import org.helioviewer.jhv.data.guielements.model.ParameterTableModel;

@SuppressWarnings("serial")
class ParameterTablePanel extends JPanel implements MouseListener, MouseMotionListener {

    public ParameterTablePanel(JHVEventParameter[] parameters) {
        setLayout(new BorderLayout());

        ParameterTableModel parameterModel = new ParameterTableModel(parameters);
        JTable table = new WrappedTable();
        table.setModel(parameterModel);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(0).setResizable(false);
        table.getColumnModel().getColumn(0).setMaxWidth(180);
        table.getColumnModel().getColumn(0).setPreferredWidth(180);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setCellRenderer(new WrappedTextRenderer());
        table.setPreferredScrollableViewportSize(new Dimension(table.getWidth(), 150));
        // table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        TableRowSorter<ParameterTableModel> sorter = new TableRowSorter<>(parameterModel);
        sorter.toggleSortOrder(0);
        table.setRowSorter(sorter);

        table.addMouseMotionListener(this);
        table.addMouseListener(this);

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private static class WrappedTable extends JTable {

            @Override
            public void columnMarginChanged(ChangeEvent e) {
                updateRowHeights();
            }

            // don't delete
            // @Override
            // public void tableChanged(TableModelEvent e) {
            //     updateRowHeights();
            // }

            private void updateRowHeights()  {
                int rowMargin = getRowMargin();
                int rowHeight = getRowHeight();
                int rows = getRowCount();
                for (int i = 0; i < rows; i++) {
                    Component comp = prepareRenderer(getCellRenderer(i, 1), i, 1);

                    int height;
                    Dimension dim = comp.getPreferredSize();
                    if (dim != null /* satisfy coverity */ && (height = dim.height + rowMargin) > rowHeight)
                        setRowHeight(i, height);
                }
            }

    }

    private static class WrappedTextRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setText(String.format("<html><div width=%d>%s</div><html>", table.getColumnModel().getColumn(column).getWidth(), value));
            return label;
        }

    }

    private static String extractURL(JTable table, int col, int row) {
        Object value = table.getValueAt(row, col);
        if (value instanceof String) {
            String strValue = (String) value;
            Matcher m = Regex.HREF.matcher(strValue);
            String url = m.find() ? m.group(1) : strValue;
            return Regex.WEB_URL.matcher(url).matches() ? url : null;
        } else {
            return null;
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        JTable table = (JTable) e.getComponent();
        Point p = e.getPoint();
        int row = table.rowAtPoint(p);
        int col = table.columnAtPoint(p);
        if (row < 0 || col < 0) {
            return;
        }

        if (col == 1 && extractURL(table, col, row) != null) {
            table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            table.setCursor(Cursor.getDefaultCursor());
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        JTable table = (JTable) e.getComponent();
        table.setCursor(Cursor.getDefaultCursor());
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        JTable table = (JTable) e.getComponent();
        Point p = e.getPoint();
        int row = table.rowAtPoint(p);
        int col = table.columnAtPoint(p);
        if (row < 0 || col != 1) {
            return;
        }

        String url = extractURL(table, col, row);
        if (url != null) {
            JHVGlobals.openURL(url);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

}
