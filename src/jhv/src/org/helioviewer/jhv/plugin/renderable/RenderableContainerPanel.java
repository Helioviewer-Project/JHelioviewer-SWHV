package org.helioviewer.jhv.plugin.renderable;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;

import org.helioviewer.jhv.display.Displayer;

public class RenderableContainerPanel extends JPanel {
    static final Border commonBorder = new MatteBorder(1, 0, 0, 0, Color.BLACK);
    private static final int ROW_HEIGHT = 20;
    private static final int ICON_WIDTH = 16;

    private static final int VISIBLEROW = 0;
    private static final int TITLEROW = 1;
    private static final int REMOVEROW = 2;
    public static final int NUMBEROFCOLUMNS = 3;

    public final JTable grid;
    private Component optionsPanel = new JPanel();
    GridBagConstraints gc = new GridBagConstraints();
    private final JPanel optionsPanelWrapper;

    public RenderableContainerPanel(final RenderableContainer renderableContainer) {
        this.setLayout(new GridBagLayout());
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.weighty = 0;
        gc.fill = GridBagConstraints.BOTH;
        grid = new JTable(renderableContainer);
        this.add(grid, gc);
        grid.setTableHeader(null);
        grid.setShowGrid(false);
        grid.setRowSelectionAllowed(true);
        grid.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        grid.setColumnSelectionAllowed(false);
        grid.setIntercellSpacing(new Dimension(0, 0));

        grid.setRowHeight(ROW_HEIGHT);
        grid.setBackground(Color.white);
        grid.getColumnModel().getColumn(VISIBLEROW).setCellRenderer(new RenderableVisibleCellRenderer());
        grid.getColumnModel().getColumn(VISIBLEROW).setPreferredWidth(ICON_WIDTH);
        grid.getColumnModel().getColumn(VISIBLEROW).setMaxWidth(ICON_WIDTH);

        grid.getColumnModel().getColumn(TITLEROW).setCellRenderer(new RenderableCellRenderer());
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
                RenderableContainer model = (RenderableContainer) grid.getModel();

                if (col == VISIBLEROW) {
                    Renderable renderable = (Renderable) renderableContainer.getValueAt(row, col);
                    renderable.setVisible(!renderable.isVisible());
                    model.fireTableCellUpdated(row, col);
                    Displayer.display();
                } else if (col == TITLEROW) {
                    Renderable renderable = (Renderable) model.getValueAt(row, col);
                    setOptionsPanel(renderable);
                } else if (col == REMOVEROW) {
                    model.removeRow(row);
                    Displayer.display();
                }
            }
        });
        grid.setDragEnabled(true);
        grid.setDropMode(DropMode.INSERT_ROWS);
        grid.setTransferHandler(new TableRowTransferHandler(grid));

        optionsPanelWrapper = new JPanel();
        optionsPanelWrapper.setBorder(BorderFactory.createTitledBorder("Options"));
        optionsPanelWrapper.add(optionsPanel);
        gc.gridy = 1;
        add(optionsPanelWrapper, gc);

    }

    private void setOptionsPanel(Renderable renderable) {
        optionsPanelWrapper.remove(optionsPanel);
        optionsPanel = renderable.getOptionsPanel();
        optionsPanelWrapper.add(optionsPanel);
        this.getParent().revalidate();
        this.getParent().repaint();
    }

}
