package org.helioviewer.plugins.eveplugin.view.linedataselector;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.plugin.renderable.Renderable;
import org.helioviewer.jhv.plugin.renderable.RenderableRemoveCellRenderer;
import org.helioviewer.jhv.plugin.renderable.TableRowTransferHandler;
import org.helioviewer.plugins.eveplugin.settings.EVESettings;
import org.helioviewer.plugins.eveplugin.view.linedataselector.cellrenderer.LineDataSelectorElementRenderer;
import org.helioviewer.plugins.eveplugin.view.linedataselector.cellrenderer.LineDataVisibleCellRenderer;
import org.helioviewer.plugins.eveplugin.view.linedataselector.cellrenderer.LoadingCellRenderer;

public class LineDateSelectorTablePanel extends JPanel {

    private static final int ROW_HEIGHT = 20;
    private static final int ICON_WIDTH = 16;

    private static final int VISIBLE_ROW = 0;
    private static final int TITLE_ROW = 1;
    public static final int LOADING_ROW = 2;
    private static final int REMOVE_ROW = 3;

    private final JTable grid;

    private final LineDataSelectorTableModel tableModel;
    private final JPanel optionsPanelWrapper;
    GridBagConstraints gc = new GridBagConstraints();
    private Component optionsPanel = new JPanel();

    public LineDateSelectorTablePanel() {
        this.setLayout(new GridBagLayout());
        tableModel = new LineDataSelectorTableModel();
        grid = new JTable(tableModel);
        tableModel.addTableModelListener(grid);

        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.weighty = 0;
        gc.fill = GridBagConstraints.BOTH;

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
        grid.getColumnModel().getColumn(VISIBLE_ROW).setCellRenderer(new LineDataVisibleCellRenderer());
        grid.getColumnModel().getColumn(VISIBLE_ROW).setPreferredWidth(ICON_WIDTH);
        grid.getColumnModel().getColumn(VISIBLE_ROW).setMaxWidth(ICON_WIDTH);

        grid.getColumnModel().getColumn(TITLE_ROW).setCellRenderer(new LineDataSelectorElementRenderer());
        grid.getColumnModel().getColumn(TITLE_ROW).setPreferredWidth(80);
        grid.getColumnModel().getColumn(TITLE_ROW).setMaxWidth(80);

        grid.getColumnModel().getColumn(LOADING_ROW).setCellRenderer(new LoadingCellRenderer());

        grid.getColumnModel().getColumn(REMOVE_ROW).setCellRenderer(new RenderableRemoveCellRenderer());
        grid.getColumnModel().getColumn(REMOVE_ROW).setPreferredWidth(ICON_WIDTH);
        grid.getColumnModel().getColumn(REMOVE_ROW).setMaxWidth(ICON_WIDTH);

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
                LineDataSelectorTableModel model = (LineDataSelectorTableModel) grid.getModel();

                if (col == VISIBLE_ROW) {
                    Renderable renderable = (Renderable) Displayer.getRenderablecontainer().getValueAt(row, col);
                    renderable.setVisible(!renderable.isVisible());
                }
                if (col == TITLE_ROW || col == VISIBLE_ROW || col == LOADING_ROW) {
                    LineDataSelectorElement lineDataElement = (LineDataSelectorElement) Displayer.getRenderablecontainer().getValueAt(row, col);
                    setOptionsPanel(lineDataElement);
                }
                if (col == REMOVE_ROW) {
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

        JPanel addLayerButtonWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        JButton addLayerButton = new JButton();
        addLayerButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ImageViewerGui.getSingletonInstance().getObservationDialog().showDialog(EVESettings.OBSERVATION_UI_NAME);
            }
        });
        addLayerButton.setText("");
        addLayerButton.setToolTipText("Click to add extra layers");
        addLayerButton.setIcon(IconBank.getIcon(JHVIcon.ADD));
        addLayerButton.setBorder(null);
        addLayerButtonWrapper.add(addLayerButton);
        jspContainer.add(addLayerButtonWrapper, BorderLayout.CENTER);

        gc.gridy = 1;
        add(optionsPanelWrapper, gc);
    }

    private void setOptionsPanel(LineDataSelectorElement lineDataElement) {
        optionsPanelWrapper.remove(optionsPanel);
        optionsPanel = lineDataElement.getOptionsPanel();
        optionsPanelWrapper.add(optionsPanel);
        this.getParent().revalidate();
        this.getParent().repaint();
    }
}
