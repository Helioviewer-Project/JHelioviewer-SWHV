package org.helioviewer.jhv.plugins.swhvhekplugin;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ComponentUtils.SmallPanel;

@SuppressWarnings("serial")
public class SWHVHEKPluginPanel extends SmallPanel {

    protected boolean icons = true;

    public SWHVHEKPluginPanel() {
        GridBagLayout gridBagLayout = new GridBagLayout();
        setLayout(gridBagLayout);

        GridBagConstraints c0 = new GridBagConstraints();
        c0.anchor = GridBagConstraints.CENTER;
        c0.weightx = 1.;
        c0.weighty = 1.;
        c0.gridy = 0;
        c0.gridx = 0;

        JCheckBox check = new JCheckBox("Icons", icons);
        check.setHorizontalTextPosition(SwingConstants.LEFT);
        check.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                icons = !icons;
                Displayer.display();
            }
        });
        add(check, c0);

        setSmall();
    }

}
