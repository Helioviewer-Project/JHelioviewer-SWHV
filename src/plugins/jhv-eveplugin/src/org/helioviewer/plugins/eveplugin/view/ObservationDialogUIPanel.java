package org.helioviewer.plugins.eveplugin.view;

import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialog;
import org.helioviewer.plugins.eveplugin.controller.DrawController;
import org.helioviewer.plugins.eveplugin.controller.ZoomController;
import org.helioviewer.plugins.eveplugin.draw.YAxisElement;
import org.helioviewer.plugins.eveplugin.lines.data.BandController;
import org.helioviewer.plugins.eveplugin.lines.data.EVECacheController;
import org.helioviewer.plugins.eveplugin.settings.BandGroup;
import org.helioviewer.plugins.eveplugin.settings.BandType;
import org.helioviewer.plugins.eveplugin.settings.BandTypeAPI;
import org.helioviewer.plugins.eveplugin.view.plot.PlotsContainerPanel;

/**
 * @author Stephan Pagel
 * */
public class ObservationDialogUIPanel extends SimpleObservationDialogUIPanel implements ActionListener {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 8932098719271808631L;
    private JLabel labelGroup;
    private JLabel labelData;
    private JComboBox comboBoxGroup;
    private JComboBox comboBoxData;

    private JPanel dataPane;

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    public ObservationDialogUIPanel(final PlotsContainerPanel plotsContainerPanel) {
        super(plotsContainerPanel);
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                // long start = System.currentTimeMillis();
                labelGroup = new JLabel("Group");
                labelData = new JLabel();
                comboBoxGroup = new JComboBox(new DefaultComboBoxModel());
                comboBoxData = new JComboBox(new DefaultComboBoxModel());
                dataPane = new JPanel();
                initVisualComponents();
                initGroups();
                // Log.debug("ObservationDialogUIPanel time: " +
                // (System.currentTimeMillis() - start));
            }
        });

    }

    private void initVisualComponents() {
        comboBoxGroup.addActionListener(this);

        dataPane.setLayout(new GridLayout(2, 2, GRIDLAYOUT_HGAP, GRIDLAYOUT_VGAP));
        dataPane.setBorder(BorderFactory.createTitledBorder(" Choose experiment specific data source "));
        dataPane.add(labelGroup);
        dataPane.add(comboBoxGroup);
        dataPane.add(labelData);
        dataPane.add(comboBoxData);

        this.add(dataPane);
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
        labelData.setText("Dataset");
        final BandController bandController = BandController.getSingletonInstance();
        final DefaultComboBoxModel model = (DefaultComboBoxModel) comboBoxData.getModel();
        final BandGroup selectedGroup = (BandGroup) comboBoxGroup.getSelectedItem();
        final String identifier = PlotsContainerPanel.PLOT_IDENTIFIER_MASTER;
        final BandType[] values = BandTypeAPI.getSingletonInstance().getBandTypes(selectedGroup);

        model.removeAllElements();

        for (final BandType value : values) {
            if (bandController.getBand(identifier, value) == null) {
                model.addElement(value);
            }
        }

        if (model.getSize() > 0) {
            comboBoxData.setSelectedIndex(0);
        }

        super.setLoadButtonEnabled(model.getSize() > 0);
        ObservationDialog.getSingletonInstance().setLoadButtonEnabled(super.getLoadButtonEnabled());
    }

    /**
     * Checks if the selected start date is before selected or equal to end
     * date. The methods checks the entered times when the dates are equal. If
     * the start time is greater than the end time the method will return false.
     * 
     * @return boolean value if selected start date is before selected end date.
     */
    private boolean isStartDateBeforeOrEqualEndDate() {
        final GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(getStartDate());

        final GregorianCalendar calendar2 = new GregorianCalendar(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        final long start = calendar2.getTimeInMillis();

        calendar.clear();
        calendar2.clear();

        calendar.setTime(getEndDate());
        calendar2.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        final long end = calendar2.getTimeInMillis();

        return start <= end;
    }

    private void updateZoomController() {
        ZoomController.getSingletonInstance().setAvailableInterval(new Interval<Date>(getStartDate(), getEndDate()));
    }

    private boolean updateBandController() {
        final BandController bandController = BandController.getSingletonInstance();

        final String identifier = PlotsContainerPanel.PLOT_IDENTIFIER_MASTER;
        final BandGroup group = (BandGroup) comboBoxGroup.getSelectedItem();
        final BandType bandType = (BandType) comboBoxData.getSelectedItem();
        bandType.setDataDownloader(EVECacheController.getSingletonInstance());

        Set<YAxisElement> yAxisElements = DrawController.getSingletonInstance().getYAxisElements(identifier);
        if (yAxisElements.size() >= 2) {
            boolean present = false;
            for (YAxisElement el : yAxisElements) {
                if (el.getOriginalLabel().equals(bandType.getLabel())) {
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

        bandController.selectBandGroup(identifier, group);
        bandController.addBand(identifier, bandType);

        if (identifier.equals(PlotsContainerPanel.PLOT_IDENTIFIER_SLAVE)) {
            plotsContainerPanel.setPlot2Visible(true);
        }
        return true;
    }

    @Override
    public void dialogOpened() {
        final Interval<Date> interval = ZoomController.getSingletonInstance().getAvailableInterval();

        final GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(interval.getEnd());
        calendar.add(Calendar.DAY_OF_MONTH, -1);

        // setStartDate(interval.getStart());
        // setEndDate(calendar.getTime());
    }

    @Override
    public boolean loadButtonPressed() {
        // check if start date is before end date -> if not show message
        if (!isStartDateBeforeOrEqualEndDate()) {
            JOptionPane.showMessageDialog(null, "End date is before start date!", "", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (updateBandController()) {
            updateZoomController();
            return true;
        }
        return true;
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Action Listener
    // //////////////////////////////////////////////////////////////////////////////

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource().equals(comboBoxGroup)) {
            updateGroupValues();
        }
    }
}
