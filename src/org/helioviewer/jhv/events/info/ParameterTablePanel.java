package org.helioviewer.jhv.events.info;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.regex.Matcher;

import javax.annotation.Nullable;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableRowSorter;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.Regex;
import org.helioviewer.jhv.events.JHVEventParameter;
import org.helioviewer.jhv.gui.components.base.WrappedTable;

@SuppressWarnings("serial")
class ParameterTablePanel extends JPanel {

    ParameterTablePanel(JHVEventParameter[] parameters) {
        setLayout(new BorderLayout());

        ParameterTableModel parameterModel = new ParameterTableModel(parameters);
        JTable table = new WrappedTable();
        table.setModel(parameterModel);
        table.setAutoCreateRowSorter(true);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getColumnModel().getColumn(0).setResizable(false);
        table.getColumnModel().getColumn(0).setMaxWidth(180);
        table.getColumnModel().getColumn(0).setPreferredWidth(180);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setCellRenderer(new WrappedTable.WrappedTextRenderer());
        table.setPreferredScrollableViewportSize(new Dimension(table.getWidth(), 150));
        // table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        TableRowSorter<ParameterTableModel> sorter = new TableRowSorter<>(parameterModel);
        sorter.toggleSortOrder(0);
        table.setRowSorter(sorter);

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                JTable t = (JTable) e.getComponent();
                Point p = e.getPoint();
                int row = t.rowAtPoint(p);
                int col = t.columnAtPoint(p);
                if (row < 0 || col < 0) {
                    return;
                }

                if (col == 1 && extractURL(t, col, row) != null) {
                    t.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else {
                    t.setCursor(Cursor.getDefaultCursor());
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                JTable t = (JTable) e.getComponent();
                t.setCursor(Cursor.getDefaultCursor());
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                JTable t = (JTable) e.getComponent();
                Point p = e.getPoint();
                int row = t.rowAtPoint(p);
                int col = t.columnAtPoint(p);
                if (row < 0 || col != 1) {
                    return;
                }

                String url = extractURL(t, col, row);
                if (url != null) {
                    JHVGlobals.openURL(url);
                }
            }
        };

        table.addMouseMotionListener(ma);
        table.addMouseListener(ma);

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    @Nullable
    private static String extractURL(JTable table, int col, int row) {
        Object value = table.getValueAt(row, col);
        if (value instanceof String str) {
            Matcher m = Regex.HREF.matcher(str);
            return m.find() ? m.group(1) : null;
        }
        return null;
    }

}
