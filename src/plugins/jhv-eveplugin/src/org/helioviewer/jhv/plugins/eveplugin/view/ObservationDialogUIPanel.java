package org.helioviewer.jhv.plugins.eveplugin.view;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.gui.dialogs.model.ObservationDialogDateModel;
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;
import org.helioviewer.jhv.plugins.eveplugin.lines.data.Band;
import org.helioviewer.jhv.plugins.eveplugin.lines.data.BandColors;
import org.helioviewer.jhv.plugins.eveplugin.lines.data.DownloadController;
import org.helioviewer.jhv.plugins.eveplugin.settings.BandGroup;
import org.helioviewer.jhv.plugins.eveplugin.settings.BandType;
import org.helioviewer.jhv.plugins.eveplugin.settings.BandTypeAPI;

@SuppressWarnings("serial")
public class ObservationDialogUIPanel extends SimpleObservationDialogUIPanel implements ActionListener {

    private final JComboBox comboBoxGroup;
    private final JComboBox comboBoxData;

    public ObservationDialogUIPanel() {
        super();

        JLabel labelGroup = new JLabel("Group", JLabel.RIGHT);
        JLabel labelData = new JLabel("Dataset", JLabel.RIGHT);

        comboBoxGroup = new JComboBox(new DefaultComboBoxModel());
        comboBoxData = new JComboBox(new DefaultComboBoxModel());
        JPanel dataPane = new JPanel();

        comboBoxGroup.addActionListener(this);

        JPanel container = new JPanel();
        container.setLayout(new BorderLayout());
        dataPane.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;

        dataPane.add(labelGroup, c);

        c.gridx = 1;
        c.gridy = 0;
        dataPane.add(comboBoxGroup, c);

        c.gridx = 0;
        c.gridy = 1;
        dataPane.add(labelData, c);

        c.gridx = 1;
        c.gridy = 1;
        dataPane.add(comboBoxData, c);

        container.add(dataPane, BorderLayout.CENTER);
        this.add(container);

        initGroups();
    }

    private void initGroups() {
        final List<BandGroup> groups = BandTypeAPI.getSingletonInstance().getOrderedGroups();
        final DefaultComboBoxModel model = (DefaultComboBoxModel) comboBoxGroup.getModel();
        model.removeAllElements();

        for (final BandGroup group : groups) {
            model.addElement(group);
        }
    }

    private void updateGroupValues() {
        final DefaultComboBoxModel model = (DefaultComboBoxModel) comboBoxData.getModel();
        final BandGroup selectedGroup = (BandGroup) comboBoxGroup.getSelectedItem();
        final BandType[] values = BandTypeAPI.getSingletonInstance().getBandTypes(selectedGroup);

        model.removeAllElements();

        for (final BandType value : values) {
            if (!EVEPlugin.ldsm.containsBandType(value)) {
                model.addElement(value);
            }
        }

        if (model.getSize() > 0) {
            comboBoxData.setSelectedIndex(0);
        }
    }

    private void updateDrawController() {
        Interval interval = defineInterval(getDate());
        EVEPlugin.dc.setSelectedInterval(interval.start, interval.end);
    }

    private boolean updateBandController() {
        BandType bandType = (BandType) comboBoxData.getSelectedItem();
        Band band = new Band(bandType);
        band.setDataColor(BandColors.getNextColor());
        DownloadController.getSingletonInstance().updateBand(band, EVEPlugin.dc.availableAxis.start, EVEPlugin.dc.availableAxis.end);
        return true;
    }

    @Override
    public boolean loadButtonPressed() {
        ObservationDialogDateModel.getInstance().setStartDate(getDate(), true);
        if (updateBandController()) {
            updateDrawController();
        }
        return true;
    }

    // Action Listener

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource().equals(comboBoxGroup)) {
            updateGroupValues();
        }
    }

}
