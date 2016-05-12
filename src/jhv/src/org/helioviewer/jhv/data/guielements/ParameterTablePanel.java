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
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.Regex;
import org.helioviewer.jhv.data.datatype.event.JHVEventParameter;
import org.helioviewer.jhv.data.guielements.model.ParameterTableModel;

@SuppressWarnings("serial")
public class ParameterTablePanel extends JPanel {

    private final JTable table;

    public ParameterTablePanel(JHVEventParameter[] parameters) {
        setLayout(new BorderLayout());

        ParameterTableModel parameterModel = new ParameterTableModel(parameters);
        table = new JTable(parameterModel) {
            @Override
            public void columnMarginChanged(ChangeEvent e) {
                updateRowHeights();
            }

            // table never changes
            // @Override
            // public void tableChanged(TableModelEvent e) {
            //     updateRowHeights();
            // }
        };

        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(0).setResizable(false);
        table.getColumnModel().getColumn(0).setMaxWidth(180);
        table.getColumnModel().getColumn(0).setPreferredWidth(180);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.setPreferredScrollableViewportSize(new Dimension(table.getWidth(), 150));
        // table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        TableRowSorter<ParameterTableModel> sorter = new TableRowSorter<ParameterTableModel>(parameterModel);
        sorter.toggleSortOrder(0);
        table.setRowSorter(sorter);

        WrappedTextCellRenderer renderer = new WrappedTextCellRenderer();
        table.getColumnModel().getColumn(1).setCellRenderer(renderer);
        table.addMouseMotionListener(renderer);
        table.addMouseListener(renderer);

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void updateRowHeights()  {
        int rowMargin = table.getRowMargin();
        int rowHeight = table.getRowHeight();
        int rows = table.getRowCount();
        for (int i = 0; i < rows; i++) {
            Component comp = table.prepareRenderer(table.getCellRenderer(i, 1), i, 1);
            table.setRowHeight(i, Math.max(rowHeight, comp.getPreferredSize().height) + rowMargin);
        }
    }

    private static class WrappedTextCellRenderer extends JLabel implements TableCellRenderer, MouseListener, MouseMotionListener {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText(String.format("<html><div WIDTH=%d>%s</div><html>", table.getColumnModel().getColumn(column).getWidth(), value));
            return this;
        }

        private String extractURL(JTable table, int col, int row) {
            Object value = table.getValueAt(row, col);
            if (value instanceof String) {
                String strValue = (String) value;

                String url;
                Matcher m = Regex.HREF.matcher(strValue);
                if (m.find()) {
                    url = m.group(1);
                } else
                    url = strValue;
                return Regex.WEB_URL.matcher(url).matches() ? url : null;
            } else {
                return null;
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            JTable table = (JTable) e.getComponent();
            Point pt = e.getPoint();

            int row = table.rowAtPoint(pt);
            int col = table.columnAtPoint(pt);
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
            Point pt = e.getPoint();

            int row = table.rowAtPoint(pt);
            int col = table.columnAtPoint(pt);
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

}
