package org.helioviewer.jhv.gui.components;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
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
import java.util.LinkedList;

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
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.metadata.ObserverMetaData;
import org.helioviewer.viewmodel.view.AbstractView;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.MovieView;
import org.helioviewer.viewmodel.view.MovieView.AnimationMode;

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
            public int getSecondsPerSecond() {
                return 0;
            }
        },
        MINUTESPERSECOND {
            @Override
            public String toString() {
                return "Solar minutes/sec";
            }

            @Override
            public int getSecondsPerSecond() {
                return 60;
            }
        },
        HOURSPERSECOND {
            @Override
            public String toString() {
                return "Solar hours/sec";
            }

            @Override
            public int getSecondsPerSecond() {
                return 3600;
            }
        },
        DAYSPERSECOND {
            @Override
            public String toString() {
                return "Solar days/sec";
            }

            @Override
            public int getSecondsPerSecond() {
                return 86400;
            }
        };

        public abstract int getSecondsPerSecond();
    }

    // Linking movies to play simultaneously
    private static MoviePanelManager moviePanelManager = new MoviePanelManager();

    public static MoviePanelManager getMoviePanelManager() {
        return moviePanelManager;
    }

    // Status
    private static boolean isAdvanced = false;
    private boolean isPlaying = false;

    // Gui elements
    private final TimeSlider timeSlider;
    private final JLabel frameNumberLabel;
    private final JButton previousFrameButton;
    private final JButton playPauseButton;
    private final JButton nextFrameButton;
    private final JButton advancedButton;
    private final JSpinner speedSpinner;
    private final JComboBox speedUnitComboBox;
    private final JComboBox animationModeComboBox;

    private final JPanel modePanel;
    private final JPanel speedPanel;

    // Icons
    private static final Icon playIcon = IconBank.getIcon(JHVIcon.PLAY);
    private static final Icon pauseIcon = IconBank.getIcon(JHVIcon.PAUSE);
    private static final Icon openIcon = IconBank.getIcon(JHVIcon.SHOW_MORE);
    private static final Icon closeIcon = IconBank.getIcon(JHVIcon.SHOW_LESS);

    private static MovieView activeView;

    /**
     * Default constructor.
     *
     * @param movieView
     *            Associated movie view
     */
    public MoviePanel(AbstractView view) {
        this();

        if (!(view instanceof MovieView)) {
            this.setEnabled(false);
            return;
        }

        // tbd
        activeView = (MovieView) view;

        timeSlider.setMaximum(((MovieView) view).getMaximumFrameNumber());
        timeSlider.setValue(((MovieView) view).getCurrentFrameNumber());

        SpeedUnit[] units;
        if (view.getMetaData() instanceof ObserverMetaData) {
            SpeedUnit[] newunits = { SpeedUnit.MINUTESPERSECOND, SpeedUnit.HOURSPERSECOND, SpeedUnit.DAYSPERSECOND, SpeedUnit.FRAMESPERSECOND };
            units = newunits;
        } else {
            SpeedUnit[] newunits = { SpeedUnit.FRAMESPERSECOND };
            units = newunits;
        }

        speedUnitComboBox.removeActionListener(this);
        speedUnitComboBox.removeAllItems();

        for (SpeedUnit unit : units) {
            speedUnitComboBox.addItem(unit);
        }

        speedUnitComboBox.setSelectedItem(SpeedUnit.FRAMESPERSECOND);
        speedUnitComboBox.addActionListener(this);

        this.setEnabled(true);
    }

    public MoviePanel() {
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

        playPauseButton = ButtonCreator.createButton(playIcon, "Play movie", this);
        buttonPanel.add(playPauseButton);

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

        // The speed panel has some distinction from above as it is one of the
        // advanced options
        // It is not included in the main Panel to save space if it is not shown

        // Speed
        speedPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        speedPanel.add(new JLabel("Speed", JLabel.RIGHT));

        speedSpinner = new JSpinner(new SpinnerNumberModel(20, 1, 99, 1));
        speedSpinner.addChangeListener(this);
        ((JSpinner.DefaultEditor) speedSpinner.getEditor()).getTextField().addActionListener(this);
        speedSpinner.setMaximumSize(speedSpinner.getPreferredSize());
        speedPanel.add(speedSpinner);

        SpeedUnit[] units = { SpeedUnit.FRAMESPERSECOND };
        speedUnitComboBox = new JComboBox(units);

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

        this.setEnabled(false);
        this.setAdvanced(MoviePanel.isAdvanced);
    }

    /**
     * Override the setEnabled method in order to keep the containing
     * components' enabledState synced with the enabledState of this component.
     */
    @Override
    public void setEnabled(boolean enabled) {
        if (activeView == null) {
            enabled = false;
        }

        super.setEnabled(enabled);
        animationModeComboBox.setEnabled(enabled);
        timeSlider.setEnabled(enabled);
        playPauseButton.setEnabled(enabled);
        nextFrameButton.setEnabled(enabled);
        previousFrameButton.setEnabled(enabled);
        speedSpinner.setEnabled(enabled);
        speedUnitComboBox.setEnabled(enabled);
        advancedButton.setEnabled(enabled);
    }

    public void setAdvanced(boolean advanced) {
        MoviePanel.isAdvanced = advanced;

        advancedButton.setIcon(advanced ? closeIcon : openIcon);
        modePanel.setVisible(advanced);
        speedPanel.setVisible(advanced);
    }

    public static void setFrameSlider(AbstractView view) {
        if (view instanceof MovieView) {
            view.getMoviePanel().timeSlider.setValue(((MovieView) view).getCurrentFrameNumber());
        }
    }

    /**
     * Jumps to the specified frame
     *
     * @param frame
     *            the number of the frame
     */
    private void jumpToFrameNumber(int frame) {
        frame = Math.min(frame, activeView.getMaximumAccessibleFrameNumber());
        timeSlider.setValue(frame);
        LinkedMovieManager.setCurrentFrame(activeView, frame);
    }

    /**
     * Toggles between playing and not playing the animation.
     */
    private void togglePlayPause() {
        setPlaying(!isPlaying, false);
    }

    private void setPlaying(boolean playing, boolean onlyGUI) {
        isPlaying = playing;

        if (!isPlaying) {
            playPauseButton.setIcon(playIcon);
            playPauseButton.setToolTipText("Play movie");
            if (!onlyGUI) {
                LinkedMovieManager.pauseLinkedMovies();
            }
        } else {
            playPauseButton.setIcon(pauseIcon);
            playPauseButton.setToolTipText("Pause movie");
            if (!onlyGUI) {
                LinkedMovieManager.playLinkedMovies();
            }
        }
    }

    /**
     * Updates the speed of the animation. This function is called when changing
     * the speed of the animation or the its unit.
     */
    private void updateMovieSpeed() {
        if (speedUnitComboBox.getSelectedItem() == SpeedUnit.FRAMESPERSECOND) {
            activeView.setDesiredRelativeSpeed(((SpinnerNumberModel) speedSpinner.getModel()).getNumber().intValue());
        } else {
            activeView.setDesiredAbsoluteSpeed(((SpinnerNumberModel) speedSpinner.getModel()).getNumber().intValue() * ((SpeedUnit) speedUnitComboBox.getSelectedItem()).getSecondsPerSecond());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == advancedButton) {
            this.setAdvanced(!MoviePanel.isAdvanced);
            ImageViewerGui.getMoviePanelContainer().ensureSize();

            // Toggle play/pause
        } else if (e.getSource() == playPauseButton) {
            togglePlayPause();

            // Previous frame
        } else if (e.getSource() == previousFrameButton) {
            if (isPlaying) {
                togglePlayPause();
            }
            jumpToFrameNumber(activeView.getCurrentFrameNumber() - 1);

            // Next frame
        } else if (e.getSource() == nextFrameButton) {
            if (isPlaying) {
                togglePlayPause();
            }
            jumpToFrameNumber(activeView.getCurrentFrameNumber() + 1);

            // Change animation speed
        } else if (e.getSource() == ((JSpinner.DefaultEditor) speedSpinner.getEditor()).getTextField()) {
            try {
                speedSpinner.commitEdit();
            } catch (ParseException e1) {
                e1.printStackTrace();
            }
            moviePanelManager.updateSpeedSpinnerLinkedMovies(this);
            updateMovieSpeed();

            // Change animation speed unit
        } else if (e.getSource() == speedUnitComboBox) {
            moviePanelManager.updateSpeedUnitComboBoxLinkedMovies(this);
            updateMovieSpeed();

            // Change animation mode
        } else if (e.getSource() == animationModeComboBox) {
            moviePanelManager.updateAnimationModeComboBoxLinkedMovies(this);
            activeView.setAnimationMode((AnimationMode) animationModeComboBox.getSelectedItem());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stateChanged(ChangeEvent e) {
        // Jump to different frame
        if (e.getSource() == timeSlider) {
            jumpToFrameNumber(timeSlider.getValue());
            frameNumberLabel.setText((activeView.getCurrentFrameNumber() + 1) + "/" + (timeSlider.getMaximum() + 1));
            if (activeView.getCurrentFrameNumber() == timeSlider.getMinimum() && animationModeComboBox.getSelectedItem() == AnimationMode.STOP) {
                togglePlayPause();
            }
            // Change animation speed
        } else if (e.getSource() == speedSpinner) {
            moviePanelManager.updateSpeedSpinnerLinkedMovies(this);
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
        moviePanelManager.someoneIsDragging = true;
        if (isPlaying) {
            LinkedMovieManager.pauseLinkedMovies();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (isEnabled()) {
            if (e.getWheelRotation() < 0) {
                jumpToFrameNumber(activeView.getCurrentFrameNumber() + 1);
            } else if (e.getWheelRotation() > 0) {
                jumpToFrameNumber(activeView.getCurrentFrameNumber() - 1);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (isPlaying) {
            LinkedMovieManager.playLinkedMovies();
        }
        moviePanelManager.someoneIsDragging = false;
    }

    public void playStateChanged(boolean playing) {
        if (playing != isPlaying && !moviePanelManager.someoneIsDragging) {
            setPlaying(playing, true);
        }
    }

    public static void cacheStatusChanged(MovieView view, boolean complete, int until) {
        MoviePanel panel = ((AbstractView) view).getMoviePanel();
        if (panel == null) { // this may come before view added as layer
            return;
        }

        if (complete) {
            panel.timeSlider.setCompleteCachedUntil(until);
        } else {
            panel.timeSlider.setPartialCachedUntil(until);
        }
        panel.timeSlider.repaint();
    }

    /**
     * Abstract base class for all static movie actions.
     *
     * Static movie actions are supposed be integrated into {@link MenuBar},
     * also to provide shortcuts. They always refer to the active layer.
     */
    private static abstract class StaticMovieAction extends AbstractAction implements ActionListener, LayersListener {

        protected MoviePanel activePanel;

        /**
         * Default constructor.
         *
         * @param name
         *            name of the action that shall be displayed on a button
         * @param icon
         *            icon of the action that shall be displayed on a button
         */
        public StaticMovieAction(String name, Icon icon) {
            super(name, icon);
            LayersModel.addLayersListener(this);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void layerAdded(AbstractView view) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void activeLayerChanged(AbstractView view) {
            activePanel = view.getMoviePanel();

            if (view instanceof MovieView)
                activeView = (MovieView) view;
            else
                activeView = null;
        }

    }

    /**
     * Action to play or pause the active layer, if it is an image series.
     *
     * Static movie actions are supposed be integrated into {@link MenuBar},
     * also to provide shortcuts. They always refer to the active layer.
     */
    public static class StaticPlayPauseAction extends StaticMovieAction {

        public StaticPlayPauseAction() {
            super("Play movie", playIcon);
            putValue(MNEMONIC_KEY, KeyEvent.VK_A);
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.ALT_MASK));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            if (activePanel != null) {
                activePanel.actionPerformed(new ActionEvent(activePanel.playPauseButton, 0, ""));
                putValue(NAME, activePanel.playPauseButton.getToolTipText());
                putValue(SMALL_ICON, activePanel.playPauseButton.getIcon());
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void activeLayerChanged(AbstractView view) {
            super.activeLayerChanged(view);
            if (activePanel != null && getValue(SMALL_ICON) != activePanel.playPauseButton.getIcon()) {
                putValue(NAME, activePanel.playPauseButton.getToolTipText());
                putValue(SMALL_ICON, activePanel.playPauseButton.getIcon());
            }
        }
    }

    /**
     * Action to step to the previous frame for the active layer, if it is an
     * image series.
     *
     * Static movie actions are supposed be integrated into {@link MenuBar},
     * also to provide shortcuts. They always refer to the active layer.
     */
    public static class StaticPreviousFrameAction extends StaticMovieAction {

        public StaticPreviousFrameAction() {
            super("Step to previous frame", IconBank.getIcon(JHVIcon.BACK));
            putValue(MNEMONIC_KEY, KeyEvent.VK_P);
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.ALT_MASK));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            if (activePanel != null) {
                activePanel.actionPerformed(new ActionEvent(activePanel.previousFrameButton, 0, ""));
            }
        }
    }

    /**
     * Action to step to the next frame for the active layer, if it is an image
     * series.
     *
     * Static movie actions are supposed be integrated into {@link MenuBar},
     * also to provide shortcuts. They always refer to the active layer.
     */
    public static class StaticNextFrameAction extends StaticMovieAction {

        public StaticNextFrameAction() {
            super("Step to next frame", IconBank.getIcon(JHVIcon.FORWARD));
            putValue(MNEMONIC_KEY, KeyEvent.VK_N);
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.ALT_MASK));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            if (activePanel != null) {
                activePanel.actionPerformed(new ActionEvent(activePanel.nextFrameButton, 0, ""));
            }
        }
    }

    /**
     * Class to synchronize linked image series.
     *
     * Synchronize the GUI elements as well as the actual movie.
     */
    public static class MoviePanelManager {

        private final LinkedList<MoviePanel> linkedMovies = new LinkedList<MoviePanel>();
        public boolean someoneIsDragging = false;

        public void playStateChanged(boolean playing) {
            for (MoviePanel panel : linkedMovies) {
                panel.playStateChanged(playing);
            }
        }

        /**
         * Adds an image series to the set of series playing simultaneous.
         *
         * <p>
         * The master movie panel may change.
         *
         * @param newPanel
         *            Panel to add
         */
        public void linkView(AbstractView view) {
            if (!(view instanceof MovieView))
                return;

            MoviePanel newPanel = view.getMoviePanel();
            if (!linkedMovies.isEmpty()) {
                // Copy Settings
                MoviePanel copyFrom = linkedMovies.element();
                newPanel.isPlaying = copyFrom.isPlaying;
                newPanel.playPauseButton.setIcon(copyFrom.playPauseButton.getIcon());
                newPanel.speedSpinner.setValue(copyFrom.speedSpinner.getValue());
                newPanel.speedUnitComboBox.setSelectedItem(copyFrom.speedUnitComboBox.getSelectedItem());
                newPanel.animationModeComboBox.setSelectedItem(copyFrom.animationModeComboBox.getSelectedItem());
            }

            linkedMovies.add(newPanel);
            LinkedMovieManager.linkMovie((MovieView) view);
        }

        /**
         * Removes an image series from the set of series playing simultaneous.
         *
         * <p>
         * The master movie panel may change.
         *
         * @param panel
         *            Panel to remove
         */
        public void unlinkView(AbstractView view) {
            if (!(view instanceof MovieView))
                return;

            LinkedMovieManager.unlinkMovie((MovieView) view);
            linkedMovies.remove(view.getMoviePanel());
        }

        /**
         * Copies the value from the speed spinner of the given panel to all
         * other linked panels.
         *
         * @param copyFrom
         *            Panel dominating the other ones right now
         */
        public void updateSpeedSpinnerLinkedMovies(MoviePanel copyFrom) {
            for (MoviePanel panel : linkedMovies) {
                panel.speedSpinner.setValue(copyFrom.speedSpinner.getValue());
            }
        }

        /**
         * Copies the value from the speed unit combobox of the given panel to
         * all other linked panels.
         *
         * @param copyFrom
         *            Panel dominating the other ones right now
         */
        public void updateSpeedUnitComboBoxLinkedMovies(MoviePanel copyFrom) {
            for (MoviePanel panel : linkedMovies) {
                panel.speedUnitComboBox.setSelectedItem(copyFrom.speedUnitComboBox.getSelectedItem());
            }
        }

        /**
         * Copies the value from the animation mode combobox of the given panel
         * to all other linked panels.
         *
         * @param copyFrom
         *            Panel dominating the other ones right now
         */
        public void updateAnimationModeComboBoxLinkedMovies(MoviePanel copyFrom) {
            for (MoviePanel panel : linkedMovies) {
                panel.animationModeComboBox.setSelectedItem(copyFrom.animationModeComboBox.getSelectedItem());
            }
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
        @Override
        public void updateUI() {
        }

        /**
         * Sets the frame number, to which partial information is loaded.
         *
         * Partial information means, that the image already can be shown, but
         * not yet in full quality.
         *
         * @param cachedUntil
         *            Frame number, to which partial information is loaded.
         */
        public void setPartialCachedUntil(int cachedUntil) {
            partialCachedUntil = cachedUntil;
            repaint();
        }

        /**
         * Sets the frame number, to which complete information is loaded.
         *
         * Complete information means, that the image can be shown in full
         * quality.
         *
         * @param cachedUntil
         *            Frame number, to which complete information is loaded.
         */
        public void setCompleteCachedUntil(int cachedUntil) {
            completeCachedUntil = cachedUntil;
            if (partialCachedUntil < cachedUntil) {
                partialCachedUntil = cachedUntil;
            }
            repaint();
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
                int height = getSize().height / 4;
                int offset = (getSize().height - height) / 2;
                int partialCachedOffset = (int) ((float) (partialCachedUntil) / (getMaximum() - getMinimum()) * trackRect.width);
                int completeCachedOffset = (int) ((float) (completeCachedUntil) / (getMaximum() - getMinimum()) * trackRect.width);

                Graphics2D g2d = (Graphics2D) g;
                g2d.setStroke(new BasicStroke(4));

                g2d.setColor(notCachedColor);
                g2d.drawLine(trackRect.x + partialCachedOffset, offset + getSize().height / 8, trackRect.x + trackRect.width - 0, offset + getSize().height / 8);

                g2d.setColor(partialCachedColor);
                g2d.drawLine(trackRect.x + completeCachedOffset, offset + getSize().height / 8, trackRect.x + partialCachedOffset, offset + getSize().height / 8);

                g2d.setColor(completeCachedColor);
                g2d.drawLine(trackRect.x, offset + getSize().height / 8, trackRect.x + completeCachedOffset, offset + getSize().height / 8);
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
