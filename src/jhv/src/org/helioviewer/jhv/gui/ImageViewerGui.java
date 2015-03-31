package org.helioviewer.jhv.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.AbstractList;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import org.helioviewer.base.message.Message;
import org.helioviewer.gl3d.camera.GL3DCameraOptionsPanel;
import org.helioviewer.gl3d.gui.GL3DCameraSelectorModel;
import org.helioviewer.gl3d.gui.GL3DTopToolBar;
import org.helioviewer.jhv.JHVSplashScreen;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.actions.ExitProgramAction;
import org.helioviewer.jhv.gui.components.ControlPanelContainer;
import org.helioviewer.jhv.gui.components.ImageSelectorPanel;
import org.helioviewer.jhv.gui.components.MainContentPanel;
import org.helioviewer.jhv.gui.components.MainImagePanel;
import org.helioviewer.jhv.gui.components.MenuBar;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.gui.components.SideContentPane;
import org.helioviewer.jhv.gui.components.StatusPanel;
import org.helioviewer.jhv.gui.components.TopToolBar;
import org.helioviewer.jhv.gui.components.statusplugins.FramerateStatusPanel;
import org.helioviewer.jhv.gui.components.statusplugins.ZoomStatusPanel;
import org.helioviewer.jhv.gui.controller.ZoomController;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialog;
import org.helioviewer.jhv.gui.filters.ChannelMixerPanel;
import org.helioviewer.jhv.gui.filters.ContrastPanel;
import org.helioviewer.jhv.gui.filters.GammaCorrectionPanel;
import org.helioviewer.jhv.gui.filters.OpacityPanel;
import org.helioviewer.jhv.gui.filters.RunningDifferencePanel;
import org.helioviewer.jhv.gui.filters.SOHOLUTPanel;
import org.helioviewer.jhv.gui.filters.SharpenPanel;
import org.helioviewer.jhv.gui.states.GuiState;
import org.helioviewer.jhv.gui.states.State;
import org.helioviewer.jhv.gui.states.StateController;
import org.helioviewer.jhv.gui.states.StateController.StateChangeListener;
import org.helioviewer.jhv.gui.states.ViewStateEnum;
import org.helioviewer.jhv.io.APIRequestManager;
import org.helioviewer.jhv.io.CommandLineProcessor;
import org.helioviewer.jhv.io.FileDownloader;
import org.helioviewer.jhv.io.JHVRequest;
import org.helioviewer.viewmodel.metadata.ImageSizeMetaData;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.ImageInfoView;
import org.helioviewer.viewmodel.view.LayeredView;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.MovieView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodelplugin.filter.FilterTabPanelManager;

/**
 * A class that sets up the graphical user interface.
 *
 * @author caplins
 * @author Benjamin Wamsler
 * @author Alen Agheksanterian
 * @author Stephan Pagel
 * @author Markus Langenberg
 * @author Andre Dau
 *
 */
public class ImageViewerGui {

    /** The sole instance of this class. */
    private static final ImageViewerGui singletonImageViewer = new ImageViewerGui();

    private static JFrame mainFrame;
    private JPanel contentPanel;
    private JSplitPane midSplitPane;
    private JScrollPane leftScrollPane;

    private MainContentPanel mainContentPanel;
    protected MainImagePanel mainImagePanel;

    private SideContentPane leftPane;
    private ImageSelectorPanel imageSelectorPanel;
    private MoviePanel moviePanel;
    private ControlPanelContainer moviePanelContainer;
    private ControlPanelContainer filterPanelContainer;
    private final JMenuBar menuBar;

    private GL3DCameraOptionsPanel cameraOptionsPanel;

    public static final int SIDE_PANEL_WIDTH = 320;
    public static final int SIDE_PADDING = 10;
    private final ObservationDialog observationDialog;

    private final GL3DTopToolBar topToolBar;

    private ComponentView mainComponentView;

    private FilterTabPanelManager filterTabPanelManager;

    /**
     * The private constructor that creates and positions all the gui
     * components.
     */
    private ImageViewerGui() {
        StateController.getInstance().addStateChangeListener(new StateChangeListener() {
            @Override
            public void stateChanged(State newState, State oldState, StateController stateController) {
                activateState(newState, oldState);
            }
        });

        mainFrame = createMainFrame();
        observationDialog = new ObservationDialog(mainFrame);
        menuBar = new MenuBar();
        menuBar.setFocusable(false);

        mainFrame.setJMenuBar(menuBar);
        mainFrame.setFocusable(true);
        topToolBar = new GL3DTopToolBar();
    }

    public void prepareGui() {
        if (contentPanel == null) {
            contentPanel = new JPanel(new BorderLayout());
            mainFrame.setContentPane(contentPanel);

            midSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false);
            midSplitPane.setOneTouchExpandable(false);

            contentPanel.add(midSplitPane, BorderLayout.CENTER);

            mainContentPanel = new MainContentPanel();
            mainContentPanel.setMainComponent(getMainImagePanel());

            // STATE - GET TOP TOOLBAR
            contentPanel.add(getTopToolBar(), BorderLayout.PAGE_START);

            // // FEATURES / EVENTS
            // solarEventCatalogsPanel = new SolarEventCatalogsPanel();
            // leftPane.add("Features/Events", solarEventCatalogsPanel, false);

            // STATE - GET LEFT PANE
            leftScrollPane = new JScrollPane(getLeftContentPane(), JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            leftScrollPane.setFocusable(false);
            leftScrollPane.getVerticalScrollBar().setUnitIncrement(10);
            midSplitPane.setLeftComponent(leftScrollPane);

            midSplitPane.setRightComponent(mainContentPanel);

            // ///////////////////////////////////////////////////////////////////////////////
            // STATUS PANEL
            // ///////////////////////////////////////////////////////////////////////////////

            ZoomStatusPanel zoomStatusPanel = ZoomStatusPanel.getSingletonInstance();
            FramerateStatusPanel framerateStatus = FramerateStatusPanel.getSingletonInstance();

            // PositionStatusPanel positionStatusPanel = PositionStatusPanel.getSingletonInstance();
            // getMainImagePanel().addPlugin(positionStatusPanel);

            StatusPanel statusPanel = new StatusPanel(SIDE_PANEL_WIDTH + 20, 5);
            statusPanel.addPlugin(zoomStatusPanel, StatusPanel.Alignment.LEFT);
            statusPanel.addPlugin(framerateStatus, StatusPanel.Alignment.LEFT);
            // statusPanel.addPlugin(positionStatusPanel, StatusPanel.Alignment.RIGHT);

            contentPanel.add(statusPanel, BorderLayout.PAGE_END);
        }
    }

    /**
     * Packs, positions and shows the GUI
     *
     * @param _show
     *            If GUI should be displayed.
     */
    public void packAndShow(final boolean _show) {

        final JHVSplashScreen splash = JHVSplashScreen.getSingletonInstance();

        // load images which should be displayed first in a separated thread
        // that splash screen will be updated
        splash.setProgressText("Loading Images...");

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                loadImagesAtStartup();

                // show GUI
                splash.setProgressText("Starting JHelioviewer...");
                splash.nextStep();
                mainFrame.pack();
                mainFrame.setLocationRelativeTo(null);
                mainFrame.setVisible(_show);
                splash.setProgressValue(100);

                // remove splash screen
                splash.dispose();
                // splash.setVisible(false);
            }
        }, "LoadImagesOnStartUp");
        thread.start();
    }

    /**
     * Initializes the main view chain.
     */
    public void createViewchains() {
        State newState = StateController.getInstance().getCurrentState();
        mainComponentView = GuiState.viewchainFactory.createNewViewchainMain();
        GL3DCameraSelectorModel.getInstance().activate();

        // prepare gui again
        updateComponentPanels();
        mainImagePanel.setInputController(newState.getDefaultInputController());
        mainComponentView.activate();
        mainFrame.validate();

        this.activateState(newState, null);
        packAndShow(true);
        mainFrame.validate();
    }

    /**
     * Method that creates and initializes the main JFrame.
     *
     * @return the created and initialized main frame.
     */
    private JFrame createMainFrame() {
        JFrame frame = new JFrame("ESA JHelioviewer v2");

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent arg0) {
                ExitProgramAction exitAction = new ExitProgramAction();
                exitAction.actionPerformed(new ActionEvent(this, 0, ""));
            }
        });

        Dimension maxSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getSize();
        Dimension minSize = new Dimension(800, 600);

        minSize.width = Math.min(minSize.width, maxSize.width);
        minSize.height = Math.min(minSize.height, maxSize.height);
        frame.setMinimumSize(minSize);
        frame.setPreferredSize(new Dimension(maxSize.width - 100, maxSize.height - 100));
        enableFullScreen(frame);
        frame.setFont(new Font("SansSerif", Font.BOLD, 12));
        return frame;
    }

    private static void enableFullScreen(Window window) {
        if (System.getProperty("jhv.os").equals("mac")) {
            try {
                Class<?> fullScreenUtilities = Class.forName("com.apple.eawt.FullScreenUtilities");
                Method setWindowCanFullScreen = fullScreenUtilities.getMethod("setWindowCanFullScreen", Window.class, boolean.class);
                setWindowCanFullScreen.invoke(fullScreenUtilities, window, true);
            } catch (Exception e) {
                throw new RuntimeException("FullScreen utilities not available", e);
            }
        }
    }

    /**
     * Returns instance of the main ComponentView.
     *
     * @return instance of the main ComponentView.
     */
    public ComponentView getMainView() {
        return mainComponentView;
    }

    /**
     * Returns the scrollpane containing the left content pane.
     *
     * @return instance of the scrollpane containing the left content pane.
     * */
    public SideContentPane getLeftContentPane() {
        // ////////////////////////////////////////////////////////////////////////////////
        // LEFT CONTROL PANEL
        // ////////////////////////////////////////////////////////////////////////////////

        if (leftPane != null) {
            return leftPane;
        } else {
            leftPane = new SideContentPane();

            // Movie control
            moviePanelContainer = new ControlPanelContainer();
            moviePanel = new MoviePanel();
            moviePanelContainer.setDefaultPanel(moviePanel);

            leftPane.add("Movie Controls", moviePanelContainer, true);

            // Layer control
            imageSelectorPanel = new ImageSelectorPanel();

            //leftPane.add("Image Layers", imageSelectorPanel, false);

            // Image adjustments and filters
            filterTabPanelManager = new FilterTabPanelManager();
            getFilterTabPanelManager().add(new OpacityPanel());
            getFilterTabPanelManager().add(new SOHOLUTPanel());
            getFilterTabPanelManager().add(new GammaCorrectionPanel());
            getFilterTabPanelManager().add(new ContrastPanel());
            getFilterTabPanelManager().add(new SharpenPanel());
            getFilterTabPanelManager().add(new ChannelMixerPanel());
            RunningDifferencePanel runningDifferencePanel = new RunningDifferencePanel();
            getFilterTabPanelManager().addAbstractFilterPanel(runningDifferencePanel);

            JPanel compactPanel = getFilterTabPanelManager().createCompactPanel();

            JPanel tab = new JPanel(new BorderLayout());
            tab.add(runningDifferencePanel, BorderLayout.NORTH);

            tab.add(compactPanel, BorderLayout.CENTER);

            tab.setEnabled(true);

            filterPanelContainer = new ControlPanelContainer();
            filterPanelContainer.setDefaultPanel(tab);

            //leftPane.add("Image Adjustments", filterPanelContainer, false);
            leftPane.add("Layers", Displayer.getRenderableContainerPanel(), true);

            JTabbedPane cameraTab = new JTabbedPane();
            cameraOptionsPanel = new GL3DCameraOptionsPanel();
            cameraTab.addTab("Camera Adjustments", cameraOptionsPanel);
            cameraTab.setEnabled(false);
            // leftPane.add("Camera Options", cameraOptionsPanel, false);

            // JTabbedPane planetTab = new JTabbedPane();
            // PlanetOptionsPanel planetOptionsPanel = new PlanetOptionsPanel();
            // planetTab.addTab("Planet Options", planetOptionsPanel);
            // planetTab.setEnabled(false);
            // leftPane.add("Object Options", planetOptionsPanel, false);
            return leftPane;
        }
    }

    /**
     * Returns the instance of the ImageSelectorPanel.
     *
     * @return instance of the image selector panel.
     * */
    public ImageSelectorPanel getImageSelectorPanel() {
        return imageSelectorPanel;
    }

    /**
     * @return the movie panel container
     */
    public ControlPanelContainer getMoviePanelContainer() {
        return moviePanelContainer;
    }

    /**
     * @return the filter panel container
     */
    public ControlPanelContainer getFilterPanelContainer() {
        return filterPanelContainer;
    }

    /**
     * @return the menu bar of jhv
     */
    public JMenuBar getMenuBar() {
        return menuBar;
    }

    public MainImagePanel getMainImagePanel() {
        if (mainImagePanel == null) {
            mainImagePanel = new MainImagePanel();
            mainImagePanel.setAutoscrolls(true);
            mainImagePanel.setFocusable(false);
        }

        return mainImagePanel;
    }

    public TopToolBar getTopToolBar() {
        return this.topToolBar;
    }

    /**
     * Change the current state
     *
     * @param stateEnum
     */
    private void activateState(final State newState, State oldState) {
        if (newState.getType() == ViewStateEnum.View3D) {
            leftPane.add("Camera Adjustments", cameraOptionsPanel, false);
        } else {
            leftPane.remove(GL3DCameraOptionsPanel.class);
        }

        this.getTopToolBar().updateStateButtons();

    }

    private void updateComponentPanels() {
        if (getMainView() != null) {
            getMainImagePanel().setView(getMainView());
        }
        mainFrame.validate();
    }

    /**
     * Loads the images which have to be displayed when the program starts.
     *
     * If there are any images defined in the command line, than this messages
     * tries to load this images. Otherwise it tries to load a default image
     * which is defined by the default entries of the observation panel.
     * */
    public void loadImagesAtStartup() {
        // get values for different command line options
        AbstractList<JHVRequest> jhvRequests = CommandLineProcessor.getJHVOptionValues();
        AbstractList<URI> jpipUris = CommandLineProcessor.getJPIPOptionValues();
        AbstractList<URI> downloadAddresses = CommandLineProcessor.getDownloadOptionValues();
        AbstractList<URI> jpxUrls = CommandLineProcessor.getJPXOptionValues();

        // Do nothing if no resource is specified
        if (jhvRequests.isEmpty() && jpipUris.isEmpty() && downloadAddresses.isEmpty() && jpxUrls.isEmpty()) {
            return;
        }

        // //////////////////////
        // -jhv
        // //////////////////////

        // go through all jhv values
        for (JHVRequest jhvRequest : jhvRequests) {
            try {
                for (int layer = 0; layer < jhvRequest.imageLayers.length; ++layer) {
                    // load image and memorize corresponding view
                    ImageInfoView imageInfoView = APIRequestManager.requestAndOpenRemoteFile(true, jhvRequest.cadence, jhvRequest.startTime, jhvRequest.endTime, jhvRequest.imageLayers[layer].observatory, jhvRequest.imageLayers[layer].instrument, jhvRequest.imageLayers[layer].detector, jhvRequest.imageLayers[layer].measurement, true);
                    if (imageInfoView != null && getMainView() != null) {
                        // get the layered view
                        LayeredView layeredView = getMainView().getAdapter(LayeredView.class);

                        // go through all sub view chains of the layered
                        // view and try to find the
                        // view chain of the corresponding image info view
                        for (int i = 0; i < layeredView.getNumLayers(); i++) {
                            View subView = layeredView.getLayer(i);

                            // if view has been found
                            if (imageInfoView.equals(subView.getAdapter(ImageInfoView.class))) {

                                // Set the correct image scale
                                ImageSizeMetaData imageSizeMetaData = (ImageSizeMetaData) imageInfoView.getAdapter(MetaDataView.class).getMetaData();
                                ZoomController zoomController = new ZoomController();
                                zoomController.zoom(ImageViewerGui.getSingletonInstance().getMainView(), imageSizeMetaData.getUnitsPerPixel() / (jhvRequest.imageScale * 1000.0));

                                // Lock movie
                                if (jhvRequest.linked) {
                                    MovieView movieView = subView.getAdapter(MovieView.class);
                                    if (movieView != null && movieView.getMaximumFrameNumber() > 0) {
                                        MoviePanel moviePanel = MoviePanel.getMoviePanel(movieView);
                                        if (moviePanel == null) {
                                            throw new InvalidViewException();
                                        }
                                        moviePanel.setMovieLink(true);
                                    }
                                }

                                break;
                            }
                        }
                    }
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                Message.err("An error occured while opening the remote file!", e.getMessage(), false);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            } catch (InvalidViewException e) {
                e.printStackTrace();
            }

        }

        // //////////////////////
        // -jpx
        // //////////////////////

        for (URI jpxUrl : jpxUrls) {
            if (jpxUrl != null) {
                try {
                    ImageInfoView imageInfoView = APIRequestManager.newLoad(jpxUrl, true, null);
                    if (imageInfoView != null && getMainView() != null) {
                        // get the layered view
                        LayeredView layeredView = getMainView().getAdapter(LayeredView.class);

                        // go through all sub view chains of the layered
                        // view and try to find the
                        // view chain of the corresponding image info view
                        for (int i = 0; i < layeredView.getNumLayers(); i++) {
                            View subView = layeredView.getLayer(i);

                            // if view has been found
                            if (imageInfoView.equals(subView.getAdapter(ImageInfoView.class))) {
                                MovieView movieView = subView.getAdapter(MovieView.class);
                                MoviePanel moviePanel = MoviePanel.getMoviePanel(movieView);
                                if (moviePanel == null) {
                                    throw new InvalidViewException();
                                }
                                moviePanel.setMovieLink(true);
                                break;
                            }
                        }
                    }
                } catch (IOException e) {
                    Message.err("An error occured while opening the remote file!", e.getMessage(), false);
                } catch (InvalidViewException e) {
                    e.printStackTrace();
                }
            }
        }
        // //////////////////////
        // -jpip
        // //////////////////////

        for (URI jpipUri : jpipUris) {
            if (jpipUri != null) {
                try {
                    APIRequestManager.newLoad(jpipUri, true, null);
                } catch (IOException e) {
                    Message.err("An error occured while opening the remote file!", e.getMessage(), false);
                }
            }
        }
        // //////////////////////
        // -download
        // //////////////////////

        for (URI downloadAddress : downloadAddresses) {
            if (downloadAddress != null) {
                try {
                    FileDownloader fileDownloader = new FileDownloader();
                    File downloadFile = fileDownloader.getDefaultDownloadLocation(downloadAddress);
                    fileDownloader.get(downloadAddress, downloadFile);
                    APIRequestManager.newLoad(downloadFile.toURI(), true, null);
                } catch (IOException e) {
                    Message.err("An error occured while opening the remote file!", e.getMessage(), false);
                }
            }
        }
    }

    /**
     * Returns the only instance of this class.
     *
     * @return the only instance of this class.
     * */
    public static ImageViewerGui getSingletonInstance() {
        return singletonImageViewer;
    }

    /**
     * Returns the main frame.
     *
     * @return the main frame.
     * */
    public static JFrame getMainFrame() {
        return mainFrame;
    }

    /**
     * Returns the scrollpane containing the left content pane.
     *
     * @return instance of the scrollpane containing the left content pane.
     * */
    public JScrollPane getLeftScrollPane() {
        return leftScrollPane;
    }

    /**
     * Toggles the visibility of the control panel on the left side.
     */
    public void toggleShowSidePanel() {
        leftScrollPane.setVisible(!leftScrollPane.isVisible());
        contentPanel.revalidate();

        int lastLocation = midSplitPane.getLastDividerLocation();
        if (lastLocation > 10) {
            midSplitPane.setDividerLocation(lastLocation);
        } else {
            midSplitPane.setDividerLocation(SIDE_PANEL_WIDTH);
        }
    }

    /**
     * Returns the content panel of JHV
     *
     * @return The content panel of JHV
     */
    public JPanel getContentPane() {
        return contentPanel;
    }

    public final MainContentPanel getMainContentPanel() {
        return mainContentPanel;
    }

    public boolean viewchainCreated() {
        return getMainView() != null;
    }

    public ObservationDialog getObservationDialog() {
        return this.observationDialog;
    }

    public FilterTabPanelManager getFilterTabPanelManager() {
        return filterTabPanelManager;
    }

}
