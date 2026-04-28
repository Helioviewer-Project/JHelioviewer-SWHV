package org.helioviewer.jhv.layers.fov;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;

import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.FOVShape;
import org.helioviewer.jhv.opengl.text.SdfTextRenderer;

import org.json.JSONObject;

@SuppressWarnings("serial")
class FOVInstrument extends DefaultMutableTreeNode implements Interfaces.JHVCell {

    enum FOVType {RECTANGULAR, CIRCULAR}

    private final FOVShape fov = new FOVShape();

    private final String name;
    private final FOVType type;
    private final double inner;
    private final double wide;
    private final double high;

    private final JPanel panel;
    private final JCheckBox checkBox;

    private double centerX = 0;
    private double centerY = 0;

    FOVInstrument(String _name, FOVType _type, double innerDeg, double wideDeg, double highDeg, JSONObject jo) {
        name = _name;
        type = _type;
        inner = 0.5 * Math.tan(innerDeg * (Math.PI / 180.));
        wide = 0.5 * Math.tan(wideDeg * (Math.PI / 180.));
        high = 0.5 * Math.tan(highDeg * (Math.PI / 180.));

        panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        boolean enabled = jo.optBoolean(name, false);
        checkBox = new JCheckBox(name, enabled);
        checkBox.addActionListener(e -> MovieDisplay.display());
        checkBox.setFocusPainted(false);
        checkBox.setOpaque(false);

        panel.add(checkBox, BorderLayout.LINE_START);
        panel.add(new JLabel("      "), BorderLayout.LINE_END); // avoid ellipsis on Windows
    }

    @Override
    public Component getComponent() {
        return panel;
    }

    void setCenterX(double _centerX) {
        centerX = _centerX;
    }

    void setCenterY(double _centerY) {
        centerY = _centerY;
    }

    void putGeometry(double distance, byte[] color, SdfTextRenderer renderer, BufVertex lineBuf, BufVertex centerBuf) {
        if (!checkBox.isSelected())
            return;

        fov.setCenter(centerX * distance, centerY * distance);
        fov.putCenter(false, color, centerBuf);

        if (inner > 0)
            fov.putCircLine(inner * distance, false, color, lineBuf);
        if (type == FOVType.RECTANGULAR) {
            fov.putRectLine(wide * distance, high * distance, false, color, lineBuf);
            FOVText.drawLabel(renderer, name, (centerX - wide) * distance, (centerY - high) * distance, high * distance);
        } else {
            fov.putCircLine(wide * distance, false, color, lineBuf);
            double halfSide = wide / Math.sqrt(2);
            FOVText.drawLabel(renderer, name, (centerX - halfSide) * distance, (centerY - halfSide) * distance, halfSide * distance);
        }
    }

    boolean isEnabled() {
        return checkBox.isSelected();
    }

    @Override
    public String toString() {
        return name;
    }

}
