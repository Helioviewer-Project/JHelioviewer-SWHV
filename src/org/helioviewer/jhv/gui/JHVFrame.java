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
import org.helioviewer.jhv.camera.Interaction;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.gui.actions.ExitProgramAction;
import org.helioviewer.jhv.gui.components.MainContentPanel;
import org.helioviewer.jhv.gui.components.MenuBar;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.gui.components.SideContentPane;
import org.helioviewer.jhv.gui.components.StatusPanel;
import org.helioviewer.jhv.gui.components.ToolBar;
import org.helioviewer.jhv.gui.components.statusplugin.CarringtonStatusPanel;
import org.helioviewer.jhv.gui.components.statusplugin.FramerateStatusPanel;
import org.helioviewer.jhv.gui.components.statusplugin.PositionStatusPanel;
import org.helioviewer.jhv.gui.components.statusplugin.ZoomStatusPanel;
import org.helioviewer.jhv.input.InputController;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.selector.LayersPanel;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.opengl.GLHelper;
import org.helioviewer.jhv.opengl.GLListener;

import com.jogamp.opengl.awt.GLCanvas;

public class JHVFrame {

    private static JFrame mainFrame;
    private static JScrollPane leftScrollPane;

    private static SideContentPane leftPane;

    private static GLCanvas glCanvas;
    private static GLListener glListener;

    private static InputController inputController;
    private static Interaction interaction;
    private static MainContentPanel mainContentPanel;

    private static ZoomStatusPanel zoomStatus;
    private static CarringtonStatusPanel carringtonStatus;

    private static LayersPanel layersPanel;
    private static Layers layers;

    private static ToolBar toolBar;
    private static MenuBar menuBar;

    public static JFrame prepare() {
        mainFrame = createFrame();
        menuBar = new MenuBar();
        mainFrame.setJMenuBar(menuBar);

        glCanvas = GLHelper.createGLCanvas(); // before camera
        glCanvas.setMinimumSize(new Dimension(1, 1)); // allow resize
        glListener = new GLListener(glCanvas);
        glCanvas.addGLEventListener(glListener);

        layers = new Layers();
        layersPanel = new LayersPanel(layers);

        leftPane = new SideContentPane();
        leftPane.add("Image Layers", MoviePanel.getInstance(), true);
        MoviePanel.setAdvanced(false);

        leftScrollPane = new JScrollPane(leftPane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        leftScrollPane.setFocusable(false);
        leftScrollPane.setBorder(null);
        leftScrollPane.getVerticalScrollBar().setUnitIncrement(layersPanel.getGridRowHeight());

        interaction = new Interaction(Display.getCamera());
        inputController = new InputController(interaction);
        glCanvas.addMouseListener(inputController);
        glCanvas.addMouseMotionListener(inputController);
        glCanvas.addMouseWheelListener(inputController);
        glCanvas.addKeyListener(inputController);

        mainContentPanel = new MainContentPanel(glCanvas);

        JSplitPane midSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
        midSplitPane.setDividerSize(2);
        midSplitPane.setBorder(null);

        midSplitPane.setLeftComponent(leftScrollPane);
        midSplitPane.setRightComponent(mainContentPanel);

        zoomStatus = new ZoomStatusPanel();
        carringtonStatus = new CarringtonStatusPanel();
        FramerateStatusPanel framerateStatus = new FramerateStatusPanel();
        PositionStatusPanel positionStatus = new PositionStatusPanel();
        inputController.addPlugin(positionStatus);

        StatusPanel statusPanel = new StatusPanel(5, 5);
        statusPanel.addPlugin(framerateStatus, StatusPanel.Alignment.LEFT);
        statusPanel.addPlugin(positionStatus, StatusPanel.Alignment.RIGHT);
        statusPanel.addPlugin(zoomStatus, StatusPanel.Alignment.RIGHT);
        statusPanel.addPlugin(carringtonStatus, StatusPanel.Alignment.RIGHT);

        toolBar = new ToolBar();
        mainFrame.add(toolBar, BorderLayout.PAGE_START);
        mainFrame.add(midSplitPane, BorderLayout.CENTER);
        mainFrame.add(statusPanel, BorderLayout.PAGE_END);

        return mainFrame;
    }

    private static JFrame createFrame() {
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
                    setWindowCanFullScreen.invoke(fullScreenUtilities, window, Boolean.TRUE);
                } catch (Exception e) {
                    Log.error("Fullscreen utilities not available");
                    e.printStackTrace();
                }
            }
        }
    }

    public static JFrame getFrame() {
        return mainFrame;
    }

    public static SideContentPane getLeftContentPane() {
        return leftPane;
    }

    public static JScrollPane getLeftScrollPane() {
        return leftScrollPane;
    }

    public static GLCanvas getGLCanvas() {
        return glCanvas;
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

    public static Layers getLayers() {
        return layers;
    }

    public static LayersPanel getLayersPanel() {
        return layersPanel;
    }

    public static Interaction getInteraction() {
        return interaction;
    }

    public static ToolBar getToolBar() {
        return toolBar;
    }

    public static MenuBar getMenuBar() {
        return menuBar;
    }

}
