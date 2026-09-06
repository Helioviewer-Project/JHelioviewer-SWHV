package org.helioviewer.jhv.event.info;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.annotation.Nullable;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableRowSorter;

import org.helioviewer.jhv.event.EventParameter;
import org.helioviewer.jhv.gui.DesktopIntegration;
import org.helioviewer.jhv.gui.component.WrappedTable;

@SuppressWarnings("serial")
class ParameterTablePanel extends JPanel {

    ParameterTablePanel(EventParameter[] parameters) {
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
                if (getURL(t, col, row) != null) {
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
                String url = getURL(t, col, row);
                if (url != null) {
                    DesktopIntegration.openURL(url);
                }
            }
        };

        table.addMouseMotionListener(ma);
        table.addMouseListener(ma);

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    @Nullable
    private static String getURL(JTable table, int col, int row) {
        if (row < 0 || col < 0 || table.convertColumnIndexToModel(col) != 1)
            return null;
        ParameterTableModel model = (ParameterTableModel) table.getModel();
        return model.getURL(table.convertRowIndexToModel(row));
    }

}
