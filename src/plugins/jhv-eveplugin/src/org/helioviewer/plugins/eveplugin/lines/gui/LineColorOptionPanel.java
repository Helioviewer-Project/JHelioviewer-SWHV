package org.helioviewer.plugins.eveplugin.lines.gui;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.plugins.eveplugin.lines.data.Band;

public class LineColorOptionPanel extends JPanel {

    private final Band band;

    public LineColorOptionPanel(Band band) {
        this.band = band;
        initVisualComponents();
    }

    private void initVisualComponents() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

        JButton pickColor = new JButton("Line color");
        pickColor.setMargin(new Insets(0, 0, 0, 0));
        pickColor.setToolTipText("Change the color of the current line");
        pickColor.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Color newColor = JColorChooser.showDialog(ImageViewerGui.getMainFrame(), "Choose Line Color", band.getGraphColor());
                if (newColor != null) {
                    band.setGraphColor(newColor);
                }
            }
        });
        add(pickColor);
    }

}
