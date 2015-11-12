package org.helioviewer.jhv.plugins.pfssplugin;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.components.base.WheelSupport;

@SuppressWarnings("serial")
public class PfssPluginPanel extends ComponentUtils.SmallPanel {

    private JSpinner levelSpinner;
    public static PfssPluginPanel currentPluginPanel;

    public PfssPluginPanel() {
        currentPluginPanel = this;
        initVisualComponents();
        setSmall();
    }

    private void initVisualComponents() {
        setLayout(new GridBagLayout());

        levelSpinner = new JSpinner();
        levelSpinner.setModel(new SpinnerNumberModel(0, 0, 8, 1));

        levelSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                PfssSettings.qualityReduction = 8 - ((Integer) levelSpinner.getValue()).intValue();
                Displayer.display();
            }
        });
        WheelSupport.installMouseWheelSupport(levelSpinner);

        GridBagConstraints c0 = new GridBagConstraints();

        c0.weightx = 1.;
        c0.weighty = 1.;
        c0.gridy = 0;

        c0.gridx = 0;
        c0.anchor = GridBagConstraints.EAST;
        add(new JLabel("Level", JLabel.RIGHT), c0);

        c0.gridx = 1;
        c0.anchor = GridBagConstraints.WEST;
        add(levelSpinner, c0);

        JCheckBox fixedColors = new JCheckBox("Fixed colors", false);
        fixedColors.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                PfssSettings.fixedColor = (e.getStateChange() == ItemEvent.SELECTED);
                Displayer.display();
            }
        });

        c0.gridx = 2;
        c0.anchor = GridBagConstraints.WEST;
        add(fixedColors, c0);

        JButton availabilityButton = new JButton("Available data");
        availabilityButton.setToolTipText("Click here to check the availability of PFSS data");
        availabilityButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String url = Settings.getSingletonInstance().getProperty("availability.pfss.url");
                JHVGlobals.openURL(url);
            }
        });

        c0.anchor = GridBagConstraints.EAST;
        c0.gridx = 3;
        add(availabilityButton, c0);
    }

    @Override
    public void setEnabled(boolean b) {
    }

    public void enableComponents(Container container, boolean enable) {
        Component[] components = container.getComponents();
        for (Component component : components) {
            component.setEnabled(enable);
            if (component instanceof Container) {
                enableComponents((Container) component, enable);
            }
        }
    }

}
