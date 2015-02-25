package org.helioviewer.gl3d;

import java.awt.BorderLayout;

import javax.swing.JFrame;

public class GL3DTest {

    /**
     * @param args
     */
    public static void main(String[] args) {
        // LogSettings.init("../settings/log4j.initial.properties",
        // "../settings/log4j.initial.properties", "/Users/simon/", true);

        JFrame frame = new JFrame("JOGL Frame Test");
        frame.getContentPane().setLayout(new BorderLayout());
        frame.setBounds(100, 100, 600, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // frame.pack();

        frame.getContentPane().add(new GL3DCanvas(), BorderLayout.CENTER);
        frame.setVisible(true);
        frame.repaint();

    }

}
