package org.helioviewer.plugins.eveplugin.lines.gui;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.gui.ImageViewerGui;

public class LineColorOptionPanel extends JPanel {

    private Color color;
    private final List<ChangeListener> listeners;

    public LineColorOptionPanel(Color startColor) {
        color = startColor;
        listeners = new ArrayList<ChangeListener>();
        initVisualComponents();
    }

    public void addChangeListener(ChangeListener listener) {
        listeners.add(listener);
    }

    public Color getColor() {
        return color;
    }

    private void initVisualComponents() {
        setLayout(new FlowLayout(FlowLayout.LEFT));

        JButton pickColor = new JButton("Pick new Color");
        pickColor.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Color newColor = JColorChooser.showDialog(ImageViewerGui.getMainFrame(), "Choose Line Color", color);
                if (newColor != null) {
                    color = newColor;
                    fireColorChanged();
                }
            }
        });

        add(pickColor);
    }

    protected void fireColorChanged() {
        ChangeEvent e = new ChangeEvent(this);
        for (ChangeListener l : listeners) {
            l.stateChanged(e);
        }
    }
}
