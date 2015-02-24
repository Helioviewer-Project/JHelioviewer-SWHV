package org.helioviewer.plugins.eveplugin.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.plugins.eveplugin.controller.ZoomController;
import org.helioviewer.plugins.eveplugin.controller.ZoomController.ZOOM;
import org.helioviewer.plugins.eveplugin.controller.ZoomControllerListener;
import org.helioviewer.plugins.eveplugin.events.model.EventModel;
import org.helioviewer.plugins.eveplugin.events.model.EventModelListener;
import org.helioviewer.plugins.eveplugin.model.TimeIntervalLockModel;
import org.helioviewer.plugins.eveplugin.settings.EVEAPI.API_RESOLUTION_AVERAGES;
import org.helioviewer.plugins.eveplugin.settings.EVESettings;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorModelListener;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorPanel;
import org.helioviewer.plugins.eveplugin.view.plot.PlotsContainerPanel;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;

/**
 * 
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class ControlsPanel extends JPanel implements ActionListener, LayersListener, EventModelListener, ZoomControllerListener, LineDataSelectorModelListener {

    /**
     *
     */
    private static final long serialVersionUID = 3639870635351984819L;

    private static ControlsPanel singletongInstance;
    private final JPanel lineDataSelectorContainer = new JPanel();
    private final ImageIcon addIcon = IconBank.getIcon(JHVIcon.ADD);
    private final JButton addLayerButton = new JButton("Add Layer", addIcon);

    private final JComboBox zoomComboBox = new JComboBox(new DefaultComboBoxModel());
    private Interval<Date> selectedIntervalByZoombox = null;
    private boolean setDefaultPeriod = true;

    // private final String[] plots = { "No Events", "Events on Plot 1",
    // "Events on Plot 2" };
    // private final JComboBox eventsComboBox = new JComboBox(plots);
    private final ImageIcon movietimeIcon = IconBank.getIcon(JHVIcon.LAYER_MOVIE_TIME);
    private final JToggleButton periodFromLayersButton = new JToggleButton(movietimeIcon);

    private boolean selectedIndexSetByProgram;

    private ControlsPanel() {
        initVisualComponents();
        ZoomController.getSingletonInstance().addZoomControllerListener(this);
        LayersModel.getSingletonInstance().addLayersListener(this);
    }

    private void initVisualComponents() {
        EventModel.getSingletonInstance().addEventModelListener(this);

        LineDataSelectorModel.getSingletonInstance().addLineDataSelectorModelListener(this);

        addLayerButton.setToolTipText("Add a new layer");
        addLayerButton.addActionListener(this);
        addLayerButton.setMargin(new Insets(0, 0, 0, 0));

        periodFromLayersButton.setToolTipText("Synchronize movie and time series display");
        periodFromLayersButton.setPreferredSize(new Dimension(movietimeIcon.getIconWidth() + 14, periodFromLayersButton.getPreferredSize().height));
        periodFromLayersButton.addActionListener(this);
        periodFromLayersButton.setMargin(new Insets(0, 0, 0, 0));

        setEnabledStateOfPeriodMovieButton();
        // this.setPreferredSize(new Dimension(100, 300));
        lineDataSelectorContainer.setLayout(new BoxLayout(lineDataSelectorContainer, BoxLayout.Y_AXIS));
        lineDataSelectorContainer.setPreferredSize(new Dimension(100, 130));
        this.setLayout(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        add(lineDataSelectorContainer, gc);

        JPanel pageEndPanel = new JPanel();
        pageEndPanel.setBackground(Color.BLUE);

        zoomComboBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        zoomComboBox.addActionListener(this);
        zoomComboBox.setEnabled(false);

        GridBagConstraints gbc = new GridBagConstraints();

        JPanel flowPanel = new JPanel(new GridBagLayout());

        gbc.gridx = 0;
        gbc.gridy = 0;

        flowPanel.add(zoomComboBox, gbc);

        gbc.gridx = 1;
        flowPanel.add(periodFromLayersButton, gbc);

        gbc.gridx = 2;
        flowPanel.add(addLayerButton, gbc);

        gc.gridy = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        gc.weighty = 1.0;
        add(flowPanel, gc);

    }

    public static ControlsPanel getSingletonInstance() {
        if (singletongInstance == null) {
            singletongInstance = new ControlsPanel();
        }

        return singletongInstance;
    }

    public void addLineDataSelector(LineDataSelectorPanel lineDataSelectorPanel) {
        lineDataSelectorContainer.add(lineDataSelectorPanel);
    }

    public void removeLineDataSelector(LineDataSelectorPanel lineDataSelectorPanel) {
        lineDataSelectorContainer.remove(lineDataSelectorPanel);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(addLayerButton)) {
            ImageViewerGui.getSingletonInstance().getObservationDialog().showDialog(EVESettings.OBSERVATION_UI_NAME);
        } else if (e.getSource() == periodFromLayersButton) {
            TimeIntervalLockModel.getInstance().setLocked(periodFromLayersButton.isSelected());
            if (periodFromLayersButton.isSelected()) {
                setDateRange();
            }
        } else if (e.getSource().equals(zoomComboBox)) {
            final ZoomComboboxItem item = (ZoomComboboxItem) zoomComboBox.getSelectedItem();
            selectedIntervalByZoombox = null;

            if (item != null && !selectedIndexSetByProgram) {
                selectedIntervalByZoombox = ZoomController.getSingletonInstance().zoomTo(item.getZoom(), item.getNumber());
            } else {
                if (selectedIndexSetByProgram) {
                    selectedIndexSetByProgram = false;
                }
            }
        }
    }

    private void setDateRange() {
        View activeView = LayersModel.getSingletonInstance().getActiveView();
        JHVJPXView jpxView = activeView.getAdapter(JHVJPXView.class);
        if (jpxView != null) {
            Date start = jpxView.getDateRange().getStart();
            Date end = jpxView.getDateRange().getEnd();
            // Log.debug("start " + start + " end " + end);
            final Interval<Date> interval = new Interval<Date>(start, end);
            ZoomController.getSingletonInstance().setSelectedInterval(interval, true);
        }
    }

    private void setEnabledStateOfPeriodMovieButton() {
        final Interval<Date> frameInterval = LayersModel.getSingletonInstance().getFrameInterval();

        periodFromLayersButton.setEnabled(frameInterval.getStart() != null && frameInterval.getEnd() != null);
    }

    @Override
    public void layerAdded(int idx) {
        if (EventQueue.isDispatchThread()) {
            setSelectedIntervalOnLayerNotification();
        } else {
            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    setSelectedIntervalOnLayerNotification();
                }

            });

        }
    }

    @Override
    public void layerRemoved(View oldView, int oldIdx) {
        if (EventQueue.isDispatchThread()) {
            setEnabledStateOfPeriodMovieButton();
        } else {
            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    setEnabledStateOfPeriodMovieButton();
                }

            });

        }
    }

    @Override
    public void layerChanged(int idx) {
        // TODO Auto-generated method stub

    }

    @Override
    public void activeLayerChanged(int idx) {
        if (EventQueue.isDispatchThread()) {
            setSelectedIntervalOnLayerNotification();
        } else {
            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    setSelectedIntervalOnLayerNotification();
                }

            });

        }
    }

    private void setSelectedIntervalOnLayerNotification() {
        setEnabledStateOfPeriodMovieButton();
        if (setDefaultPeriod || TimeIntervalLockModel.getInstance().isLocked()) {
            setDefaultPeriod = false;
            View activeView = LayersModel.getSingletonInstance().getActiveView();
            if (activeView != null) {
                JHVJPXView jpxView = activeView.getAdapter(JHVJPXView.class);
                if (jpxView != null) {
                    Date start = jpxView.getDateRange().getStart();
                    Date end = jpxView.getDateRange().getEnd();
                    // Log.debug("start " + start + " end " + end);
                    final Interval<Date> interval = new Interval<Date>(start, end);
                    // ZoomController.getSingletonInstance().setAvailableInterval(interval);
                    if (TimeIntervalLockModel.getInstance().isLocked()) {
                        ZoomController.getSingletonInstance().setSelectedInterval(interval, false);
                    }
                }
            }
        }
    }

    @Override
    public void viewportGeometryChanged() {
        // TODO Auto-generated method stub

    }

    @Override
    public void timestampChanged(int idx) {
        // TODO Auto-generated method stub

    }

    @Override
    public void subImageDataChanged() {
        // TODO Auto-generated method stub

    }

    @Override
    public void layerDownloaded(int idx) {
        // TODO Auto-generated method stub

    }

    @Override
    public void eventsDeactivated() {
        repaint();
    }

    private void fillZoomComboBox() {
        selectedIndexSetByProgram = true;
        final Interval<Date> interval = ZoomController.getSingletonInstance().getAvailableInterval();
        final Date startDate = interval.getStart();

        final Calendar calendar = new GregorianCalendar();
        calendar.clear();
        calendar.setTime(startDate);
        calendar.add(Calendar.YEAR, 1);
        calendar.add(Calendar.HOUR, 1);

        calendar.clear();
        calendar.setTime(startDate);
        calendar.add(Calendar.MONTH, 3);
        ;

        final DefaultComboBoxModel model = (DefaultComboBoxModel) zoomComboBox.getModel();
        model.removeAllElements();
        selectedIndexSetByProgram = true;
        model.addElement(new ZoomComboboxItem(ZOOM.CUSTOM, 0));
        selectedIndexSetByProgram = true;
        model.addElement(new ZoomComboboxItem(ZOOM.All, 0));

        // addElementToModel(model, startDate, interval, Calendar.YEAR, 10,
        // ZOOM.Year);
        // addElementToModel(model, startDate, interval, Calendar.YEAR, 5,
        // ZOOM.Year);

        addElementToModel(model, startDate, interval, Calendar.YEAR, 1, ZOOM.Year);
        addElementToModel(model, startDate, interval, Calendar.MONTH, 6, ZOOM.Month);
        addElementToModel(model, startDate, interval, Calendar.MONTH, 3, ZOOM.Month);
        // addElementToModel(model, startDate, interval, Calendar.MONTH, 1,
        // ZOOM.Month);

        // addCarringtonRotationToModel(model, startDate, interval, 6);
        // addCarringtonRotationToModel(model, startDate, interval, 3);
        addCarringtonRotationToModel(model, startDate, interval, 1);

        // if (!years) {
        // addElementToModel(model, startDate, interval, Calendar.DATE, 14,
        // ZOOM.Day);
        addElementToModel(model, startDate, interval, Calendar.DATE, 7, ZOOM.Day);
        // addElementToModel(model, startDate, interval, Calendar.DATE, 1,
        // ZOOM.Day);

        addElementToModel(model, startDate, interval, Calendar.HOUR, 12, ZOOM.Hour);
        addElementToModel(model, startDate, interval, Calendar.HOUR, 6, ZOOM.Hour);
        addElementToModel(model, startDate, interval, Calendar.HOUR, 1, ZOOM.Hour);

    }

    private boolean addCarringtonRotationToModel(final DefaultComboBoxModel model, final Date startDate, final Interval<Date> interval, final int numberOfRotations) {
        final Calendar calendar = new GregorianCalendar();

        calendar.clear();
        calendar.setTime(new Date(startDate.getTime() + numberOfRotations * 2356585820l));

        // if (interval.containsPointInclusive(calendar.getTime())) {
        model.addElement(new ZoomComboboxItem(ZOOM.Carrington, numberOfRotations));
        return true;
        // }

        // return false;
    }

    private boolean addElementToModel(final DefaultComboBoxModel model, final Date startDate, final Interval<Date> interval, final int calendarField, final int calendarValue, final ZOOM zoom) {
        final Calendar calendar = new GregorianCalendar();

        calendar.clear();
        calendar.setTime(startDate);
        calendar.add(calendarField, calendarValue);

        // if (interval.containsPointInclusive(calendar.getTime())) {
        model.addElement(new ZoomComboboxItem(zoom, calendarValue));
        return true;
        // }

        // return false;
    }

    private class ZoomComboboxItem {

        // //////////////////////////////////////////////////////////////////////////
        // Definitions
        // //////////////////////////////////////////////////////////////////////////

        private final ZOOM zoom;
        private final int number;

        // //////////////////////////////////////////////////////////////////////////
        // Methods
        // //////////////////////////////////////////////////////////////////////////

        public ZoomComboboxItem(final ZOOM zoom, final int number) {
            this.zoom = zoom;
            this.number = number;
        }

        public ZOOM getZoom() {
            return zoom;
        }

        public int getNumber() {
            return number;
        }

        @Override
        public String toString() {
            final String plural = number > 1 ? "s" : "";

            switch (zoom) {
            case All:
                return "Maximum Interval";
            case Hour:
                return Integer.toString(number) + " Hour" + plural;
            case Day:
                return Integer.toString(number) + " Day" + plural;
            case Month:
                return Integer.toString(number) + " Month" + plural;
            case Year:
                return Integer.toString(number) + " Year" + plural;
            case Carrington:
                return Integer.toString(number) + " Carrington Rotation" + plural;
            default:
                break;
            }

            return "Custom Interval";
        }
    }

    @Override
    public void availableIntervalChanged(Interval<Date> newInterval) {
        if (newInterval.getStart() != null || newInterval.getEnd() != null) {
            final Calendar calendar = new GregorianCalendar();
            calendar.clear();
            calendar.setTime(newInterval.getEnd());
            calendar.add(Calendar.DATE, -1);
            selectedIndexSetByProgram = true;
            fillZoomComboBox();
        }
    }

    @Override
    public void selectedIntervalChanged(Interval<Date> newInterval, boolean keepFullValueSpace) {
        if (selectedIntervalByZoombox != null && newInterval != null) {
            if (!selectedIntervalByZoombox.equals(newInterval)) {
                try {
                    selectedIndexSetByProgram = true;
                    zoomComboBox.setSelectedIndex(0);
                } catch (final IllegalArgumentException ex) {
                }
            }
        }
    }

    @Override
    public void selectedResolutionChanged(API_RESOLUTION_AVERAGES newResolution) {
        // TODO Auto-generated method stub

    }

    @Override
    public void downloadStartded(LineDataSelectorElement element) {
        // TODO Auto-generated method stub

    }

    @Override
    public void downloadFinished(LineDataSelectorElement element) {
        // TODO Auto-generated method stub

    }

    @Override
    public void lineDataAdded(LineDataSelectorElement element) {
        zoomComboBox.setEnabled(true);
    }

    @Override
    public void lineDataRemoved(LineDataSelectorElement element) {
        if (LineDataSelectorModel.getSingletonInstance().getNumberOfAvailableLineData(PlotsContainerPanel.PLOT_IDENTIFIER_MASTER) == 0) {
            zoomComboBox.setEnabled(false);
        }
    }

    @Override
    public void lineDataUpdated(LineDataSelectorElement element) {
        // TODO Auto-generated method stub

    }
}
