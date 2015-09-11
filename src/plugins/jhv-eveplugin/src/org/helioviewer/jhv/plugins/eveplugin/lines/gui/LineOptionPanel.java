package org.helioviewer.jhv.plugins.eveplugin.lines.gui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.plugins.eveplugin.lines.data.Band;
import org.helioviewer.jhv.plugins.eveplugin.lines.model.EVEDrawController;

//Class will not be serialized so we suppress the warnings
@SuppressWarnings({ "serial" })
public class LineOptionPanel extends JPanel {

    private final Band band;

    public LineOptionPanel(Band band) {
        this.band = band;
        initVisualComponents();
    }

    private void initVisualComponents() {
        // setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.NONE;
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
        add(pickColor, c);

        JLabel yAxis = new JLabel("Y-axis");
        c.anchor = GridBagConstraints.EAST;
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.NONE;
        add(yAxis, c);

        String[] options = { "Left", "Right" };
        JComboBox<String> changeAxis = new JComboBox<String>(options);
        changeAxis.setToolTipText("Switch the axis");
        changeAxis.setSelectedIndex(EVEDrawController.getSingletonInstance().getAxisLocation(band));
        changeAxis.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                EVEDrawController.getSingletonInstance().changeAxis(band);
            }
        });
        changeAxis.setEnabled(EVEDrawController.getSingletonInstance().canChangeAxis(band));

        c.anchor = GridBagConstraints.EAST;
        c.gridx = 2;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 1;
        c.fill = GridBagConstraints.NONE;
        add(changeAxis, c);
    }

}
