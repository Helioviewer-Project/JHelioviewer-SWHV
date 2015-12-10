package org.helioviewer.jhv.data.guielements;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.RowFilter;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.text.html.HTMLDocument;

import org.helioviewer.jhv.base.Regex;
import org.helioviewer.jhv.data.datatype.event.JHVEventParameter;
import org.helioviewer.jhv.data.guielements.model.EmptyRowFilter;
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

    /** the table */
    private final JTable table;
    /** the model for the table */
    private final ParameterTableModel parameterModel;
    /** checkbox indicating null value should be visible */
    private final JCheckBox nullValue;

    private final TableRowSorter<ParameterTableModel> sorter;

    /**
     * Creates a table panel for the given parameters.
     *
     * @param parameters
     *            the parameters
     */
    public ParameterTablePanel(Collection<JHVEventParameter> parameters) {
        super();
        setLayout(new BorderLayout());
        parameterModel = new ParameterTableModel(parameters);
        table = new JTable(parameterModel) {
            /*
             * @Override public boolean getScrollableTracksViewportWidth() {
             * return getPreferredSize().width < getParent().getWidth(); }
             */
        };
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(180);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(0).setResizable(false);
        table.getColumnModel().getColumn(0).setMaxWidth(180);
        table.setPreferredScrollableViewportSize(new Dimension(table.getWidth(), 150));
        // table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        sorter = new TableRowSorter<ParameterTableModel>(parameterModel);
        table.setRowSorter(sorter);
        sorter.toggleSortOrder(0);

        WrappedTextCellRenderer renderer = new WrappedTextCellRenderer();
        table.setDefaultRenderer(String.class, renderer);
        table.addMouseMotionListener(renderer);
        table.addMouseListener(renderer);

        nullValue = new JCheckBox();
        JPanel nullValuePanel = new JPanel();
        JLabel nullValueLabel = new JLabel("Show Empty Parameters");
        nullValuePanel.add(nullValue);
        nullValuePanel.add(nullValueLabel);
        RowFilter<ParameterTableModel, Integer> rf = null;
        // If current expression doesn't parse, don't update.
        try {
            rf = new EmptyRowFilter();
        } catch (java.util.regex.PatternSyntaxException ex) {
            return;
        }
        sorter.setRowFilter(rf);

        nullValue.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (nullValue.isSelected()) {
                    sorter.setRowFilter(null);
                } else {
                    RowFilter<ParameterTableModel, Integer> rf = null;
                    // If current expression doesn't parse, don't update.
                    try {
                        rf = new EmptyRowFilter();
                    } catch (java.util.regex.PatternSyntaxException ex) {
                        return;
                    }
                    sorter.setRowFilter(rf);
                }

            }
        });
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(nullValuePanel, BorderLayout.PAGE_END);
    }

    private static class WrappedTextCellRenderer extends JTextPane implements TableCellRenderer, MouseListener, MouseMotionListener {
        private int row = -1;
        private int col = -1;
        private boolean isRollover;

        public WrappedTextCellRenderer() {
            setContentType("text/html");
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String str = "";
            if (value != null) {
                str = value.toString();
            }

            if (isRolloverCell(table, row, column) && isValueURL(value)) {
                setText("<html><u><font color='blue'>" + str + "</font></u></html>");
            } else if (isValueURL(value)) {
                setText("<html><font color='blue'>" + str + "</font></u></html>");
            } else {
                setText(str);
            }

            Font font = UIManager.getFont("Label.font");
            String bodyRule = "body { font-family: " + font.getFamily() + "; " + "font-size: " + font.getSize() + "pt; }";
            ((HTMLDocument) getDocument()).getStyleSheet().addRule(bodyRule);

            setSize(table.getColumnModel().getColumn(column).getWidth(), getPreferredSize().height);
            if (table.getRowHeight(row) != getPreferredSize().height) {
                table.setRowHeight(row, getPreferredSize().height);
            }
            return this;
        }

        private boolean isValueURL(Object value) {
            if (value instanceof String) {
                String strValue = (String) value;
                strValue = strValue.trim();
                strValue = strValue.toLowerCase();
                return Regex.WEB_URL.matcher(strValue).matches();
            } else {
                return false;
            }
        }

        protected boolean isRolloverCell(JTable table, int row, int column) {
            return !table.isEditing() && this.row == row && col == column && isRollover;
        }

        private boolean isURLColumn(JTable table, int col, int row) {
            return isValueURL(table.getValueAt(row, col));
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            JTable table = (JTable) e.getComponent();
            int prevRow = row;
            int prevCol = col;
            boolean prevRollover = isRollover;

            Point pt = e.getPoint();
            row = table.rowAtPoint(pt);
            col = table.columnAtPoint(pt);
            if (row < 0 || col < 0) {
                return;
            }

            isRollover = isURLColumn(table, col, row);
            if (row == prevRow && col == prevCol && isRollover == prevRollover || !isRollover && !prevRollover) {
                return;
            }

            Rectangle repaintRect;
            if (isRollover) {
                Rectangle r = table.getCellRect(row, col, false);
                repaintRect = prevRollover ? r.union(table.getCellRect(prevRow, prevCol, false)) : r;
                table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } else {
                repaintRect = table.getCellRect(prevRow, prevCol, false);
                table.setCursor(Cursor.getDefaultCursor());
            }
            table.repaint(repaintRect);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if (row < 0 || col < 0) {
                return;
            }

            JTable table = (JTable) e.getComponent();
            if (isURLColumn(table, col, row)) {
                table.repaint(table.getCellRect(row, col, false));
                row = -1;
                col = -1;
                isRollover = false;
                table.setCursor(Cursor.getDefaultCursor());
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            JTable table = (JTable) e.getComponent();

            Point pt = e.getPoint();
            int ccol = table.columnAtPoint(pt);
            int crow = table.rowAtPoint(pt);
            if (ccol < 0 || crow < 0) {
                return;
            }

            if (isURLColumn(table, ccol, crow)) {
                String value = (String) table.getValueAt(crow, ccol);
                try {
                    URL url = new URL(value);
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(url.toURI());
                    }
                } catch (URISyntaxException ex) {
                    ex.printStackTrace();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
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
