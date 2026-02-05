package org.helioviewer.jhv.gui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;

import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.export.ExportMovie;
import org.helioviewer.jhv.gui.Actions;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.components.base.JHVSpinner;
import org.helioviewer.jhv.gui.components.timeselector.TimeSelectorPanel;
import org.helioviewer.jhv.gui.dialogs.ObservationDialog;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.layers.Movie.AdvanceMode;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.draw.DrawController;

import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideToggleButton;
import com.jidesoft.swing.JideSplitButton;

@SuppressWarnings("serial")
public class MoviePanel extends JPanel implements Interfaces.ObservationSelector {

    // different animation speeds
    private enum SpeedUnit {
        FRAMESPERSECOND("Frames/sec", 0), MINUTESPERSECOND("Solar minutes/sec", 60), HOURSPERSECOND("Solar hours/sec", 3600), DAYSPERSECOND("Solar days/sec", 86400);

        private final String str;
        final int secPerSecond;

        SpeedUnit(String _str, int s) {
            str = _str;
            secPerSecond = s;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    public enum RecordMode {
        LOOP, SHOT, FREE
    }

    private enum RecordSize {
        ORIGINAL {
            @Override
            public String toString() {
                return "On screen";
            }

            @Override
            Dimension getSize() {
                return new Dimension(Display.fullViewport.width, Display.fullViewport.height);
            }

            @Override
            boolean isInternal() {
                return false;
            }
        },
        H1024 {
            @Override
            public String toString() {
                return "1024×1024";
            }

            @Override
            Dimension getSize() {
                return new Dimension(1024, 1024);
            }

            @Override
            boolean isInternal() {
                return true;
            }
        },
        H1080 {
            @Override
            public String toString() {
                return "1920×1080";
            }

            @Override
            Dimension getSize() {
                return new Dimension(1920, 1080);
            }

            @Override
            boolean isInternal() {
                return true;
            }
        },
        H2048 {
            @Override
            public String toString() {
                return "2048×2048";
            }

            @Override
            Dimension getSize() {
                return new Dimension(2048, 2048);
            }

            @Override
            boolean isInternal() {
                return true;
            }
        },
        H2160 {
            @Override
            public String toString() {
                return "3840×2160";
            }

            @Override
            Dimension getSize() {
                return new Dimension(3840, 2160);
            }

            @Override
            boolean isInternal() {
                return true;
            }
        },
        H4096 {
            @Override
            public String toString() {
                return "4096×4096";
            }

            @Override
            Dimension getSize() {
                return new Dimension(4096, 4096);
            }

            @Override
            boolean isInternal() {
                return true;
            }
        };

        abstract boolean isInternal();

        abstract Dimension getSize();
    }

    private static boolean isAdvanced;

    private static final TimeSelectorPanel timeSelectorPanel = new TimeSelectorPanel();
    private final ImageSelectorPanel imageSelectorPanel;
    private final JideSplitButton addLayerButton;

    private static TimeSlider timeSlider;
    private static JideButton prevFrameButton;
    private static JideButton nextFrameButton;
    private static JideButton playButton;

    private static RecordButton recordButton;

    private static JideButton advancedButton;
    private static JHVSpinner speedSpinner;
    private static JComboBox<SpeedUnit> speedUnitComboBox;
    private static JComboBox<AdvanceMode> advanceModeComboBox;

    private static final JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 0, 0));
    private static final JPanel recordPanel = new JPanel(new GridBagLayout());

    private static MoviePanel instance;

    public static MoviePanel getInstance() {
        return instance == null ? instance = new MoviePanel() : instance;
    }

    public static void unsetMovie() {
        timeSlider.setMaximum(0);
        timeSlider.repaint();
        setEnabledState(false);

        clickRecordButton();
        recordButton.setEnabled(false);
    }

    public static void setMovie(int max) {
        timeSlider.setMaximum(max);
        timeSlider.repaint();
        setEnabledState(true);

        recordButton.setEnabled(true);
    }

    private MoviePanel() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        // Time slider
        timeSlider = new TimeSlider(TimeSlider.HORIZONTAL, 0, 0, 0);
        timeSlider.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false), "RIGHT_ARROW");
        timeSlider.getActionMap().put("RIGHT_ARROW", nextFrameAction);
        timeSlider.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false), "LEFT_ARROW");
        timeSlider.getActionMap().put("LEFT_ARROW", prevFrameAction);
        timeSlider.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false), "SPACE");
        timeSlider.getActionMap().put("SPACE", playPauseAction);

        JPanel sliderPanel = new JPanel(new BorderLayout());
        sliderPanel.add(timeSlider);

        JPanel secondLine = new JPanel(new BorderLayout());
        // Control buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 1, 0));
        int small = 18, big = 26;

        prevFrameButton = new JideButton(Buttons.backward);
        prevFrameButton.setFont(Buttons.getMaterialFont(small));
        prevFrameButton.setToolTipText("Step to previous frame");
        prevFrameButton.addActionListener(prevFrameAction);
        buttonPanel.add(prevFrameButton);

        playButton = new JideButton(Buttons.play);
        playButton.setFont(Buttons.getMaterialFont(big));
        playButton.setToolTipText("Play movie");
        playButton.addActionListener(playPauseAction);
        buttonPanel.add(playButton);

        nextFrameButton = new JideButton(Buttons.forward);
        nextFrameButton.setFont(Buttons.getMaterialFont(small));
        nextFrameButton.setToolTipText("Step to next frame");
        nextFrameButton.addActionListener(nextFrameAction);
        buttonPanel.add(nextFrameButton);

        recordButton = new RecordButton(small);
        buttonPanel.add(recordButton);

        advancedButton = new JideButton(Buttons.optionsDown);
        advancedButton.setToolTipText("Options to control playback and recording");
        advancedButton.addActionListener(e -> setAdvanced(!isAdvanced));
        buttonPanel.add(advancedButton);

        secondLine.add(buttonPanel, BorderLayout.LINE_START);

        // Current frame number
        JLabel frameNumberPanel = timeSlider.getFrameNumberPanel();
        frameNumberPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        secondLine.add(frameNumberPanel, BorderLayout.LINE_END);

        // Speed
        modePanel.add(new JLabel(" Play ", JLabel.RIGHT));

        int speedMax = 120;
        speedSpinner = new JHVSpinner(Movie.FPS_RELATIVE_DEFAULT, 1, speedMax, 1);
        speedSpinner.setToolTipText("Maximum " + speedMax + " fps");
        speedSpinner.addChangeListener(e -> updateMovieSpeed());
        modePanel.add(speedSpinner);

        speedUnitComboBox = new JComboBox<>(new SpeedUnit[]{SpeedUnit.FRAMESPERSECOND, SpeedUnit.MINUTESPERSECOND, SpeedUnit.HOURSPERSECOND, SpeedUnit.DAYSPERSECOND});
        speedUnitComboBox.setSelectedItem(SpeedUnit.FRAMESPERSECOND);
        speedUnitComboBox.addActionListener(e -> updateMovieSpeed());
        modePanel.add(speedUnitComboBox);

        // Animation mode
        modePanel.add(new JLabel(" and ", JLabel.RIGHT));

        advanceModeComboBox = new JComboBox<>(new AdvanceMode[]{AdvanceMode.Loop, AdvanceMode.Stop, AdvanceMode.Swing});
        advanceModeComboBox.addActionListener(e -> Movie.setAdvanceMode((AdvanceMode) Objects.requireNonNull(advanceModeComboBox.getSelectedItem())));
        modePanel.add(advanceModeComboBox);

        // Record
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        JRadioButton loopButton = new JRadioButton("One loop");
        JRadioButton shotButton = new JRadioButton("Screenshot");
        JRadioButton freeButton = new JRadioButton("Unlimited");
        loopButton.setSelected(true);

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

        loopButton.addActionListener(e -> recordButton.setRecordMode(RecordMode.LOOP));
        shotButton.addActionListener(e -> recordButton.setRecordMode(RecordMode.SHOT));
        freeButton.addActionListener(e -> recordButton.setRecordMode(RecordMode.FREE));

        c.gridy = 1;
        c.gridx = 2;
        recordPanel.add(new JLabel("Size ", JLabel.RIGHT), c);

        JComboBox<RecordSize> recordSizeCombo = new JComboBox<>(RecordSize.values());
        recordSizeCombo.setSelectedItem(RecordSize.ORIGINAL);
        recordSizeCombo.addActionListener(e -> recordButton.setRecordSize((RecordSize) Objects.requireNonNull(recordSizeCombo.getSelectedItem())));
        c.gridx = 3;
        recordPanel.add(recordSizeCombo, c);

        timeSelectorPanel.addListener(Layers.getInstance());

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

        JideButton syncButton = new JideButton(Buttons.syncLayers);
        syncButton.setToolTipText("Synchronize time intervals of all layers");
        syncButton.addActionListener(e -> syncLayersSpan());

        JPanel addLayerPanel = new JPanel(new BorderLayout());
        addLayerPanel.add(addLayerButton, BorderLayout.LINE_START);
        addLayerPanel.add(syncButton, BorderLayout.LINE_END);
        add(addLayerPanel);

        add(JHVFrame.getLayersPanel());

        setEnabledState(false);
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
            imageSelectorPanel.load(null, getStartTime(), getEndTime(), getCadence());
    }

    @Override
    public void setAvailabilityEnabled(boolean enabled) {
    }

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

    private static void clickRecordButton() {
        if (recordButton.isSelected()) {
            recordButton.setSelected(false);
            recordButton.actionPerformed(null);
        }
    }

    public static void unselectRecordButton() {
        if (recordButton.isSelected())
            recordButton.setSelected(false);
    }

    public static void setEnabledOptions(boolean enabled) {
        ComponentUtils.setEnabled(modePanel, enabled);
        ComponentUtils.setEnabled(recordPanel, enabled);
    }

    private static class RecordButton extends JideToggleButton implements ActionListener {

        private RecordMode mode = RecordMode.LOOP;
        private RecordSize size = RecordSize.ORIGINAL;

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
                SpeedUnit unit = (SpeedUnit) Objects.requireNonNull(speedUnitComboBox.getSelectedItem());
                int fps = unit == SpeedUnit.FRAMESPERSECOND ? ((SpinnerNumberModel) speedSpinner.getModel()).getNumber().intValue() : Movie.FPS_ABSOLUTE;
                ExportMovie.start(size.getSize().width, size.getSize().height, size.isInternal(), fps, mode);
            } else {
                ExportMovie.shallStop();
            }
        }

        void setRecordMode(RecordMode _mode) {
            mode = _mode;
        }

        void setRecordSize(RecordSize _size) {
            size = _size;
        }

    }

    private static void setEnabledState(boolean enabled) {
        advanceModeComboBox.setEnabled(enabled);
        timeSlider.setEnabled(enabled);
        playButton.setEnabled(enabled);
        nextFrameButton.setEnabled(enabled);
        prevFrameButton.setEnabled(enabled);
        recordButton.setEnabled(enabled);
        speedSpinner.setEnabled(enabled);
        speedUnitComboBox.setEnabled(enabled);
        advancedButton.setEnabled(enabled);
    }

    public static void setAdvanced(boolean advanced) {
        isAdvanced = advanced;
        advancedButton.setText(advanced ? Buttons.optionsDown : Buttons.optionsRight);
        modePanel.setVisible(advanced);
        recordPanel.setVisible(advanced);
    }

    private static void updateMovieSpeed() {
        int speed = ((SpinnerNumberModel) speedSpinner.getModel()).getNumber().intValue();
        SpeedUnit unit = (SpeedUnit) Objects.requireNonNull(speedUnitComboBox.getSelectedItem());
        if (unit == SpeedUnit.FRAMESPERSECOND) {
            Movie.setDesiredRelativeSpeed(speed);
        } else {
            Movie.setDesiredAbsoluteSpeed(speed * unit.secPerSecond);
        }
    }

    public static void setFrameSlider(int frame) {
        timeSlider.setAllowFrame(false);
        timeSlider.setValue(frame);
        timeSlider.setAllowFrame(true);
    }

    // only for Layers
    public static void setPlayState(boolean play) {
        if (play) {
            playButton.setText(Buttons.pause);
            playButton.setToolTipText("Pause movie");
        } else {
            playButton.setText(Buttons.play);
            playButton.setToolTipText("Play movie");
        }
    }

    public static TimeSlider getTimeSlider() {
        return timeSlider;
    }

    private static final AbstractAction playPauseAction = new PlayPauseAction();
    private static final AbstractAction prevFrameAction = new PreviousFrameAction();
    private static final AbstractAction nextFrameAction = new NextFrameAction();

    static AbstractAction getPlayPauseAction() {
        return playPauseAction;
    }

    static AbstractAction getPreviousFrameAction() {
        return prevFrameAction;
    }

    static AbstractAction getNextFrameAction() {
        return nextFrameAction;
    }

    private static class PlayPauseAction extends Actions.AbstractKeyAction {

        PlayPauseAction() {
            super("Play/Pause Movie", KeyStroke.getKeyStroke(KeyEvent.VK_P, UIGlobals.menuShortcutMask));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Movie.toggle();
            putValue(NAME, playButton.getToolTipText());
        }

    }

    private static class PreviousFrameAction extends Actions.AbstractKeyAction {

        PreviousFrameAction() {
            super("Step to Previous Frame", KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, UIGlobals.menuShortcutMask | InputEvent.ALT_DOWN_MASK));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (Movie.isPlaying())
                Movie.pause();
            Movie.previousFrame();
        }

    }

    private static class NextFrameAction extends Actions.AbstractKeyAction {

        NextFrameAction() {
            super("Step to Next Frame", KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, UIGlobals.menuShortcutMask | InputEvent.ALT_DOWN_MASK));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (Movie.isPlaying())
                Movie.pause();
            Movie.nextFrame();
        }

    }

}
