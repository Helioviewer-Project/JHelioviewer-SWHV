package org.helioviewer.jhv.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Platform;
import org.helioviewer.jhv.camera.Interaction;
import org.helioviewer.jhv.display.Display;
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
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.layers.selector.LayersPanel;
import org.helioviewer.jhv.opengl.AngleCanvas;
import org.helioviewer.jhv.opengl.JHVCanvas;
import org.helioviewer.jhv.opengl.RenderSurface;

public class JHVFrame {

    private static JFrame mainFrame;
    private static JScrollPane leftScrollPane;

    private static SideContentPane leftPane;

    private static RenderSurface renderSurface;
    private static Component renderComponent;
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

        renderSurface = createRenderSurface();
        renderComponent = (Component) renderSurface;
        renderComponent.setMinimumSize(new Dimension(1, 1)); // allow resize

        layers = Layers.getInstance();
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
        renderComponent.addMouseListener(inputController);
        renderComponent.addMouseMotionListener(inputController);
        renderComponent.addMouseWheelListener(inputController);
        renderComponent.addKeyListener(inputController);

        mainContentPanel = new MainContentPanel(renderComponent);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(leftScrollPane, BorderLayout.WEST);
        centerPanel.add(mainContentPanel, BorderLayout.CENTER);

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

        JPanel toolBarPanel = new JPanel(new BorderLayout());
        toolBarPanel.add(toolBar, BorderLayout.CENTER);

        mainFrame.getContentPane().add(toolBarPanel, BorderLayout.NORTH);
        mainFrame.getContentPane().add(centerPanel, BorderLayout.CENTER);
        mainFrame.getContentPane().add(statusPanel, BorderLayout.SOUTH);

        Movie.setMaster(Layers.getActiveImageLayer()); //! for nullImageLayer

        return mainFrame;
    }

    private static JFrame createFrame() {
        JFrame frame = new JFrame(JHVGlobals.programName);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setTransferHandler(JHVTransferHandler.getInstance());
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Actions.ExitProgram exitAction = new Actions.ExitProgram();
                exitAction.actionPerformed(new ActionEvent(this, 0, ""));
            }
        });

        if (Platform.isMacOS()) {
            frame.getRootPane().putClientProperty("apple.awt.fullWindowContent", true);
            frame.getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
            frame.getRootPane().putClientProperty("apple.awt.windowTitleVisible", false);
            frame.getRootPane().putClientProperty(com.formdev.flatlaf.FlatClientProperties.MACOS_WINDOW_BUTTONS_SPACING,
                    com.formdev.flatlaf.FlatClientProperties.MACOS_WINDOW_BUTTONS_SPACING_MEDIUM);
        }

        Dimension maxSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getSize();
        Dimension minSize = new Dimension(800, 600);
        minSize.width = Math.min(minSize.width, maxSize.width);
        minSize.height = Math.min(minSize.height, maxSize.height);

        frame.setMinimumSize(minSize);

        int preferredWidth = readSizeEnv("JHV_PREFERRED_WIDTH", maxSize.width - 100);
        int preferredHeight = readSizeEnv("JHV_PREFERRED_HEIGHT", maxSize.height - 100);
        preferredWidth = Math.min(preferredWidth, maxSize.width);
        preferredHeight = Math.min(preferredHeight, maxSize.height);
        frame.setPreferredSize(new Dimension(preferredWidth, preferredHeight));

        frame.setIconImage(IconBank.getIcon(IconBank.JHVIcon.HVLOGO_SMALL).getImage());

        return frame;
    }

    private static int readSizeEnv(String name, int fallback) {
        String raw = System.getenv(name);
        if (raw == null || raw.isBlank())
            return fallback;

        try {
            int value = Integer.parseInt(raw.trim());
            if (value > 0)
                return value;
        } catch (NumberFormatException ignore) {
        }
        return fallback;
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

    public static Component getRenderComponent() {
        return renderComponent;
    }

    public static void requestRender() {
        renderSurface.requestRender();
    }

    public static void setWhiteBackground(boolean whiteBackground) {
        renderSurface.setWhiteBackground(whiteBackground);
    }

    public static int getFramerate() {
        return renderSurface.getFramerate();
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

    private static RenderSurface createRenderSurface() {
        if (Platform.isMacOS() && !"false".equalsIgnoreCase(System.getProperty("jhv.metal.host")))
            return new AngleCanvas();
        return JHVCanvas.create(); // before camera
    }

}
