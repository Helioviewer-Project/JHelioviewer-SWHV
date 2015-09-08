package org.helioviewer.jhv.gui.components;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.text.ParseException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;

import org.helioviewer.jhv.gui.ButtonCreator;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.viewmodel.imagecache.ImageCacheStatus;
import org.helioviewer.viewmodel.imagecache.ImageCacheStatus.CacheStatus;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.View.AnimationMode;

/**
 * Panel containing the movie controls.
 *
 * <p>
 * This panel provides the capability to start and stop an movie, step to
 * certain frames and switch the movie speed as well as the movie mode.
 *
 * @author Markus Langenberg
 * @author Malte Nuhn
 *
 */
@SuppressWarnings("serial")
public class MoviePanel extends JPanel implements ActionListener, ChangeListener, MouseListener, MouseWheelListener {

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

    // Status
    private static boolean isAdvanced = false;
    private static boolean wasPlaying = false;

    // Gui elements
    private static TimeSlider timeSlider;
    private static JLabel frameNumberLabel;
    private static JButton previousFrameButton;
    private static JButton nextFrameButton;
    private static JButton playButton;
    private static JButton advancedButton;
    private static JSpinner speedSpinner;
    private static JComboBox speedUnitComboBox;
    private static JComboBox animationModeComboBox;

    private final JPanel modePanel;
    private final JPanel speedPanel;

    // Icons
    private static final Icon playIcon = IconBank.getIcon(JHVIcon.PLAY);
    private static final Icon pauseIcon = IconBank.getIcon(JHVIcon.PAUSE);
    private static final Icon openIcon = IconBank.getIcon(JHVIcon.SHOW_MORE);
    private static final Icon closeIcon = IconBank.getIcon(JHVIcon.SHOW_LESS);

    private static boolean someoneIsDragging = false;

    private static MoviePanel instance;

    public static MoviePanel getSingletonInstance() {
        if (instance == null) {
            instance = new MoviePanel();
        }
        return instance;
    }

    public static void unsetMovie() {
        timeSlider.setMaximum(0);
        timeSlider.setValue(0);
        setState(false);
    }

    public static void setMovie(View view) {
        timeSlider.setMaximum(view.getMaximumFrameNumber());
        timeSlider.setValue(view.getCurrentFrameNumber());
        setState(true);
    }

    private MoviePanel() {
        super(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        add(mainPanel, BorderLayout.NORTH);

        // Time line
        timeSlider = new TimeSlider(TimeSlider.HORIZONTAL, 0, 0, 0);
        timeSlider.setSnapToTicks(true);
        timeSlider.addChangeListener(this);
        timeSlider.addMouseListener(this);
        addMouseWheelListener(this);

        mainPanel.add(timeSlider);

        JPanel secondLine = new JPanel(new BorderLayout());

        // Control buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 1));

        previousFrameButton = ButtonCreator.createButton(IconBank.getIcon(JHVIcon.BACK), "Step to previous frame", this);
        buttonPanel.add(previousFrameButton);

        playButton = ButtonCreator.createButton(playIcon, "Play movie", this);
        buttonPanel.add(playButton);

        nextFrameButton = ButtonCreator.createButton(IconBank.getIcon(JHVIcon.FORWARD), "Step to next frame", this);
        buttonPanel.add(nextFrameButton);
        secondLine.add(buttonPanel, BorderLayout.WEST);

        buttonPanel.add(new JSeparator(SwingConstants.VERTICAL));

        advancedButton = ButtonCreator.createTextButton(IconBank.getIcon(JHVIcon.SHOW_MORE), "More options", "More options to control playback", this);
        buttonPanel.add(advancedButton);

        // Current frame number
        frameNumberLabel = new JLabel((timeSlider.getValue() + 1) + "/" + (timeSlider.getMaximum() + 1), JLabel.RIGHT);
        frameNumberLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        secondLine.add(frameNumberLabel, BorderLayout.EAST);

        mainPanel.add(secondLine);

        // The speed panel has some distinction from above as it is one of the advanced options
        // It is not included in the main Panel to save space if it is not shown

        // Speed
        speedPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        speedPanel.add(new JLabel("Speed", JLabel.RIGHT));

        speedSpinner = new JSpinner(new SpinnerNumberModel(20, 1, 99, 1));
        speedSpinner.addChangeListener(this);
        ((JSpinner.DefaultEditor) speedSpinner.getEditor()).getTextField().addActionListener(this);
        speedSpinner.setMaximumSize(speedSpinner.getPreferredSize());
        speedPanel.add(speedSpinner);

        SpeedUnit[] units = { SpeedUnit.MINUTESPERSECOND, SpeedUnit.HOURSPERSECOND, SpeedUnit.DAYSPERSECOND, SpeedUnit.FRAMESPERSECOND };
        speedUnitComboBox = new JComboBox(units);
        speedUnitComboBox.setSelectedItem(SpeedUnit.FRAMESPERSECOND);
        speedUnitComboBox.addActionListener(this);
        speedPanel.add(speedUnitComboBox);

        mainPanel.add(speedPanel);

        // Animation mode
        modePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        modePanel.add(new JLabel("Animation mode", JLabel.RIGHT));

        AnimationMode[] modi = { AnimationMode.LOOP, AnimationMode.STOP, AnimationMode.SWING };
        animationModeComboBox = new JComboBox(modi);
        animationModeComboBox.setPreferredSize(speedUnitComboBox.getPreferredSize());
        animationModeComboBox.addActionListener(this);
        modePanel.add(animationModeComboBox);

        mainPanel.add(modePanel);

        setState(false);
        setAdvanced(isAdvanced);
    }

    private static void setState(boolean enabled) {
        animationModeComboBox.setEnabled(enabled);
        timeSlider.setEnabled(enabled);
        playButton.setEnabled(enabled);
        nextFrameButton.setEnabled(enabled);
        previousFrameButton.setEnabled(enabled);
        speedSpinner.setEnabled(enabled);
        speedUnitComboBox.setEnabled(enabled);
        advancedButton.setEnabled(enabled);
    }

    private void setAdvanced(boolean advanced) {
        isAdvanced = advanced;

        advancedButton.setIcon(advanced ? closeIcon : openIcon);
        modePanel.setVisible(advanced);
        speedPanel.setVisible(advanced);
    }

    public void setFrameSlider(int frame) {
        // update just UI, tbd
        timeSlider.removeChangeListener(this);
        timeSlider.setValue(frame);
        frameNumberLabel.setText((frame + 1) + "/" + (timeSlider.getMaximum() + 1));
        timeSlider.addChangeListener(this);
    }

    /**
     * Updates the speed of the animation. This function is called when changing
     * the speed of the animation or the its unit.
     */
    private static void updateMovieSpeed() {
        if (speedUnitComboBox.getSelectedItem() == SpeedUnit.FRAMESPERSECOND) {
            Layers.setDesiredRelativeSpeed(((SpinnerNumberModel) speedSpinner.getModel()).getNumber().intValue());
        } /* else {
            Layers.setDesiredAbsoluteSpeed(((SpinnerNumberModel) speedSpinner.getModel()).getNumber().intValue() *
                                           ((SpeedUnit) speedUnitComboBox.getSelectedItem()).getSecondsPerSecond());
        }
        */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == advancedButton) {
            setAdvanced(!isAdvanced);
            // Toggle play/pause
        } else if (e.getSource() == playButton) {
            Layers.toggleMovie();
            // Previous frame
        } else if (e.getSource() == previousFrameButton) {
            if (Layers.isMoviePlaying()) {
                Layers.toggleMovie();
            }
            Layers.previousFrame();
            // Next frame
        } else if (e.getSource() == nextFrameButton) {
            if (Layers.isMoviePlaying()) {
                Layers.toggleMovie();
            }
            Layers.nextFrame();
            // Change animation speed
        } else if (e.getSource() == ((JSpinner.DefaultEditor) speedSpinner.getEditor()).getTextField()) {
            try {
                speedSpinner.commitEdit();
            } catch (ParseException e1) {
                e1.printStackTrace();
            }
            updateMovieSpeed();
            // Change animation speed unit
        } else if (e.getSource() == speedUnitComboBox) {
            updateMovieSpeed();
            // Change animation mode
        } else if (e.getSource() == animationModeComboBox) {
            Layers.setAnimationMode((AnimationMode) animationModeComboBox.getSelectedItem());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stateChanged(ChangeEvent e) {
        // Jump to different frame
        if (e.getSource() == timeSlider) {
            int val = timeSlider.getValue();
            Layers.setFrame(val);
            frameNumberLabel.setText((val + 1) + "/" + (timeSlider.getMaximum() + 1));
            // Change animation speed
        } else if (e.getSource() == speedSpinner) {
            updateMovieSpeed();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseClicked(MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseEntered(MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseExited(MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mousePressed(MouseEvent e) {
        someoneIsDragging = true;
        wasPlaying = Layers.isMoviePlaying();
        if (wasPlaying) {
            Layers.pauseMovie();
        }
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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
            if (!play) {
                ImageViewerGui.getFramerateStatusPanel().updateFramerate(0); // somewhat hackish
                playButton.setIcon(playIcon);
                playButton.setToolTipText("Play movie");
            } else {
                playButton.setIcon(pauseIcon);
                playButton.setToolTipText("Pause movie");
            }
        }
    }

    // called from other threads
    public static void cacheStatusChanged(View view) {
        EventQueue.invokeLater(new Runnable() {
            private View view;

            @Override
            public void run() {
               if (view == Layers.getActiveView()) {
                    timeSlider.repaint();
                }
            }

            public Runnable init(View _view) {
                view = _view;
                return this;
            }
        }.init(view));
    }

    /**
     * Action to play or pause the active layer, if it is an image series.
     *
     * Static movie actions are supposed be integrated into {@link MenuBar},
     * also to provide shortcuts. They always refer to the active layer.
     */
    public static class StaticPlayPauseAction extends AbstractAction implements ActionListener {

        public StaticPlayPauseAction() {
            super("Play/Pause movie");
            putValue(MNEMONIC_KEY, KeyEvent.VK_A);
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.ALT_MASK));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            Layers.toggleMovie();
            putValue(NAME, playButton.getToolTipText());
        }

    }

    /**
     * Action to step to the previous frame for the active layer, if it is an
     * image series.
     *
     * Static movie actions are supposed be integrated into {@link MenuBar},
     * also to provide shortcuts. They always refer to the active layer.
     */
    public static class StaticPreviousFrameAction extends AbstractAction implements ActionListener {

        public StaticPreviousFrameAction() {
            super("Step to previous frame");
            putValue(MNEMONIC_KEY, KeyEvent.VK_P);
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.ALT_MASK));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            instance.actionPerformed(new ActionEvent(previousFrameButton, 0, ""));
        }

    }

    /**
     * Action to step to the next frame for the active layer, if it is an image
     * series.
     *
     * Static movie actions are supposed be integrated into {@link MenuBar},
     * also to provide shortcuts. They always refer to the active layer.
     */
    public static class StaticNextFrameAction extends AbstractAction implements ActionListener {

        public StaticNextFrameAction() {
            super("Step to next frame");
            putValue(MNEMONIC_KEY, KeyEvent.VK_N);
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.ALT_MASK));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            instance.actionPerformed(new ActionEvent(nextFrameButton, 0, ""));
        }

    }

    /**
     * Extension of JSlider displaying the caching status on the track.
     *
     * This element provides its own look and feel. Therefore, it is independent
     * from the global look and feel.
     */
    private static class TimeSlider extends JSlider {

        private static final Color notCachedColor = Color.LIGHT_GRAY;
        private static final Color partialCachedColor = new Color(0x8080FF);
        private static final Color completeCachedColor = new Color(0x4040FF);

        /**
         * Default constructor
         *
         * @param orientation
         *            specified orientation
         * @param min
         *            specified minimum
         * @param max
         *            specified maximum
         * @param value
         *            initial value
         */
        public TimeSlider(int orientation, int min, int max, int value) {
            super(orientation, min, max, value);
            setUI(new TimeSliderUI(this));
        }

        /**
         * Overrides updateUI, to keep own SliderUI.
         */
        @Override
        public void updateUI() {
        }

        /**
         * Extension of BasicSliderUI overriding some drawing functions.
         *
         * All functions for size calculations stay the same.
         */
        private class TimeSliderUI extends BasicSliderUI {

            /**
             * Default constructor.
             *
             * @param component
             *            the component where this UI delegate is being
             *            installed
             */
            public TimeSliderUI(JSlider component) {
                super(component);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected TrackListener createTrackListener(JSlider slider) {
                return new TimeTrackListener();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected void scrollDueToClickInTrack(int dir) {
                setValue(this.valueForXPosition(((TimeTrackListener) trackListener).getCurrentX()));
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void paintThumb(Graphics g) {
                g.setColor(Color.BLACK);
                g.drawRect(thumbRect.x, thumbRect.y, thumbRect.width - 1, thumbRect.height - 1);

                int x = thumbRect.x + (thumbRect.width - 1) / 2;
                g.drawLine(x, thumbRect.y, x, thumbRect.y + thumbRect.height - 1);
            }

            /**
             * {@inheritDoc}
             *
             * Draws the different region (no/partial/complete information
             * loaded) in different colors.
             */
            @Override
            public void paintTrack(Graphics g) {
                View view = Layers.getActiveView();
                if (view == null)
                    return;

                ImageCacheStatus cacheStatus = view.getImageCacheStatus();
                int len = view.getMaximumFrameNumber();

                int height = getSize().height / 4;
                int offset = (getSize().height - height) / 2;

                Graphics2D g2d = (Graphics2D) g;
                g2d.setStroke(new BasicStroke(4));

                for (int i = 0; i <= len; i++) {
                    int begin = (int) ((float) (i) / (len) * trackRect.width);
                    int end = (int) ((float) (i + 1) / (len) * trackRect.width);

                    if (end == begin)
                        end++;
                    if (end > trackRect.width)
                        end = trackRect.width;

                    if (cacheStatus.getImageStatus(i) == CacheStatus.PARTIAL) {
                        g2d.setColor(partialCachedColor);
                    } else if (cacheStatus.getImageStatus(i) == CacheStatus.COMPLETE) {
                        g2d.setColor(completeCachedColor);
                    } else {
                        g2d.setColor(notCachedColor);
                    }
                    g2d.drawLine(trackRect.x + begin, offset + getSize().height / 8, trackRect.x + end, offset + getSize().height / 8);
                }

                g2d.setStroke(new BasicStroke(1));
            }

            /**
             * Overrides the track listener to access currentX
             */
            protected class TimeTrackListener extends TrackListener {
                public int getCurrentX() {
                    return currentMouseX;
                }
            }
        }

    }

}
