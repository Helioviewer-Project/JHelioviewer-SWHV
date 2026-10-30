package org.helioviewer.jhv.layers.selector;

import java.awt.BorderLayout;
import java.awt.Component;
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
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.DesktopIntegration;
import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.TransferAccess;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.gui.component.TableValue;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.ViewpointLayer;

@SuppressWarnings("serial")
final class LayerListPanel extends JPanel {

    private static final int ICON_WIDTH = 12;

    private final LayersTable grid;
    private final LayerOptions options = new LayerOptions();
    private final JPanel optionsPanelWrapper;

    private final class LayersTable extends JTable implements Interfaces.LazyComponent {

        LayersTable(TableModel tm) {
            super(tm);
            UITimer.register(this);
        }

        @Override
        public void changeSelection(int row, int col, boolean toggle, boolean extend) {
            if (col != LayersTableModel.ENABLED_COL && col != LayersTableModel.REMOVE_COL)
                super.changeSelection(row, col, toggle, extend);
            // otherwise prevent changing selection
        }

        @Override
        public void tableChanged(TableModelEvent e) {
            if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == LayersTableModel.TIME_COL) {
                Rectangle visible = getVisibleRect();
                int lastRow = Math.min(e.getLastRow(), getModel().getRowCount() - 1);
                for (int modelRow = e.getFirstRow(); modelRow <= lastRow; modelRow++) {
                    int row = convertRowIndexToView(modelRow);
                    if (row >= 0) {
                        Rectangle cell = getCellRect(row, LayersTableModel.TIME_COL, true).intersection(visible);
                        if (!cell.isEmpty())
                            repaint(cell.x, cell.y, cell.width, cell.height);
                    }
                }
                return;
            }

            super.tableChanged(e);
            if (e.getType() != TableModelEvent.UPDATE || e.getLastRow() == Integer.MAX_VALUE)
                LayerListPanel.this.revalidate();
            if (e.getType() == TableModelEvent.INSERT) {
                int row = convertRowIndexToView(e.getLastRow());
                if (row >= 0 && getValueAt(row, 0) instanceof ImageLayer)
                    setRowSelectionInterval(row, row);
            }
            if (grid != null && e.getType() == TableModelEvent.UPDATE && e.getColumn() != LayersTableModel.NAME_COL && e.getColumn() != LayersTableModel.TIME_COL) {
                int row = getSelectedRow();
                if (row >= 0) {
                    int modelRow = convertRowIndexToModel(row);
                    if (e.getFirstRow() <= modelRow && modelRow <= e.getLastRow())
                        refreshSelectedOptionsPanel();
                }
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

        @Override
        public void lazyRepaint() {
            if (dirty) {
                super.repaint();
            } else if (dirtyRect != null)
                super.repaint(dirtyRect.x, dirtyRect.y, dirtyRect.width, dirtyRect.height);
            dirty = false;
            dirtyRect = null;
        }

    }

    LayerListPanel(boolean images) {
        setLayout(new GridBagLayout());
        LayersTableModel model = new LayersTableModel();

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.weighty = 1;
        gc.fill = GridBagConstraints.BOTH;

        grid = new LayersTable(model);
        grid.setRowSorter(createSorter(model, images));

        JPanel tablePanel = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                int rows = Math.max(images ? 4 : 0, grid.getRowCount());
                return new Dimension(-1, grid.getRowHeight() * rows + getInsets().top);
            }
        };
        if (images)
            tablePanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, getBackground().brighter()));
        tablePanel.add(grid, BorderLayout.CENTER);
        add(tablePanel, gc);

        grid.setTableHeader(null);
        grid.setShowHorizontalLines(true);
        grid.setRowSelectionAllowed(true);
        grid.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        grid.setColumnSelectionAllowed(false);
        grid.setIntercellSpacing(new Dimension(0, 0));

        TableColumn enabledColumn = grid.getColumnModel().getColumn(LayersTableModel.ENABLED_COL);
        enabledColumn.setCellRenderer(new CellRenderer.Enabled());
        enabledColumn.setPreferredWidth(ICON_WIDTH + 8);
        enabledColumn.setMaxWidth(ICON_WIDTH + 8);

        grid.getColumnModel().getColumn(LayersTableModel.NAME_COL).setCellRenderer(new CellRenderer.Name());

        TableColumn timeColumn = grid.getColumnModel().getColumn(LayersTableModel.TIME_COL);
        CellRenderer.Time timeRenderer = new CellRenderer.Time();
        timeRenderer.setFont(CellRenderer.Time.font);
        timeRenderer.setText("2000-01-01T12:00:00.000");
        int timeWidth = timeRenderer.getPreferredSize().width;
        timeColumn.setCellRenderer(timeRenderer);
        timeColumn.setMinWidth(timeWidth);
        timeColumn.setPreferredWidth(timeWidth);
        timeColumn.setMaxWidth(timeWidth);

        TableColumn removeColumn = grid.getColumnModel().getColumn(LayersTableModel.REMOVE_COL);
        removeColumn.setCellRenderer(new CellRenderer.Remove());
        removeColumn.setPreferredWidth(ICON_WIDTH + 2);
        removeColumn.setMaxWidth(ICON_WIDTH + 2);

        grid.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                refreshSelectedOptionsPanel();
        });

        grid.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                TableValue v = TableValue.tableValueAtPoint(grid, e.getPoint());
                if (v == null || !(v.value instanceof Layer layer))
                    return;

                if ((v.col == LayersTableModel.NAME_COL || v.col == LayersTableModel.TIME_COL) && layer instanceof ImageLayer il && e.getClickCount() == 2) {
                    MainFrame.getLayersPanel().getImageDialog().changeDataset(il);
                    return;
                }

                if (v.col == LayersTableModel.ENABLED_COL) {
                    layer.setEnabled(!layer.isEnabled());
                    model.updateCell(grid.convertRowIndexToModel(v.row), v.col);
                    DisplayController.render();
                } else if (v.col == LayersTableModel.NAME_COL && layer instanceof ImageLayer il) {
                    Layers.setActiveImageLayer(il);
                    grid.repaint(); // multiple rows involved
                } else if (v.col == LayersTableModel.REMOVE_COL && layer.isDeletable()) {
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

        if (images) {
            grid.setDragEnabled(true);
            grid.setDropMode(DropMode.INSERT_ROWS);
            grid.setTransferHandler(new TableRowTransferHandler(grid));
        }

        gc.gridy = 1;
        gc.weighty = 0;
        optionsPanelWrapper = new JPanel(new BorderLayout());
        add(optionsPanelWrapper, gc);
    }

    private static TableRowSorter<LayersTableModel> createSorter(LayersTableModel model, boolean images) {
        TableRowSorter<LayersTableModel> sorter = new TableRowSorter<>(model);
        for (int column = 0; column < model.getColumnCount(); column++)
            sorter.setSortable(column, false);
        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(RowFilter.Entry<? extends LayersTableModel, ? extends Integer> entry) {
                Layer layer = (Layer) entry.getValue(0);
                return !(layer instanceof ViewpointLayer) && (layer instanceof ImageLayer) == images;
            }
        });
        return sorter;
    }

    int getGridRowHeight() {
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
            setOptionsPanel(null);
            return;
        }
        int row = Math.min(preferredRow, rowCount - 1);
        grid.getSelectionModel().setSelectionInterval(row, row);
    }

    private void refreshSelectedOptionsPanel() {
        setOptionsPanel(selectedLayer());
    }

    void setOptionsPanel(@Nullable Layer layer) {
        Component optionsPanel = layer == null ? null : options.getOptionsPanel(layer);
        if (optionsPanel != null)
            ComponentUtils.setEnabled(optionsPanel, !(layer instanceof ImageLayer) || layer.isEnabled());
        Component current = optionsPanelWrapper.getComponentCount() == 0 ? null : optionsPanelWrapper.getComponent(0);
        if (current != optionsPanel) {
            optionsPanelWrapper.removeAll();
            if (optionsPanel != null)
                optionsPanelWrapper.add(optionsPanel, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
    }

}
