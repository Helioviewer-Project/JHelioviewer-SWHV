package org.helioviewer.jhv.renderable.gui.cellrenderer;

import java.awt.Component;
import java.awt.Font;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JTable;

import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.gui.components.base.JHVTableCellRenderer;
import org.helioviewer.jhv.renderable.gui.Renderable;

@SuppressWarnings("serial")
public class RendererLoading extends JHVTableCellRenderer {

    private final Font font = Buttons.getMaterialFont(getFont().getSize2D());
    private final JLayer<JComponent> layer = new JLayer<>(null, UITimer.busyIndicator);

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        label.setBorder(cellBorder);

        // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
        if (value instanceof Renderable) {
            Renderable renderable = (Renderable) value;
            if (renderable.isDownloading()) {
                table.repaint(); // lazy

                layer.setForeground(label.getForeground());
                layer.setView(label);
                return layer;
            } else if (renderable.isLocal()) {
                label.setFont(font);
                label.setText(Buttons.check);
            } else
                label.setText(null);
        }
        return label;
    }

}
