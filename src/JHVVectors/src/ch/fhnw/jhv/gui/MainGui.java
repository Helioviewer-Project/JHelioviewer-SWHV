/**
 * 
 */
package ch.fhnw.jhv.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import ch.fhnw.jhv.gui.components.CollapsiblePane;
import ch.fhnw.jhv.gui.components.CollapsiblePane.CollapsiblePaneResizeListener;
import ch.fhnw.jhv.gui.components.MoviePanel;
import ch.fhnw.jhv.gui.components.PluginChooserControlPlugin;
import ch.fhnw.jhv.gui.components.controller.AnimatorController;
import ch.fhnw.jhv.gui.components.controller.TimeDimensionManager;
import ch.fhnw.jhv.gui.viewport.ViewPort;
import ch.fhnw.jhv.plugins.PluginBundle.PluginBundleType;
import ch.fhnw.jhv.plugins.PluginManager;

/**
 * Main Gui contains all the GUI components. The sizes are defined here.
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public class MainGui implements PluginChooserControlPlugin.Listener, CollapsiblePaneResizeListener {

    /**
     * Size of the left pane
     */
    private final static int LEFT_PANE_WIDTH = 320;

    /**
     * Title of the application
     */
    private final static String TITLE = "JHVVectors";

    /**
     * Start width screen Size (also defined as minimum size)
     */
    private final static int SCREEN_WIDTH = 1024;

    /**
     * Start height screen Size (also defined as minimum size)
     */
    private final static int SCREEN_HEIGHT = 750;

    /**
     * Space between each ControlPlugin
     */
    private final static int CONTROLPLUGIN_SPACE_PIXELS = 40;

    /**
     * JFrame containg all the subcomponents
     */
    private JFrame frame;

    /**
     * Scrollable left pane contains the leftPanel
     */
    private JScrollPane scrollableLeftPanel;

    /**
     * This left panel contains all the ControlPlugins
     */
    private JPanel leftPanel;

    /**
     * ViewPort which is responsible for rendering
     */
    private ViewPort viewPort;

    /**
     * Plugin Manager is managing all the plugins
     */
    private PluginManager pluginManager;

    /**
     * MoviePanel is used to steer the time dimensions
     */
    private MoviePanel moviePanel;

    /**
     * Plugin Chooser ControlPlugin is always displayed and has to be loaded
     * directly and not over the PluginManager
     */
    private PluginChooserControlPlugin pluginChooserControlPlugin;

    /**
     * Collapsable Pane
     */
    private Map<CollapsiblePane, Dimension> collapsePaneMap = new HashMap<CollapsiblePane, Dimension>();

    /**
     * Constructor
     */
    public MainGui() {
        AnimatorController animator = new AnimatorController();
        TimeDimensionManager.getInstance().setAnimatorController(animator);
        moviePanel = new MoviePanel();
        moviePanel.setAnimator(animator);

        viewPort = ViewPort.getInstance();
        viewPort.createAnimator(SCREEN_WIDTH, SCREEN_HEIGHT, animator);

        pluginManager = PluginManager.getInstance();
        pluginManager.setLeftPaneSize(LEFT_PANE_WIDTH);

        frame = new JFrame(TITLE);
        frame.setSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));

        // So the leftPanel gets always resized correctly
        frame.addComponentListener(new java.awt.event.ComponentAdapter() {

            public void componentResized(ComponentEvent event) {
                leftPanel.setPreferredSize(new Dimension(LEFT_PANE_WIDTH, frame.getHeight()));
            }
        });

        pluginChooserControlPlugin = new PluginChooserControlPlugin();
        pluginChooserControlPlugin.addListener(this);

        leftPanel = new JPanel();
        leftPanel.setPreferredSize(new Dimension(LEFT_PANE_WIDTH, SCREEN_HEIGHT));

        loadLeftPane();

        scrollableLeftPanel = new JScrollPane(leftPanel);
        scrollableLeftPanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scrollableLeftPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollableLeftPanel.setPreferredSize(new Dimension(LEFT_PANE_WIDTH, SCREEN_HEIGHT));

        frame.add(scrollableLeftPanel, BorderLayout.WEST);
        frame.add(viewPort, BorderLayout.CENTER);

        frame.pack();
        frame.setVisible(true);

        // Start animator
        viewPort.start();
    }

    /**
     * Load all components for the leftPanel.
     * 
     * Order: - PluginChooser - MoviePanel - Get all other ControlPlugins over
     * the PluginManager for the current loaded plugin
     * 
     */
    public void loadLeftPane() {

        // remove all old components
        leftPanel.removeAll();
        leftPanel.validate();

        CollapsiblePane panePluginChooser = new CollapsiblePane(pluginChooserControlPlugin.getTitle(), pluginChooserControlPlugin.getComponent(), pluginChooserControlPlugin.shouldStartExpanded(), LEFT_PANE_WIDTH);
        panePluginChooser.addListener(this);

        CollapsiblePane paneMoviePanel = new CollapsiblePane("Movie Panel", moviePanel, false, LEFT_PANE_WIDTH);
        paneMoviePanel.addListener(this);

        leftPanel.add(panePluginChooser);
        leftPanel.add(paneMoviePanel);

        leftPanel.add(PluginManager.getInstance().getControlPanels(this));

        leftPanel.validate();

        frame.pack();
    }

    /**
     * Listener Method if a new plugin has been loaded. Load the new
     * ControlPlugins in the leftPanel.
     * 
     * @param type
     *            the plugin type
     */
    public void pluginChanged(PluginBundleType type) {
        // clear the map
        collapsePaneMap.clear();

        loadLeftPane();
    }

    /**
     * One of the collapsible panes got resized. We have to get notified because
     * of the Scrollbar on the left side.
     * 
     * @param pane
     *            CollapsiblePane
     * @param size
     *            Dimension
     * 
     */
    public void gotResized(CollapsiblePane pane, Dimension size) {

        boolean shouldAdjustHeight = collapsePaneMap.containsKey(pane);

        collapsePaneMap.put(pane, size);

        int heightSum = 0;

        for (Map.Entry<CollapsiblePane, Dimension> entry : collapsePaneMap.entrySet()) {
            heightSum += entry.getValue().height;
        }

        if (shouldAdjustHeight) {

            leftPanel.setPreferredSize(new Dimension(leftPanel.getPreferredSize().width, heightSum + CONTROLPLUGIN_SPACE_PIXELS));

            // Handle the Scrollbar manually because the
            // VERTICAL_SCROLLBAR_AS_NEEDED
            // didn't work properly..
            if ((heightSum + CONTROLPLUGIN_SPACE_PIXELS) > frame.getHeight()) {
                scrollableLeftPanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            } else {
                scrollableLeftPanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            }
        }
    }
}
