package org.helioviewer.jhv.plugin.renderable;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;

import org.helioviewer.base.datetime.ImmutableDateTime;
import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.dialogs.model.ObservationDialogDateModel;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.renderable.RenderableImageLayer;
import org.helioviewer.viewmodel.view.AbstractView;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;

@SuppressWarnings({"serial"})
public class RenderableContainerPanel extends JPanel implements LayersListener {

    static final Border commonBorder = new MatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY);
    static final Border commonLeftBorder = new MatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY);
    static final Border commonRightBorder = new MatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY);

    private static final int ROW_HEIGHT = 20;
    private static final int ICON_WIDTH = 16;

    private static final int VISIBLEROW = 0;
    private static final int TITLEROW = 1;
    public static final int TIMEROW = 2;
    private static final int REMOVEROW = 3;
    public static final int NUMBEROFCOLUMNS = 4;

    private final Action addLayerAction = new AbstractAction("Add layer", IconBank.getIcon(JHVIcon.ADD)) {
        {
            putValue(SHORT_DESCRIPTION, "Add a new layer");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(ActionEvent arg0) {
            // Check the dates if possible
            AbstractView activeView = LayersModel.getActiveView();
            if (activeView instanceof JHVJPXView) {
                JHVJPXView jpxView = (JHVJPXView) activeView;
                if (jpxView.getMaximumAccessibleFrameNumber() == jpxView.getMaximumFrameNumber()) {
                    Date start = LayersModel.getStartDate(activeView);
                    Date end = LayersModel.getEndDate(activeView);
                    try {
                        Date obsStartDate = ImmutableDateTime.apiDateFormat.parse(ImageViewerGui.getObservationImagePane().getStartTime());
                        Date obsEndDate = ImmutableDateTime.apiDateFormat.parse(ImageViewerGui.getObservationImagePane().getEndTime());
                        // only updates if its really necessary with a
                        // tolerance of an hour
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
            }
            // Show dialog
            ImageViewerGui.getObservationDialog().showDialog();
        }
    };

    public final JTable grid;
    private Component optionsPanel = null;
    private final GridBagConstraints gc = new GridBagConstraints();
    private final JPanel optionsPanelWrapper;

    public RenderableContainerPanel(final RenderableContainer renderableContainer) {
        this.setLayout(new GridBagLayout());
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.weighty = 0;
        gc.fill = GridBagConstraints.BOTH;
        grid = new JTable(renderableContainer);
        renderableContainer.addTableModelListener(grid);

        JScrollPane jsp = new JScrollPane(grid, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        jsp.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        jsp.setPreferredSize(new Dimension(ImageViewerGui.SIDE_PANEL_WIDTH, ROW_HEIGHT * 5 + 2));

        JPanel jspContainer = new JPanel(new BorderLayout());
        jspContainer.setBorder(BorderFactory.createTitledBorder(""));
        jspContainer.add(jsp, BorderLayout.NORTH);
        this.add(jspContainer, gc);

        grid.setTableHeader(null);
        grid.setShowGrid(false);
        grid.setRowSelectionAllowed(true);
        grid.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        grid.setColumnSelectionAllowed(false);
        grid.setIntercellSpacing(new Dimension(0, 0));

        grid.setRowHeight(ROW_HEIGHT);
        grid.setBackground(Color.white);
        grid.getColumnModel().getColumn(VISIBLEROW).setCellRenderer(new RenderableVisibleCellRenderer());
        grid.getColumnModel().getColumn(VISIBLEROW).setPreferredWidth(ICON_WIDTH + 3);
        grid.getColumnModel().getColumn(VISIBLEROW).setMaxWidth(ICON_WIDTH + 3);

        grid.getColumnModel().getColumn(TITLEROW).setCellRenderer(new RenderableCellRenderer());
        grid.getColumnModel().getColumn(TITLEROW).setPreferredWidth(80);
        grid.getColumnModel().getColumn(TITLEROW).setMaxWidth(80);

        grid.getColumnModel().getColumn(TIMEROW).setCellRenderer(new RenderableTimeCellRenderer());

        grid.getColumnModel().getColumn(REMOVEROW).setCellRenderer(new RenderableRemoveCellRenderer());
        grid.getColumnModel().getColumn(REMOVEROW).setPreferredWidth(ICON_WIDTH);
        grid.getColumnModel().getColumn(REMOVEROW).setMaxWidth(ICON_WIDTH);

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
                Renderable renderable = (Renderable) renderableContainer.getValueAt(row, col);

                if (col == VISIBLEROW) {
                    renderable.setVisible(!renderable.isVisible());
                    Displayer.display();
                }
                if (col == TITLEROW || col == VISIBLEROW || col == TIMEROW) {
                    if (renderable instanceof RenderableImageLayer) {
                        LayersModel.setActiveLayer(((RenderableImageLayer) renderable).getMainLayerView());
                    }
                    setOptionsPanel(renderable);
                }
                if (col == REMOVEROW && renderable.isDeletable()) {
                    ((RenderableContainer) grid.getModel()).removeRow(row);
                    Displayer.display();
                }
            }
        });

        grid.setDragEnabled(true);
        grid.setDropMode(DropMode.INSERT_ROWS);
        grid.setTransferHandler(new TableRowTransferHandler(grid));

        optionsPanelWrapper = new JPanel(new BorderLayout());

        JButton addLayerButton = new JButton(addLayerAction);
        addLayerButton.setBorder(null);
        addLayerButton.setText(null);
        addLayerButton.setBorderPainted(false);
        addLayerButton.setFocusPainted(false);
        addLayerButton.setContentAreaFilled(false);

        addLayerButton.setToolTipText("Add extra data layers");
        addLayerButton.setIcon(IconBank.getIcon(JHVIcon.ADD));

        JPanel addLayerButtonWrapper = new JPanel(new BorderLayout());
        addLayerButtonWrapper.add(addLayerButton, BorderLayout.EAST);
        jspContainer.add(addLayerButtonWrapper, BorderLayout.CENTER);

        gc.gridy = 1;
        add(optionsPanelWrapper, gc);
        LayersModel.addLayersListener(this);
    }

    void setOptionsPanel(Renderable renderable) {
        setOptionsPanel(renderable.getOptionsPanel());
    }

    private void setOptionsPanel(Component cmp) {
        if (optionsPanel != null)
            optionsPanelWrapper.remove(optionsPanel);
        optionsPanel = cmp;
        if (optionsPanel != null)
            optionsPanelWrapper.add(optionsPanel, BorderLayout.CENTER);

        // this.getParent().revalidate();
        // this.getParent().repaint();
        // java 6 compat
        this.getRootPane().revalidate();
    }

    @Override
    public void layerAdded(int idx) {
    }

    @Override
    public void layerRemoved(int oldIdx) {
    }

    @Override
    public void activeLayerChanged(AbstractView view) {
        if (view != null) {
            setOptionsPanel(view.getImageLayer());
            int index = ImageViewerGui.getRenderableContainer().getRowIndex(view.getImageLayer());
            grid.getSelectionModel().setSelectionInterval(index, index);
        } else {
            JPanel jpl = new JPanel();
            jpl.add(new JLabel("No layer selected"));
            setOptionsPanel(jpl);
        }
    }

}
