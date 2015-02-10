package org.helioviewer.plugins.eveplugin.view;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.internal_plugins.filter.SOHOLUTFilterPlugin.LUT;
import org.helioviewer.plugins.eveplugin.radio.filter.FilterModel;

public class RadioOptionsPanel extends JPanel implements ActionListener {

    private JComboBox lut;
    private JLabel color;

    public RadioOptionsPanel() {
        super();
        initVisualComponents();
    }

    private void initVisualComponents() {

        setLayout(new BorderLayout());

        lut = new JComboBox(LUT.getStandardList().keySet().toArray());
        lut.addActionListener(this);
        color = new JLabel("Color:");

        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Radio Options"));
        panel.setLayout(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;

        panel.add(color, gc);

        gc.gridx = 1;
        gc.weightx = 1;
        panel.add(lut, gc);

        add(panel, BorderLayout.CENTER);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        FilterModel.getInstance().setLUT(LUT.getStandardList().get((lut.getSelectedItem())));
    }

}
