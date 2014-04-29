package org.helioviewer.plugins.eveplugin.radio.test;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

public class TestRadioPane extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private BufferedImage image;

    /**
     * Create the panel.
     */
    public TestRadioPane(BufferedImage im) {
        this.image = im;
        this.setPreferredSize(new Dimension(im.getWidth(), im.getHeight()));
    }

    public void paintComponent(Graphics g) {
        if (image != null) {
            g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), 0, 0, image.getTileWidth(), image.getHeight(), null);
        }
    }
}
