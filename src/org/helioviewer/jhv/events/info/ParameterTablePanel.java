package org.helioviewer.jhv.events.info;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
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
class ParameterTablePanel extends JPanel implements MouseListener, MouseMotionListener {

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

        table.addMouseMotionListener(this);
        table.addMouseListener(this);

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    @Nullable
    private static String extractURL(JTable table, int col, int row) {
        Object value = table.getValueAt(row, col);
        if (value instanceof String) {
            Matcher m = Regex.HREF.matcher((String) value);
            return m.find() ? m.group(1) : null;
        }
        return null;
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
