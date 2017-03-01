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
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;

import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.view.linedataselector.cellrenderer.LineColorRenderer;
import org.helioviewer.jhv.timelines.view.linedataselector.cellrenderer.LineDataSelectorElementRenderer;
import org.helioviewer.jhv.timelines.view.linedataselector.cellrenderer.LineDataVisibleCellRenderer;
import org.helioviewer.jhv.timelines.view.linedataselector.cellrenderer.LoadingCellRenderer;
import org.helioviewer.jhv.timelines.view.linedataselector.cellrenderer.RemoveCellRenderer;

import com.jidesoft.swing.JideButton;

@SuppressWarnings("serial")
public class LineDataSelectorTablePanel extends JPanel {

    public static final Border commonBorder = new MatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY);

    private static final int ICON_WIDTH = 12;
    private static final int VISIBLE_COL = 0;
    private static final int TITLE_COL = 1;
    private static final int LOADING_COL = 2;
    private static final int LINECOLOR_COL = 3;
    private static final int REMOVE_COL = 4;

    private final JPanel optionsPanelWrapper;

    public LineDataSelectorTablePanel() {
        setLayout(new GridBagLayout());

        JTable grid = new JTable(Timelines.ldsm) {

            @Override
            public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
                if (columnIndex == VISIBLE_COL || columnIndex == REMOVE_COL) {
                    // prevent changing selection
                    return;
                }
                super.changeSelection(rowIndex, columnIndex, toggle, extend);
            }

            @Override
            public void clearSelection() {
                // prevent losing selection
            }

        };

        LineDataSelectorModel.addLineDataSelectorModelListener(new LineDataSelectorModelListener() {

            @Override
            public void lineDataAdded(LineDataSelectorElement element) {
                int i = LineDataSelectorModel.getRowIndex(element);
                grid.getSelectionModel().setSelectionInterval(i, i);
            }

            @Override
            public void lineDataRemoved() {
                int i = Timelines.ldsm.getRowCount() - 1;
                if (i >= 0) {
                    grid.getSelectionModel().setSelectionInterval(i, i);
                }
            }

            @Override
            public void lineDataVisibility() {
            }

        });

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

        grid.getColumnModel().getColumn(VISIBLE_COL).setCellRenderer(new LineDataVisibleCellRenderer());
        grid.getColumnModel().getColumn(VISIBLE_COL).setPreferredWidth(ICON_WIDTH + 8);
        grid.getColumnModel().getColumn(VISIBLE_COL).setMaxWidth(ICON_WIDTH + 8);

        grid.getColumnModel().getColumn(TITLE_COL).setCellRenderer(new LineDataSelectorElementRenderer());

        grid.getColumnModel().getColumn(LINECOLOR_COL).setCellRenderer(new LineColorRenderer());
        grid.getColumnModel().getColumn(LINECOLOR_COL).setPreferredWidth(20);
        grid.getColumnModel().getColumn(LINECOLOR_COL).setMaxWidth(20);

        grid.getColumnModel().getColumn(LOADING_COL).setCellRenderer(new LoadingCellRenderer());
        grid.getColumnModel().getColumn(LOADING_COL).setPreferredWidth(ICON_WIDTH + 2);
        grid.getColumnModel().getColumn(LOADING_COL).setMaxWidth(ICON_WIDTH + 2);

        grid.getColumnModel().getColumn(REMOVE_COL).setCellRenderer(new RemoveCellRenderer());
        grid.getColumnModel().getColumn(REMOVE_COL).setPreferredWidth(ICON_WIDTH + 2);
        grid.getColumnModel().getColumn(REMOVE_COL).setMaxWidth(ICON_WIDTH + 2);

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

                LineDataSelectorModel model = (LineDataSelectorModel) grid.getModel();

                if (col == VISIBLE_COL) {
                    LineDataSelectorElement lineDataElement = (LineDataSelectorElement) model.getValueAt(row, col);
                    boolean visible = !lineDataElement.isVisible();

                    lineDataElement.setVisibility(visible);
                    LineDataSelectorModel.fireLineDataSelectorElementVisibility(lineDataElement, visible);
                }
                if (col == TITLE_COL || col == LOADING_COL || col == LINECOLOR_COL) {
                    LineDataSelectorElement lineDataElement = (LineDataSelectorElement) model.getValueAt(row, col);
                    setOptionsPanel(lineDataElement);
                }
                if (col == REMOVE_COL) {
                    LineDataSelectorModel.removeRow(row);
                }
                revalidate();
                repaint();
            }
        });

        grid.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                setOptionsPanel((LineDataSelectorElement) grid.getValueAt(grid.getSelectedRow(), 0));
            }
        });

        int h = getGridRowHeight(grid);
        jsp.setPreferredSize(new Dimension(ImageViewerGui.SIDE_PANEL_WIDTH, h * 4 + 1));
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

    private void setOptionsPanel(LineDataSelectorElement lineDataElement) {
        optionsPanelWrapper.removeAll();
        Component optionsPanel = lineDataElement.getOptionsPanel();
        if (optionsPanel != null) {
            ComponentUtils.setEnabled(optionsPanel, lineDataElement.isVisible());
            optionsPanelWrapper.add(optionsPanel, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
    }

}
