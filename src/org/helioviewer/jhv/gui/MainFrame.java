package org.helioviewer.jhv.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;

import org.helioviewer.jhv.app.AppInfo;
import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.app.Message;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.gui.component.Buttons;
import org.helioviewer.jhv.gui.component.CollapsiblePaneButton;
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
import org.helioviewer.jhv.movie.ExportMovie;
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
        private AngleCanvas canvas;
        private boolean surfaceVisible = true;

        RenderStartupHost() {
            super(new BorderLayout());
            JPanel placeholder = new JPanel();
            placeholder.setBackground(Color.BLACK);
            add(placeholder, BorderLayout.CENTER);
        }

        void attachCanvas(AngleCanvas _canvas) {
            canvas = _canvas;
            canvas.setHostVisible(surfaceVisible);
            removeAll();
            add(canvas, BorderLayout.CENTER);
            revalidate();
            repaint();
        }

        void setSurfaceVisible(boolean visible) {
            surfaceVisible = visible;
            if (canvas != null)
                canvas.setHostVisible(visible);
        }
    }

    private static JFrame mainFrame;
    private static JScrollPane leftScrollPane;
    private static FixedWidthPanel leftPaneHost;

    private static SideContentPane leftPane;

    private static final RenderStartupHost renderHost = new RenderStartupHost();
    private static AwtInputAdapter awtInputAdapter;
    private static MainContentPanel mainContentPanel;

    private static LayersPanel layersPanel;

    private static MenuBar menuBar;

    public static JFrame prepare() {
        mainFrame = createFrame();

        Message.setHandler(new MessageHandler());

        menuBar = new MenuBar();
        mainFrame.setJMenuBar(menuBar);

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

        CollapsiblePaneButton sidebarCollapseHandle = new CollapsiblePaneButton(SwingConstants.VERTICAL);
        sidebarCollapseHandle.setSelected(true);
        sidebarCollapseHandle.setFont(UIGlobals.uiFontSmallBold);
        sidebarCollapseHandle.setHorizontalAlignment(SwingConstants.CENTER);
        sidebarCollapseHandle.setToolTipText("Collapse the sidebar");
        sidebarCollapseHandle.setText(Buttons.collapseLeft);
        sidebarCollapseHandle.addActionListener(e -> {
            boolean collapsed = leftPaneHost.isVisible();
            leftPaneHost.setVisible(!collapsed);
            sidebarCollapseHandle.setSelected(true);
            sidebarCollapseHandle.setText(collapsed ? Buttons.collapseRight : Buttons.collapseLeft);
            sidebarCollapseHandle.setToolTipText(collapsed ? "Show the sidebar" : "Collapse the sidebar");
            centerPanel.revalidate();
            mainFrame.validate();
            centerPanel.repaint();
        });

        JPanel westPanel = new JPanel(new BorderLayout());
        westPanel.add(leftPaneHost, BorderLayout.CENTER);
        westPanel.add(sidebarCollapseHandle, BorderLayout.LINE_END);
        centerPanel.add(westPanel, BorderLayout.WEST);
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
        if (renderHost.canvas != null) // impossible
            return;

        AngleCanvas canvas = new AngleCanvas();
        canvas.setMinimumSize(new Dimension(1, 1)); // allow resize
        canvas.addMouseListener(awtInputAdapter);
        canvas.addMouseMotionListener(awtInputAdapter);
        canvas.addMouseWheelListener(awtInputAdapter);
        canvas.addKeyListener(awtInputAdapter);
        renderHost.attachCanvas(canvas);
        // Force ANGLE surface/context creation immediately instead of waiting for the next UI event.
        canvas.requestRender();
        DisplayController.setRenderRequestHandler(canvas::requestRender);
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
        return renderHost.canvas != null ? renderHost.canvas : renderHost;
    }

    public static int getFramerate() {
        return renderHost.canvas != null ? renderHost.canvas.getFramerate() : 0;
    }

    public static void setRenderSurfaceVisible(boolean visible) {
        ExportMovie.setMainCanvasVisible(visible);
        renderHost.setSurfaceVisible(visible);
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
}
