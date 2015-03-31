package org.helioviewer.jhv.plugin.renderable;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.ImageViewerGui;

public class RenderableContainerPanel extends JPanel {
    static final Border commonBorder = new MatteBorder(1, 0, 0, 0, Color.BLACK);
    private static final int ROW_HEIGHT = 20;
    private static final int ICON_WIDTH = 16;

    private static final int VISIBLEROW = 0;
    private static final int TITLEROW = 1;
    public static final int TIMEROW = 2;
    private static final int REMOVEROW = 3;
    public static final int NUMBEROFCOLUMNS = 4;

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
        JScrollPane jsp = new JScrollPane(grid, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        jsp.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        jsp.setPreferredSize(new Dimension(ImageViewerGui.SIDE_PANEL_WIDTH, ROW_HEIGHT * 5 + 2));

        renderableContainer.addTableModelListener(grid);
        this.add(jsp, gc);
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
                RenderableContainer model = (RenderableContainer) grid.getModel();

                if (col == VISIBLEROW) {
                    Renderable renderable = (Renderable) Displayer.getRenderablecontainer().getValueAt(row, col);
                    renderable.setVisible(!renderable.isVisible());
                    Displayer.display();
                }
                if (col == TITLEROW || col == VISIBLEROW || col == TIMEROW) {
                    Renderable renderable = (Renderable) Displayer.getRenderablecontainer().getValueAt(row, col);
                    setOptionsPanel(renderable);
                }
                if (col == REMOVEROW) {
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
        JPanel addLayerButtonWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        JButton addLayerButton = new JButton(ImageViewerGui.getSingletonInstance().getImageSelectorPanel().addLayerAction);
        addLayerButton.setText("");
        addLayerButton.setToolTipText("Click to add extra layers");
        addLayerButton.setIcon(IconBank.getIcon(JHVIcon.ADD));
        addLayerButton.setBorder(null);
        addLayerButtonWrapper.add(addLayerButton);
        add(addLayerButtonWrapper, gc);

        gc.gridy = 2;
        add(optionsPanelWrapper, gc);

    }

    void setOptionsPanel(Renderable renderable) {
        optionsPanelWrapper.remove(optionsPanel);
        optionsPanel = renderable.getOptionsPanel();
        optionsPanelWrapper.add(optionsPanel);
        this.getParent().revalidate();
        this.getParent().repaint();
    }

}
