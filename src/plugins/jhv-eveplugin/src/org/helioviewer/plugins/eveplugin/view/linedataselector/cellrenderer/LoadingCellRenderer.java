package org.helioviewer.plugins.eveplugin.view.linedataselector.cellrenderer;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDateSelectorTablePanel;

public class LoadingCellRenderer extends DefaultTableCellRenderer {

    private final JProgressBar downloadProgressBar = new JProgressBar();

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        LineDataSelectorElement element = (LineDataSelectorElement) value;
        if (element.isDownloading()) {
            downloadProgressBar.setIndeterminate(true);
            downloadProgressBar.setVisible(element.isDownloading());
            downloadProgressBar.setBorder(LineDateSelectorTablePanel.commonBorder);
            return downloadProgressBar;
        } else {
            JPanel p = new JPanel();
            p.setBorder(LineDateSelectorTablePanel.commonBorder);
            p.setBackground(Color.WHITE);
            return p;
        }
    }

}
