package org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.cellrenderer;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorTablePanel;

@SuppressWarnings("serial")
public class LoadingCellRenderer extends DefaultTableCellRenderer {

    private final JProgressBar downloadProgressBar = new JProgressBar();

    public LoadingCellRenderer() {
        downloadProgressBar.setIndeterminate(true);
        downloadProgressBar.setOpaque(true);
        downloadProgressBar.setPreferredSize(new Dimension(20, -1));
        downloadProgressBar.setBorder(LineDataSelectorTablePanel.commonBorder);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
        if (value instanceof LineDataSelectorElement && ((LineDataSelectorElement) value).isDownloading()) {
            downloadProgressBar.setBackground(label.getBackground());
            return downloadProgressBar;
        }

        label.setText(null);
        label.setBorder(LineDataSelectorTablePanel.commonBorder);
        return label;
    }

}
