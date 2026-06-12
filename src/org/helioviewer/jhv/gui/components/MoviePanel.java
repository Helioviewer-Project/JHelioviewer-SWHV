package org.helioviewer.jhv.gui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.app.state.ViewState;
import org.helioviewer.jhv.gui.Actions;
import org.helioviewer.jhv.gui.CompletionNotifications;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.components.base.HoldRepeat;
import org.helioviewer.jhv.gui.components.base.JHVSpinner;
import org.helioviewer.jhv.gui.components.timeselector.TimeSelectorPanel;
import org.helioviewer.jhv.gui.dialogs.ObservationDialog;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.movie.ExportMovie;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.draw.DrawController;

import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideSplitButton;
import com.jidesoft.swing.JideToggleButton;

@SuppressWarnings("serial")
public class MoviePanel extends JPanel implements Interfaces.ObservationSelector, Movie.StatusListener, ExportMovie.StatusListener, ViewState.PlaybackConfigListener, ViewState.RecordingConfigListener {

    private static final int FRAME_HOLD_REPEAT_MS = 125;
    private int fixedPreferredWidth = -1;

    private boolean isAdvanced;

    private final TimeSelectorPanel timeSelectorPanel = new TimeSelectorPanel();
    private final ImageSelectorPanel imageSelectorPanel;
    private final JideSplitButton addLayerButton;

    private static TimeSlider timeSlider;
    private final JideButton playButton;

    private final RecordButton recordButton;

    private final JideButton advancedButton;
    private final JHVSpinner speedSpinner;
    private final JComboBox<ViewState.PlaybackSpeedUnit> speedUnitComboBox;
    private final JComboBox<Movie.AdvanceMode> advanceModeComboBox;
    private final JRadioButton loopButton;
    private final JRadioButton shotButton;
    private final JRadioButton freeButton;
    private final JComboBox<ViewState.RecordingSize> recordSizeComboBox;

    private final JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 0, 0));
    private final JPanel recordPanel = new JPanel(new GridBagLayout());

    private static MoviePanel instance;

    public static MoviePanel getInstance() {
        return instance == null ? instance = new MoviePanel() : instance;
    }

    private MoviePanel() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        // Time slider
        timeSlider = new TimeSlider(TimeSlider.HORIZONTAL, 0, 0, 0);

        JPanel sliderPanel = new JPanel(new BorderLayout());
        sliderPanel.add(timeSlider);

        JPanel secondLine = new JPanel(new BorderLayout());
        // Control buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 1, 0));
        int small = 18, big = 26;

        JideButton prevFrameButton = new JideButton(Buttons.backward);
        prevFrameButton.setFont(Buttons.getMaterialFont(small));
        prevFrameButton.setToolTipText("Step to previous frame");
        prevFrameButton.addActionListener(Actions.PREVIOUS_FRAME);
        HoldRepeat.install(prevFrameButton, FRAME_HOLD_REPEAT_MS);
        buttonPanel.add(prevFrameButton);

        playButton = new JideButton(Buttons.play);
        playButton.setFont(Buttons.getMaterialFont(big));
        playButton.setToolTipText("Play movie");
        playButton.addActionListener(Actions.PLAY_PAUSE);
        buttonPanel.add(playButton);

        JideButton nextFrameButton = new JideButton(Buttons.forward);
        nextFrameButton.setFont(Buttons.getMaterialFont(small));
        nextFrameButton.setToolTipText("Step to next frame");
        nextFrameButton.addActionListener(Actions.NEXT_FRAME);
        HoldRepeat.install(nextFrameButton, FRAME_HOLD_REPEAT_MS);
        buttonPanel.add(nextFrameButton);

        recordButton = new RecordButton(small);
        buttonPanel.add(recordButton);

        advancedButton = new JideButton(Buttons.optionsDown);
        advancedButton.setToolTipText("Options to control playback and recording");
        advancedButton.addActionListener(e -> setAdvanced(!isAdvanced));
        buttonPanel.add(advancedButton);

        secondLine.add(buttonPanel, BorderLayout.LINE_START);

        // Current frame number
        JComponent frameNumberPanel = timeSlider.getFrameNumberPanel();
        frameNumberPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        secondLine.add(frameNumberPanel, BorderLayout.LINE_END);

        // Speed
        modePanel.add(new JLabel(" Play ", JLabel.RIGHT));

        speedSpinner = new JHVSpinner(ViewState.playbackData().speed(), ViewState.PLAYBACK_SPEED_MIN, ViewState.PLAYBACK_SPEED_MAX, 1);
        speedSpinner.setToolTipText("Maximum " + ViewState.PLAYBACK_SPEED_MAX + " fps");
        speedSpinner.addChangeListener(e -> updatePlaybackConfig());
        modePanel.add(speedSpinner);

        speedUnitComboBox = new JComboBox<>(ViewState.PlaybackSpeedUnit.values());
        speedUnitComboBox.addActionListener(e -> updatePlaybackConfig());
        modePanel.add(speedUnitComboBox);

        // Animation mode
        modePanel.add(new JLabel(" and ", JLabel.RIGHT));

        advanceModeComboBox = new JComboBox<>(new Movie.AdvanceMode[]{Movie.AdvanceMode.Loop, Movie.AdvanceMode.Stop, Movie.AdvanceMode.Swing});
        advanceModeComboBox.addActionListener(e -> ViewState.setPlaybackAdvanceMode((Movie.AdvanceMode) advanceModeComboBox.getSelectedItem()));
        modePanel.add(advanceModeComboBox);

        // Record
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        loopButton = new JRadioButton(ViewState.RecordingMode.LOOP.toString());
        shotButton = new JRadioButton(ViewState.RecordingMode.SHOT.toString());
        freeButton = new JRadioButton(ViewState.RecordingMode.FREE.toString());

        c.gridy = 0;
        c.gridx = 0;
        recordPanel.add(new JLabel("Record ", JLabel.RIGHT), c);
        c.gridx = 1;
        recordPanel.add(loopButton, c);
        c.gridx = 2;
        recordPanel.add(shotButton, c);
        c.gridx = 3;
        recordPanel.add(freeButton, c);

        ButtonGroup group = new ButtonGroup();
        group.add(loopButton);
        group.add(shotButton);
        group.add(freeButton);

        loopButton.addActionListener(e -> ViewState.setRecordingMode(ViewState.RecordingMode.LOOP));
        shotButton.addActionListener(e -> ViewState.setRecordingMode(ViewState.RecordingMode.SHOT));
        freeButton.addActionListener(e -> ViewState.setRecordingMode(ViewState.RecordingMode.FREE));

        c.gridy = 1;
        c.gridx = 2;
        recordPanel.add(new JLabel("Size ", JLabel.RIGHT), c);

        recordSizeComboBox = new JComboBox<>(ViewState.RecordingSize.values());
        recordSizeComboBox.addActionListener(e -> ViewState.setRecordingSize((ViewState.RecordingSize) recordSizeComboBox.getSelectedItem()));
        c.gridx = 3;
        recordPanel.add(recordSizeComboBox, c);

        timeSelectorPanel.addListener(Layers.timeSelectionListener);

        add(sliderPanel);
        add(secondLine);
        add(modePanel);
        add(recordPanel);
        add(timeSelectorPanel);

        ObservationDialog.getInstance(); // make sure it's instanced
        imageSelectorPanel = new ImageSelectorPanel(this);

        addLayerButton = new JideSplitButton(Buttons.newLayer);
        addLayerButton.setAlwaysDropdown(true);
        addLayerButton.add(imageSelectorPanel);
        addLayerButton.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                EventQueue.invokeLater(() -> imageSelectorPanel.getFocused().grabFocus());
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {}
        });

        JideButton syncButton = new JideButton(Buttons.syncLayers);
        syncButton.setToolTipText("Synchronize time intervals of all layers");
        syncButton.addActionListener(e -> syncLayersSpan());

        JPanel addLayerPanel = new JPanel(new BorderLayout());
        addLayerPanel.add(addLayerButton, BorderLayout.LINE_START);
        addLayerPanel.add(syncButton, BorderLayout.LINE_END);
        add(addLayerPanel);

        add(JHVFrame.getLayersPanel());

        Movie.addStatusListener(this);
        ExportMovie.addStatusListener(this);
        ViewState.addPlaybackConfigListener(this);
        ViewState.addRecordingConfigListener(this);
    }

    @Override
    public int getCadence() {
        return TimeUtils.defaultCadence(getStartTime(), getEndTime());
    }

    @Override
    public void setTime(long start, long end) {
        timeSelectorPanel.setTime(start, end);
    }

    @Override
    public long getStartTime() {
        return timeSelectorPanel.getStartTime();
    }

    @Override
    public long getEndTime() {
        return timeSelectorPanel.getEndTime();
    }

    @Override
    public void load(String server, int sourceId) {
        addLayerButton.doClickOnMenu();
        if (checkSanity())
            imageSelectorPanel.load(null, server, sourceId, getStartTime(), getEndTime(), getCadence());
    }

    @Override
    public void setAvailabilityEnabled(boolean enabled) {}

    private boolean checkSanity() {
        long start = getStartTime();
        long end = getEndTime();
        if (start > end) {
            setTime(end, end);
            JOptionPane.showMessageDialog(null, "End date is before start date", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    public void syncLayersSpan(long start, long end) {
        setTime(start, end);
        syncLayersSpan();
    }

    private void syncLayersSpan() {
        if (checkSanity()) {
            long start = getStartTime();
            long end = getEndTime();
            DrawController.setSelectedInterval(start, end);
            ImageLayers.syncLayersSpan(start, end, getCadence());
        }
    }

    private static class RecordButton extends JideToggleButton implements ActionListener {
        RecordButton(float fontSize) {
            super(Buttons.record);
            setFont(Buttons.getMaterialFont(fontSize));
            setForeground(Color.decode("#800000"));
            setToolTipText("Record movie");
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (isSelected()) {
                Commands.recordStart(CompletionNotifications.recordingContext(), null);
            } else {
                Commands.recordStop();
            }
        }
    }

    public void setAdvanced(boolean advanced) {
        isAdvanced = advanced;
        advancedButton.setText(advanced ? Buttons.optionsDown : Buttons.optionsRight);
        modePanel.setVisible(advanced);
        recordPanel.setVisible(advanced);
    }

    public void setFixedPreferredWidth(int width) {
        fixedPreferredWidth = width;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        if (fixedPreferredWidth > 0)
            size.width = fixedPreferredWidth;
        return size;
    }

    private void updatePlaybackConfig() {
        int speed = ((Number) speedSpinner.getValue()).intValue();
        ViewState.PlaybackSpeedUnit unit = (ViewState.PlaybackSpeedUnit) speedUnitComboBox.getSelectedItem();
        if (unit == null)
            return;
        ViewState.setPlaybackSpeed(speed, unit);
    }

    public static TimeSlider getTimeSlider() {
        return timeSlider;
    }

    @Override
    public void movieStatusChanged() {
        boolean playing = Movie.isPlaying();

        if (playing) {
            playButton.setText(Buttons.pause);
            playButton.setToolTipText("Pause movie");
        } else {
            playButton.setText(Buttons.play);
            playButton.setToolTipText("Play movie");
        }
    }

    @Override
    public void recordingStatusChanged() {
        boolean recording = ExportMovie.isRecording();
        if (recordButton.isSelected() != recording)
            recordButton.setSelected(recording);
        ComponentUtils.setEnabled(modePanel, !recording);
        ComponentUtils.setEnabled(recordPanel, !recording);
    }

    @Override
    public void playbackConfigChanged() {
        ViewState.PlaybackData playbackData = ViewState.playbackData();

        if (advanceModeComboBox.getSelectedItem() != playbackData.advanceMode())
            advanceModeComboBox.setSelectedItem(playbackData.advanceMode());

        int speed = playbackData.speed();
        // Do not call speedSpinner.getValue() here: JHVSpinner commits editor text on read,
        // and this passive UI sync must not force-commit an in-progress edit.
        Number spinnerSpeed = ((SpinnerNumberModel) speedSpinner.getModel()).getNumber();
        if (spinnerSpeed.intValue() != speed)
            speedSpinner.setValue(speed);

        if (speedUnitComboBox.getSelectedItem() != playbackData.speedUnit())
            speedUnitComboBox.setSelectedItem(playbackData.speedUnit());
    }

    @Override
    public void recordingConfigChanged() {
        ViewState.RecordingData recordingData = ViewState.recordingData();
        switch (recordingData.mode()) {
            case LOOP -> loopButton.setSelected(true);
            case SHOT -> shotButton.setSelected(true);
            case FREE -> freeButton.setSelected(true);
        }
        if (recordSizeComboBox.getSelectedItem() != recordingData.size())
            recordSizeComboBox.setSelectedItem(recordingData.size());
    }

}
