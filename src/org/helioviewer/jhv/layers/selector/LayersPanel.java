package org.helioviewer.jhv.layers.selector;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableModel;

import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.gui.DesktopIntegration;
import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.TransferAccess;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.gui.component.TableValue;
import org.helioviewer.jhv.gui.dialog.ObservationDialog;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;

@SuppressWarnings("serial")
public final class LayersPanel extends JPanel {

    private static final int ICON_WIDTH = 12;

    static final int ENABLED_COL = 0;
    static final int NAME_COL = 1;
    static final int TIME_COL = 2;
    static final int DOWNLOAD_COL = 3;
    static final int REMOVE_COL = 4;
    static final int NUMBER_COLUMNS = 5;

    private static final int NUMBEROFVISIBLEROWS = 9;

    private final LayersTable grid;
    private final LayerOptionSections sections;

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
        public void tableChanged(TableModelEvent e) {
            if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == TIME_COL) {
                dirtyTimeColumn = true;
                return;
            }

            super.tableChanged(e);
            if (e.getType() == TableModelEvent.INSERT) {
                int row = e.getLastRow();
                if (row >= 0 && getValueAt(row, 0) instanceof ImageLayer)
                    setRowSelectionInterval(row, row);
            }
        }

        // Repaint the whole table because ImageLayers can have heterogenous frame names.

        @Override
        public void repaint() {
            dirty = true;
        }

        @Override
        public void repaint(int x, int y, int width, int height) {
            if (dirtyRect == null)
                dirtyRect = new Rectangle(x, y, width, height);
            else
                dirtyRect.add(new Rectangle(x, y, width, height));
        }

        private boolean dirty = false;
        @Nullable
        private Rectangle dirtyRect;
        private boolean dirtyTimeColumn = false;

        @Override
        public void lazyRepaint() {
            if (dirty) {
                super.repaint();
            } else if (dirtyRect != null) {
                super.repaint(dirtyRect.x, dirtyRect.y, dirtyRect.width, dirtyRect.height);
            } else if (dirtyTimeColumn) {
                repaintTimeColumn();
            }
            dirty = false;
            dirtyRect = null;
            dirtyTimeColumn = false;
        }

        private void repaintTimeColumn() {
            Rectangle visible = getVisibleRect();
            Rectangle cell = getCellRect(0, TIME_COL, true);

            int x0 = Math.max(cell.x, visible.x);
            int x1 = Math.min(cell.x + cell.width, visible.x + visible.width);
            if (x0 < x1)
                super.repaint(x0, visible.y, x1 - x0, visible.height);
        }

    }

    public LayersPanel(LayerOptionSections sections) {
        this.sections = sections;
        setLayout(new GridBagLayout());
        LayersTableModel model = new LayersTableModel();

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.weighty = 1;
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

        grid.getColumnModel().getColumn(NAME_COL).setCellRenderer(new CellRenderer.Name());

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
            if (!e.getValueIsAdjusting())
                refreshSelectedOptionsPanel();
        });

        model.addTableModelListener(e -> {
            if (e.getType() != TableModelEvent.UPDATE || e.getColumn() == NAME_COL || e.getColumn() == TIME_COL)
                return;

            int row = grid.getSelectedRow();
            if (row >= 0 && e.getFirstRow() <= row && row <= e.getLastRow())
                refreshSelectedOptionsPanel();
        });

        grid.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                TableValue v = TableValue.tableValueAtPoint(grid, e.getPoint());
                if (v == null || !(v.value instanceof Layer layer))
                    return;

                if ((v.col == NAME_COL || v.col == TIME_COL) && layer instanceof ImageLayer il && e.getClickCount() == 2) {
                    ObservationDialog.getInstance().showDialog(false, il);
                    return;
                }

                if (v.col == ENABLED_COL) {
                    layer.setEnabled(!layer.isEnabled());
                    model.updateCell(v.row, v.col);
                    DisplayController.render(1);
                } else if (v.col == NAME_COL && layer instanceof ImageLayer il) {
                    Layers.setActiveImageLayer(il);
                    grid.repaint(); // multiple rows involved
                } else if (v.col == REMOVE_COL && layer.isDeletable()) {
                    boolean selected = selectedLayer() == layer;
                    Layers.remove(layer);
                    if (selected)
                        selectExistingRow(v.row);
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

        grid.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, DesktopIntegration.menuShortcutMask, false), "copy_time");
        grid.getActionMap().put("copy_time", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Layer layer = selectedLayer();
                if (layer != null) {
                    String timeString = layer.getTimeString();
                    if (timeString != null)
                        TransferAccess.writeClipboard(timeString);
                }
            }
        });

        grid.setDragEnabled(true);
        grid.setDropMode(DropMode.INSERT_ROWS);
        grid.setTransferHandler(new TableRowTransferHandler(grid));

        jsp.setPreferredSize(new Dimension(-1, grid.getRowHeight() * NUMBEROFVISIBLEROWS + 1));
    }

    public int getGridRowHeight() {
        return grid.getRowHeight();
    }

    @Nullable
    private Layer selectedLayer() {
        int row = grid.getSelectedRow();
        if (row < 0)
            return null;
        return grid.getValueAt(row, 0) instanceof Layer layer ? layer : null;
    }

    private void selectExistingRow(int preferredRow) {
        int rowCount = grid.getRowCount();
        if (rowCount == 0) {
            sections.setSelectedLayer(null);
            return;
        }
        int row = Math.min(preferredRow, rowCount - 1);
        grid.getSelectionModel().setSelectionInterval(row, row);
        refreshSelectedOptionsPanel();
    }

    private void refreshSelectedOptionsPanel() {
        sections.setSelectedLayer(selectedLayer());
    }

    public void setSelectedLayer(@Nullable Layer layer) {
        sections.setSelectedLayer(layer);
    }

}
