package org.helioviewer.jhv.plugins.swek;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

@SuppressWarnings("serial")
final class SWEKLayerOptionsPanel extends JPanel {

    SWEKLayerOptionsPanel(SWEKLayer layer) {
        super(new GridBagLayout());

        JCheckBox check = new JCheckBox("Icons", layer.isIcons());
        check.setHorizontalTextPosition(SwingConstants.LEFT);
        check.addActionListener(e -> layer.setIcons(check.isSelected()));

        GridBagConstraints c0 = new GridBagConstraints();
        c0.anchor = GridBagConstraints.CENTER;
        c0.weightx = 1.;
        c0.weighty = 1.;
        c0.gridy = 0;
        c0.gridx = 0;
        add(check, c0);
    }

}
