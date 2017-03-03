package org.helioviewer.jhv.timelines.view.linedataselector;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;

import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.view.linedataselector.cellrenderer.TimelineColorRenderer;
import org.helioviewer.jhv.timelines.view.linedataselector.cellrenderer.TimelineLoadingRenderer;
import org.helioviewer.jhv.timelines.view.linedataselector.cellrenderer.TimelineNameRenderer;
import org.helioviewer.jhv.timelines.view.linedataselector.cellrenderer.TimelineRemoveRenderer;
import org.helioviewer.jhv.timelines.view.linedataselector.cellrenderer.TimelineVisibleRenderer;

import com.jidesoft.swing.JideButton;

@SuppressWarnings("serial")
public class TimelinePanel extends JPanel {

    private static final int ICON_WIDTH = 12;

    private static final int VISIBLE_COL = 0;
    private static final int TITLE_COL = 1;
    private static final int LOADING_COL = 2;
    private static final int LINECOLOR_COL = 3;
    private static final int REMOVE_COL = 4;

    static final int NUMBEROFCOLUMNS = 5;
    private static final int NUMBEROFVISIBLEROWS = 4;

    private final JPanel optionsPanelWrapper;

    public TimelinePanel(TimelineTableModel model) {
        setLayout(new GridBagLayout());

        JTable grid = new JTable(model) {

            @Override
            public void changeSelection(int row, int col, boolean toggle, boolean extend) {
                if (col == VISIBLE_COL || col == REMOVE_COL) {
                    // prevent changing selection
                    return;
                }
                super.changeSelection(row, col, toggle, extend);
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
                    setRowSelectionInterval(row, row);
                }
            }

        };

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.weighty = 0;
        gc.fill = GridBagConstraints.BOTH;

        JScrollPane jsp = new JScrollPane(grid, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        jsp.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        jsp.getViewport().setBackground(grid.getBackground());

        JideButton addLayerButton = new JideButton(Buttons.newLayer);
        addLayerButton.addActionListener(e -> Timelines.td.showDialog());

        JPanel addLayerButtonWrapper = new JPanel(new BorderLayout());
        addLayerButtonWrapper.add(addLayerButton, BorderLayout.WEST);
        addLayerButtonWrapper.add(DrawController.getOptionsPanel(), BorderLayout.EAST);

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

        grid.getColumnModel().getColumn(VISIBLE_COL).setCellRenderer(new TimelineVisibleRenderer());
        grid.getColumnModel().getColumn(VISIBLE_COL).setPreferredWidth(ICON_WIDTH + 8);
        grid.getColumnModel().getColumn(VISIBLE_COL).setMaxWidth(ICON_WIDTH + 8);

        grid.getColumnModel().getColumn(TITLE_COL).setCellRenderer(new TimelineNameRenderer());

        grid.getColumnModel().getColumn(LOADING_COL).setCellRenderer(new TimelineLoadingRenderer());
        grid.getColumnModel().getColumn(LOADING_COL).setPreferredWidth(ICON_WIDTH + 2);
        grid.getColumnModel().getColumn(LOADING_COL).setMaxWidth(ICON_WIDTH + 2);

        grid.getColumnModel().getColumn(LINECOLOR_COL).setCellRenderer(new TimelineColorRenderer());
        grid.getColumnModel().getColumn(LINECOLOR_COL).setPreferredWidth(20);
        grid.getColumnModel().getColumn(LINECOLOR_COL).setMaxWidth(20);

        grid.getColumnModel().getColumn(REMOVE_COL).setCellRenderer(new TimelineRemoveRenderer());
        grid.getColumnModel().getColumn(REMOVE_COL).setPreferredWidth(ICON_WIDTH + 2);
        grid.getColumnModel().getColumn(REMOVE_COL).setMaxWidth(ICON_WIDTH + 2);

        grid.getSelectionModel().addListSelectionListener(e -> {
            int row = grid.getSelectedRow();
            if (row != -1 && !e.getValueIsAdjusting()) {
                setOptionsPanel((TimelineRenderable) grid.getValueAt(row, 0));
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
                if (row < 0 || col < 0) {
                    return;
                }

                TimelineRenderable timeline = (TimelineRenderable) grid.getValueAt(row, col);

                if (col == VISIBLE_COL) {
                    timeline.setVisibility(!timeline.isVisible());
                    model.updateCell(row, col);
                    DrawController.graphAreaChanged();
                } else if (col == REMOVE_COL && timeline.isDeletable()) {
                    model.removeLineData(timeline);
                    int idx = grid.getSelectedRow();
                    if (row <= idx)
                        grid.getSelectionModel().setSelectionInterval(idx - 1, idx - 1);
                }
            }
        });

        int h = getGridRowHeight(grid);
        jsp.setPreferredSize(new Dimension(ImageViewerGui.SIDE_PANEL_WIDTH, h * NUMBEROFVISIBLEROWS + 1));
        grid.setRowHeight(h);

        optionsPanelWrapper = new JPanel(new BorderLayout());

        gc.gridy = 1;
        add(optionsPanelWrapper, gc);
    }

    private int rowHeight = -1;

    private int getGridRowHeight(JTable table) {
        if (rowHeight == -1) {
            rowHeight = table.getRowHeight() + 4;
        }
        return rowHeight;
    }

    private void setOptionsPanel(TimelineRenderable timeline) {
        optionsPanelWrapper.removeAll();
        Component optionsPanel = timeline.getOptionsPanel();
        if (optionsPanel != null) {
            ComponentUtils.setEnabled(optionsPanel, timeline.isVisible());
            optionsPanelWrapper.add(optionsPanel, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
    }

}
