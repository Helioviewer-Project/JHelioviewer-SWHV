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
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.actions.ExitProgramAction;
import org.helioviewer.jhv.gui.components.ControlPanelContainer;
import org.helioviewer.jhv.gui.components.MainContentPanel;
import org.helioviewer.jhv.gui.components.MainImagePanel;
import org.helioviewer.jhv.gui.components.MenuBar;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.gui.components.SideContentPane;
import org.helioviewer.jhv.gui.components.StatusPanel;
import org.helioviewer.jhv.gui.components.TopToolBar;
import org.helioviewer.jhv.gui.components.statusplugins.FramerateStatusPanel;
import org.helioviewer.jhv.gui.components.statusplugins.PositionStatusPanel;
import org.helioviewer.jhv.gui.components.statusplugins.ZoomStatusPanel;
import org.helioviewer.jhv.gui.dialogs.observation.ImageDataPanel;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialog;
import org.helioviewer.jhv.io.APIRequestManager;
import org.helioviewer.jhv.io.CommandLineProcessor;
import org.helioviewer.jhv.io.FileDownloader;
import org.helioviewer.jhv.io.JHVRequest;
import org.helioviewer.viewmodel.view.AbstractView;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
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

    public static final int SIDE_PANEL_WIDTH = 320;
    public static final int SIDE_PADDING = 10;

    private static ImageViewerGui instance;

    private static JFrame mainFrame;
    private static JSplitPane midSplitPane;
    private static JScrollPane leftScrollPane;

    private static SideContentPane leftPane;
    private static MoviePanel moviePanel;
    private static ControlPanelContainer moviePanelContainer;

    private static MainContentPanel mainContentPanel;
    private static MainImagePanel mainImagePanel;

    private static ObservationDialog observationDialog;

    private static ComponentView mainComponentView;
    private static ImageDataPanel imageObservationPanel;

    private ImageViewerGui() {
    }

    public static ImageViewerGui getSingletonInstance() {
        if (instance == null) {
            prepareGui();
            instance = new ImageViewerGui();
        }
        return instance;
    }

    public static void prepareGui() {
        mainFrame = createMainFrame();

        JMenuBar menuBar = new MenuBar();
        mainFrame.setJMenuBar(menuBar);
        observationDialog = new ObservationDialog(mainFrame);

        JPanel contentPanel = new JPanel(new BorderLayout());
        mainFrame.setContentPane(contentPanel);

        midSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false);
        midSplitPane.setOneTouchExpandable(false);
        contentPanel.add(midSplitPane, BorderLayout.CENTER);

        mainImagePanel = new MainImagePanel();
        mainImagePanel.setAutoscrolls(true);
        mainImagePanel.setFocusable(false);

        mainContentPanel = new MainContentPanel();
        mainContentPanel.setMainComponent(mainImagePanel);

        TopToolBar topToolBar = new TopToolBar();
        contentPanel.add(topToolBar, BorderLayout.PAGE_START);

        leftPane = new SideContentPane();
        // Movie control
        moviePanelContainer = new ControlPanelContainer();
        moviePanel = new MoviePanel();
        moviePanelContainer.setDefaultPanel(moviePanel);
        leftPane.add("Movie Controls", moviePanelContainer, true);
        // Layer control
        imageObservationPanel = new ImageDataPanel();
        observationDialog.addUserInterface("Image data", imageObservationPanel);
        leftPane.add("Image Layers", Displayer.getRenderableContainerPanel(), true);

        leftScrollPane = new JScrollPane(leftPane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        leftScrollPane.setFocusable(false);
        leftScrollPane.getVerticalScrollBar().setUnitIncrement(10);

        midSplitPane.setLeftComponent(leftScrollPane);
        midSplitPane.setRightComponent(mainContentPanel);

        // STATUS PANEL
        ZoomStatusPanel zoomStatusPanel = ZoomStatusPanel.getSingletonInstance();
        FramerateStatusPanel framerateStatus = FramerateStatusPanel.getSingletonInstance();
        PositionStatusPanel positionStatusPanel = PositionStatusPanel.getSingletonInstance();
        mainImagePanel.addPlugin(positionStatusPanel);

        StatusPanel statusPanel = new StatusPanel(SIDE_PANEL_WIDTH + 20, 5);
        statusPanel.addPlugin(zoomStatusPanel, StatusPanel.Alignment.LEFT);
        statusPanel.addPlugin(framerateStatus, StatusPanel.Alignment.LEFT);
        statusPanel.addPlugin(positionStatusPanel, StatusPanel.Alignment.RIGHT);

        contentPanel.add(statusPanel, BorderLayout.PAGE_END);

        mainComponentView = new ComponentView();
        mainImagePanel.setView(mainComponentView);
        mainComponentView.activate();

        mainFrame.pack();
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
    }

    private static JFrame createMainFrame() {
        JFrame frame = new JFrame("ESA JHelioviewer NT");

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
        frame.setFont(new Font("SansSerif", Font.BOLD, 12));
        enableFullScreen(frame);

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

    public void loadAtStart() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                loadImagesAtStartup();
            }
        }, "LoadImagesOnStartUp");
        thread.start();
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

        // -jhv
        // go through all jhv values
        for (JHVRequest jhvRequest : jhvRequests) {
            try {
                for (int layer = 0; layer < jhvRequest.imageLayers.length; ++layer) {
                    // load image and memorize corresponding view
                    AbstractView view = APIRequestManager.requestAndOpenRemoteFile(true, jhvRequest.cadence, jhvRequest.startTime, jhvRequest.endTime, jhvRequest.imageLayers[layer].observatory, jhvRequest.imageLayers[layer].instrument, jhvRequest.imageLayers[layer].detector, jhvRequest.imageLayers[layer].measurement, true);
                    if (view != null && getMainView() != null) {
                        // get the layered view

                        // go through all sub view chains of the layered
                        // view and try to find the
                        // view chain of the corresponding image info view
                        for (int i = 0; i < Displayer.getLayersModel().getNumLayers(); i++) {
                            AbstractView subView = Displayer.getLayersModel().getLayer(i);

                            // if view has been found
                            if (view.equals(subView)) {
                                // Lock movie
                                if (jhvRequest.linked) {
                                    if (subView instanceof JHVJPXView && ((JHVJPXView) subView).getMaximumFrameNumber() > 0) {
                                        MoviePanel moviePanel = MoviePanel.getMoviePanel((JHVJPXView) subView);
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

        // -jpx
        for (URI jpxUrl : jpxUrls) {
            if (jpxUrl != null) {
                try {
                    AbstractView view = APIRequestManager.newLoad(jpxUrl, true);
                    if (view != null && getMainView() != null) {

                        // go through all sub view chains of the layered
                        // view and try to find the
                        // view chain of the corresponding image info view
                        for (int i = 0; i < Displayer.getLayersModel().getNumLayers(); i++) {
                            AbstractView subView = Displayer.getLayersModel().getLayer(i);

                            // if view has been found
                            if (view.equals(subView) && subView instanceof JHVJPXView) {
                                JHVJPXView movieView = (JHVJPXView) subView;
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

        // -jpip
        for (URI jpipUri : jpipUris) {
            if (jpipUri != null) {
                try {
                    APIRequestManager.newLoad(jpipUri, true);
                } catch (IOException e) {
                    Message.err("An error occured while opening the remote file!", e.getMessage(), false);
                }
            }
        }

        // -download
        for (URI downloadAddress : downloadAddresses) {
            if (downloadAddress != null) {
                try {
                    FileDownloader fileDownloader = new FileDownloader();
                    File downloadFile = fileDownloader.getDefaultDownloadLocation(downloadAddress);
                    fileDownloader.get(downloadAddress, downloadFile);
                    APIRequestManager.newLoad(downloadFile.toURI(), true);
                } catch (IOException e) {
                    Message.err("An error occured while opening the remote file!", e.getMessage(), false);
                }
            }
        }
    }

    /**
     * Toggles the visibility of the control panel on the left side.
     */
    public void toggleShowSidePanel() {
        leftScrollPane.setVisible(!leftScrollPane.isVisible());

        int lastLocation = midSplitPane.getLastDividerLocation();
        if (lastLocation > 10) {
            midSplitPane.setDividerLocation(lastLocation);
        } else {
            midSplitPane.setDividerLocation(SIDE_PANEL_WIDTH);
        }
    }

    public static JFrame getMainFrame() {
        return mainFrame;
    }

    public SideContentPane getLeftContentPane() {
        return leftPane;
    }

    public final MainContentPanel getMainContentPanel() {
        return mainContentPanel;
    }

    public ObservationDialog getObservationDialog() {
        return this.observationDialog;
    }

    public ImageDataPanel getObservationImagePane() {
        return imageObservationPanel;
    }

    public ControlPanelContainer getMoviePanelContainer() {
        return moviePanelContainer;
    }

    public MainImagePanel getMainImagePanel() {
        return mainImagePanel;
    }

    public ComponentView getMainView() {
        return mainComponentView;
    }

}
