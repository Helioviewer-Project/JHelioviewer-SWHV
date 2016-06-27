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

import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
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

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.actions.NewLayerAction;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialog;
import org.helioviewer.jhv.io.APIRequestManager;
import org.helioviewer.jhv.layers.ImageLayer;

@SuppressWarnings("serial")
public class RenderableContainerPanel extends JPanel {

    static final Border commonBorder = new MatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY);

    private static final int ICON_WIDTH = 20;

    private static final int VISIBLE_COL = 0;
    private static final int TITLE_COL = 1;
    public static final int TIME_COL = 2;
    private static final int REMOVE_COL = 3;

    public static final int NUMBER_COLUMNS = 4;
    private static final int NUMBEROFVISIBLEROWS = 7;

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
                    if (grid.getValueAt(idx, 0) instanceof ImageLayer)
                        grid.getSelectionModel().setSelectionInterval(idx, idx);
                }
            }
        });

        JScrollPane jsp = new JScrollPane(grid, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        jsp.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        jsp.getViewport().setBackground(Color.WHITE);

        JButton addLayerButton = new JButton("New Layer", IconBank.getIcon(JHVIcon.ADD));
        addLayerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NewLayerAction layerAction = new NewLayerAction(true, false);
                layerAction.actionPerformed(new ActionEvent(this, 0, ""));
            }
        });
        // addLayerButton.setBorder(null);
        addLayerButton.setHorizontalTextPosition(SwingConstants.LEADING);
        addLayerButton.setBorderPainted(false);
        addLayerButton.setFocusPainted(false);
        addLayerButton.setContentAreaFilled(false);

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
        addLayerButtonWrapper.add(multiview, BorderLayout.CENTER);

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

        grid.setBackground(Color.white);

        grid.getColumnModel().getColumn(VISIBLE_COL).setCellRenderer(new RenderableVisibleCellRenderer());
        grid.getColumnModel().getColumn(VISIBLE_COL).setPreferredWidth(ICON_WIDTH + 2);
        grid.getColumnModel().getColumn(VISIBLE_COL).setMaxWidth(ICON_WIDTH + 2);

        grid.getColumnModel().getColumn(TITLE_COL).setCellRenderer(new RenderableCellRenderer());

        grid.getColumnModel().getColumn(TIME_COL).setCellRenderer(new RenderableTimeCellRenderer());
        int timeWidth = (new JLabel("2000-01-01T00:00:00")).getPreferredSize().width;
        grid.getColumnModel().getColumn(TITLE_COL).setPreferredWidth(timeWidth);
        grid.getColumnModel().getColumn(TITLE_COL).setMaxWidth(timeWidth);

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
                Point pt = e.getPoint();
                int row = grid.rowAtPoint(pt);
                int col = grid.columnAtPoint(pt);
                if (row < 0 || col < 0)
                    return;

                Renderable renderable = (Renderable) grid.getValueAt(row, col);

                if ((col == TITLE_COL || col == TIME_COL) && renderable instanceof ImageLayer && e.getClickCount() == 2) {
                    APIRequestManager.APIRequest apiRequest = ((ImageLayer) renderable).getAPIRequest();
                    if (apiRequest != null) { // loaded and remote
                        ObservationDialog.getInstance().getObservationImagePane().setStartTime(apiRequest.startTime, false);
                        ObservationDialog.getInstance().getObservationImagePane().setEndTime(apiRequest.endTime, false);
                        ObservationDialog.getInstance().getObservationImagePane().setCadence(apiRequest.cadence);
                        ObservationDialog.getInstance().getObservationImagePane().setSourceSelection(apiRequest.server, apiRequest.sourceId);
                        ObservationDialog.getInstance().showDialog();
                        return;
                    }
                }

                if (col == VISIBLE_COL) {
                    renderable.setVisible(!renderable.isVisible());
                    renderableContainer.fireListeners();
                    Displayer.display();
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
            public void mouseMoved(MouseEvent arg0) {
                int row = grid.rowAtPoint(arg0.getPoint());
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

    private void setOptionsPanel(Renderable renderable) {
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
