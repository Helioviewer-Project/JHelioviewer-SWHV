package org.helioviewer.jhv.layers.fov;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.interfaces.JHVCell;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.FOVShape;
import org.helioviewer.jhv.opengl.text.JhvTextRenderer;

@SuppressWarnings("serial")
class FOVInstrument extends DefaultMutableTreeNode implements JHVCell {

    enum FOVType {RECTANGULAR, CIRCULAR}

    private final String name;
    private final FOVType type;
    private final double inner;
    private final double wide;
    private final double high;
    private final byte[] color;

    private final JPanel panel;
    private final JCheckBox checkBox;

    private double centerX = 0;
    private double centerY = 0;

    FOVInstrument(String _name, FOVType _type, double innerDeg, double wideDeg, double highDeg, byte[] _color) {
        name = _name;
        type = _type;
        inner = 0.5 * Math.tan(innerDeg * (Math.PI / 180.));
        wide = 0.5 * Math.tan(wideDeg * (Math.PI / 180.));
        high = 0.5 * Math.tan(highDeg * (Math.PI / 180.));
        color = _color;

        panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        checkBox = new JCheckBox(name);
        checkBox.addActionListener(e -> MovieDisplay.display());
        checkBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3 && checkBox.isSelected()) {
                    zoom();
                    MovieDisplay.render(1);
                }
            }
        });

        checkBox.setFocusPainted(false);
        checkBox.setOpaque(false);
        panel.add(checkBox, BorderLayout.LINE_START);
        panel.add(new JLabel("      "), BorderLayout.LINE_END); // avoid ellipsis on Windows
        ComponentUtils.smallVariant(panel);
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

    void putFOV(FOVShape f, double distance, BufVertex lineBuf, BufVertex centerBuf, JhvTextRenderer renderer) {
        if (!checkBox.isSelected())
            return;

        f.setCenter(centerX * distance, centerY * distance);
        f.putCenter(centerBuf, color);

        if (inner > 0)
            f.putCircLine(inner * distance, lineBuf, color);
        if (type == FOVType.RECTANGULAR) {
            f.putRectLine(wide * distance, high * distance, lineBuf, color);
            FOVText.drawLabel(renderer, name, (centerX - wide) * distance, (centerY - high) * distance, high * distance);
        } else {
            f.putCircLine(wide * distance, lineBuf, color);
            double halfSide = wide / Math.sqrt(2);
            FOVText.drawLabel(renderer, name, (centerX - halfSide) * distance, (centerY - halfSide) * distance, halfSide * distance);
        }
    }

    private void zoom() {
        Camera camera = Display.getCamera();
        double distance = camera.getViewpoint().distance;
        camera.setTranslation(-centerX * distance, -centerY * distance);
        camera.resetDragRotation();
        camera.setFOV(2 * wide);
    }

    boolean isEnabled() {
        return checkBox.isSelected();
    }

}
