package org.helioviewer.plugins.eveplugin.view;

import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialog;
import org.helioviewer.plugins.eveplugin.controller.ZoomController;
import org.helioviewer.plugins.eveplugin.lines.data.BandController;
import org.helioviewer.plugins.eveplugin.lines.data.EVECacheController;
import org.helioviewer.plugins.eveplugin.settings.BandType;
import org.helioviewer.plugins.eveplugin.settings.BandGroup;
import org.helioviewer.plugins.eveplugin.settings.BandTypeAPI;


import org.helioviewer.plugins.eveplugin.view.plot.PlotsContainerPanel;

/**
 * @author Stephan Pagel
 * */
public class ObservationDialogUIPanel extends SimpleObservationDialogUIPanel{//ObservationDialogPanel implements JHVCalendarListener, ActionListener {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////
    
    //private static final long serialVersionUID = 1L;
    
    //private static final String[] DATA_LABEL_TEXTS = new String[] {"Band", "Band", "Diodes", "Lines"};
    
    /*private final PlotsContainerPanel plotsContainerPanel;
    
    private boolean enableLoadButton = true;
    
    private final JLabel labelStartDate = new JLabel("Start Date");
    private final JLabel labelEndDate = new JLabel("End Date");
    private final JHVCalendarDatePicker calendarStartDate = new JHVCalendarDatePicker();
    private final JHVCalendarDatePicker calendarEndDate = new JHVCalendarDatePicker();
    
    private final JComboBox plotComboBox = new JComboBox(new String[] {"Plot 1", "Plot 2"});*/
    
    private JLabel labelGroup;
    private JLabel labelData;
    private JComboBox comboBoxGroup;
    private JComboBox comboBoxData;
    
    private JPanel timePane;
    private JPanel plotPane;
    private JPanel dataPane;

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////
    
    public ObservationDialogUIPanel(final PlotsContainerPanel plotsContainerPanel) {
        super(plotsContainerPanel);
        EventQueue.invokeLater(new Runnable() {
            public void run() {
		        labelGroup = new JLabel("Group");
		        labelData = new JLabel();
		        comboBoxGroup = new JComboBox(new DefaultComboBoxModel());
		        comboBoxData = new JComboBox(new DefaultComboBoxModel());
		        
		        timePane = new JPanel();
		        plotPane = new JPanel();
		        dataPane = new JPanel();
		        initVisualComponents();
		        initGroups();
            }
        });
        
        
    }
    
    private void initVisualComponents() {
        // set up time settings
        /*calendarStartDate.setDateFormat(Settings.getSingletonInstance().getProperty("default.date.format"));
        calendarStartDate.addJHVCalendarListener(this);
        calendarStartDate.setToolTipText("Date in UTC starting the observation");
        
        calendarEndDate.setDateFormat(Settings.getSingletonInstance().getProperty("default.date.format"));
        calendarEndDate.addJHVCalendarListener(this);
        calendarEndDate.setToolTipText("Date in UTC ending the observation.");
        
        final JPanel startDatePane = new JPanel(new BorderLayout());
        startDatePane.add(labelStartDate, BorderLayout.PAGE_START);
        startDatePane.add(calendarStartDate, BorderLayout.CENTER);
        
        final JPanel endDatePane = new JPanel(new BorderLayout());
        endDatePane.add(labelEndDate, BorderLayout.PAGE_START);
        endDatePane.add(calendarEndDate, BorderLayout.CENTER);
        
        timePane.setLayout(new GridLayout(1, 2, GRIDLAYOUT_HGAP, GRIDLAYOUT_VGAP));
        timePane.setBorder(BorderFactory.createTitledBorder(" Select time range of interest "));
        timePane.add(startDatePane);
        timePane.add(endDatePane);
        
        // set up plot selection
        plotPane.setLayout(new FlowLayout(FlowLayout.LEFT));
        plotPane.setBorder(BorderFactory.createTitledBorder(" Choose plot where to display the data "));
        plotPane.add(plotComboBox);
        
        plotComboBox.addActionListener(this);
        */
        // set up data selection
        comboBoxGroup.addActionListener(this);
        
        dataPane.setLayout(new GridLayout(2, 2, GRIDLAYOUT_HGAP, GRIDLAYOUT_VGAP));
        dataPane.setBorder(BorderFactory.createTitledBorder(" Choose experiment specific data source "));
        dataPane.add(labelGroup);
        dataPane.add(comboBoxGroup);
        dataPane.add(labelData);
        dataPane.add(comboBoxData);
        
        // set basic layout
        //this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        //this.add(timePane);
        //this.add(plotPane);
        this.add(dataPane);
    }
    
    private void initGroups() {
        final BandGroup[] groups = BandTypeAPI.getSingletonInstance().getGroups();
        final DefaultComboBoxModel model = (DefaultComboBoxModel) comboBoxGroup.getModel();
        model.removeAllElements();
        
        for (final BandGroup group : groups) {
            model.addElement(group);
        }
    }
    
    private void updateGroupValues() {
        // change text of data label
        labelData.setText("Dataset");
        // update values in data combobox
        final BandController bandController = BandController.getSingletonInstance();
        final DefaultComboBoxModel model = (DefaultComboBoxModel) comboBoxData.getModel(); 
        final BandGroup selectedGroup = (BandGroup) comboBoxGroup.getSelectedItem();
        final String identifier = super.getPlotComboBoxSelectedIndex() == 0 ? PlotsContainerPanel.PLOT_IDENTIFIER_MASTER : PlotsContainerPanel.PLOT_IDENTIFIER_SLAVE;
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
    
    /*public void setStartDate(final Date start) {
        calendarStartDate.setDate(start);
    }
    
    public void setEndDate(final Date end) {
        calendarEndDate.setDate(end);
    }
    
    public Date getStartDate() {
        return calendarStartDate.getDate();
    }
    
    public Date getEndDate() {
        return calendarEndDate.getDate();
    }*/

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
    	//PlotTimeSpace.getInstance().setMinAndMaxTime(getStartDate(), getEndDate());
    }
    
    private void updateBandController() {
        final BandController bandController = BandController.getSingletonInstance();
        
        final String identifier = plotComboBox.getSelectedIndex() == 0 ? PlotsContainerPanel.PLOT_IDENTIFIER_MASTER : PlotsContainerPanel.PLOT_IDENTIFIER_SLAVE;
        final BandGroup group = (BandGroup) comboBoxGroup.getSelectedItem();
        final BandType bandType = (BandType) comboBoxData.getSelectedItem();
        bandType.setDataDownloader(EVECacheController.getSingletonInstance());
        
        if (!bandController.getSelectedGroup(identifier).equals(group)) {
            bandController.removeAllBands(identifier);
        }
        
        bandController.selectBandGroup(identifier, group);
        bandController.addBand(identifier, bandType);
        
        if (identifier.equals(PlotsContainerPanel.PLOT_IDENTIFIER_SLAVE)) {
            plotsContainerPanel.setPlot2Visible(true);
        }
    }
    
    @Override
    public void dialogOpened() {
        final Interval<Date> interval = ZoomController.getSingletonInstance().getAvailableInterval();
        
        final GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(interval.getEnd());
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        
        setStartDate(interval.getStart());
        setEndDate(calendar.getTime());
        
        plotComboBox.setSelectedIndex(0);
    }
    
    /*@Override
    public void selected() {
        ObservationDialog.getSingletonInstance().setLoadButtonEnabled(enableLoadButton);
    }

    @Override
    public void deselected() {}*/

    @Override
    public boolean loadButtonPressed() {
        // check if start date is before end date -> if not show message
        if (!isStartDateBeforeOrEqualEndDate()) {
            JOptionPane.showMessageDialog(null, "End date is before start date!", "", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        updateZoomController();
        updateBandController();
        
        return true;
    }

    /*
    @Override
    public void cancelButtonPressed() { }
    */

    // //////////////////////////////////////////////////////////////////////////////
    // JHV Calendar Listener
    // //////////////////////////////////////////////////////////////////////////////
    /*
    public void actionPerformed(final JHVCalendarEvent e) {
        if (e.getSource() == calendarStartDate && !isStartDateBeforeOrEqualEndDate()) {
            calendarEndDate.setDate(calendarStartDate.getDate());
        }

        if (e.getSource() == calendarEndDate && !isStartDateBeforeOrEqualEndDate()) {
            calendarStartDate.setDate(calendarStartDate.getDate());
        }
    }
    */
    // //////////////////////////////////////////////////////////////////////////////
    // Action Listener
    // //////////////////////////////////////////////////////////////////////////////

    public void actionPerformed(final ActionEvent e) {
        if (e.getSource().equals(comboBoxGroup)) {
            updateGroupValues();
        } else {
        	super.actionPerformed(e);
        }/*if (e.getSource().equals(plotComboBox)) {
            final String identifier = plotComboBox.getSelectedIndex() == 0 ? PlotsContainerPanel.PLOT_IDENTIFIER_MASTER : PlotsContainerPanel.PLOT_IDENTIFIER_SLAVE;
            final BandGroup group = BandController.getSingletonInstance().getSelectedGroup(identifier);
            
            comboBoxGroup.setSelectedItem(group);
        }*/
    }
}
