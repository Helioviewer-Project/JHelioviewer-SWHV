package org.helioviewer.jhv.gui.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.export.ExportMovie;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.ComponentUtils.SmallPanel;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.base.BusyIndicator;
import org.helioviewer.jhv.gui.components.base.TerminatedFormatterFactory;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.input.KeyShortcuts;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.GLHelper;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.View.AnimationMode;

@SuppressWarnings("serial")
public class MoviePanel extends JPanel implements ChangeListener, MouseListener, MouseWheelListener {

    // different animation speeds
    private enum SpeedUnit {
        FRAMESPERSECOND {
            @Override
            public String toString() {
                return "Frames/sec";
            }

            @Override
            protected int getSecondsPerSecond() {
                return 0;
            }
        },
        MINUTESPERSECOND {
            @Override
            public String toString() {
                return "Solar minutes/sec";
            }

            @Override
            protected int getSecondsPerSecond() {
                return 60;
            }
        },
        HOURSPERSECOND {
            @Override
            public String toString() {
                return "Solar hours/sec";
            }

            @Override
            protected int getSecondsPerSecond() {
                return 3600;
            }
        },
        DAYSPERSECOND {
            @Override
            public String toString() {
                return "Solar days/sec";
            }

            @Override
            protected int getSecondsPerSecond() {
                return 86400;
            }
        };

        protected abstract int getSecondsPerSecond();
    }

    public enum RecordMode {
        LOOP, SHOT, FREE
    }

    private enum RecordSize {
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
        H720 {
            @Override
            public String toString() {
                return "1280×720";
            }

            @Override
            protected Dimension getSize() {
                return new Dimension(1280, 720);
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

    // Status
    private static boolean isAdvanced = false;
    private static boolean wasPlaying = false;

    // Gui elements
    private static TimeSlider timeSlider;
    private static JLabel frameNumberLabel;
    private static JButton previousFrameButton;
    private static JButton nextFrameButton;
    private static JButton playButton;

    private static RecordButton recordButton;

    private static JButton advancedButton;
    private static JSpinner speedSpinner;
    private static JComboBox<SpeedUnit> speedUnitComboBox;
    private static JComboBox<AnimationMode> animationModeComboBox;

    private static SmallPanel speedPanel;
    private static SmallPanel modePanel;
    private static SmallPanel recordPanel;

    // Icons
    private static final Icon playIcon = IconBank.getIcon(JHVIcon.PLAY);
    private static final Icon pauseIcon = IconBank.getIcon(JHVIcon.PAUSE);
    private static final Icon recordIcon = IconBank.getIcon(JHVIcon.RECORD);
    private static final Icon openIcon = IconBank.getIcon(JHVIcon.SHOW_MORE);
    private static final Icon closeIcon = IconBank.getIcon(JHVIcon.SHOW_LESS);

    private static boolean someoneIsDragging = false;

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
        setEnabledState(false);

        clickRecordButton();
        recordButton.setEnabled(false);
    }

    public static void setMovie(View view) {
        timeSlider.setMaximum(view.getMaximumFrameNumber());
        timeSlider.setValue(view.getCurrentFrameNumber());
        setEnabledState(true);

        recordButton.setEnabled(true);
    }

    private MoviePanel() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        // Time line
        timeSlider = new TimeSlider(TimeSlider.HORIZONTAL, 0, 0, 0);
        timeSlider.setSnapToTicks(true);
        timeSlider.addChangeListener(this);
        timeSlider.addMouseListener(this);
        addMouseWheelListener(this);

        timeSlider.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false), "RIGHT_ARROW");
        timeSlider.getActionMap().put("RIGHT_ARROW", getNextFrameAction());
        timeSlider.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false), "LEFT_ARROW");
        timeSlider.getActionMap().put("LEFT_ARROW", getPreviousFrameAction());

        JPanel secondLine = new JPanel(new BorderLayout());

        // Control buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 1));

        previousFrameButton = new JButton(IconBank.getIcon(JHVIcon.BACK));
        previousFrameButton.setToolTipText("Step to previous frame");
        previousFrameButton.addActionListener(getPreviousFrameAction());
        buttonPanel.add(previousFrameButton);

        playButton = new JButton(playIcon);
        playButton.setToolTipText("Play movie");
        playButton.addActionListener(getPlayPauseAction());
        buttonPanel.add(playButton);

        nextFrameButton = new JButton(IconBank.getIcon(JHVIcon.FORWARD));
        nextFrameButton.setToolTipText("Step to next frame");
        nextFrameButton.addActionListener(getNextFrameAction());
        buttonPanel.add(nextFrameButton);

        recordButton = new RecordButton();
        buttonPanel.add(recordButton);

        advancedButton = new JButton("Options", IconBank.getIcon(JHVIcon.SHOW_MORE));
        advancedButton.setToolTipText("Options to control playback and recording");
        advancedButton.addActionListener(e -> setAdvanced(!isAdvanced));
        advancedButton.setHorizontalTextPosition(SwingConstants.LEADING);
        advancedButton.setBorderPainted(false);
        advancedButton.setFocusPainted(false);
        advancedButton.setContentAreaFilled(false);
        buttonPanel.add(advancedButton);

        int recordButtonHeight = recordButton.getMinimumSize().height;
        previousFrameButton.setPreferredSize(new Dimension(previousFrameButton.getMinimumSize().width, recordButtonHeight));
        playButton.setPreferredSize(new Dimension(playButton.getMinimumSize().width, recordButtonHeight));
        nextFrameButton.setPreferredSize(new Dimension(nextFrameButton.getMinimumSize().width, recordButtonHeight));
        advancedButton.setPreferredSize(new Dimension(advancedButton.getMinimumSize().width, recordButtonHeight));

        secondLine.add(buttonPanel, BorderLayout.WEST);

        // Current frame number
        frameNumberLabel = new JLabel((timeSlider.getValue() + 1) + "/" + (timeSlider.getMaximum() + 1), JLabel.RIGHT);
        frameNumberLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        secondLine.add(frameNumberLabel, BorderLayout.EAST);

        // The speed panel has some distinction from above as it is one of the advanced options
        // It is not included in the main Panel to save space if it is not shown

        // Speed
        speedPanel = new SmallPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        speedPanel.add(new JLabel("Speed", JLabel.RIGHT));

        int speedMin = 1, speedMax = 60;
        speedSpinner = new JSpinner(new SpinnerNumberModel(Double.valueOf(20), Double.valueOf(1), Double.valueOf(speedMax), Double.valueOf(speedMin)));
        speedSpinner.setToolTipText("Maximum " + speedMax + " fps");
        speedSpinner.addChangeListener(e -> updateMovieSpeed());

        JFormattedTextField fx = ((JSpinner.DefaultEditor) speedSpinner.getEditor()).getTextField();
        fx.setFormatterFactory(new TerminatedFormatterFactory("%.0f", "", speedMin, speedMax));

        speedSpinner.setMaximumSize(speedSpinner.getPreferredSize());
        WheelSupport.installMouseWheelSupport(speedSpinner);
        speedPanel.add(speedSpinner);

        speedUnitComboBox = new JComboBox<>(new SpeedUnit[]{SpeedUnit.FRAMESPERSECOND /*, SpeedUnit.MINUTESPERSECOND, SpeedUnit.HOURSPERSECOND, SpeedUnit.DAYSPERSECOND */});
        speedUnitComboBox.setSelectedItem(SpeedUnit.FRAMESPERSECOND);
        speedUnitComboBox.addActionListener(e -> updateMovieSpeed());
        speedPanel.add(speedUnitComboBox);

        // Animation mode
        modePanel = new SmallPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        modePanel.add(new JLabel("Animation mode", JLabel.RIGHT));

        animationModeComboBox = new JComboBox<>(new AnimationMode[]{AnimationMode.LOOP, AnimationMode.STOP, AnimationMode.SWING});
        animationModeComboBox.setPreferredSize(speedUnitComboBox.getPreferredSize());
        animationModeComboBox.addActionListener(e -> Layers.setAnimationMode((AnimationMode) animationModeComboBox.getSelectedItem()));
        modePanel.add(animationModeComboBox);

        recordPanel = new SmallPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
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

        JComboBox<RecordSize> recordSizeCombo = new JComboBox<>(new RecordSize[]{RecordSize.ORIGINAL, RecordSize.H720, RecordSize.H1080});
        recordSizeCombo.setSelectedItem(RecordSize.ORIGINAL);
        recordSizeCombo.addActionListener(e -> recordButton.setRecordSize((RecordSize) (recordSizeCombo.getSelectedItem())));
        c.gridx = 3;
        recordPanel.add(recordSizeCombo, c);

        add(timeSlider);
        add(secondLine);
        add(speedPanel);
        add(modePanel);
        add(recordPanel);

        speedPanel.setSmall();
        modePanel.setSmall();
        recordPanel.setSmall();

        setEnabledState(false);
        sliderTimer.start();
    }

    public static void clickRecordButton() {
        if (recordButton.isSelected())
            recordButton.doClick();
    }

    public static void recordPanelSetEnabled(boolean enabled) {
        ComponentUtils.setEnabled(recordPanel, enabled);
    }

    private static class RecordButton extends JToggleButton implements ActionListener {

        private RecordMode mode = RecordMode.LOOP;
        private RecordSize size = RecordSize.ORIGINAL;

        public RecordButton() {
            super("REC", recordIcon);
            setToolTipText("Record movie");
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (isSelected()) {
                setText("BUSY");
                int fps = 20;
                if (speedUnitComboBox.getSelectedItem() == SpeedUnit.FRAMESPERSECOND)
                    fps = ((SpinnerNumberModel) speedSpinner.getModel()).getNumber().intValue();
                ExportMovie.start(size.getSize().width, size.getSize().height, size.isInternal(), fps, mode);
            } else {
                setText("REC");
                ExportMovie.stop();
            }
        }

        public void setRecordMode(RecordMode _mode) {
            mode = _mode;
        }

        public void setRecordSize(RecordSize _size) {
            size = _size;
        }

    }

    private static void setEnabledState(boolean enabled) {
        animationModeComboBox.setEnabled(enabled);
        timeSlider.setEnabled(enabled);
        playButton.setEnabled(enabled);
        nextFrameButton.setEnabled(enabled);
        previousFrameButton.setEnabled(enabled);
        recordButton.setEnabled(enabled);
        speedSpinner.setEnabled(enabled);
        speedUnitComboBox.setEnabled(enabled);
        advancedButton.setEnabled(enabled);
    }

    public static void setAdvanced(boolean advanced) {
        isAdvanced = advanced;
        advancedButton.setIcon(advanced ? closeIcon : openIcon);
        modePanel.setVisible(advanced);
        speedPanel.setVisible(advanced);
        recordPanel.setVisible(advanced);
    }

    /**
     * Updates the speed of the animation. This function is called when changing
     * the speed of the animation or its unit.
     */
    private static void updateMovieSpeed() {
        int speed = ((SpinnerNumberModel) speedSpinner.getModel()).getNumber().intValue();
        if (speedUnitComboBox.getSelectedItem() == SpeedUnit.FRAMESPERSECOND) {
            Layers.setDesiredRelativeSpeed(speed);
        } else {
            Layers.setDesiredAbsoluteSpeed(speed * ((SpeedUnit) speedUnitComboBox.getSelectedItem()).getSecondsPerSecond());
        }
    }

    public static void setFrameSlider(int frame) {
        // update just UI, tbd
        timeSlider.removeChangeListener(instance);
        timeSlider.setValue(frame);
        frameNumberLabel.setText((frame + 1) + "/" + (timeSlider.getMaximum() + 1));
        timeSlider.addChangeListener(instance);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        // Jump to different frame
        int val = timeSlider.getValue();
        Layers.setFrame(val);
        frameNumberLabel.setText((val + 1) + "/" + (timeSlider.getMaximum() + 1));
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (isEnabled()) {
            if (e.getWheelRotation() < 0) {
                Layers.nextFrame();
            } else if (e.getWheelRotation() > 0) {
                Layers.previousFrame();
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        someoneIsDragging = true;
        wasPlaying = Layers.isMoviePlaying();
        if (wasPlaying) {
            Layers.pauseMovie();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (wasPlaying) {
            Layers.playMovie();
        }
        someoneIsDragging = false;
    }

    // only for Layers
    public static void setPlayState(boolean play) {
        if (!someoneIsDragging) {
            if (play) {
                playButton.setIcon(pauseIcon);
                playButton.setToolTipText("Pause movie");
            } else {
                playButton.setIcon(playIcon);
                playButton.setToolTipText("Play movie");
            }
        }
    }

    private static final Timer sliderTimer = new Timer(1000 / 10, new SliderListener());

    public static final BusyIndicator busyIndicator = new BusyIndicator();

    private static volatile boolean cacheChanged = false;

    // accessed from J2KReader threads
    public static void cacheStatusChanged() {
        cacheChanged = true;
    }

    private static class SliderListener implements ActionListener {

        private int frameRate = -1;

        @Override
        public void actionPerformed(ActionEvent e) {
            busyIndicator.actionPerformed(e);

            if (cacheChanged) {
                cacheChanged = false;
                timeSlider.repaint();
            }
            timeSlider.lazyRepaint();
            ImageViewerGui.getRenderableContainerPanel().lazyRepaint();

            int f = 0;
            if (Layers.isMoviePlaying()) {
                f = (int) (Layers.getActiveView().getCurrentFramerate() + 0.5f);
            }

            if (f != frameRate) {
                frameRate = f;
                ImageViewerGui.getFramerateStatusPanel().update(f);
            }
        }

    }

    private static AbstractAction playPauseAction = null;
    private static AbstractAction previousFrameAction = null;
    private static AbstractAction nextFrameAction = null;

    public static AbstractAction getPlayPauseAction() {
        if (playPauseAction == null)
            playPauseAction = new PlayPauseAction();
        return playPauseAction;
    }

    public static AbstractAction getPreviousFrameAction() {
        if (previousFrameAction == null)
            previousFrameAction = new PreviousFrameAction();
        return previousFrameAction;
    }

    public static AbstractAction getNextFrameAction() {
        if (nextFrameAction == null)
            nextFrameAction = new NextFrameAction();
        return nextFrameAction;
    }

    private static class PlayPauseAction extends AbstractAction {

        public PlayPauseAction() {
            super("Play/Pause Movie");
            KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
            putValue(ACCELERATOR_KEY, key);
            KeyShortcuts.registerKey(key, this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Layers.toggleMovie();
            putValue(NAME, playButton.getToolTipText());
        }

    }

    private static class PreviousFrameAction extends AbstractAction {

        public PreviousFrameAction() {
            super("Step to Previous Frame");
            KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | ActionEvent.ALT_MASK);
            putValue(ACCELERATOR_KEY, key);
            KeyShortcuts.registerKey(key, this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (Layers.isMoviePlaying())
                Layers.pauseMovie();
            Layers.previousFrame();
        }

    }

    private static class NextFrameAction extends AbstractAction {

        public NextFrameAction() {
            super("Step to Next Frame");
            KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | ActionEvent.ALT_MASK);
            putValue(ACCELERATOR_KEY, key);
            KeyShortcuts.registerKey(key, this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (Layers.isMoviePlaying())
                Layers.pauseMovie();
            Layers.nextFrame();
        }

    }

}
