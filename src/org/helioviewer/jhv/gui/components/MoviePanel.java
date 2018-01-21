package org.helioviewer.jhv.gui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
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
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.astronomy.Carrington;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.export.ExportMovie;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.actions.NewLayerAction;
import org.helioviewer.jhv.gui.components.base.TerminatedFormatterFactory;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.gui.dialogs.ObservationDialog;
import org.helioviewer.jhv.gui.interfaces.ObservationSelector;
import org.helioviewer.jhv.input.KeyShortcuts;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.opengl.GLHelper;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.view.View;
import org.helioviewer.jhv.view.View.AnimationMode;

import com.jidesoft.swing.ButtonStyle;
import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideSplitButton;
import com.jidesoft.swing.JideToggleButton;

@SuppressWarnings("serial")
public class MoviePanel extends JPanel implements ChangeListener, ObservationSelector {

    private enum SkipUnit {
        Day(TimeUtils.DAY_IN_MILLIS), Week(7 * TimeUtils.DAY_IN_MILLIS), Rotation(Carrington.CR_SYNODIC_MEAN * TimeUtils.DAY_IN_MILLIS);

        final long skipMillis;

        SkipUnit(double m) {
            skipMillis = (long) m;
        }
    }

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
        H2160 {
            @Override
            public String toString() {
                return "4096×4096";
            }

            @Override
            protected Dimension getSize() {
                return new Dimension(4096, 4096);
            }

            @Override
            protected boolean isInternal() {
                return true;
            }
        },
        H1080 {
            @Override
            public String toString() {
                return "1920×1080";
            }

            @Override
            protected Dimension getSize() {
                return new Dimension(1920, 1080);
            }

            @Override
            protected boolean isInternal() {
                return true;
            }
        },
        ORIGINAL {
            @Override
            public String toString() {
                return "On screen";
            }

            @Override
            protected Dimension getSize() {
                return GLHelper.GL2AWTDimension(Displayer.fullViewport.width, Displayer.fullViewport.height);
            }

            @Override
            protected boolean isInternal() {
                return false;
            }
        };
        protected abstract boolean isInternal();

        protected abstract Dimension getSize();
    }

    private static boolean isAdvanced;

    private static final TimeSelectorPanel timeSelectorPanel = new TimeSelectorPanel();
    private static final CadencePanel cadencePanel = new CadencePanel();
    private final ImageSelectorPanel imageSelectorPanel;

    private static TimeSlider timeSlider;
    private static JideButton prevFrameButton;
    private static JideButton nextFrameButton;
    private static JideButton playButton;
    private static JideSplitButton skipBackwardButton;
    private static JideSplitButton skipForwardButton;

    private static RecordButton recordButton;

    private static JideButton advancedButton;
    private static JSpinner speedSpinner;
    private static JComboBox<SpeedUnit> speedUnitComboBox;
    private static JComboBox<AnimationMode> animationModeComboBox;

    private static final JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    private static final JPanel recordPanel = new JPanel(new GridBagLayout());

    private static MoviePanel instance;

    public static MoviePanel getInstance() {
        if (instance == null) {
            instance = new MoviePanel();
        }
        return instance;
    }

    public static void unsetMovie() {
        timeSlider.setMaximum(0);
        timeSlider.setValue(0);
        timeSlider.repaint();
        setEnabledState(false);

        clickRecordButton();
        recordButton.setEnabled(false);
    }

    public static void setMovie(View view) {
        timeSlider.setMaximum(view.getMaximumFrameNumber());
        timeSlider.setValue(view.getCurrentFrameNumber());
        timeSlider.repaint();
        setEnabledState(true);

        recordButton.setEnabled(true);
    }

    private static ButtonGroup createSkipMenu(JideSplitButton menu) {
        ButtonGroup group = new ButtonGroup();
        for (SkipUnit unit : SkipUnit.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(unit.toString());
            item.setActionCommand(unit.toString());
            if (unit == SkipUnit.Day)
                item.setSelected(true);
            group.add(item);
            menu.add(item);
        }
        return group;
    }

    private MoviePanel() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        add(timeSelectorPanel);

        ObservationDialog.getInstance(); // make sure it's instanced
        imageSelectorPanel = new ImageSelectorPanel(this);

        JideSplitButton addLayerButton = new JideSplitButton(Buttons.newLayer);
        addLayerButton.setButtonStyle(ButtonStyle.FLAT_STYLE);
        addLayerButton.setFocusable(false);
        addLayerButton.add(imageSelectorPanel);
        addLayerButton.addActionListener(e -> new NewLayerAction().actionPerformed(new ActionEvent(addLayerButton, 0, "")));

        JPanel addLayerButtonWrapper = new JPanel(new BorderLayout());
        addLayerButtonWrapper.add(addLayerButton, BorderLayout.WEST);

        add(addLayerButtonWrapper);

        JPanel sliderPanel = new JPanel(new GridBagLayout());
        // Time line
        timeSlider = new TimeSlider(TimeSlider.HORIZONTAL, 0, 0, 0);
        timeSlider.addChangeListener(this);

        timeSlider.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false), "RIGHT_ARROW");
        timeSlider.getActionMap().put("RIGHT_ARROW", getNextFrameAction());
        timeSlider.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false), "LEFT_ARROW");
        timeSlider.getActionMap().put("LEFT_ARROW", getPreviousFrameAction());

        JideButton setButton = new JideButton("Set");
        setButton.addActionListener(e -> {
            long start = timeSelectorPanel.getStartTime();
            long end = timeSelectorPanel.getEndTime();
            if (start > end)
                timeSelectorPanel.setStartTime(end);
            else
                ImageLayers.syncLayersSpan(start, end, cadencePanel.getCadence());
        });

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0;

        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 1;
        sliderPanel.add(timeSlider, c);
        c.gridx = 1;
        c.weightx = 0;
        sliderPanel.add(setButton, c);

        JPanel secondLine = new JPanel(new BorderLayout());
        // Control buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
        int small = 18, big = 26;

        skipBackwardButton = new JideSplitButton(Buttons.skipBackward);
        skipBackwardButton.setFont(Buttons.getMaterialFont(small));
        skipBackwardButton.setToolTipText("Move time interval backward");
        ButtonGroup skipBackwardGroup = createSkipMenu(skipBackwardButton);
        skipBackwardButton.addActionListener(e -> {
            long skip = -SkipUnit.valueOf(skipBackwardGroup.getSelection().getActionCommand()).skipMillis;
            timeSelectorPanel.setStartTime(timeSelectorPanel.getStartTime() + skip);
            timeSelectorPanel.setEndTime(timeSelectorPanel.getEndTime() + skip);
            setButton.doClick();
        });
        buttonPanel.add(skipBackwardButton);

        prevFrameButton = new JideButton(Buttons.backward);
        prevFrameButton.setFont(Buttons.getMaterialFont(small));
        prevFrameButton.setToolTipText("Step to previous frame");
        prevFrameButton.addActionListener(getPreviousFrameAction());
        buttonPanel.add(prevFrameButton);

        playButton = new JideButton(Buttons.play);
        playButton.setFont(Buttons.getMaterialFont(big));
        playButton.setToolTipText("Play movie");
        playButton.addActionListener(getPlayPauseAction());
        buttonPanel.add(playButton);

        nextFrameButton = new JideButton(Buttons.forward);
        nextFrameButton.setFont(Buttons.getMaterialFont(small));
        nextFrameButton.setToolTipText("Step to next frame");
        nextFrameButton.addActionListener(getNextFrameAction());
        buttonPanel.add(nextFrameButton);

        skipForwardButton = new JideSplitButton(Buttons.skipForward);
        skipForwardButton.setFont(Buttons.getMaterialFont(small));
        skipForwardButton.setToolTipText("Move time interval forward");
        ButtonGroup skipForwardGroup = createSkipMenu(skipForwardButton);
        skipForwardButton.addActionListener(e -> {
            long skip = SkipUnit.valueOf(skipForwardGroup.getSelection().getActionCommand()).skipMillis;
            timeSelectorPanel.setStartTime(timeSelectorPanel.getStartTime() + skip);
            timeSelectorPanel.setEndTime(timeSelectorPanel.getEndTime() + skip);
            setButton.doClick();
        });
        buttonPanel.add(skipForwardButton);

        recordButton = new RecordButton(small);
        buttonPanel.add(recordButton);

        advancedButton = new JideButton(Buttons.optionsDown);
        advancedButton.setToolTipText("Options to control playback and recording");
        advancedButton.addActionListener(e -> setAdvanced(!isAdvanced));
        buttonPanel.add(advancedButton);

        secondLine.add(buttonPanel, BorderLayout.WEST);

        // Current frame number
        JLabel frameNumberLabel = new JLabel((timeSlider.getValue() + 1) + "/" + (timeSlider.getMaximum() + 1), JLabel.RIGHT);
        frameNumberLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        timeSlider.setLabel(frameNumberLabel);
        secondLine.add(frameNumberLabel, BorderLayout.EAST);

        // Speed
        modePanel.add(new JLabel("Play", JLabel.RIGHT));

        int speedMin = 1, speedMax = 60;
        speedSpinner = new JSpinner(new SpinnerNumberModel(Double.valueOf(20), Double.valueOf(1), Double.valueOf(speedMax), Double.valueOf(speedMin)));
        speedSpinner.setToolTipText("Maximum " + speedMax + " fps");
        speedSpinner.addChangeListener(e -> updateMovieSpeed());

        JFormattedTextField fx = ((JSpinner.DefaultEditor) speedSpinner.getEditor()).getTextField();
        fx.setFormatterFactory(new TerminatedFormatterFactory("%.0f", "", speedMin, speedMax));

        speedSpinner.setMaximumSize(speedSpinner.getPreferredSize());
        WheelSupport.installMouseWheelSupport(speedSpinner);
        modePanel.add(speedSpinner);

        speedUnitComboBox = new JComboBox<>(new SpeedUnit[]{SpeedUnit.FRAMESPERSECOND /*, SpeedUnit.MINUTESPERSECOND, SpeedUnit.HOURSPERSECOND, SpeedUnit.DAYSPERSECOND */});
        speedUnitComboBox.setSelectedItem(SpeedUnit.FRAMESPERSECOND);
        speedUnitComboBox.addActionListener(e -> updateMovieSpeed());
        modePanel.add(speedUnitComboBox);

        // Animation mode
        modePanel.add(new JLabel("and", JLabel.RIGHT));

        animationModeComboBox = new JComboBox<>(new AnimationMode[]{AnimationMode.Loop, AnimationMode.Stop, AnimationMode.Swing});
        animationModeComboBox.setPreferredSize(speedUnitComboBox.getPreferredSize());
        animationModeComboBox.addActionListener(e -> Movie.setAnimationMode((AnimationMode) Objects.requireNonNull(animationModeComboBox.getSelectedItem())));
        modePanel.add(animationModeComboBox);

        // Record
        c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        JRadioButton loopButton = new JRadioButton("One loop");
        JRadioButton shotButton = new JRadioButton("Screenshot");
        JRadioButton freeButton = new JRadioButton("Unlimited");
        loopButton.setSelected(true);

        c.gridy = 0;
        c.gridx = 0;
        recordPanel.add(new JLabel("Record", JLabel.RIGHT), c);
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
        recordPanel.add(new JLabel("Size", JLabel.RIGHT), c);

        JComboBox<RecordSize> recordSizeCombo = new JComboBox<>(new RecordSize[]{RecordSize.ORIGINAL, RecordSize.H1080, RecordSize.H2160});
        recordSizeCombo.setSelectedItem(RecordSize.ORIGINAL);
        recordSizeCombo.addActionListener(e -> recordButton.setRecordSize((RecordSize) Objects.requireNonNull(recordSizeCombo.getSelectedItem())));
        c.gridx = 3;
        recordPanel.add(recordSizeCombo, c);

        add(sliderPanel);
        add(secondLine);
        add(cadencePanel);
        add(modePanel);
        add(recordPanel);
        ComponentUtils.smallVariant(this);
        setEnabledState(false);

        add(ImageViewerGui.getLayersPanel());
    }

    @Override
    public int getCadence() {
        return cadencePanel.getCadence();
    }

    @Override
    public void setStartTime(long time) {
        timeSelectorPanel.setStartTime(time);
    }

    @Override
    public void setEndTime(long time) {
        timeSelectorPanel.setEndTime(time);
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
        imageSelectorPanel.load(null, getStartTime(), getEndTime(), getCadence());
    }

    public static void clickRecordButton() {
        if (recordButton.isSelected())
            recordButton.doClick();
    }

    public static void recordPanelSetEnabled(boolean enabled) {
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
                int fps = 20;
                SpeedUnit unit = (SpeedUnit) Objects.requireNonNull(speedUnitComboBox.getSelectedItem());
                if (unit == SpeedUnit.FRAMESPERSECOND)
                    fps = ((SpinnerNumberModel) speedSpinner.getModel()).getNumber().intValue();
                ExportMovie.start(size.getSize().width, size.getSize().height, size.isInternal(), fps, mode);
            } else {
                ExportMovie.stop();
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
        animationModeComboBox.setEnabled(enabled);
        timeSlider.setEnabled(enabled);
        playButton.setEnabled(enabled);
        nextFrameButton.setEnabled(enabled);
        prevFrameButton.setEnabled(enabled);
        skipBackwardButton.setEnabled(enabled);
        skipForwardButton.setEnabled(enabled);
        recordButton.setEnabled(enabled);
        speedSpinner.setEnabled(enabled);
        speedUnitComboBox.setEnabled(enabled);
        advancedButton.setEnabled(enabled);
    }

    public static void setAdvanced(boolean advanced) {
        isAdvanced = advanced;
        advancedButton.setText(advanced ? Buttons.optionsDown : Buttons.optionsRight);
        cadencePanel.setVisible(advanced);
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
        // update just UI, tbd
        timeSlider.removeChangeListener(instance);
        timeSlider.setValue(frame);
        timeSlider.addChangeListener(instance);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (!timeSlider.getValueIsAdjusting())
            Movie.setFrame(timeSlider.getValue());
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

    private static AbstractAction playPauseAction = null;
    private static AbstractAction prevFrameAction = null;
    private static AbstractAction nextFrameAction = null;

    public static AbstractAction getPlayPauseAction() {
        if (playPauseAction == null)
            playPauseAction = new PlayPauseAction();
        return playPauseAction;
    }

    public static AbstractAction getPreviousFrameAction() {
        if (prevFrameAction == null)
            prevFrameAction = new PreviousFrameAction();
        return prevFrameAction;
    }

    public static AbstractAction getNextFrameAction() {
        if (nextFrameAction == null)
            nextFrameAction = new NextFrameAction();
        return nextFrameAction;
    }

    private static class PlayPauseAction extends AbstractAction {

        PlayPauseAction() {
            super("Play/Pause Movie");
            KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
            putValue(ACCELERATOR_KEY, key);
            KeyShortcuts.registerKey(key, this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Movie.toggle();
            putValue(NAME, playButton.getToolTipText());
        }

    }

    private static class PreviousFrameAction extends AbstractAction {

        PreviousFrameAction() {
            super("Step to Previous Frame");
            KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | InputEvent.ALT_DOWN_MASK);
            putValue(ACCELERATOR_KEY, key);
            KeyShortcuts.registerKey(key, this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (Movie.isPlaying())
                Movie.pause();
            Movie.previousFrame();
        }

    }

    private static class NextFrameAction extends AbstractAction {

        NextFrameAction() {
            super("Step to Next Frame");
            KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | InputEvent.ALT_DOWN_MASK);
            putValue(ACCELERATOR_KEY, key);
            KeyShortcuts.registerKey(key, this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (Movie.isPlaying())
                Movie.pause();
            Movie.nextFrame();
        }

    }

}
