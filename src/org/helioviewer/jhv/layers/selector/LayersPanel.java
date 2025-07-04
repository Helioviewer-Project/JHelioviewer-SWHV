package org.helioviewer.jhv.layers.selector;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.KeyStroke;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableModel;

import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.JHVTransferHandler;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.gui.components.base.TableValue;
import org.helioviewer.jhv.gui.dialogs.ObservationDialog;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.MovieDisplay;

@SuppressWarnings("serial")
public final class LayersPanel extends JPanel {

    private static final int ICON_WIDTH = 12;

    private static final int ENABLED_COL = 0;
    private static final int TITLE_COL = 1;
    public static final int TIME_COL = 2;
    private static final int DOWNLOAD_COL = 3;
    private static final int REMOVE_COL = 4;

    public static final int NUMBER_COLUMNS = 5;
    private static final int NUMBEROFVISIBLEROWS = 9;

    private final LayersTable grid;
    private final JPanel optionsPanelWrapper;

    private static class LayersTable extends JTable implements Interfaces.LazyComponent {

        LayersTable(TableModel tm) {
            super(tm);
            UITimer.register(this);
        }

        @Override
        public void changeSelection(int row, int col, boolean toggle, boolean extend) {
            if (col != ENABLED_COL && col != REMOVE_COL)
                super.changeSelection(row, col, toggle, extend);
            // otherwise prevent changing selection
        }

        @Override
        public void clearSelection() {
            // prevent losing selection
        }

        @Override
        public void tableChanged(TableModelEvent e) {
            super.tableChanged(e);
            if (e.getType() == TableModelEvent.INSERT) {
                int row = e.getLastRow();
                if (getValueAt(row, 0) instanceof ImageLayer)
                    setRowSelectionInterval(row, row);
            }
        }

        @Override
        public void repaint() {
            dirty = true;
        }

        @Override
        public void repaint(int x, int y, int width, int height) {
            dirty = true;
        }

        private boolean dirty = false;

        @Override
        public void lazyRepaint() {
            if (dirty) {
                super.repaint();
                dirty = false;
            }
        }

    }

    public LayersPanel(Layers model) {
        setLayout(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.weighty = 0;
        gc.fill = GridBagConstraints.BOTH;

        grid = new LayersTable(model);

        JScrollPane jsp = new JScrollPane(grid, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        jsp.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, getBackground().brighter()));
        jsp.getViewport().setBackground(grid.getBackground());
        add(jsp, gc);

        grid.setTableHeader(null);
        grid.setShowHorizontalLines(true);
        grid.setRowSelectionAllowed(true);
        grid.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        grid.setColumnSelectionAllowed(false);
        grid.setIntercellSpacing(new Dimension(0, 0));

        grid.getColumnModel().getColumn(ENABLED_COL).setCellRenderer(new CellRenderer.Enabled());
        grid.getColumnModel().getColumn(ENABLED_COL).setPreferredWidth(ICON_WIDTH + 8);
        grid.getColumnModel().getColumn(ENABLED_COL).setMaxWidth(ICON_WIDTH + 8);

        grid.getColumnModel().getColumn(TITLE_COL).setCellRenderer(new CellRenderer.Name());

        grid.getColumnModel().getColumn(TIME_COL).setCellRenderer(new CellRenderer.Time());
        int timeWidth = SwingUtilities.computeStringWidth(grid.getFontMetrics(CellRenderer.Time.font), "2000-01-01T12:00:00.000");
        grid.getColumnModel().getColumn(TIME_COL).setMinWidth(timeWidth);

        grid.getColumnModel().getColumn(DOWNLOAD_COL).setCellRenderer(new CellRenderer.Loading());
        grid.getColumnModel().getColumn(DOWNLOAD_COL).setPreferredWidth(ICON_WIDTH + 2);
        grid.getColumnModel().getColumn(DOWNLOAD_COL).setMaxWidth(ICON_WIDTH + 2);

        grid.getColumnModel().getColumn(REMOVE_COL).setCellRenderer(new CellRenderer.Remove());
        grid.getColumnModel().getColumn(REMOVE_COL).setPreferredWidth(ICON_WIDTH + 2);
        grid.getColumnModel().getColumn(REMOVE_COL).setMaxWidth(ICON_WIDTH + 2);

        grid.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                setOptionsPanel((Layer) grid.getValueAt(grid.getSelectedRow(), 0));
            }
        });

        grid.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                TableValue v = TableValue.tableValueAtPoint(grid, e.getPoint());
                if (v == null || !(v.value instanceof Layer layer))
                    return;

                if ((v.col == TITLE_COL || v.col == TIME_COL) && layer instanceof ImageLayer il && e.getClickCount() == 2) {
                    ObservationDialog.getInstance().showDialog(false, il);
                    return;
                }

                if (v.col == ENABLED_COL) {
                    layer.setEnabled(!layer.isEnabled());
                    model.updateCell(v.row, v.col);
                    if (grid.getSelectedRow() == v.row)
                        setOptionsPanel(layer);
                    MovieDisplay.render(1);
                } else if (v.col == TITLE_COL && layer instanceof ImageLayer il) {
                    Layers.setActiveImageLayer(il);
                    grid.repaint(); // multiple rows involved
                } else if (v.col == REMOVE_COL && layer.isDeletable()) {
                    model.remove(layer);
                    int idx = grid.getSelectedRow();
                    if (v.row <= idx)
                        grid.getSelectionModel().setSelectionInterval(idx - 1, idx - 1);
                }
            }
        });
        grid.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = grid.rowAtPoint(e.getPoint());
                if (row >= 0 && grid.getValueAt(row, 0) instanceof ImageLayer) {
                    grid.setCursor(UIGlobals.openHandCursor);
                } else {
                    grid.setCursor(Cursor.getDefaultCursor());
                }
            }
        });

        grid.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, UIGlobals.menuShortcutMask, false), "copy_time");
        grid.getActionMap().put("copy_time", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (model.getValueAt(grid.getSelectedRow(), 0) instanceof Layer layer) {
                    String timeString = layer.getTimeString();
                    if (timeString != null)
                        JHVTransferHandler.getInstance().toClipboard(timeString);
                }
            }
        });

        grid.setDragEnabled(true);
        grid.setDropMode(DropMode.INSERT_ROWS);
        grid.setTransferHandler(new TableRowTransferHandler(grid));

        jsp.setPreferredSize(new Dimension(-1, grid.getRowHeight() * NUMBEROFVISIBLEROWS + 1));

        gc.gridy = 1;
        optionsPanelWrapper = new JPanel(new BorderLayout());
        add(optionsPanelWrapper, gc);
    }

    public int getGridRowHeight() {
        return grid.getRowHeight();
    }

    public void setOptionsPanel(Layer layer) {
        optionsPanelWrapper.removeAll();
        Component optionsPanel = layer == null ? null : layer.getOptionsPanel();
        if (optionsPanel != null) {
            ComponentUtils.setEnabled(optionsPanel, layer.isEnabled());
            optionsPanelWrapper.add(optionsPanel, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
    }

    public void refresh() {
        grid.repaint();
    }

}
