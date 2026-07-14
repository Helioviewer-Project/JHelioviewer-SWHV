package org.helioviewer.jhv.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Taskbar;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.TransferHandler;

import org.helioviewer.jhv.app.AppInfo;
import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.app.Message;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.gui.component.MainContentPanel;
import org.helioviewer.jhv.gui.component.MenuBar;
import org.helioviewer.jhv.gui.component.MoviePanel;
import org.helioviewer.jhv.gui.component.SideContentPane;
import org.helioviewer.jhv.gui.component.StatusPanel;
import org.helioviewer.jhv.gui.component.ToolBar;
import org.helioviewer.jhv.gui.status.FramerateStatusPanel;
import org.helioviewer.jhv.gui.status.PositionStatusPanel;
import org.helioviewer.jhv.gui.status.ViewpointStatusPanel;
import org.helioviewer.jhv.input.InputController;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.selector.LayersPanel;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.opengl.AngleCanvas;
import org.helioviewer.jhv.opengl.angle.AngleRenderer;
import org.helioviewer.jhv.opengl.angle.MacAngleBridge;
import org.helioviewer.jhv.thread.Task;

public final class MainFrame {

    @SuppressWarnings("serial")
    private static final class FixedWidthPanel extends JPanel {
        private int fixedWidth = -1;

        FixedWidthPanel() {
            super(new BorderLayout());
        }

        void setFixedWidth(int width) {
            fixedWidth = width;
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            if (fixedWidth > 0)
                size.width = fixedWidth;
            return size;
        }

        @Override
        public Dimension getMinimumSize() {
            Dimension size = super.getMinimumSize();
            if (fixedWidth > 0)
                size.width = fixedWidth;
            return size;
        }
    }

    @SuppressWarnings("serial")
    private static final class RenderStartupHost extends JPanel {
        private final JPanel placeholder = new JPanel();
        private AngleCanvas canvas;

        RenderStartupHost() {
            super(new BorderLayout());
            placeholder.setBackground(Color.BLACK);
            add(placeholder, BorderLayout.CENTER);
        }

        void attachCanvas(AngleCanvas _canvas) {
            if (canvas != null)
                return;
            canvas = _canvas;
            remove(placeholder);
            add(canvas, BorderLayout.CENTER);
            revalidate();
            repaint();
        }
    }

    private static JFrame mainFrame;
    private static JScrollPane leftScrollPane;
    private static FixedWidthPanel leftPaneHost;

    private static SideContentPane leftPane;

    private static AngleCanvas renderCanvas;
    private static RenderStartupHost renderHost;
    private static AwtInputAdapter awtInputAdapter;
    private static MainContentPanel mainContentPanel;

    private static LayersPanel layersPanel;

    private static MenuBar menuBar;

    public static JFrame prepare() {
        mainFrame = createFrame();

        Message.setHandler(new MessageHandler());

        menuBar = new MenuBar();
        mainFrame.setJMenuBar(menuBar);

        renderCanvas = null;
        renderHost = new RenderStartupHost();

        layersPanel = new LayersPanel();

        leftPane = new SideContentPane();
        leftPane.add("Image Layers", MoviePanel.getInstance(), true);

        leftScrollPane = new JScrollPane(leftPane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        leftScrollPane.setFocusable(false);
        leftScrollPane.setBorder(null);
        leftScrollPane.getVerticalScrollBar().setUnitIncrement(layersPanel.getGridRowHeight());
        leftPaneHost = new FixedWidthPanel();
        leftPaneHost.add(leftScrollPane, BorderLayout.CENTER);

        awtInputAdapter = new AwtInputAdapter();

        mainContentPanel = new MainContentPanel(renderHost);
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(leftPaneHost, BorderLayout.WEST);
        centerPanel.add(mainContentPanel, BorderLayout.CENTER);

        ViewpointStatusPanel viewpointStatus = new ViewpointStatusPanel();
        FramerateStatusPanel framerateStatus = new FramerateStatusPanel();
        PositionStatusPanel positionStatus = new PositionStatusPanel();
        InputController.addListener(positionStatus);

        StatusPanel statusPanel = new StatusPanel(5, 5);
        statusPanel.addPlugin(framerateStatus, StatusPanel.Alignment.LEFT);
        statusPanel.addPlugin(positionStatus, StatusPanel.Alignment.RIGHT);
        statusPanel.addPlugin(viewpointStatus, StatusPanel.Alignment.RIGHT);

        ToolBar toolBar = new ToolBar();

        JPanel toolBarPanel = new JPanel(new BorderLayout());
        toolBarPanel.add(toolBar, BorderLayout.CENTER);

        mainFrame.getContentPane().add(toolBarPanel, BorderLayout.NORTH);
        mainFrame.getContentPane().add(centerPanel, BorderLayout.CENTER);
        mainFrame.getContentPane().add(statusPanel, BorderLayout.SOUTH);

        Player.setMaster(Layers.getActiveImageLayer()); //! for nullImageLayer

        // Prewarm ANGLE off the EDT, then return here via attachAndRender() to attach the real render canvas.
        startAngleWarmup();
        return mainFrame;
    }

    private static void startAngleWarmup() {
        Task.submit("angle-warmup", () -> {
            if (Platform.isMacOS())
                MacAngleBridge.prewarm();
            AngleRenderer.prewarm();
            return null;
        }, ignored -> EventQueue.invokeLater(MainFrame::attachAndRender), (context, error) -> {
            Log.warn("ANGLE warmup failed", error);
            EventQueue.invokeLater(MainFrame::attachAndRender);
        });
    }

    private static void attachAndRender() {
        if (renderCanvas != null) // impossible
            return;

        renderCanvas = new AngleCanvas();
        renderCanvas.setMinimumSize(new Dimension(1, 1)); // allow resize
        renderCanvas.addMouseListener(awtInputAdapter);
        renderCanvas.addMouseMotionListener(awtInputAdapter);
        renderCanvas.addMouseWheelListener(awtInputAdapter);
        renderCanvas.addKeyListener(awtInputAdapter);
        renderHost.attachCanvas(renderCanvas);
        // Force ANGLE surface/context creation immediately instead of waiting for the next UI event.
        renderCanvas.requestRender();
        DisplayController.setRenderRequestHandler(renderCanvas::requestRender);
    }

    private static JFrame createFrame() {
        JFrame frame = new JFrame(AppInfo.programName);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDrop() && TransferAccess.canImport(support.getTransferable());
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support))
                    return false;
                return TransferAccess.importTransferable(support.getTransferable());
            }
        });
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
        setAppIcon();

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
        } catch (NumberFormatException ignore) {}
        return fallback;
    }

    public static JFrame get() {
        return mainFrame;
    }

    public static SideContentPane getLeftContentPane() {
        return leftPane;
    }

    public static void stabilizeLeftPaneWidth() {
        MoviePanel moviePanel = MoviePanel.getInstance();
        // Freeze the left pane to the widest startup state so the scrollbar never overlaps options panels.
        moviePanel.setAdvanced(true);
        int contentWidth = measureMoviePanelWidth(moviePanel, null);
        contentWidth = Math.max(contentWidth, measureMoviePanelWidth(moviePanel, Layers.getViewpointLayer()));
        contentWidth = Math.max(contentWidth, measureMoviePanelWidth(moviePanel, Layers.getConnectionLayer()));

        layersPanel.setOptionsPanel(null);
        moviePanel.setAdvanced(false);
        moviePanel.setFixedPreferredWidth(contentWidth);
        leftPane.revalidate();

        int scrollbarWidth = leftScrollPane.getVerticalScrollBar().getPreferredSize().width;
        leftPaneHost.setFixedWidth(contentWidth + scrollbarWidth);
        leftPaneHost.revalidate();
    }

    private static int measureMoviePanelWidth(MoviePanel moviePanel, Layer optionsLayer) {
        layersPanel.setOptionsPanel(optionsLayer);
        moviePanel.revalidate();
        moviePanel.doLayout();
        return moviePanel.getPreferredSize().width;
    }

    public static Component getRenderComponent() {
        return renderCanvas != null ? renderCanvas : renderHost;
    }

    public static int getFramerate() {
        return renderCanvas != null ? renderCanvas.getFramerate() : 0;
    }

    public static MainContentPanel getMainContentPanel() {
        return mainContentPanel;
    }

    public static LayersPanel getLayersPanel() {
        return layersPanel;
    }

    public static MenuBar getMenuBar() {
        return menuBar;
    }

    private MainFrame() {}

    // The window icon above does not reach the macOS Dock or the Windows taskbar: without this the
    // running app is represented by the generic Java icon. The usual remedy, -Xdock:icon, only
    // applies when the app happens to be launched through a script that passes it, so set the icon
    // from inside the app, where it always holds.
    private static void setAppIcon() {
        try {
            if (!Taskbar.isTaskbarSupported())
                return;
            Taskbar taskbar = Taskbar.getTaskbar();
            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE))
                taskbar.setIconImage(IconBank.getIcon(IconBank.JHVIcon.HVLOGO_APP).getImage());
        } catch (Exception e) { // the icon is cosmetic and must never stop startup
            Log.warn("Could not set the application icon", e);
        }
    }
}
