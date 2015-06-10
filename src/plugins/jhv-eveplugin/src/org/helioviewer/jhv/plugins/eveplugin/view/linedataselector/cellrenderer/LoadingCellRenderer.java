package org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.cellrenderer;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDateSelectorTablePanel;

//Class will not be serialized so we suppress the warnings
@SuppressWarnings("serial")
public class LoadingCellRenderer extends DefaultTableCellRenderer {

    private final JProgressBar downloadProgressBar = new JProgressBar();

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value != null) { // In some case this can be called with value null
            // (getAccessibleChild(int i) of JTable )

            LineDataSelectorElement element = (LineDataSelectorElement) value;
            if (element.isDownloading()) {
                downloadProgressBar.setIndeterminate(true);
                downloadProgressBar.setVisible(element.isDownloading());
                downloadProgressBar.setBorder(LineDateSelectorTablePanel.commonBorder);
                downloadProgressBar.setOpaque(false);
                downloadProgressBar.setPreferredSize(new Dimension(20, downloadProgressBar.getPreferredSize().height));
                return downloadProgressBar;
            } else {
                JLabel p = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                p.setBorder(LineDateSelectorTablePanel.commonBorder);
                p.setText("");
                return p;
            }
        } else {
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

}
