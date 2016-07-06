package org.helioviewer.jhv.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Method;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.Interaction;
import org.helioviewer.jhv.camera.InteractionAnnotate;
import org.helioviewer.jhv.camera.InteractionPan;
import org.helioviewer.jhv.camera.InteractionRotate;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.input.InputController;
import org.helioviewer.jhv.input.NEWTKeyAdapter;
import org.helioviewer.jhv.input.NEWTMouseAdapter;
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
import org.helioviewer.jhv.opengl.GLHelper;
import org.helioviewer.jhv.opengl.GLListener;
import org.helioviewer.jhv.renderable.components.RenderableGrid;
import org.helioviewer.jhv.renderable.components.RenderableMiniview;
import org.helioviewer.jhv.renderable.components.RenderableTimeStamp;
import org.helioviewer.jhv.renderable.components.RenderableViewpoint;
import org.helioviewer.jhv.renderable.gui.RenderableContainer;
import org.helioviewer.jhv.renderable.gui.RenderableContainerPanel;

import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.opengl.GLWindow;

public class ImageViewerGui {

    public static final int SIDE_PANEL_WIDTH = 320;
    public static final int SIDE_PANEL_WIDTH_EXTRA = 20;

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

    private static RenderableContainer renderableContainer;
    private static RenderableViewpoint renderableViewpoint;
    private static RenderableGrid renderableGrid;
    private static RenderableMiniview renderableMiniview;

    private static InteractionRotate rotationInteraction;
    private static InteractionPan panInteraction;
    private static InteractionAnnotate annotateInteraction;
    private static Interaction currentInteraction;

    public static void prepareGui() {
        mainFrame = createMainFrame();
        mainFrame.setJMenuBar(new MenuBar());

        Camera camera = Displayer.getCamera();
        rotationInteraction = new InteractionRotate(camera);
        panInteraction = new InteractionPan(camera);
        annotateInteraction = new InteractionAnnotate(camera);
        currentInteraction = rotationInteraction;

        leftPane = new SideContentPane();
        leftPane.add("Movie Controls", MoviePanel.getInstance(), true);
        MoviePanel.setAdvanced(false);

        // Layer control
        renderableContainer = new RenderableContainer();
        renderableGrid = new RenderableGrid();
        renderableContainer.addRenderable(renderableGrid);
        renderableViewpoint = new RenderableViewpoint();
        renderableContainer.addRenderable(renderableViewpoint);
        renderableContainer.addRenderable(new RenderableTimeStamp());
        renderableMiniview = new RenderableMiniview();
        renderableContainer.addRenderable(renderableMiniview);
        RenderableContainerPanel renderableContainerPanel = new RenderableContainerPanel(renderableContainer);

        leftPane.add("Image Layers", renderableContainerPanel, true);
        leftScrollPane = new JScrollPane(leftPane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        leftScrollPane.setFocusable(false);
        leftScrollPane.setBorder(null);
        leftScrollPane.getVerticalScrollBar().setUnitIncrement(renderableContainerPanel.getGridRowHeight());

        glWindow = GLHelper.createGLWindow();
        glWindow.setUndecorated(true);
        glWindow.setTitle(mainFrame.getTitle());
        glListener = new GLListener(glWindow);
        glWindow.addGLEventListener(glListener);

        inputController = new InputController();
        glWindow.addMouseListener(new NEWTMouseAdapter(inputController));
        glWindow.addKeyListener(new NEWTKeyAdapter(inputController));

        glComponent = new NewtCanvasAWT(glWindow);
        mainContentPanel = new MainContentPanel(glComponent);

        JSplitPane midSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false);
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

        StatusPanel statusPanel = new StatusPanel(leftScrollPane.getPreferredSize().width + SIDE_PANEL_WIDTH_EXTRA, 5);
        statusPanel.addPlugin(zoomStatus, StatusPanel.Alignment.LEFT);
        statusPanel.addPlugin(carringtonStatus, StatusPanel.Alignment.LEFT);
        statusPanel.addPlugin(framerateStatus, StatusPanel.Alignment.LEFT);
        statusPanel.addPlugin(positionStatus, StatusPanel.Alignment.RIGHT);

        mainFrame.add(new TopToolBar(), BorderLayout.PAGE_START);
        mainFrame.add(midSplitPane, BorderLayout.CENTER);
        mainFrame.add(statusPanel, BorderLayout.PAGE_END);

        mainFrame.pack();
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
    }

    private static JFrame createMainFrame() {
        JFrame frame = new JFrame(JHVGlobals.programName);

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

        return frame;
    }

    private static void enableFullScreen(java.awt.Window window) {
        if (System.getProperty("jhv.os").equals("mac")) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            try {
                Class<?> fullScreenUtilities = Class.forName("com.apple.eawt.FullScreenUtilities");
                Method setWindowCanFullScreen = fullScreenUtilities.getMethod("setWindowCanFullScreen", java.awt.Window.class, boolean.class);
                setWindowCanFullScreen.invoke(fullScreenUtilities, window, true);
            } catch (Exception e) {
                Log.error("Fullscreen utilities not available");
                e.printStackTrace();
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

    public static RenderableViewpoint getRenderableViewpoint() {
        return renderableViewpoint;
    }

    public static RenderableGrid getRenderableGrid() {
        return renderableGrid;
    }

    public static RenderableMiniview getRenderableMiniview() {
        return renderableMiniview;
    }

    public static RenderableContainer getRenderableContainer() {
        return renderableContainer;
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

    public static InteractionAnnotate getAnnotateInteraction() {
        return annotateInteraction;
    }

}
