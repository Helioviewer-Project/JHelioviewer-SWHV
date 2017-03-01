package org.helioviewer.jhv.renderable.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableModel;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.actions.NewLayerAction;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialog;
import org.helioviewer.jhv.gui.interfaces.LazyComponent;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.renderable.gui.cellrenderer.DownloadingCellRenderer;
import org.helioviewer.jhv.renderable.gui.cellrenderer.RemoveCellRenderer;
import org.helioviewer.jhv.renderable.gui.cellrenderer.RenderableCellRenderer;
import org.helioviewer.jhv.renderable.gui.cellrenderer.TimeCellRenderer;
import org.helioviewer.jhv.renderable.gui.cellrenderer.VisibleCellRenderer;

import com.jidesoft.swing.JideButton;

@SuppressWarnings("serial")
public class RenderableContainerPanel extends JPanel {

    private static final int ICON_WIDTH = 12;

    private static final int VISIBLE_COL = 0;
    private static final int TITLE_COL = 1;
    public static final int TIME_COL = 2;
    private static final int DOWNLOAD_COL = 3;
    private static final int REMOVE_COL = 4;

    public static final int NUMBER_COLUMNS = 5;
    private static final int NUMBEROFVISIBLEROWS = 7;

    private final RenderableContainerTable grid;
    private final JPanel optionsPanelWrapper;

    public void lazyRepaint() {
        grid.lazyRepaint();
    }

    private static class RenderableContainerTable extends JTable implements LazyComponent {

        public RenderableContainerTable(TableModel tm) {
            super(tm);
        }

        @Override
        public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
            if (columnIndex != VISIBLE_COL && columnIndex != REMOVE_COL)
                super.changeSelection(rowIndex, columnIndex, toggle, extend);
            // otherwise prevent changing selection
        }

        @Override
        public void clearSelection() {
            // prevent losing selection
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

    public RenderableContainerPanel(RenderableContainer renderableContainer) {
        setLayout(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.weighty = 0;
        gc.fill = GridBagConstraints.BOTH;

        grid = new RenderableContainerTable(renderableContainer);

        renderableContainer.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.INSERT) {
                int idx = e.getFirstRow();
                if (grid.getValueAt(idx, 0) instanceof ImageLayer)
                    grid.getSelectionModel().setSelectionInterval(idx, idx);
            }
        });

        JScrollPane jsp = new JScrollPane(grid, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        jsp.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        jsp.getViewport().setBackground(grid.getBackground());

        JideButton addLayerButton = new JideButton(Buttons.newLayer);
        addLayerButton.addActionListener(e -> {
            NewLayerAction layerAction = new NewLayerAction();
            layerAction.actionPerformed(new ActionEvent(addLayerButton, 0, ""));
        });

        JideButton syncSpanButton = new JideButton(Buttons.syncLayers);
        syncSpanButton.setToolTipText("Synchronize layers time span");
        syncSpanButton.addActionListener(e -> {
            Layers.syncLayersSpan();
            renderableContainer.fireListeners();
        });

        JCheckBox multiview = new JCheckBox("Multiview", Displayer.multiview);
        multiview.setHorizontalTextPosition(SwingConstants.LEADING);
        multiview.addActionListener(e -> {
            Displayer.multiview = multiview.isSelected();
            Layers.arrangeMultiView(Displayer.multiview);
        });
        ComponentUtils.smallVariant(multiview);

        JPanel addLayerButtonWrapper = new JPanel(new BorderLayout());
        addLayerButtonWrapper.add(addLayerButton, BorderLayout.WEST);

        JPanel flowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        flowPanel.add(syncSpanButton);
        addLayerButtonWrapper.add(flowPanel, BorderLayout.CENTER);
        addLayerButtonWrapper.add(multiview, BorderLayout.EAST);

        JPanel jspContainer = new JPanel(new BorderLayout());
        jspContainer.add(addLayerButtonWrapper, BorderLayout.CENTER);
        jspContainer.add(jsp, BorderLayout.SOUTH);
        add(jspContainer, gc);

        grid.setTableHeader(null);
        grid.setShowGrid(false);
        grid.setRowSelectionAllowed(true);
        grid.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        grid.setColumnSelectionAllowed(false);
        grid.setIntercellSpacing(new Dimension(0, 0));

        grid.getColumnModel().getColumn(VISIBLE_COL).setCellRenderer(new VisibleCellRenderer());
        grid.getColumnModel().getColumn(VISIBLE_COL).setPreferredWidth(ICON_WIDTH + 8);
        grid.getColumnModel().getColumn(VISIBLE_COL).setMaxWidth(ICON_WIDTH + 8);

        grid.getColumnModel().getColumn(TITLE_COL).setCellRenderer(new RenderableCellRenderer());

        grid.getColumnModel().getColumn(TIME_COL).setCellRenderer(new TimeCellRenderer());
        int timeWidth = new JLabel("2000-01-01T00:00:00").getPreferredSize().width;
        grid.getColumnModel().getColumn(TIME_COL).setMinWidth(timeWidth);

        grid.getColumnModel().getColumn(DOWNLOAD_COL).setCellRenderer(new DownloadingCellRenderer());
        grid.getColumnModel().getColumn(DOWNLOAD_COL).setPreferredWidth(ICON_WIDTH + 2);
        grid.getColumnModel().getColumn(DOWNLOAD_COL).setMaxWidth(ICON_WIDTH + 2);

        grid.getColumnModel().getColumn(REMOVE_COL).setCellRenderer(new RemoveCellRenderer());
        grid.getColumnModel().getColumn(REMOVE_COL).setPreferredWidth(ICON_WIDTH + 2);
        grid.getColumnModel().getColumn(REMOVE_COL).setMaxWidth(ICON_WIDTH + 2);

        grid.getSelectionModel().addListSelectionListener(e -> {
            int row = grid.getSelectedRow();
            if (!e.getValueIsAdjusting() && row != -1 && row < grid.getRowCount()) {
                setOptionsPanel((Renderable) grid.getValueAt(row, 0));
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

            /**
             * Handle with right-click menus
             *
             * @param e
             */
            public void handlePopup(MouseEvent e) {
            }

            /**
             * Handle with clicks on hide/show/remove layer icons
             */
            @Override
            public void mouseClicked(MouseEvent e) {
                Point pt = e.getPoint();
                int row = grid.rowAtPoint(pt);
                int col = grid.columnAtPoint(pt);
                if (row < 0 || col < 0)
                    return;

                Renderable renderable = (Renderable) grid.getValueAt(row, col);

                if ((col == TITLE_COL || col == TIME_COL) && renderable instanceof ImageLayer && e.getClickCount() == 2) {
                    ObservationDialog.getInstance().showDialog(false, (ImageLayer) renderable);
                    return;
                }

                if (col == VISIBLE_COL) {
                    renderable.setVisible(!renderable.isVisible());
                    renderableContainer.fireListeners();
                    Displayer.render(1);
                }
                if (col == TITLE_COL && renderable instanceof ImageLayer) {
                    ((ImageLayer) renderable).setActiveImageLayer();
                    renderableContainer.fireListeners();
                }
                if (col == REMOVE_COL && renderable.isDeletable()) {
                    ((RenderableContainer) grid.getModel()).removeRow(row);
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

        jsp.setPreferredSize(new Dimension(ImageViewerGui.SIDE_PANEL_WIDTH, getGridRowHeight() * NUMBEROFVISIBLEROWS + 1));
        grid.setRowHeight(getGridRowHeight());

        optionsPanelWrapper = new JPanel(new BorderLayout());

        gc.gridy = 1;
        add(optionsPanelWrapper, gc);
    }

    private int rowHeight = -1;

    public int getGridRowHeight() {
        if (rowHeight == -1) {
            rowHeight = grid.getRowHeight() + 4;
        }
        return rowHeight;
    }

    public void setOptionsPanel(Renderable renderable) {
        optionsPanelWrapper.removeAll();
        Component optionsPanel = renderable.getOptionsPanel();
        if (optionsPanel != null) {
            ComponentUtils.setEnabled(optionsPanel, renderable.isVisible());
            optionsPanelWrapper.add(optionsPanel, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
    }

}
