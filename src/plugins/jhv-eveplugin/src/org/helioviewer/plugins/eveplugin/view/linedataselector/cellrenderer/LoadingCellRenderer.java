package org.helioviewer.plugins.eveplugin.view.linedataselector.cellrenderer;

import java.awt.Component;

import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.base.logging.Log;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;

public class LoadingCellRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 2173458369966852891L;

    private final JProgressBar downloadProgressBar = new JProgressBar();

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        LineDataSelectorElement element = (LineDataSelectorElement) value;
        Log.debug("element is downloading : " + element.isDownloading());
        if (element.isDownloading()) {
            downloadProgressBar.setIndeterminate(true);
            downloadProgressBar.setVisible(element.isDownloading());
            return downloadProgressBar;
        } else {
            return null;
        }
    }
}
