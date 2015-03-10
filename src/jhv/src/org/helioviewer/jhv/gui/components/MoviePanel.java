package org.helioviewer.jhv.gui.components;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
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
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;

import org.helioviewer.jhv.gui.ButtonCreator;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.ViewListenerDistributor;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.changeevent.CacheStatusChangedReason;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.LayerChangedReason;
import org.helioviewer.viewmodel.changeevent.LayerChangedReason.LayerChangeType;
import org.helioviewer.viewmodel.changeevent.PlayStateChangedReason;
import org.helioviewer.viewmodel.changeevent.SubImageDataChangedReason;
import org.helioviewer.viewmodel.metadata.ObserverMetaData;
import org.helioviewer.viewmodel.view.CachedMovieView;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.MovieView;
import org.helioviewer.viewmodel.view.MovieView.AnimationMode;
import org.helioviewer.viewmodel.view.TimedMovieView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

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
public class MoviePanel extends JPanel implements ActionListener, ChangeListener, MouseListener, MouseWheelListener, ViewListener {

    private static final long serialVersionUID = 1L;

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
    private static LinkedMovieManager linkedMovieManager = new LinkedMovieManager();
    private static LinkedList<MoviePanel> panelList = new LinkedList<MoviePanel>();

    // Status
    private static boolean isAdvanced = false;
    private boolean isPlaying = false;
    private final boolean isDragging = false;

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

    // References
    private MovieView view;
    private TimedMovieView timedView = null;

    // Icons
    private static final Icon playIcon = IconBank.getIcon(JHVIcon.PLAY);
    private static final Icon pauseIcon = IconBank.getIcon(JHVIcon.PAUSE);
    private static final Icon openIcon = IconBank.getIcon(JHVIcon.SHOW_MORE);
    private static final Icon closeIcon = IconBank.getIcon(JHVIcon.SHOW_LESS);

    /**
     * Default constructor.
     *
     * @param movieView
     *            Associated movie view
     */
    public MoviePanel(MovieView movieView) {
        this();

        if (movieView == null) {
            return;
        }

        view = movieView;
        if (view instanceof TimedMovieView) {
            timedView = (TimedMovieView) movieView;
        }

        timeSlider.setMaximum(movieView.getMaximumFrameNumber());
        timeSlider.setValue(movieView.getCurrentFrameNumber());

        SpeedUnit[] units;
        if (view.getAdapter(MetaDataView.class) != null && view.getAdapter(MetaDataView.class).getMetaData() instanceof ObserverMetaData) {
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

        if (view instanceof CachedMovieView) {
            timeSlider.setPartialCachedUntil(Math.min(((CachedMovieView) view).getImageCacheStatus().getImageCachedPartiallyUntil(), ((CachedMovieView) view).getDateTimeCache().getMetaStatus()));
            timeSlider.setCompleteCachedUntil(Math.min(((CachedMovieView) view).getImageCacheStatus().getImageCachedCompletelyUntil(), ((CachedMovieView) view).getDateTimeCache().getMetaStatus()));
        }

        ViewListenerDistributor.getSingletonInstance().addViewListener(this);
        this.setEnabled(true);
    }

    public MoviePanel() {
        super(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(mainPanel, BorderLayout.NORTH);

        // Time line
        timeSlider = new TimeSlider(TimeSlider.HORIZONTAL, 0, 0, 0);
        timeSlider.setBorder(BorderFactory.createEmptyBorder());
        timeSlider.setSnapToTicks(true);
        timeSlider.addChangeListener(this);
        timeSlider.addMouseListener(this);
        addMouseWheelListener(this);

        mainPanel.add(timeSlider);

        JPanel secondLine = new JPanel(new BorderLayout());

        // Control buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 1));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        previousFrameButton = ButtonCreator.createButton(IconBank.getIcon(JHVIcon.BACK), "Step to Previous Frame", this);
        buttonPanel.add(previousFrameButton);

        playPauseButton = ButtonCreator.createButton(playIcon, "Play movie", this);
        buttonPanel.add(playPauseButton);

        nextFrameButton = ButtonCreator.createButton(IconBank.getIcon(JHVIcon.FORWARD), "Step to Next Frame", this);
        buttonPanel.add(nextFrameButton);
        secondLine.add(buttonPanel, BorderLayout.WEST);

        buttonPanel.add(new JSeparator(SwingConstants.VERTICAL));

        advancedButton = ButtonCreator.createTextButton(IconBank.getIcon(JHVIcon.SHOW_MORE), "More Options", "More Options to Control Playback", this);
        buttonPanel.add(advancedButton);

        // Current frame number
        frameNumberLabel = new JLabel((timeSlider.getValue() + 1) + "/" + (timeSlider.getMaximum() + 1));
        frameNumberLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        frameNumberLabel.setHorizontalAlignment(JLabel.RIGHT);
        frameNumberLabel.setPreferredSize(new Dimension(75, 20));
        secondLine.add(frameNumberLabel, BorderLayout.EAST);

        mainPanel.add(secondLine);

        // The speed panel has some distinction from above as it is one of the
        // advanced options
        // It is not included in the main Panel to save space if it is not shown

        // Speed
        speedPanel = new JPanel(new BorderLayout());
        speedPanel.add(new JSeparator(SwingConstants.VERTICAL), BorderLayout.PAGE_START);
        speedPanel.add(new JLabel("Speed:     "), BorderLayout.WEST);

        speedSpinner = new JSpinner(new SpinnerNumberModel(20, 1, 99, 1));
        speedSpinner.addChangeListener(this);
        ((JSpinner.DefaultEditor) speedSpinner.getEditor()).getTextField().addActionListener(this);
        speedSpinner.setMaximumSize(speedSpinner.getPreferredSize());
        speedPanel.add(speedSpinner, BorderLayout.CENTER);

        SpeedUnit[] units = { SpeedUnit.FRAMESPERSECOND };
        speedUnitComboBox = new JComboBox(units);

        speedUnitComboBox.addActionListener(this);
        speedPanel.add(speedUnitComboBox, BorderLayout.EAST);

        mainPanel.add(speedPanel);

        // Animation mode
        modePanel = new JPanel(new BorderLayout());
        modePanel.add(new JLabel("Animation mode:"), BorderLayout.WEST);

        AnimationMode[] modi = { AnimationMode.LOOP, AnimationMode.STOP, AnimationMode.SWING };
        animationModeComboBox = new JComboBox(modi);
        animationModeComboBox.setPreferredSize(speedUnitComboBox.getPreferredSize());
        animationModeComboBox.addActionListener(this);
        modePanel.add(animationModeComboBox, BorderLayout.EAST);

        mainPanel.add(modePanel);
        panelList.add(this);

        this.setEnabled(false);
        this.setAdvanced(MoviePanel.isAdvanced);
    }

    /**
     * Override the setEnabled method in order to keep the containing
     * components' enabledState synced with the enabledState of this component.
     */
    @Override
    public void setEnabled(boolean enabled) {
        if (timedView == null) {
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

    /**
     * Returns the movie panel for the given view
     *
     * @return movie panel if available, else null
     */
    public static MoviePanel getMoviePanel(MovieView view) {
        if (view.getMaximumFrameNumber() <= 0) {
            return null;
        }

        for (MoviePanel moviePanel : panelList) {
            if (moviePanel.view != null && moviePanel.view.equals(view)) {
                return moviePanel;
            }
        }
        return null;
    }

    /**
     * Jumps to the specified frame
     *
     * @param frame
     *            the number of the frame
     */
    public void jumpToFrameNumber(int frame) {
        if (view instanceof CachedMovieView) {
            frame = Math.min(frame, view.getMaximumAccessibleFrameNumber());
        }
        timeSlider.setValue(frame);
        view.setCurrentFrame(frame, new ChangeEvent());
    }

    /**
     * Returns the current frame number
     *
     * @return the current frame number
     */
    public int getCurrentFrameNumber() {
        return view.getCurrentFrameNumber();
    }

    /**
     * Toggles between playing and not playing the animation.
     */
    public void togglePlayPause() {
        setPlaying(!isPlaying, false);
    }

    public void setPlaying(boolean playing, boolean onlyGUI) {

        isPlaying = playing;

        if (!isPlaying) {
            playPauseButton.setIcon(playIcon);
            playPauseButton.setToolTipText("Play movie");
            if (!onlyGUI) {
                view.pauseMovie();
                timeSlider.setValue(view.getCurrentFrameNumber());
            }
        } else {
            playPauseButton.setIcon(pauseIcon);
            playPauseButton.setToolTipText("Pause movie");
            if (!onlyGUI) {
                view.playMovie();
            }
        }

    }

    /**
     * Updates the speed of the animation. This function is called when changing
     * the speed of the animation or the its unit.
     */
    private void updateMovieSpeed() {
        if (speedUnitComboBox.getSelectedItem() == SpeedUnit.FRAMESPERSECOND) {
            view.setDesiredRelativeSpeed(((SpinnerNumberModel) speedSpinner.getModel()).getNumber().intValue());
        } else {
            timedView.setDesiredAbsoluteSpeed(((SpinnerNumberModel) speedSpinner.getModel()).getNumber().intValue() * ((SpeedUnit) speedUnitComboBox.getSelectedItem()).getSecondsPerSecond());
        }
    }

    /**
     * Locks or unlocks the movie, Should only be called by LayersModel
     *
     * In future developments, the concept of linked movies might either be
     * dropped (when introducing a global timestamp/timeline) or be moved to
     * LayersModel
     *
     * @param link
     *            true, if it should be locked, else false
     */
    public void setMovieLink(boolean link) {
        if (!link) {
            linkedMovieManager.unlinkMoviePanel(this);
        } else {
            linkedMovieManager.linkMoviePanel(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == advancedButton) {
            this.setAdvanced(!MoviePanel.isAdvanced);
            ImageViewerGui.getSingletonInstance().getMoviePanelContainer().updateActiveView();

            // Toggle play/pause
        } else if (e.getSource() == playPauseButton) {

            togglePlayPause();

            // Previous frame
        } else if (e.getSource() == previousFrameButton) {
            if (isPlaying) {
                togglePlayPause();
            }

            jumpToFrameNumber(getCurrentFrameNumber() - 1);

            // Next frame
        } else if (e.getSource() == nextFrameButton) {
            if (isPlaying) {
                togglePlayPause();
            }

            jumpToFrameNumber(getCurrentFrameNumber() + 1);
            // Change animation speed
        } else if (e.getSource() == ((JSpinner.DefaultEditor) speedSpinner.getEditor()).getTextField()) {
            try {
                speedSpinner.commitEdit();
            } catch (ParseException e1) {
                e1.printStackTrace();
            }

            linkedMovieManager.updateSpeedSpinnerLinkedMovies(this);

            updateMovieSpeed();

            // Change animation speed unit
        } else if (e.getSource() == speedUnitComboBox) {

            linkedMovieManager.updateSpeedUnitComboBoxLinkedMovies(this);
            updateMovieSpeed();

            // Change animation mode
        } else if (e.getSource() == animationModeComboBox) {

            linkedMovieManager.updateAnimationModeComboBoxLinkedMovies(this);
            view.setAnimationMode((AnimationMode) animationModeComboBox.getSelectedItem());
        }

    }

    // This is needed for the CardLayout
    @Override
    @SuppressWarnings("deprecation")
    public void show(boolean visible) {
        super.show(visible);
        this.setAdvanced(MoviePanel.isAdvanced);
        // update
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stateChanged(javax.swing.event.ChangeEvent e) {

        // Jump to different frame
        if (e.getSource() == timeSlider) {
            jumpToFrameNumber(timeSlider.getValue());
            frameNumberLabel.setText((getCurrentFrameNumber() + 1) + "/" + (timeSlider.getMaximum() + 1));
            if (getCurrentFrameNumber() == timeSlider.getMinimum() && animationModeComboBox.getSelectedItem() == AnimationMode.STOP) {
                togglePlayPause();
            }
            // Change animation speed
        } else if (e.getSource() == speedSpinner) {
            linkedMovieManager.updateSpeedSpinnerLinkedMovies(this);
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
        linkedMovieManager.someoneIsDragging = true;

        if (isPlaying) {
            view.pauseMovie();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (isEnabled()) {
            if (e.getWheelRotation() < 0) {
                jumpToFrameNumber(getCurrentFrameNumber() + 1);
            } else if (e.getWheelRotation() > 0) {
                jumpToFrameNumber(getCurrentFrameNumber() - 1);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (isPlaying) {
            view.playMovie();
        }

        linkedMovieManager.someoneIsDragging = false;
    }

    @Override
    public void viewChanged(View sender, ChangeEvent aEvent) {
        //Log.debug("Sender : " + sender);
        //Log.debug("Event : " + aEvent);
        // Stop movie, when the layer was removed.
        LayerChangedReason layerReason = aEvent.getLastChangedReasonByType(LayerChangedReason.class);

        if (layerReason != null && layerReason.getSubView().getAdapter(MovieView.class) == view && layerReason.getLayerChangeType() == LayerChangeType.LAYER_REMOVED) {
            linkedMovieManager.unlinkMoviePanel(this);
            panelList.remove(this);
            return;
        }

        // Update time slider and linked frames
        SubImageDataChangedReason subImageDataChangedReason = aEvent.getLastChangedReasonByTypeAndView(SubImageDataChangedReason.class, view);
        if (subImageDataChangedReason != null) {
            if (!isDragging) {
                //Thread.dumpStack();
                //Log.debug(view.getCurrentFrameNumber());
                timeSlider.setValue(view.getCurrentFrameNumber());
            }
        }

        // Update start-stop-button. In animation mode "stop", it has to change
        // when the movie stops after reaching the last frame.
        if (aEvent.reasonOccurred(PlayStateChangedReason.class)) {
            PlayStateChangedReason pscr = aEvent.getLastChangedReasonByType(PlayStateChangedReason.class);

            // check if the event belongs to the same group of linked movies
            if (timedView.getLinkedMovieManager() == pscr.getLinkedMovieManager()) {

                if (pscr.isPlaying() != isPlaying) {

                    if (!isDragging && !(linkedMovieManager.someoneIsDragging)) {

                        // only update GUI
                        // Log.debug("Switching to " + pscr.isPlaying());
                        setPlaying(pscr.isPlaying(), true);

                    } else {
                        // Log.debug("Not switching because of dragging");
                    }

                } else {
                    // Log.debug("Playstate already ok");
                }

            }
        }

        CacheStatusChangedReason cacheReason = aEvent.getLastChangedReasonByType(CacheStatusChangedReason.class);
        if (cacheReason != null && cacheReason.getView() == view) {
            switch (cacheReason.getType()) {
            case PARTIAL:
                timeSlider.setPartialCachedUntil(cacheReason.getValue());
                break;
            case COMPLETE:
                timeSlider.setCompleteCachedUntil(cacheReason.getValue());
                break;
            }
            timeSlider.repaint();
        }
    }

    /**
     * Abstract base class for all static movie actions.
     *
     * Static movie actions are supposed be integrated into {@link MenuBar},
     * also to provide shortcuts. They always refer to the active layer.
     */
    private static abstract class StaticMovieAction extends AbstractAction implements ActionListener, LayersListener {
        private static final long serialVersionUID = 1L;
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

            LayersModel.getSingletonInstance().addLayersListener(this);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void layerAdded(int idx) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void layerRemoved(View oldView, int oldIdx) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void layerChanged(int idx) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void activeLayerChanged(int idx) {
            this.searchCorrespondingMoviePanel(LayersModel.getSingletonInstance().getLayer(idx));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void viewportGeometryChanged() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void subImageDataChanged() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void timestampChanged(int idx) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void layerDownloaded(int idx) {
        }

        /**
         * Searches the movie panel corresponding to the given view.
         *
         * All static movie actions are performed by accessing the movie panel
         * of the active and basically clicking on the corresponding button.
         *
         * @param view
         *            View to search panel for
         */
        private void searchCorrespondingMoviePanel(View view) {
            if (view != null) {
                MovieView movieView = view.getAdapter(MovieView.class);
                if (movieView != null) {
                    setEnabled(true);

                    for (MoviePanel panel : panelList) {
                        if (panel.view == movieView) {
                            activePanel = panel;
                            return;
                        }
                    }
                }
            }
            setEnabled(false);
        }
    }

    /**
     * Action to play or pause the active layer, if it is an image series.
     *
     * Static movie actions are supposed be integrated into {@link MenuBar},
     * also to provide shortcuts. They always refer to the active layer.
     */
    public static class StaticPlayPauseAction extends StaticMovieAction {
        private static final long serialVersionUID = 1L;

        /**
         * Default constructor.
         */
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
        public void activeLayerChanged(int idx) {
            super.activeLayerChanged(idx);
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
        private static final long serialVersionUID = 1L;

        /**
         * Default constructor.
         */
        public StaticPreviousFrameAction() {
            super("Step to Previous Frame", IconBank.getIcon(JHVIcon.BACK));
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
        private static final long serialVersionUID = 1L;

        /**
         * Default constructor.
         */
        public StaticNextFrameAction() {
            super("Step to Next Frame", IconBank.getIcon(JHVIcon.FORWARD));
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
    private static class LinkedMovieManager {

        private final LinkedList<MoviePanel> linkedMovies = new LinkedList<MoviePanel>();
        public boolean someoneIsDragging = false;

        /**
         * Adds an image series to the set of series playing simultaneous.
         *
         * <p>
         * The master movie panel may change.
         *
         * @param newPanel
         *            Panel to add
         */
        public void linkMoviePanel(MoviePanel newPanel) {

            if (newPanel.timedView == null) {
                return;
            }

            newPanel.timedView.linkMovie();

            if (!linkedMovies.isEmpty()) {
                // Copy Settings
                MoviePanel copyFrom = linkedMovies.element();
                newPanel.isPlaying = copyFrom.isPlaying;
                newPanel.playPauseButton.setIcon(copyFrom.playPauseButton.getIcon());
                newPanel.speedSpinner.setValue(copyFrom.speedSpinner.getValue());
                newPanel.speedUnitComboBox.setSelectedItem(copyFrom.speedUnitComboBox.getSelectedItem());
                newPanel.animationModeComboBox.setSelectedItem(copyFrom.animationModeComboBox.getSelectedItem());

                // move frame
                ImmutableDateTime maxAvialableDateTime = newPanel.timedView.getFrameDateTime(newPanel.view.getMaximumAccessibleFrameNumber());

                if (maxAvialableDateTime.getMillis() >= copyFrom.timedView.getCurrentFrameDateTime().getMillis()) {
                    newPanel.timedView.setCurrentFrame(copyFrom.timedView.getCurrentFrameDateTime(), new ChangeEvent());

                } else {
                    newPanel.timedView.setCurrentFrame(newPanel.view.getMaximumAccessibleFrameNumber(), new ChangeEvent());
                }
            }

            linkedMovies.add(newPanel);
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
        public void unlinkMoviePanel(MoviePanel panel) {
            panel.timedView.unlinkMovie();
            linkedMovies.remove(panel);
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

                // g.fillRect(trackRect.x + partialCachedOffset, offset ,
                // trackRect.x + partialCachedO, offset + getSize().height / 8);

                g2d.setColor(partialCachedColor);
                g2d.drawLine(trackRect.x + completeCachedOffset, offset + getSize().height / 8, trackRect.x + partialCachedOffset, offset + getSize().height / 8);
                // g.fillRect(trackRect.x + completeCachedOffset, offset,
                // partialCachedOffset - completeCachedOffset, height);

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
