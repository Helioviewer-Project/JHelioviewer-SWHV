package ch.fhnw.jhv.gui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.text.ParseException;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;

import ch.fhnw.jhv.gui.components.controller.AnimatorController;
import ch.fhnw.jhv.gui.components.controller.AnimatorController.AnimationMode;
import ch.fhnw.jhv.plugins.vectors.control.SpringUtilities;

/**
 * Panel containing the movie controls.
 * 
 * <p>
 * This panel provides the capability to start and stop an movie, step to
 * certain frames and switch the movie speed as well as the movie mode.
 * 
 * <p>
 * Apart from that, this component is responsible for playing multiple movie
 * simultaneous. This is done by actual playing only one movie, the one with the
 * most frames per time. All other image series just jump to the frame being
 * closest to the current frame of the series currently playing. That way, it is
 * impossible that different series get asynchronous.
 * 
 * <p>
 * For further information about image series, see
 * {@link org.helioviewer.viewmodel.view.MovieView} and
 * {@link org.helioviewer.viewmodel.view.TimedMovieView}.
 * 
 * @author Markus Langenberg
 * @author Malte Nuhn
 * 
 */
public class MoviePanel extends JPanel implements ActionListener, ChangeListener, MouseListener, MouseWheelListener {

    private static final long serialVersionUID = 1L;

    // Status
    private static boolean isAdvanced = false;
    private boolean isPlaying = false;
    private boolean isDragging = false;

    // Gui elements
    private TimeSlider timeSlider;
    private JLabel frameNumberLabel;
    private JButton previousFrameButton;
    private JButton playPauseButton;
    private JButton nextFrameButton;
    private JButton advancedButton;
    private JSpinner interpolationTimeSpinner;
    private JComboBox animationModeComboBox;
    private JCheckBox interpolateCheckBox;

    // Listeners

    // Panel containing all elements for advanced options
    private JPanel advancedPanel;

    // Icons
    private static Icon playIcon = new ImageIcon(MoviePanel.class.getResource("play.png"));
    private static Icon pauseIcon = new ImageIcon(MoviePanel.class.getResource("pause.png"));
    private static Icon forwardIcon = new ImageIcon(MoviePanel.class.getResource("forward.png"));
    private static Icon backIcon = new ImageIcon(MoviePanel.class.getResource("back.png"));

    /**
     * how many seconds passed between a render call
     */
    private int currentFrameNumber = 0;
    private int maximumFrames = 0;

    AnimatorController animator;

    public MoviePanel() {
        super(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(mainPanel, BorderLayout.NORTH);

        // Time line
        timeSlider = new TimeSlider(SwingConstants.HORIZONTAL, 0, 0, 0);
        timeSlider.setBorder(BorderFactory.createEmptyBorder());
        timeSlider.setSnapToTicks(true);
        timeSlider.addChangeListener(this);
        timeSlider.addMouseListener(this);
        timeSlider.setToolTipText("The bar represents the current time dimension. It is possible to drag and drop the bar to change to another time dimension.");
        addMouseWheelListener(this);

        mainPanel.add(timeSlider);

        JPanel secondLine = new JPanel(new BorderLayout());

        // Control buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 1));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        previousFrameButton = new JButton();
        previousFrameButton.setIcon(backIcon);
        previousFrameButton.addActionListener(this);
        previousFrameButton.setToolTipText("Step back to previous frame");
        buttonPanel.add(previousFrameButton);

        playPauseButton = new JButton();
        playPauseButton.setIcon(playIcon);
        playPauseButton.addActionListener(this);
        playPauseButton.setToolTipText("Start playing");
        buttonPanel.add(playPauseButton);

        nextFrameButton = new JButton();
        nextFrameButton.setIcon(forwardIcon);
        nextFrameButton.addActionListener(this);
        nextFrameButton.setToolTipText("Step forward to next frame");
        buttonPanel.add(nextFrameButton);
        secondLine.add(buttonPanel, BorderLayout.WEST);

        buttonPanel.add(new JSeparator(SwingConstants.VERTICAL));

        advancedButton = new JButton();
        advancedButton.setText("Advanced");
        advancedButton.setToolTipText("Expand a menu with more detailed settings");
        advancedButton.addActionListener(this);
        buttonPanel.add(advancedButton);

        // Current frame number
        frameNumberLabel = new JLabel("-");
        frameNumberLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        frameNumberLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        frameNumberLabel.setPreferredSize(new Dimension(75, 20));
        frameNumberLabel.setToolTipText("Displays the current time dimension");
        secondLine.add(frameNumberLabel, BorderLayout.EAST);
        mainPanel.add(secondLine);

        advancedPanel = new JPanel(new SpringLayout());

        JLabel lblAnimationDelay = new JLabel("Animation delay [s]");
        lblAnimationDelay.setToolTipText("Define the animation delay between two time dimensions in seconds");

        advancedPanel.add(lblAnimationDelay);
        interpolationTimeSpinner = new JSpinner(new SpinnerNumberModel(2.0f, 0.0f, 10.0f, 0.1f));
        interpolationTimeSpinner.setToolTipText("Define the amount of seconds which the animation is delayed between two time dimensions");
        interpolationTimeSpinner.addChangeListener(this);
        interpolationTimeSpinner.setMaximumSize(interpolationTimeSpinner.getPreferredSize());
        advancedPanel.add(interpolationTimeSpinner);

        // Interpolation
        interpolateCheckBox = new JCheckBox();

        JLabel lblActiveInterpolation = new JLabel("Activate Interpolation");
        lblActiveInterpolation.setToolTipText("Turn on a linear Interpolation between the time dimension");

        advancedPanel.add(lblActiveInterpolation);
        interpolateCheckBox.addChangeListener(this);
        advancedPanel.add(interpolateCheckBox);

        SpringUtilities.makeCompactGrid(advancedPanel, 2, 2, // rows, cols
                6, 6, // initX, initY
                6, 6); // xPad, yPad

        mainPanel.add(advancedPanel);

        this.setEnabled(false);
        this.setAdvanced(MoviePanel.isAdvanced);

    }

    public void init(int dimensions) {
        maximumFrames = dimensions;
        currentFrameNumber = 1;
        timeSlider.setMinimum(1);
        timeSlider.setMaximum(maximumFrames);
        timeSlider.setValue(currentFrameNumber);
    }

    public void updateMoviePanel(int frame) {
        currentFrameNumber = frame + 1;
        timeSlider.setValue(currentFrameNumber);
        frameNumberLabel.setText(currentFrameNumber + " / " + maximumFrames);
    }

    /**
     * Override the setEnabled method in order to keep the containing
     * components' enabledState synced with the enabledState of this component.
     */

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        timeSlider.setEnabled(enabled);
        playPauseButton.setEnabled(enabled);
        nextFrameButton.setEnabled(enabled);
        previousFrameButton.setEnabled(enabled);
        interpolationTimeSpinner.setEnabled(enabled);
        advancedButton.setEnabled(enabled);

        if (!enabled) {
            frameNumberLabel.setText("-");
            timeSlider.setValue(timeSlider.getMinimum());
            currentFrameNumber = -1;
            maximumFrames = -1;
        }

    }

    public void setAdvanced(boolean advanced) {
        MoviePanel.isAdvanced = advanced;
        advancedPanel.setVisible(advanced);
    }

    public void setAnimator(AnimatorController controller) {
        this.animator = controller;
        animator.setMoviePanel(this);
    }

    /**
     * Toggles between playing and not playing the animation.
     */
    public void togglePlayPause() {
        setPlaying(!isPlaying);
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;

        if (!isPlaying) {
            playPauseButton.setIcon(playIcon);
            playPauseButton.setToolTipText("Play movie");
        } else {
            playPauseButton.setIcon(pauseIcon);
            playPauseButton.setToolTipText("Pause movie");
        }

        animator.setPlaying(isPlaying);
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == advancedButton) {
            this.setAdvanced(!MoviePanel.isAdvanced);

            // Toggle play/pause
        } else if (e.getSource() == playPauseButton) {

            togglePlayPause();

            // Previous frame
        } else if (e.getSource() == previousFrameButton) {
            if (isPlaying) {
                togglePlayPause();
            }
            animator.previousFrame();

            // Next frame
        } else if (e.getSource() == nextFrameButton) {
            if (isPlaying) {
                togglePlayPause();
            }
            animator.nextFrame();
            // Change interpolation time
        } else if (e.getSource() == ((JSpinner.DefaultEditor) interpolationTimeSpinner.getEditor()).getTextField()) {
            try {
                interpolationTimeSpinner.commitEdit();
            } catch (ParseException e1) {
                e1.printStackTrace();
            }

            // Change
        } else if (e.getSource() == animationModeComboBox) {
            animator.setAnimationMode((AnimationMode) animationModeComboBox.getSelectedItem());
        }

    }

    // This is needed for the CardLayout

    @SuppressWarnings("deprecation")
    public void show(boolean visible) {
        super.show(visible);
        this.setAdvanced(MoviePanel.isAdvanced);
        // update
    }

    /**
     * {@inheritDoc}
     */
    public void stateChanged(javax.swing.event.ChangeEvent e) {

        // Jump to different frame
        if (e.getSource() == timeSlider) {
            if (timeSlider.getValueIsAdjusting()) {
                isDragging = true;
                if (isPlaying) {
                    setPlaying(false);
                }
            } else if (isDragging) {
                isDragging = false;
                animator.jumpToFrameNumber(timeSlider.getValue() - 1);
            }

        } else if (e.getSource() == interpolationTimeSpinner) {
            // change time-delta between frames
            double interpoltime = (Double) interpolationTimeSpinner.getValue();
            animator.setInterpolationTime((float) interpoltime);
        } else if (e.getSource() == interpolateCheckBox) {
            animator.setInterpolatd(interpolateCheckBox.isSelected());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void mouseClicked(MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    public void mouseEntered(MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    public void mouseExited(MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    public void mousePressed(MouseEvent e) {

        if (isPlaying) {

        }
    }

    /**
     * {@inheritDoc}
     */
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (isEnabled()) {
            if (e.getWheelRotation() < 0) {
                animator.nextFrame();
            } else if (e.getWheelRotation() > 0) {
                animator.previousFrame();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void mouseReleased(MouseEvent e) {
        if (isPlaying) {
        }

    }

    /**
     * Extension of JSlider displaying the caching status on the track.
     * 
     * This element provides its own look and feel. Therefore, it is independent
     * from the global look and feel.
     */
    private static class TimeSlider extends JSlider {

        private static final long serialVersionUID = 1L;

        private static final Color notCachedColor = Color.LIGHT_GRAY;
        private static final Color partialCachedColor = new Color(0x8080FF);
        private static final Color completeCachedColor = new Color(0x4040FF);

        private int partialCachedUntil = 0;
        private int completeCachedUntil = 0;

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

            protected TrackListener createTrackListener(JSlider slider) {
                return new TimeTrackListener();
            }

            /**
             * {@inheritDoc}
             */

            protected void scrollDueToClickInTrack(int dir) {
                setValue(this.valueForXPosition(((TimeTrackListener) trackListener).getCurrentX()));
            }

            /**
             * {@inheritDoc}
             */

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

            public void paintTrack(Graphics g) {

                int height = getSize().height / 4;
                int offset = (getSize().height - height) / 2;

                int partialCachedOffset = (int) ((float) (partialCachedUntil) / (getMaximum() - getMinimum()) * trackRect.width);

                int completeCachedOffset = (int) ((float) (completeCachedUntil) / (getMaximum() - getMinimum()) * trackRect.width);

                g.setColor(notCachedColor);
                g.fillRect(trackRect.x + partialCachedOffset, offset, trackRect.width - partialCachedOffset, height);

                g.setColor(partialCachedColor);
                g.fillRect(trackRect.x + completeCachedOffset, offset, partialCachedOffset - completeCachedOffset, height);

                g.setColor(completeCachedColor);
                g.fillRect(trackRect.x, offset, completeCachedOffset, height);
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
