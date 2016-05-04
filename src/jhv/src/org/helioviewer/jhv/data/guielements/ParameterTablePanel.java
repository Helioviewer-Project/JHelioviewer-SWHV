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

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.Regex;
import org.helioviewer.jhv.data.datatype.event.JHVEventParameter;
import org.helioviewer.jhv.data.guielements.model.ParameterTableModel;

/**
 * Represents a panel with a table containing all the parameters from the given
 * list.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
@SuppressWarnings("serial")
public class ParameterTablePanel extends JPanel {

    public ParameterTablePanel(JHVEventParameter[] parameters) {
        setLayout(new BorderLayout());

        ParameterTableModel parameterModel = new ParameterTableModel(parameters);
        JTable table = new JTable(parameterModel);

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

    private static class WrappedTextCellRenderer extends JTextPane implements TableCellRenderer, MouseListener, MouseMotionListener {

        public WrappedTextCellRenderer() {
            setContentType("text/html");
            putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText("" + value);
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

            if (extractURL(table, col, row) != null) {
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
