package org.helioviewer.gl3d.plugin.pfss;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.plugins.pfssplugin.PfssSettings;

/**
 * Panel of Pfss-Plugin
 *
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssPluginPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private JSpinner qualitySpinner;
    public static PfssPluginPanel currentPluginPanel;

    /**
     * Default constructor
     *
     * */
    public PfssPluginPanel() {
        currentPluginPanel = this;

        initVisualComponents();
    }

    /**
     * Sets up the visual sub components and the visual part of the component
     * itself.
     * */
    private void initVisualComponents() {
        // set general appearance
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[] { 0, 0, 0 };
        gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0, 0, 0 };
        gridBagLayout.columnWeights = new double[] { 0.0, 1., 0.0, Double.MIN_VALUE };
        gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
        setLayout(gridBagLayout);

        setEnabled(true);

        GridBagConstraints c0 = new GridBagConstraints();
        c0.anchor = GridBagConstraints.WEST;
        c0.insets = new Insets(0, 0, 5, 0);
        c0.gridx = 0;
        c0.gridy = 1;
        this.qualitySpinner = new JSpinner();
        this.qualitySpinner.setModel(new SpinnerNumberModel(new Integer(0), new Integer(0), new Integer(8), new Integer(1)));

        this.qualitySpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(javax.swing.event.ChangeEvent e) {
                PfssSettings.qualityReduction = 8 - ((Integer) qualitySpinner.getValue()).intValue();
                Displayer.display();
            }
        });
        WheelSupport.installMouseWheelSupport(qualitySpinner);

        JPanel helpPanel = new JPanel();
        helpPanel.add(new JLabel("Level"));
        helpPanel.add(qualitySpinner);

        JCheckBox fixedColors = new JCheckBox("Fixed colors", false);
        fixedColors.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                PfssSettings.fixedColor = (e.getStateChange() == ItemEvent.SELECTED);
                Displayer.display();
            }
        });
        helpPanel.add(fixedColors);
        this.add(helpPanel, c0);

        GridBagConstraints gbc = new GridBagConstraints();

        JButton availabilityButton = new JButton("Available data");
        availabilityButton.setToolTipText("Click here to check the availability of PFSS data");
        availabilityButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String url = Settings.getSingletonInstance().getProperty("availability.pfss.url");
                JHVGlobals.openURL(url);
            }
        });
        gbc.insets = new Insets(0, 0, 5, 0);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        this.add(availabilityButton, gbc);
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
