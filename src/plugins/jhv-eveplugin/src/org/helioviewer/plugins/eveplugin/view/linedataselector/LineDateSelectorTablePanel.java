package org.helioviewer.plugins.eveplugin.view.linedataselector;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;
import javax.swing.JTable;

public class LineDateSelectorTablePanel extends JPanel {

    private static final int ROW_HEIGHT = 20;
    private static final int ICON_WIDTH = 16;

    private static final int VISIBLE_ROW = 0;
    private static final int TITLE_ROW = 1;
    public static final int LOADING_ROW = 2;
    private static final int REMOVE_ROW = 3;

    private final JTable grid;

    private final LineDataSelectorTableModel tableModel;

    GridBagConstraints gc = new GridBagConstraints();

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

    }
}
