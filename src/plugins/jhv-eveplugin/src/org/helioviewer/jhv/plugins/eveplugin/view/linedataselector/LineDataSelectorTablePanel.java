package org.helioviewer.jhv.plugins.eveplugin.view.linedataselector;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.plugins.eveplugin.settings.EVESettings;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.cellrenderer.LineColorRenderer;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.cellrenderer.LineDataSelectorElementRenderer;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.cellrenderer.LineDataVisibleCellRenderer;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.cellrenderer.LoadingCellRenderer;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.cellrenderer.RemoveCellRenderer;

//Class will not be serialized so we suppress the warnings
@SuppressWarnings("serial")
public class LineDataSelectorTablePanel extends JPanel implements TableModelListener, ListSelectionListener {

    public static final Border commonBorder = new MatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY);
    public static final Border commonLeftBorder = new MatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY);
    public static final Border commonRightBorder = new MatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY);

    private static final int ROW_HEIGHT = 20;
    private static final int ICON_WIDTH = 16;

    private static final int VISIBLE_COL = 0;
    private static final int TITLE_COL = 1;
    private static final int LOADING_COL = 2;
    private static final int LINECOLOR_COL = 3;
    private static final int REMOVE_COL = 4;

    private final JTable grid;

    private final LineDataSelectorModel tableModel;
    private final JPanel optionsPanelWrapper;
    private final GridBagConstraints gc = new GridBagConstraints();
    private Component optionsPanel = new JPanel();
    private final IntervalOptionPanel intervalOptionPanel;

    private int lastSelectedIndex = -1;

    public LineDataSelectorTablePanel() {
        intervalOptionPanel = new IntervalOptionPanel();
        this.setLayout(new GridBagLayout());
        tableModel = LineDataSelectorModel.getSingletonInstance();

        grid = new JTable(tableModel) {
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
        tableModel.addTableModelListener(grid);

        grid.getSelectionModel().addListSelectionListener(this);
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.weighty = 0;
        gc.fill = GridBagConstraints.BOTH;

        JScrollPane jsp = new JScrollPane(grid, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        jsp.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        jsp.setPreferredSize(new Dimension(ImageViewerGui.SIDE_PANEL_WIDTH, ROW_HEIGHT * 4));
        jsp.getViewport().setBackground(Color.WHITE);

        JPanel jspContainer = new JPanel(new BorderLayout());

        JButton addLayerButton = new JButton();
        addLayerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ImageViewerGui.getObservationDialog().showDialog(EVESettings.OBSERVATION_UI_NAME);
            }
        });

        addLayerButton.setBorder(null);
        addLayerButton.setText("Add layer");
        addLayerButton.setHorizontalTextPosition(SwingConstants.LEADING);
        addLayerButton.setBorderPainted(false);
        addLayerButton.setFocusPainted(false);
        addLayerButton.setContentAreaFilled(false);
        addLayerButton.setIcon(IconBank.getIcon(JHVIcon.ADD));

        JPanel addLayerButtonWrapper = new JPanel(new BorderLayout());
        addLayerButtonWrapper.add(addLayerButton, BorderLayout.EAST);

        jspContainer.add(addLayerButtonWrapper, BorderLayout.CENTER);
        jspContainer.add(jsp, BorderLayout.SOUTH);
        this.add(jspContainer, gc);

        grid.setTableHeader(null);
        grid.setShowGrid(false);
        grid.setRowSelectionAllowed(true);
        grid.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        grid.setColumnSelectionAllowed(false);
        grid.setIntercellSpacing(new Dimension(0, 0));
        tableModel.addTableModelListener(this);
        grid.setRowHeight(ROW_HEIGHT);
        grid.setBackground(Color.white);
        grid.getColumnModel().getColumn(VISIBLE_COL).setCellRenderer(new LineDataVisibleCellRenderer());
        grid.getColumnModel().getColumn(VISIBLE_COL).setPreferredWidth(ICON_WIDTH + 2);
        grid.getColumnModel().getColumn(VISIBLE_COL).setMaxWidth(ICON_WIDTH + 2);

        grid.getColumnModel().getColumn(TITLE_COL).setCellRenderer(new LineDataSelectorElementRenderer());
        // grid.getColumnModel().getColumn(TITLE_COL).setPreferredWidth(80);
        // grid.getColumnModel().getColumn(TITLE_COL).setMaxWidth(80);

        grid.getColumnModel().getColumn(LINECOLOR_COL).setCellRenderer(new LineColorRenderer());
        grid.getColumnModel().getColumn(LINECOLOR_COL).setPreferredWidth(20);
        grid.getColumnModel().getColumn(LINECOLOR_COL).setMaxWidth(20);

        grid.getColumnModel().getColumn(LOADING_COL).setCellRenderer(new LoadingCellRenderer());
        grid.getColumnModel().getColumn(LOADING_COL).setPreferredWidth(20);
        grid.getColumnModel().getColumn(LOADING_COL).setMaxWidth(20);

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
                int row = grid.rowAtPoint(new Point(e.getX(), e.getY()));
                int col = grid.columnAtPoint(new Point(e.getX(), e.getY()));
                LineDataSelectorModel model = (LineDataSelectorModel) grid.getModel();

                if (col == VISIBLE_COL) {
                    LineDataSelectorElement renderable = (LineDataSelectorElement) model.getValueAt(row, col);
                    renderable.setVisibility(!renderable.isVisible());
                }
                if (col == TITLE_COL || col == LOADING_COL || col == LINECOLOR_COL) {
                    LineDataSelectorElement lineDataElement = (LineDataSelectorElement) model.getValueAt(row, col);
                    setOptionsPanel(lineDataElement);
                }
                if (col == REMOVE_COL) {
                    model.removeRow(row);
                }
                revalidate();
                repaint();
            }
        });
        // grid.setDragEnabled(true);
        // grid.setDropMode(DropMode.INSERT_ROWS);
        // grid.setTransferHandler(new TableRowTransferHandler(grid));

        optionsPanelWrapper = new JPanel();
        optionsPanelWrapper.setLayout(new BorderLayout());

        optionsPanelWrapper.add(optionsPanel, BorderLayout.CENTER);
        optionsPanelWrapper.add(intervalOptionPanel, BorderLayout.PAGE_END);

        gc.gridy = 1;
        add(optionsPanelWrapper, gc);
    }

    private void setOptionsPanel(LineDataSelectorElement lineDataElement) {
        optionsPanelWrapper.remove(optionsPanel);
        optionsPanel = null;
        if (lineDataElement != null) {
            optionsPanel = lineDataElement.getOptionsPanel();
        }
        if (optionsPanel == null) {
            optionsPanel = new JPanel();
        }
        optionsPanelWrapper.add(optionsPanel, BorderLayout.CENTER);
        revalidate();
        repaint();

    }

    @Override
    public void tableChanged(TableModelEvent e) {
        if (tableModel.getRowCount() > 0) {
            grid.setRowSelectionInterval(tableModel.getRowCount() - 1, tableModel.getRowCount() - 1);
        }
        checkOptionPanel();
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (grid.getSelectedRow() == -1 && lastSelectedIndex > -1) {
            if (tableModel.getRowCount() > lastSelectedIndex) {
                grid.setRowSelectionInterval(lastSelectedIndex, lastSelectedIndex);
            } else {
                lastSelectedIndex = grid.getSelectedRow();
            }
        } else {
            lastSelectedIndex = grid.getSelectedRow();
        }
    }

    private void checkOptionPanel() {
        int[] sr = grid.getSelectedRows();
        if (sr.length > 0) {
            setOptionsPanel((LineDataSelectorElement) tableModel.getValueAt(sr[0], 0));
        } else {
            if (tableModel.getRowCount() > 0) {
                setOptionsPanel((LineDataSelectorElement) tableModel.getValueAt(0, 0));
            } else {
                setOptionsPanel(null);
            }
        }
    }
}
