package org.helioviewer.jhv.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Method;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.Interaction;
import org.helioviewer.jhv.camera.InteractionAnnotate;
import org.helioviewer.jhv.camera.InteractionAxis;
import org.helioviewer.jhv.camera.InteractionPan;
import org.helioviewer.jhv.camera.InteractionRotate;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.actions.ExitProgramAction;
import org.helioviewer.jhv.gui.components.MainContentPanel;
import org.helioviewer.jhv.gui.components.MenuBar;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.gui.components.SideContentPane;
import org.helioviewer.jhv.gui.components.StatusPanel;
import org.helioviewer.jhv.gui.components.TopToolBar;
import org.helioviewer.jhv.gui.components.statusplugins.CarringtonStatusPanel;
import org.helioviewer.jhv.gui.components.statusplugins.FramerateStatusPanel;
import org.helioviewer.jhv.gui.components.statusplugins.PositionStatusPanel;
import org.helioviewer.jhv.gui.components.statusplugins.ZoomStatusPanel;
import org.helioviewer.jhv.input.InputController;
import org.helioviewer.jhv.input.NEWTKeyAdapter;
import org.helioviewer.jhv.input.NEWTMouseAdapter;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.selector.LayersPanel;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.opengl.GLHelper;
import org.helioviewer.jhv.opengl.GLListener;

import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.opengl.GLWindow;

public class ImageViewerGui {

    private static JFrame mainFrame;
    private static JScrollPane leftScrollPane;

    private static SideContentPane leftPane;

    private static GLWindow glWindow;
    private static NewtCanvasAWT glComponent;
    private static GLListener glListener;

    private static InputController inputController;
    private static MainContentPanel mainContentPanel;

    private static ZoomStatusPanel zoomStatus;
    private static CarringtonStatusPanel carringtonStatus;
    private static FramerateStatusPanel framerateStatus;

    private static LayersPanel layersPanel;
    private static Layers layers;

    private static InteractionPan panInteraction;
    private static InteractionRotate rotationInteraction;
    private static InteractionAxis axisInteraction;
    private static InteractionAnnotate annotateInteraction;
    private static Interaction currentInteraction;

    private static TopToolBar toolBar;
    private static MenuBar menuBar;

    public static JFrame prepareGui() {
        mainFrame = createMainFrame();
        menuBar = new MenuBar();
        mainFrame.setJMenuBar(menuBar);

        Camera camera = Displayer.getCamera();
        panInteraction = new InteractionPan(camera);
        rotationInteraction = new InteractionRotate(camera);
        axisInteraction = new InteractionAxis(camera);
        annotateInteraction = new InteractionAnnotate(camera);
        currentInteraction = rotationInteraction;

        layers = new Layers();
        layersPanel = new LayersPanel(layers);

        leftPane = new SideContentPane();
        leftPane.add("Image Layers", MoviePanel.getInstance(), true);
        MoviePanel.setAdvanced(false);

        leftScrollPane = new JScrollPane(leftPane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        leftScrollPane.setFocusable(false);
        leftScrollPane.setBorder(null);
        leftScrollPane.getVerticalScrollBar().setUnitIncrement(layersPanel.getGridRowHeight());

        glWindow = GLHelper.createGLWindow();
        glWindow.setTitle(mainFrame.getTitle());
        glListener = new GLListener(glWindow);
        glWindow.addGLEventListener(glListener);

        inputController = new InputController();
        glWindow.addMouseListener(new NEWTMouseAdapter(inputController));
        glWindow.addKeyListener(new NEWTKeyAdapter(inputController));

        glComponent = new NewtCanvasAWT(glWindow);
        mainContentPanel = new MainContentPanel(glComponent);

        JSplitPane midSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
        midSplitPane.setDividerSize(2);
        midSplitPane.setBorder(null);

        midSplitPane.setLeftComponent(leftScrollPane);
        midSplitPane.setRightComponent(mainContentPanel);

        // STATUS PANEL
        zoomStatus = new ZoomStatusPanel();
        carringtonStatus = new CarringtonStatusPanel();
        framerateStatus = new FramerateStatusPanel();
        PositionStatusPanel positionStatus = new PositionStatusPanel();
        inputController.addPlugin(positionStatus);

        StatusPanel statusPanel = new StatusPanel(leftScrollPane.getPreferredSize().width, 5);
        statusPanel.addPlugin(zoomStatus, StatusPanel.Alignment.LEFT);
        statusPanel.addPlugin(carringtonStatus, StatusPanel.Alignment.LEFT);
        statusPanel.addPlugin(framerateStatus, StatusPanel.Alignment.LEFT);
        statusPanel.addPlugin(positionStatus, StatusPanel.Alignment.RIGHT);

        toolBar = new TopToolBar();
        mainFrame.add(toolBar, BorderLayout.PAGE_START);
        mainFrame.add(midSplitPane, BorderLayout.CENTER);
        mainFrame.add(statusPanel, BorderLayout.PAGE_END);

        return mainFrame;
    }

    private static JFrame createMainFrame() {
        JFrame frame = new JFrame(JHVGlobals.programName);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
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

        return frame;
    }

    private static void enableFullScreen(Window window) {
        if (System.getProperty("jhv.os").equals("mac")) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            if (Double.parseDouble(System.getProperty("java.specification.version")) < 1.9) {
                try {
                    Class<?> fullScreenUtilities = Class.forName("com.apple.eawt.FullScreenUtilities");
                    Method setWindowCanFullScreen = fullScreenUtilities.getMethod("setWindowCanFullScreen", Window.class, boolean.class);
                    setWindowCanFullScreen.invoke(fullScreenUtilities, window, true);
                } catch (Exception e) {
                    Log.error("Fullscreen utilities not available");
                    e.printStackTrace();
                }
            }
        }
    }

    public static JFrame getMainFrame() {
        return mainFrame;
    }

    public static SideContentPane getLeftContentPane() {
        return leftPane;
    }

    public static JScrollPane getLeftScrollPane() {
        return leftScrollPane;
    }

    public static GLWindow getGLWindow() {
        return glWindow;
    }

    public static NewtCanvasAWT getGLComponent() {
        return glComponent;
    }

    public static GLListener getGLListener() {
        return glListener;
    }

    public static MainContentPanel getMainContentPanel() {
        return mainContentPanel;
    }

    public static InputController getInputController() {
        return inputController;
    }

    public static ZoomStatusPanel getZoomStatusPanel() {
        return zoomStatus;
    }

    public static CarringtonStatusPanel getCarringtonStatusPanel() {
        return carringtonStatus;
    }

    public static FramerateStatusPanel getFramerateStatusPanel() {
        return framerateStatus;
    }

    public static Layers getLayers() {
        return layers;
    }

    public static LayersPanel getLayersPanel() {
        return layersPanel;
    }

    public static void setCurrentInteraction(Interaction _currentInteraction) {
        currentInteraction = _currentInteraction;
    }

    public static Interaction getCurrentInteraction() {
        return currentInteraction;
    }

    public static Interaction getPanInteraction() {
        return panInteraction;
    }

    public static Interaction getRotateInteraction() {
        return rotationInteraction;
    }

    public static Interaction getAxisInteraction() {
        return axisInteraction;
    }

    public static InteractionAnnotate getAnnotateInteraction() {
        return annotateInteraction;
    }

    public static TopToolBar getToolBar() {
        return toolBar;
    }

    public static MenuBar getMenuBar() {
        return menuBar;
    }

}
