package org.helioviewer.jhv.plugins.swek.renderable;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JCheckBox;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ComponentUtils.SmallPanel;

@SuppressWarnings("serial")
class SWEKRenderableOptionsPanel extends SmallPanel {

    boolean icons = true;

    public SWEKRenderableOptionsPanel() {
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
        check.addItemListener(e -> {
            icons = !icons;
            Displayer.display();
        });
        add(check, c0);

        setSmall();
    }

}
