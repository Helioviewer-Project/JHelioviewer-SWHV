package org.helioviewer.jhv.gui.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.helioviewer.jhv.app.Settings;
import org.helioviewer.jhv.app.state.ViewState;
import org.helioviewer.jhv.gui.Actions;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.movie.ExportFormat;
import org.helioviewer.jhv.movie.ExportMovie;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.opengl.GLRenderer;
import org.helioviewer.jhv.time.JHVTime;

import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideToggleButton;

@SuppressWarnings("serial")
public class MoviePanel extends JPanel implements Player.StatusListener, ExportMovie.StatusListener, ViewState.PlaybackConfigListener, ViewState.RecordingConfigListener, Interfaces.LazyComponent {

    private static final int FRAME_HOLD_REPEAT_MS = 125;

    private final JTextField programTime = new JTextField() {
        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            // Keep one extra pixel beyond Swing's caret margin to prevent horizontal scrolling.
            size.width++;
            return size;
        }
    };
    private long displayedTime = Long.MIN_VALUE;

    private final JideButton playButton;

    private final JideToggleButton recordButton;

    private final JideToggleButton advancedButton;
    private final JHVSpinner speedSpinner;
    private final JComboBox<ViewState.PlaybackSpeedUnit> speedUnitComboBox;
    private final JComboBox<Player.AdvanceMode> advanceModeComboBox;
    private final JRadioButton loopButton;
    private final JRadioButton shotButton;
    private final JRadioButton freeButton;
    private final JComboBox<ViewState.RecordingSize> recordSizeComboBox;
    private final JComboBox<ExportFormat> formatComboBox;

    private final JPanel optionsPanel = new JPanel();

    private static MoviePanel instance;

    public static MoviePanel getInstance() {
        return instance == null ? instance = new MoviePanel() : instance;
    }

    private MoviePanel() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        programTime.setFont(UIGlobals.uiFontMonoSmall);
        programTime.setEditable(false);
        programTime.setOpaque(false);
        programTime.setBorder(BorderFactory.createEmptyBorder());
        programTime.setHorizontalAlignment(JTextField.RIGHT);
        programTime.setToolTipText("Program time");
        lazyRepaint();
        UITimer.register(this);

        // Time slider
        TimeSlider timeSlider = new TimeSlider(0, 0, 0);

        JPanel secondLine = new JPanel(new BorderLayout());
        // Control buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        int buttonSize = 18;

        JideButton prevFrameButton = new JideButton(Buttons.backward);
        prevFrameButton.setFont(Buttons.getMaterialFont(buttonSize));
        prevFrameButton.setToolTipText("Step to previous frame");
        prevFrameButton.addActionListener(Actions.PREVIOUS_FRAME);
        HoldRepeat.install(prevFrameButton, FRAME_HOLD_REPEAT_MS);
        buttonPanel.add(prevFrameButton);

        playButton = new JideButton(Buttons.play);
        playButton.setFont(Buttons.getMaterialFont(buttonSize));
        playButton.setToolTipText("Play movie");
        playButton.addActionListener(Actions.PLAY_PAUSE);
        buttonPanel.add(playButton);

        JideButton nextFrameButton = new JideButton(Buttons.forward);
        nextFrameButton.setFont(Buttons.getMaterialFont(buttonSize));
        nextFrameButton.setToolTipText("Step to next frame");
        nextFrameButton.addActionListener(Actions.NEXT_FRAME);
        HoldRepeat.install(nextFrameButton, FRAME_HOLD_REPEAT_MS);
        buttonPanel.add(nextFrameButton);

        recordButton = new JideToggleButton(Buttons.record);
        recordButton.setFont(Buttons.getMaterialFont(buttonSize));
        recordButton.setToolTipText("Record movie");
        recordButton.addActionListener(Actions.RECORD);
        buttonPanel.add(recordButton);

        advancedButton = new JideToggleButton(Buttons.settings);
        advancedButton.setFont(Buttons.getMaterialFont(buttonSize));
        advancedButton.setToolTipText("Playback and recording options");
        advancedButton.getAccessibleContext().setAccessibleName("Playback and recording options");
        advancedButton.addActionListener(e -> setAdvanced(advancedButton.isSelected()));
        buttonPanel.add(advancedButton);

        secondLine.add(buttonPanel, BorderLayout.LINE_START);

        // Current frame number
        JComponent frameNumberPanel = timeSlider.getFrameNumberPanel();
        JPanel positionPanel = new JPanel(new BorderLayout(4, 0));
        positionPanel.add(frameNumberPanel, BorderLayout.CENTER);
        positionPanel.add(programTime, BorderLayout.LINE_END);
        secondLine.add(positionPanel, BorderLayout.CENTER);

        // Speed
        JPanel playbackSpeed = new JPanel(new FlowLayout(FlowLayout.TRAILING, 0, 0));
        playbackSpeed.add(new JLabel("Play ", JLabel.RIGHT));

        speedSpinner = new JHVSpinner(ViewState.playbackData().speed(), ViewState.PLAYBACK_SPEED_MIN, ViewState.PLAYBACK_SPEED_MAX, 1);
        speedSpinner.setToolTipText("Maximum " + ViewState.PLAYBACK_SPEED_MAX + " fps");
        speedSpinner.addChangeListener(e -> updatePlaybackConfig());
        playbackSpeed.add(speedSpinner);

        speedUnitComboBox = new JComboBox<>(ViewState.PlaybackSpeedUnit.values());
        speedUnitComboBox.addActionListener(e -> updatePlaybackConfig());
        advanceModeComboBox = new JComboBox<>(new Player.AdvanceMode[]{Player.AdvanceMode.Loop, Player.AdvanceMode.Stop, Player.AdvanceMode.Swing});
        advanceModeComboBox.addActionListener(e -> ViewState.setPlaybackAdvanceMode((Player.AdvanceMode) advanceModeComboBox.getSelectedItem()));

        optionsPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 0));

        // Record
        loopButton = new JRadioButton(ViewState.RecordingMode.LOOP.toString());
        shotButton = new JRadioButton(ViewState.RecordingMode.SHOT.toString());
        freeButton = new JRadioButton(ViewState.RecordingMode.FREE.toString());

        JPanel recordingModes = new JPanel(new FlowLayout(FlowLayout.TRAILING, 0, 0));
        recordingModes.add(new JLabel("Record ", JLabel.RIGHT));
        recordingModes.add(loopButton);
        recordingModes.add(shotButton);
        recordingModes.add(freeButton);

        ButtonGroup group = new ButtonGroup();
        group.add(loopButton);
        group.add(shotButton);
        group.add(freeButton);

        loopButton.addActionListener(e -> ViewState.setRecordingMode(ViewState.RecordingMode.LOOP));
        shotButton.addActionListener(e -> ViewState.setRecordingMode(ViewState.RecordingMode.SHOT));
        freeButton.addActionListener(e -> ViewState.setRecordingMode(ViewState.RecordingMode.FREE));

        recordSizeComboBox = new JComboBox<>(ViewState.RecordingSize.values());
        recordSizeComboBox.addActionListener(e -> ViewState.setRecordingSize((ViewState.RecordingSize) recordSizeComboBox.getSelectedItem()));

        formatComboBox = new JComboBox<>(ExportFormat.values());
        formatComboBox.setSelectedItem(ExportFormat.fromSettings());
        formatComboBox.addActionListener(e -> Settings.setProperty("video.format", ((ExportFormat) Objects.requireNonNull(formatComboBox.getSelectedItem())).name()));

        playbackSpeed.add(speedUnitComboBox);
        playbackSpeed.add(new JLabel(" and "));
        playbackSpeed.add(advanceModeComboBox);

        JPanel output = new JPanel(new FlowLayout(FlowLayout.TRAILING, 0, 0));
        output.add(new JLabel("Output size "));
        output.add(recordSizeComboBox);
        output.add(new JLabel("Format "));
        output.add(formatComboBox);

        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.PAGE_AXIS));
        optionsPanel.add(playbackSpeed);
        optionsPanel.add(recordingModes);
        optionsPanel.add(output);

        add(timeSlider);
        add(secondLine);
        add(optionsPanel);

        Player.addStatusListener(this);
        ExportMovie.addStatusListener(this);
        ViewState.addPlaybackConfigListener(this);
        ViewState.addRecordingConfigListener(this);
    }

    public void setAdvanced(boolean advanced) {
        advancedButton.setSelected(advanced);
        optionsPanel.setVisible(advanced);
        revalidate();
        repaint();
    }

    @Override
    public void lazyRepaint() {
        JHVTime time = GLRenderer.getDisplayedViewpoint().time;
        if (displayedTime != time.milli) {
            displayedTime = time.milli;
            int selectionStart = programTime.getSelectionStart();
            int selectionEnd = programTime.getSelectionEnd();
            programTime.setText(time.toString());
            if (selectionStart != selectionEnd)
                programTime.select(selectionStart, selectionEnd);
        }
    }

    private void updatePlaybackConfig() {
        int speed = ((Number) speedSpinner.getValue()).intValue();
        ViewState.PlaybackSpeedUnit unit = (ViewState.PlaybackSpeedUnit) speedUnitComboBox.getSelectedItem();
        if (unit == null)
            return;
        ViewState.setPlaybackSpeed(speed, unit);
    }

    @Override
    public void movieStatusChanged() {
        boolean playing = Player.isPlaying();

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
        recordButton.setForeground(recording ? Color.RED : new Color(0x800000));
        ComponentUtils.setEnabled(optionsPanel, !recording);
        updateFormatEnabled();
    }

    private void updateFormatEnabled() {
        boolean screenshot = ViewState.recordingData().mode() == ViewState.RecordingMode.SHOT;
        formatComboBox.setEnabled(!screenshot && !ExportMovie.isRecording());
        formatComboBox.setToolTipText(screenshot ? "Screenshots are saved as PNG" : null);
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
        updateFormatEnabled();
    }

}
