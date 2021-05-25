package org.helioviewer.jhv.layers.selector.cellrenderer;

import java.awt.Component;
import java.awt.Font;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JTable;

import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.gui.components.base.JHVTableCellRenderer;
import org.helioviewer.jhv.layers.Layer;

@SuppressWarnings("serial")
public class RendererLoading extends JHVTableCellRenderer {

    private final Font font = Buttons.getMaterialFont(UIGlobals.uiFont.getSize2D());
    private final JLayer<JComponent> over = new JLayer<>(null, UITimer.busyIndicator);

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        label.setBorder(cellBorder);
        label.setText(null);

        // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
        if (value instanceof Layer) {
            Layer layer = (Layer) value;
            if (layer.isDownloading()) {
                table.repaint(); // lazy

                over.setForeground(label.getForeground());
                over.setView(label);
                return over;
            } else if (layer.isLocal()) {
                label.setFont(font);
                label.setText(Buttons.check);
            }
        }
        return label;
    }

}
