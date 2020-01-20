package org.helioviewer.jhv.layers.selector;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableModel;

import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.gui.dialogs.ObservationDialog;
import org.helioviewer.jhv.gui.interfaces.LazyComponent;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.layers.selector.cellrenderer.RendererEnabled;
import org.helioviewer.jhv.layers.selector.cellrenderer.RendererLoading;
import org.helioviewer.jhv.layers.selector.cellrenderer.RendererName;
import org.helioviewer.jhv.layers.selector.cellrenderer.RendererRemove;
import org.helioviewer.jhv.layers.selector.cellrenderer.RendererTime;

@SuppressWarnings("serial")
public class LayersPanel extends JPanel {

    private static final int ICON_WIDTH = 12;

    private static final int ENABLED_COL = 0;
    private static final int TITLE_COL = 1;
    public static final int TIME_COL = 2;
    private static final int DOWNLOAD_COL = 3;
    private static final int REMOVE_COL = 4;

    public static final int NUMBER_COLUMNS = 5;
    private static final int NUMBEROFVISIBLEROWS = 7;

    private final LayersTable grid;
    private final JPanel optionsPanelWrapper;

    private static class LayersTable extends JTable implements LazyComponent {

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
        jsp.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        jsp.getViewport().setBackground(grid.getBackground());
        add(jsp, gc);

        grid.setTableHeader(null);
        grid.setShowGrid(false);
        grid.setRowSelectionAllowed(true);
        grid.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        grid.setColumnSelectionAllowed(false);
        grid.setIntercellSpacing(new Dimension(0, 0));

        grid.getColumnModel().getColumn(ENABLED_COL).setCellRenderer(new RendererEnabled());
        grid.getColumnModel().getColumn(ENABLED_COL).setPreferredWidth(ICON_WIDTH + 8);
        grid.getColumnModel().getColumn(ENABLED_COL).setMaxWidth(ICON_WIDTH + 8);

        grid.getColumnModel().getColumn(TITLE_COL).setCellRenderer(new RendererName());

        grid.getColumnModel().getColumn(TIME_COL).setCellRenderer(new RendererTime());
        int timeWidth = new JLabel("2000-01-01T00:00:00").getPreferredSize().width;
        grid.getColumnModel().getColumn(TIME_COL).setMinWidth(timeWidth);

        grid.getColumnModel().getColumn(DOWNLOAD_COL).setCellRenderer(new RendererLoading());
        grid.getColumnModel().getColumn(DOWNLOAD_COL).setPreferredWidth(ICON_WIDTH + 2);
        grid.getColumnModel().getColumn(DOWNLOAD_COL).setMaxWidth(ICON_WIDTH + 2);

        grid.getColumnModel().getColumn(REMOVE_COL).setCellRenderer(new RendererRemove());
        grid.getColumnModel().getColumn(REMOVE_COL).setPreferredWidth(ICON_WIDTH + 2);
        grid.getColumnModel().getColumn(REMOVE_COL).setMaxWidth(ICON_WIDTH + 2);

        grid.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                setOptionsPanel((Layer) grid.getValueAt(grid.getSelectedRow(), 0));
            }
        });

        grid.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    handlePopup(e);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    handlePopup(e);
                }
            }

            private void handlePopup(MouseEvent e) {
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                Point pt = e.getPoint();
                int row = grid.rowAtPoint(pt);
                int col = grid.columnAtPoint(pt);
                Object obj = grid.getValueAt(row, col);
                if (!(obj instanceof Layer))
                    return;

                Layer layer = (Layer) obj;

                if ((col == TITLE_COL || col == TIME_COL) && layer instanceof ImageLayer && e.getClickCount() == 2) {
                    ObservationDialog.getInstance().showDialog(false, (ImageLayer) layer);
                    return;
                }

                if (col == ENABLED_COL) {
                    layer.setEnabled(!layer.isEnabled());
                    model.updateCell(row, col);
                    MovieDisplay.render(1);
                } else if (col == TITLE_COL && layer instanceof ImageLayer) {
                    Layers.setActiveImageLayer((ImageLayer) layer);
                    grid.repaint(); // multiple rows involved
                } else if (col == REMOVE_COL && layer.isDeletable()) {
                    model.remove(layer);
                    int idx = grid.getSelectedRow();
                    if (row <= idx)
                        grid.getSelectionModel().setSelectionInterval(idx - 1, idx - 1);
                }
            }
        });

        grid.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = grid.rowAtPoint(e.getPoint());
                if (grid.getValueAt(row, 0) instanceof ImageLayer) {
                    grid.setCursor(UIGlobals.openHandCursor);
                } else {
                    grid.setCursor(Cursor.getDefaultCursor());
                }
            }
        });

        grid.setDragEnabled(true);
        grid.setDropMode(DropMode.INSERT_ROWS);
        grid.setTransferHandler(new TableRowTransferHandler(grid));

        jsp.setPreferredSize(new Dimension(-1, getGridRowHeight() * NUMBEROFVISIBLEROWS + 1));
        grid.setRowHeight(getGridRowHeight());

        gc.gridy = 1;
        optionsPanelWrapper = new JPanel(new BorderLayout());
        add(optionsPanelWrapper, gc);
    }

    private int rowHeight = -1;

    public int getGridRowHeight() {
        if (rowHeight == -1) {
            rowHeight = grid.getRowHeight() + 4;
        }
        return rowHeight;
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
