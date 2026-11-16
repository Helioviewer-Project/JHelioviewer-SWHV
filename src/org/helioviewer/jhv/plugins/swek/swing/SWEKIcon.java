package org.helioviewer.jhv.plugins.swek.swing;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.Icon;

import org.helioviewer.jhv.plugins.swek.SWEKIconImages;

// Paints the event icon on the device pixels it covers
final class SWEKIcon implements Icon {

    private final String iconName;
    private final int size;

    SWEKIcon(String _iconName, int _size) {
        iconName = _iconName;
        size = _size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g;
        SWEKIconImages.draw(g2, g2.getTransform(), iconName, x, y, size);
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }
}
