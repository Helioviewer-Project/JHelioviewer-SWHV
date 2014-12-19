package org.helioviewer.plugins.eveplugin.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialog;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.plugins.eveplugin.controller.ZoomController;
import org.helioviewer.plugins.eveplugin.events.model.EventModel;
import org.helioviewer.plugins.eveplugin.events.model.EventModelListener;
import org.helioviewer.plugins.eveplugin.model.TimeIntervalLockModel;
import org.helioviewer.plugins.eveplugin.settings.EVESettings;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorPanel;
import org.helioviewer.viewmodel.view.View;

/**
 * 
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class ControlsPanel extends JPanel implements ActionListener, LayersListener, EventModelListener {

    /**
     * 
     */
    private static final long serialVersionUID = 3639870635351984819L;

    private static ControlsPanel singletongInstance;
    private final JPanel lineDataSelectorContainer = new JPanel();
    private final ImageIcon addIcon = IconBank.getIcon(JHVIcon.ADD);
    private final JButton addLayerButton = new JButton("Add Layer", addIcon);

    // private final String[] plots = { "No Events", "Events on Plot 1",
    // "Events on Plot 2" };
    // private final JComboBox eventsComboBox = new JComboBox(plots);
    private final JCheckBox eventsCheckBox = new JCheckBox();
    private final JLabel eventsLabel = new JLabel("Display events: ");
    private final ImageIcon movietimeIcon = IconBank.getIcon(JHVIcon.LAYER_MOVIE_TIME);
    private final JToggleButton periodFromLayersButton = new JToggleButton(movietimeIcon);

    private ControlsPanel() {
        initVisualComponents();
        LayersModel.getSingletonInstance().addLayersListener(this);
    }

    private void initVisualComponents() {
        EventModel.getSingletonInstance().addEventModelListener(this);

        eventsCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (eventsCheckBox.isSelected()) {
                    EventModel.getSingletonInstance().activateEvents();
                } else {
                    EventModel.getSingletonInstance().deactivateEvents();
                }
            }
        });

        addLayerButton.setToolTipText("Add a new layer");
        addLayerButton.addActionListener(this);

        periodFromLayersButton.setToolTipText("Request data of selected movie interval");
        periodFromLayersButton.setPreferredSize(new Dimension(movietimeIcon.getIconWidth() + 14, periodFromLayersButton.getPreferredSize().height));
        periodFromLayersButton.addActionListener(this);
        setEnabledStateOfPeriodMovieButton();
        // this.setPreferredSize(new Dimension(100, 300));
        lineDataSelectorContainer.setLayout(new BoxLayout(lineDataSelectorContainer, BoxLayout.Y_AXIS));
        lineDataSelectorContainer.setPreferredSize(new Dimension(100, 130));
        this.setLayout(new BorderLayout());

        add(lineDataSelectorContainer, BorderLayout.CENTER);

        JPanel pageEndPanel = new JPanel();
        pageEndPanel.setBackground(Color.BLUE);
        JPanel flowPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        flowPanel.add(eventsLabel);
        flowPanel.add(eventsCheckBox);
        flowPanel.add(periodFromLayersButton);
        flowPanel.add(addLayerButton);

        add(flowPanel, BorderLayout.PAGE_END);
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
            ObservationDialog.getSingletonInstance().showDialog(EVESettings.OBSERVATION_UI_NAME);
        } else if (e.getSource() == periodFromLayersButton) {
            final Interval<Date> interval = new Interval<Date>(LayersModel.getSingletonInstance().getFirstDate(), LayersModel.getSingletonInstance().getLastDate());
            ZoomController.getSingletonInstance().setSelectedInterval(interval, true);
            TimeIntervalLockModel.getInstance().setLocked(periodFromLayersButton.isSelected());
        }
    }

    private void setEnabledStateOfPeriodMovieButton() {
        final Interval<Date> frameInterval = LayersModel.getSingletonInstance().getFrameInterval();

        periodFromLayersButton.setEnabled(frameInterval.getStart() != null && frameInterval.getEnd() != null);
    }

    @Override
    public void layerAdded(int idx) {
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
        // TODO Auto-generated method stub

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
        eventsCheckBox.setSelected(false);
        repaint();
    }
}
