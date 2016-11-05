package org.helioviewer.jhv.plugins.eveplugin.view;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.gui.dialogs.model.ObservationDialogDateModel;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;
import org.helioviewer.jhv.plugins.eveplugin.lines.Band;
import org.helioviewer.jhv.plugins.eveplugin.lines.BandColors;
import org.helioviewer.jhv.plugins.eveplugin.lines.BandGroup;
import org.helioviewer.jhv.plugins.eveplugin.lines.BandType;
import org.helioviewer.jhv.plugins.eveplugin.lines.BandTypeAPI;
import org.helioviewer.jhv.plugins.eveplugin.lines.DownloadController;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorModelListener;

@SuppressWarnings("serial")
public class ObservationDialogUIPanel extends SimpleObservationDialogUIPanel implements LineDataSelectorModelListener {

    private final JComboBox<BandGroup> comboBoxGroup;
    private final JComboBox<BandType> comboBoxData;

    public ObservationDialogUIPanel() {
        JLabel labelGroup = new JLabel("Group", JLabel.RIGHT);
        JLabel labelData = new JLabel("Dataset", JLabel.RIGHT);

        comboBoxGroup = new JComboBox<BandGroup>(BandTypeAPI.getSingletonInstance().getOrderedGroups().toArray(new BandGroup[0]));
        comboBoxData = new JComboBox<BandType>();
        JPanel dataPane = new JPanel();

        comboBoxGroup.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateGroupValues();
            }
        });

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

        EVEPlugin.ldsm.addLineDataSelectorModelListener(this);
    }

    private void updateGroupValues() {
        DefaultComboBoxModel<BandType> model = (DefaultComboBoxModel<BandType>) comboBoxData.getModel();
        BandGroup selectedGroup = (BandGroup) comboBoxGroup.getSelectedItem();
        BandType[] values = BandTypeAPI.getSingletonInstance().getBandTypes(selectedGroup);

        model.removeAllElements();

        for (BandType value : values) {
            if (!EVEPlugin.ldsm.containsBandType(value)) {
                model.addElement(value);
            }
        }

        if (model.getSize() > 0) {
            comboBoxData.setSelectedIndex(0);
        }
    }

    private void updateBandController() {
        BandType bandType = (BandType) comboBoxData.getSelectedItem();
        Band band = new Band(bandType);
        band.setDataColor(BandColors.getNextColor());
        DownloadController.getSingletonInstance().updateBand(band, EVEPlugin.dc.availableAxis.start, EVEPlugin.dc.availableAxis.end);
    }

    private void updateDrawController() {
        Interval interval = defineInterval(getTime());
        EVEPlugin.dc.setSelectedInterval(interval.start, interval.end);
    }

    private Interval defineInterval(long time) {
        Interval movieInterval = new Interval(Layers.getStartDate().milli, Layers.getEndDate().milli);
        if (movieInterval.containsPointInclusive(time)) {
            return movieInterval;
        }

        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(time);
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_MONTH, 1);

        long endTime = cal.getTimeInMillis();
        long now = System.currentTimeMillis();
        if (endTime > now) {
            cal.setTimeInMillis(now);
            cal.set(Calendar.HOUR, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            endTime = cal.getTimeInMillis();
        }

        cal.add(Calendar.DAY_OF_MONTH, -2);

        return new Interval(cal.getTimeInMillis(), endTime);
    }

    @Override
    public boolean loadButtonPressed(Object layer) {
        ObservationDialogDateModel.getInstance().setStartTime(getTime(), true);
        updateBandController();
        updateDrawController();
        return true;
    }

    @Override
    public void setupLayer(Object layer) {
    }

    @Override
    public void lineDataAdded(LineDataSelectorElement element) {
        updateGroupValues();
    }

    @Override
    public void lineDataRemoved(LineDataSelectorElement element) {
        updateGroupValues();
    }

    @Override
    public void lineDataVisibility(LineDataSelectorElement element, boolean flag) {
    }

}
