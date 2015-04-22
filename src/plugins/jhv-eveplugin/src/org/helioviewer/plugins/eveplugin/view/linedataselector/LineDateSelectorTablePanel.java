package org.helioviewer.plugins.eveplugin.view.linedataselector;

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
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.plugin.renderable.RenderableRemoveCellRenderer;
import org.helioviewer.jhv.plugin.renderable.TableRowTransferHandler;
import org.helioviewer.plugins.eveplugin.settings.EVESettings;
import org.helioviewer.plugins.eveplugin.view.linedataselector.cellrenderer.LineDataSelectorElementRenderer;
import org.helioviewer.plugins.eveplugin.view.linedataselector.cellrenderer.LineDataVisibleCellRenderer;
import org.helioviewer.plugins.eveplugin.view.linedataselector.cellrenderer.LoadingCellRenderer;

public class LineDateSelectorTablePanel extends JPanel implements TableModelListener, LineDataSelectorModelListener {

    private static final long serialVersionUID = -8443699382736126351L;
    private static final int ROW_HEIGHT = 20;
    private static final int ICON_WIDTH = 16;

    private static final int VISIBLE_ROW = 0;
    private static final int TITLE_ROW = 1;
    public static final int LOADING_ROW = 2;
    private static final int REMOVE_ROW = 3;

    private final JTable grid;

    private final LineDataSelectorModel tableModel;
    private final JPanel optionsPanelWrapper;
    private final GridBagConstraints gc = new GridBagConstraints();
    private Component optionsPanel = new JPanel();
    private final IntervalOptionPanel intervalOptionPanel;

    private static final String PROGRESSBAR_TOOLTIP_ACTIVEDOWNLOAD = "Downloading data from server.";
    private static final String PROGRESSBAR_TOOLTIP_INACTIVEDOWNLOAD = "No data selected to download from server.";

    private final JProgressBar progressBar = new JProgressBar();

    public LineDateSelectorTablePanel() {
        intervalOptionPanel = new IntervalOptionPanel();
        this.setLayout(new GridBagLayout());
        tableModel = LineDataSelectorModel.getSingletonInstance();
        grid = new JTable(tableModel);
        tableModel.addTableModelListener(grid);
        tableModel.addLineDataSelectorModelListener(this);

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
        tableModel.addTableModelListener(this);
        grid.setRowHeight(ROW_HEIGHT);
        grid.setBackground(Color.white);
        grid.getColumnModel().getColumn(VISIBLE_ROW).setCellRenderer(new LineDataVisibleCellRenderer());
        grid.getColumnModel().getColumn(VISIBLE_ROW).setPreferredWidth(ICON_WIDTH);
        grid.getColumnModel().getColumn(VISIBLE_ROW).setMaxWidth(ICON_WIDTH);

        grid.getColumnModel().getColumn(TITLE_ROW).setCellRenderer(new LineDataSelectorElementRenderer());
        // grid.getColumnModel().getColumn(TITLE_ROW).setPreferredWidth(80);
        // grid.getColumnModel().getColumn(TITLE_ROW).setMaxWidth(80);

        grid.getColumnModel().getColumn(LOADING_ROW).setCellRenderer(new LoadingCellRenderer());
        grid.getColumnModel().getColumn(LOADING_ROW).setPreferredWidth(20);
        grid.getColumnModel().getColumn(LOADING_ROW).setMaxWidth(20);

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
                LineDataSelectorModel model = (LineDataSelectorModel) grid.getModel();

                if (col == VISIBLE_ROW) {
                    LineDataSelectorElement renderable = (LineDataSelectorElement) model.getValueAt(row, col);
                    renderable.setVisibility(!renderable.isVisible());
                }
                if (col == TITLE_ROW || col == VISIBLE_ROW || col == LOADING_ROW) {
                    LineDataSelectorElement lineDataElement = (LineDataSelectorElement) model.getValueAt(row, col);
                    setOptionsPanel(lineDataElement);
                }
                if (col == REMOVE_ROW) {
                    model.removeRow(row);
                }
                revalidate();
                repaint();
            }
        });
        grid.setDragEnabled(true);
        grid.setDropMode(DropMode.INSERT_ROWS);
        grid.setTransferHandler(new TableRowTransferHandler(grid));

        optionsPanelWrapper = new JPanel();
        optionsPanelWrapper.setBorder(BorderFactory.createTitledBorder("Options"));
        optionsPanelWrapper.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        optionsPanelWrapper.add(optionsPanel, gbc);
        gbc.gridy = 1;
        optionsPanelWrapper.add(intervalOptionPanel, gbc);

        JPanel addLayerButtonWrapper = new JPanel(new GridBagLayout());
        GridBagConstraints gbc2 = new GridBagConstraints();

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

        progressBar.setToolTipText(PROGRESSBAR_TOOLTIP_INACTIVEDOWNLOAD);

        gbc2.gridx = 0;
        gbc2.gridy = 0;
        gbc2.fill = GridBagConstraints.HORIZONTAL;
        gbc2.weightx = 1;
        addLayerButtonWrapper.add(progressBar, gbc2);

        gbc2.gridx = 1;
        gbc2.fill = GridBagConstraints.NONE;
        gbc2.weightx = 0;
        addLayerButtonWrapper.add(addLayerButton, gbc2);

        jspContainer.add(addLayerButtonWrapper, BorderLayout.CENTER);

        gc.gridy = 1;
        add(optionsPanelWrapper, gc);
    }

    private void setOptionsPanel(LineDataSelectorElement lineDataElement) {
        optionsPanelWrapper.remove(optionsPanel);
        optionsPanel = null;
        if (lineDataElement != null) {
            optionsPanel = lineDataElement.getOptionsPanel();
        }
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        if (optionsPanel == null) {
            optionsPanel = new JPanel();
        }
        optionsPanelWrapper.add(optionsPanel, gbc);
        revalidate();
        repaint();
    }

    @Override
    public void tableChanged(TableModelEvent e) {
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

    @Override
    public void downloadStartded(LineDataSelectorElement element) {
        if (element.isAvailable()) {
            progressBar.setIndeterminate(true);
            progressBar.setToolTipText(PROGRESSBAR_TOOLTIP_ACTIVEDOWNLOAD);
        }
    }

    @Override
    public void downloadFinished(LineDataSelectorElement element) {
        boolean active = tableModel.atLeastOneDownloading();

        progressBar.setIndeterminate(active);

        if (!active) {
            progressBar.setToolTipText(PROGRESSBAR_TOOLTIP_INACTIVEDOWNLOAD);
        }

    }

    @Override
    public void lineDataAdded(LineDataSelectorElement element) {
        // TODO Auto-generated method stub

    }

    @Override
    public void lineDataRemoved(LineDataSelectorElement element) {
        // TODO Auto-generated method stub

    }

    @Override
    public void lineDataUpdated(LineDataSelectorElement element) {
        // TODO Auto-generated method stub

    }
}
