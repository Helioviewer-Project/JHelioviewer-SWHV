package org.helioviewer.plugins.eveplugin.view.linedataselector;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.helioviewer.base.interval.Interval;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.plugins.eveplugin.controller.DrawController;
import org.helioviewer.plugins.eveplugin.controller.TimingListener;
import org.helioviewer.plugins.eveplugin.controller.ZoomController;
import org.helioviewer.plugins.eveplugin.controller.ZoomController.ZOOM;
import org.helioviewer.plugins.eveplugin.model.TimeIntervalLockModel;
import org.helioviewer.viewmodel.view.AbstractView;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;

public class IntervalOptionPanel extends JPanel implements ActionListener, LayersListener, TimingListener, LineDataSelectorModelListener {

    private final JComboBox zoomComboBox = new JComboBox(new DefaultComboBoxModel());
    private final ImageIcon movietimeIcon = IconBank.getIcon(JHVIcon.LAYER_MOVIE_TIME);
    private final JToggleButton periodFromLayersButton = new JToggleButton(movietimeIcon);
    private boolean setDefaultPeriod = true;
    private boolean selectedIndexSetByProgram;
    private Interval<Date> selectedIntervalByZoombox = null;

    public IntervalOptionPanel() {
        Displayer.getLayersModel().addLayersListener(this);
        DrawController.getSingletonInstance().addTimingListener(this);
        LineDataSelectorModel.getSingletonInstance().addLineDataSelectorModelListener(this);
        initVisualComponents();
    }

    private void initVisualComponents() {
        zoomComboBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        fillZoomComboBox();
        zoomComboBox.addActionListener(this);
        zoomComboBox.setEnabled(false);

        periodFromLayersButton.setToolTipText("Synchronize movie and time series display");
        periodFromLayersButton.setPreferredSize(new Dimension(movietimeIcon.getIconWidth() + 14, periodFromLayersButton.getPreferredSize().height));
        periodFromLayersButton.addActionListener(this);
        periodFromLayersButton.setMargin(new Insets(0, 0, 0, 0));
        setEnabledStateOfPeriodMovieButton();

        GridBagConstraints gbc = new GridBagConstraints();
        setLayout(new GridBagLayout());
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(zoomComboBox, gbc);

        gbc.gridx = 1;
        add(periodFromLayersButton, gbc);

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == periodFromLayersButton) {
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

    @Override
    public void downloadStartded(LineDataSelectorElement element) {
    }

    @Override
    public void downloadFinished(LineDataSelectorElement element) {
    }

    @Override
    public void lineDataAdded(LineDataSelectorElement element) {
        zoomComboBox.setEnabled(true);
    }

    @Override
    public void lineDataRemoved(LineDataSelectorElement element) {
        if (LineDataSelectorModel.getSingletonInstance().getNumberOfAvailableLineData() == 0) {
            zoomComboBox.setEnabled(false);
        }
    }

    @Override
    public void lineDataUpdated(LineDataSelectorElement element) {
    }

    private void addCarringtonRotationToModel(final DefaultComboBoxModel model, final int numberOfRotations) {
        model.addElement(new ZoomComboboxItem(ZOOM.Carrington, numberOfRotations));
    }

    private boolean addElementToModel(final DefaultComboBoxModel model, final int calendarField, final int calendarValue, final ZOOM zoom) {
        model.addElement(new ZoomComboboxItem(zoom, calendarValue));
        return true;
    }

    private void setDateRange() {
        AbstractView activeView = Displayer.getLayersModel().getActiveView();
        if (activeView instanceof JHVJPXView) {
            JHVJPXView jpxView = (JHVJPXView) activeView;
            Date start = Displayer.getLayersModel().getStartDate(jpxView).getTime();
            Date end = Displayer.getLayersModel().getEndDate(jpxView).getTime();

            Interval<Date> interval = new Interval<Date>(start, end);
            DrawController.getSingletonInstance().setSelectedInterval(interval, true);
        }
    }

    private void setEnabledStateOfPeriodMovieButton() {
        Date start = Displayer.getLayersModel().getFirstDate();
        Date end = Displayer.getLayersModel().getLastDate();

        periodFromLayersButton.setEnabled(start != null && end != null);
    }

    private void fillZoomComboBox() {
        final DefaultComboBoxModel model = (DefaultComboBoxModel) zoomComboBox.getModel();
        model.removeAllElements();
        model.addElement(new ZoomComboboxItem(ZOOM.CUSTOM, 0));
        model.addElement(new ZoomComboboxItem(ZOOM.All, 0));

        addElementToModel(model, Calendar.YEAR, 1, ZOOM.Year);
        addElementToModel(model, Calendar.MONTH, 6, ZOOM.Month);
        addElementToModel(model, Calendar.MONTH, 3, ZOOM.Month);
        addCarringtonRotationToModel(model, 1);

        addElementToModel(model, Calendar.DATE, 7, ZOOM.Day);

        addElementToModel(model, Calendar.HOUR, 12, ZOOM.Hour);
        addElementToModel(model, Calendar.HOUR, 6, ZOOM.Hour);
        addElementToModel(model, Calendar.HOUR, 1, ZOOM.Hour);
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
                return "Carrington Rotation" + plural;
            default:
                break;
            }

            return "Custom Interval";
        }
    }

    @Override
    public void layerAdded(int idx) {
    }

    @Override
    public void layerRemoved(int oldIdx) {
        setEnabledStateOfPeriodMovieButton();
    }

    @Override
    public void activeLayerChanged(AbstractView view) {
        setEnabledStateOfPeriodMovieButton();
        if (setDefaultPeriod || TimeIntervalLockModel.getInstance().isLocked()) {
            setDefaultPeriod = false;
            if (view instanceof JHVJPXView) {
                JHVJPXView jpxView = (JHVJPXView) view;
                Date start = Displayer.getLayersModel().getStartDate(jpxView).getTime();
                Date end = Displayer.getLayersModel().getEndDate(jpxView).getTime();

                Interval<Date> interval = new Interval<Date>(start, end);
                // ZoomController.getSingletonInstance().setAvailableInterval(interval);
                if (TimeIntervalLockModel.getInstance().isLocked()) {
                    DrawController.getSingletonInstance().setSelectedInterval(interval, false);
                }
            }
        }
    }

    @Override
    public void availableIntervalChanged() {
    }

    @Override
    public void selectedIntervalChanged() {
        Interval<Date> newInterval = DrawController.getSingletonInstance().getSelectedInterval();
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

}
