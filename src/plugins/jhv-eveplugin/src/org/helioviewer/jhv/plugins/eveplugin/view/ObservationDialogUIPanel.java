package org.helioviewer.jhv.plugins.eveplugin.view;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.dialogs.model.ObservationDialogDateModel;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawController;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxisElement;
import org.helioviewer.jhv.plugins.eveplugin.lines.model.EVEDrawController;
import org.helioviewer.jhv.plugins.eveplugin.settings.BandGroup;
import org.helioviewer.jhv.plugins.eveplugin.settings.BandType;
import org.helioviewer.jhv.plugins.eveplugin.settings.BandTypeAPI;

@SuppressWarnings("serial")
public class ObservationDialogUIPanel extends SimpleObservationDialogUIPanel implements ActionListener {

    private final JComboBox comboBoxGroup;
    private final JComboBox comboBoxData;

    private final JPanel dataPane;

    public ObservationDialogUIPanel() {
        super();

        JLabel labelGroup = new JLabel("Group", JLabel.RIGHT);
        JLabel labelData = new JLabel("Dataset", JLabel.RIGHT);

        comboBoxGroup = new JComboBox(new DefaultComboBoxModel());
        comboBoxData = new JComboBox(new DefaultComboBoxModel());
        dataPane = new JPanel();

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
        final EVEDrawController eveDrawController = EVEDrawController.getSingletonInstance();
        final DefaultComboBoxModel model = (DefaultComboBoxModel) comboBoxData.getModel();
        final BandGroup selectedGroup = (BandGroup) comboBoxGroup.getSelectedItem();
        final BandType[] values = BandTypeAPI.getSingletonInstance().getBandTypes(selectedGroup);

        model.removeAllElements();

        for (final BandType value : values) {
            if (!eveDrawController.containsBandType(value)) {
                model.addElement(value);
            }
        }

        if (model.getSize() > 0) {
            comboBoxData.setSelectedIndex(0);
        }
    }

    /**
     * Checks if the selected start date is before selected or equal to end
     * date. The methods checks the entered times when the dates are equal. If
     * the start time is greater than the end time the method will return false.
     *
     * @return boolean value if selected start date is before selected end date.
     */
    /*
     * private boolean isStartDateBeforeOrEqualEndDate() { final
     * GregorianCalendar calendar = new GregorianCalendar();
     * calendar.setTime(getStartDate());
     *
     * final GregorianCalendar calendar2 = new
     * GregorianCalendar(calendar.get(Calendar.YEAR),
     * calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)); final
     * long start = calendar2.getTimeInMillis();
     *
     * calendar.clear(); calendar2.clear();
     *
     * calendar.setTime(getEndDate());
     * calendar2.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
     * calendar.get(Calendar.DAY_OF_MONTH)); final long end =
     * calendar2.getTimeInMillis();
     *
     * return start <= end; }
     */

    private void updateDrawController() {
        Interval<Date> interval = defineInterval(getDate());
        DrawController.getSingletonInstance().setAvailableInterval(interval);
        DrawController.getSingletonInstance().setSelectedInterval(interval, true);
    }

    private boolean updateBandController() {

        final BandGroup group = (BandGroup) comboBoxGroup.getSelectedItem();
        final BandType bandType = (BandType) comboBoxData.getSelectedItem();

        List<YAxisElement> yAxisElements = DrawController.getSingletonInstance().getYAxisElements();
        if (yAxisElements.size() >= 2) {
            boolean present = false;
            for (YAxisElement el : yAxisElements) {
                if (el.getOriginalLabel().equals(bandType.getUnitLabel())) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                // Show dialog box to unselect one of the lines.
                JOptionPane.showMessageDialog(ImageViewerGui.getMainFrame(), "No more than two Y-axes can be used. Remove some of the lines before adding a new line.", "Too much y-axes", JOptionPane.WARNING_MESSAGE);
                return false;
            }
        }

        /*
         * if (!bandController.getSelectedGroup(identifier).equals(group)) {
         * bandController.removeAllBands(identifier); }
         */

        EVEDrawController.getSingletonInstance().bandAdded(bandType);

        return true;
    }

    @Override
    public boolean loadButtonPressed() {
        // check if start date is before end date -> if not show message
        /*
         * if (!isStartDateBeforeOrEqualEndDate()) {
         * JOptionPane.showMessageDialog(null, "End date is before start date!",
         * "", JOptionPane.ERROR_MESSAGE); return false; }
         */

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
