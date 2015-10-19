package org.helioviewer.jhv.renderable.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.time.TimeUtils;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.dialogs.model.ObservationDialogDateModel;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.renderable.components.RenderableImageLayer;
import org.helioviewer.viewmodel.view.View;

@SuppressWarnings("serial")
public class RenderableContainerPanel extends JPanel {

    static final Border commonBorder = new MatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY);
    static final Border commonLeftBorder = new MatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY);
    static final Border commonRightBorder = new MatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY);

    private static final int ROW_HEIGHT = 20;
    private static final int ICON_WIDTH = 16;
    private static final int TITLE_WIDTH = 140;

    private static final int VISIBLE_COL = 0;
    private static final int TITLE_COL = 1;
    public static final int TIME_COL = 2;
    private static final int REMOVE_COL = 3;

    public static final int NUMBER_COLUMNS = 4;
    private static final int NUMBEROFVISIBLEROWS = 7;

    private final Action addLayerAction = new AbstractAction() {
        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(ActionEvent arg0) {
            // Check the dates if possible
            View activeView = Layers.getActiveView();
            if (activeView != null && activeView.isMultiFrame()) {
                Date start = Layers.getStartDate(activeView);
                Date end = Layers.getEndDate(activeView);
                try {
                    Date obsStartDate = TimeUtils.apiDateFormat.parse(ImageViewerGui.getObservationImagePane().getStartTime());
                    Date obsEndDate = TimeUtils.apiDateFormat.parse(ImageViewerGui.getObservationImagePane().getEndTime());
                    // only updates if it's really necessary with a tolerance of an hour
                    final int tolerance = 60 * 60 * 1000;
                    if (Math.abs(start.getTime() - obsStartDate.getTime()) > tolerance || Math.abs(end.getTime() - obsEndDate.getTime()) > tolerance) {
                        if (ObservationDialogDateModel.getInstance().getStartDate() == null || !ObservationDialogDateModel.getInstance().isStartDateSetByUser()) {
                            ObservationDialogDateModel.getInstance().setStartDate(start, false);
                        }
                        if (ObservationDialogDateModel.getInstance().getEndDate() == null || !ObservationDialogDateModel.getInstance().isEndDateSetByUser()) {
                            ObservationDialogDateModel.getInstance().setEndDate(end, false);
                        }
                    }
                } catch (ParseException e) {
                    // Should not happen
                    Log.error("Cannot update observation dialog", e);
                }
            }
            // Show dialog
            ImageViewerGui.getObservationDialog().showDialog();
        }
    };

    private final JTable grid;
    private final JPanel optionsPanelWrapper;

    public RenderableContainerPanel(final RenderableContainer renderableContainer) {
        setLayout(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.weighty = 0;
        gc.fill = GridBagConstraints.BOTH;

        grid = new JTable(renderableContainer) {
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
                };

        renderableContainer.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.INSERT) {
                    int idx = e.getFirstRow();
                    if (grid.getValueAt(idx, 0) instanceof RenderableImageLayer)
                        grid.getSelectionModel().setSelectionInterval(idx, idx);
                }
            }
        });

        JScrollPane jsp = new JScrollPane(grid, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        jsp.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        jsp.setPreferredSize(new Dimension(ImageViewerGui.SIDE_PANEL_WIDTH, ROW_HEIGHT * NUMBEROFVISIBLEROWS));
        jsp.getViewport().setBackground(Color.WHITE);

        JPanel jspContainer = new JPanel(new BorderLayout());
        JButton addLayerButton = new JButton(addLayerAction);
        addLayerButton.setBorder(null);
        addLayerButton.setText("Add layer");
        addLayerButton.setHorizontalTextPosition(SwingConstants.LEADING);
        addLayerButton.setBorderPainted(false);
        addLayerButton.setFocusPainted(false);
        addLayerButton.setContentAreaFilled(false);

        addLayerButton.setIcon(IconBank.getIcon(JHVIcon.ADD));
        final JCheckBox multiview = new JCheckBox("Multiview", Displayer.multiview);
        multiview.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Displayer.multiview = multiview.isSelected();
                ImageViewerGui.getRenderableContainer().arrangeMultiView(Displayer.multiview);
            }
        });
        JPanel addLayerButtonWrapper = new JPanel(new BorderLayout());
        addLayerButtonWrapper.add(addLayerButton, BorderLayout.EAST);
        // addLayerButtonWrapper.add(multiview, BorderLayout.CENTER);

        jspContainer.add(addLayerButtonWrapper, BorderLayout.CENTER);
        jspContainer.add(jsp, BorderLayout.SOUTH);
        add(jspContainer, gc);

        grid.setTableHeader(null);
        grid.setShowGrid(false);
        grid.setRowSelectionAllowed(true);
        grid.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        grid.setColumnSelectionAllowed(false);
        grid.setIntercellSpacing(new Dimension(0, 0));

        grid.setRowHeight(ROW_HEIGHT);
        grid.setBackground(Color.white);

        grid.getColumnModel().getColumn(VISIBLE_COL).setCellRenderer(new RenderableVisibleCellRenderer());
        grid.getColumnModel().getColumn(VISIBLE_COL).setPreferredWidth(ICON_WIDTH + 2);
        grid.getColumnModel().getColumn(VISIBLE_COL).setMaxWidth(ICON_WIDTH + 2);

        grid.getColumnModel().getColumn(TITLE_COL).setCellRenderer(new RenderableCellRenderer());
        grid.getColumnModel().getColumn(TITLE_COL).setPreferredWidth(TITLE_WIDTH);
        grid.getColumnModel().getColumn(TITLE_COL).setMaxWidth(TITLE_WIDTH);

        grid.getColumnModel().getColumn(TIME_COL).setCellRenderer(new RenderableTimeCellRenderer());

        grid.getColumnModel().getColumn(REMOVE_COL).setCellRenderer(new RenderableRemoveCellRenderer());
        grid.getColumnModel().getColumn(REMOVE_COL).setPreferredWidth(ICON_WIDTH + 2);
        grid.getColumnModel().getColumn(REMOVE_COL).setMaxWidth(ICON_WIDTH + 2);

        grid.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    setOptionsPanel((Renderable) grid.getValueAt(grid.getSelectedRow(), 0));
                }
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
                int row = grid.rowAtPoint(new Point(e.getX(), e.getY()));
                int col = grid.columnAtPoint(new Point(e.getX(), e.getY()));
                Renderable renderable = (Renderable) grid.getValueAt(row, col);

                if (col == VISIBLE_COL) {
                    renderable.setVisible(!renderable.isVisible());
                    Displayer.getViewports()[0].computeActive();
                    renderableContainer.fireListeners();
                    Displayer.display();
                    renderableContainer.arrangeMultiView(Displayer.multiview);
                }
                if (col == TITLE_COL && renderable instanceof RenderableImageLayer) {
                    Layers.setActiveView(((RenderableImageLayer) renderable).getView());
                    renderableContainer.fireListeners();
                }
                if (col == REMOVE_COL && renderable.isDeletable()) {
                    ((RenderableContainer) grid.getModel()).removeRow(row);
                    int idx = grid.getSelectedRow();
                    if (row < idx)
                        grid.getSelectionModel().setSelectionInterval(idx - 1, idx - 1);
                }
            }
        });

        grid.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent arg0) {
                int row = grid.rowAtPoint(arg0.getPoint());
                if (grid.getValueAt(row, 0) instanceof RenderableImageLayer) {
                    grid.setCursor(UIGlobals.openHandCursor);
                } else {
                    grid.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });

        grid.setDragEnabled(true);
        grid.setDropMode(DropMode.INSERT_ROWS);
        grid.setTransferHandler(new TableRowTransferHandler(grid));

        optionsPanelWrapper = new JPanel(new BorderLayout());

        gc.gridy = 1;
        add(optionsPanelWrapper, gc);
    }

    private void setOptionsPanel(Renderable renderable) {
        Component optionsPanel = renderable.getOptionsPanel();
        if (optionsPanel != null) {
            ComponentUtils.enableComponents(optionsPanel, renderable.isVisible());
        }
        setOptionsComponent(optionsPanel);
    }

    private void setOptionsComponent(Component cmp) {
        optionsPanelWrapper.removeAll();
        if (cmp != null) {
            optionsPanelWrapper.add(cmp, BorderLayout.CENTER);
        }
        super.revalidate();
        // super.repaint();
    }

}
