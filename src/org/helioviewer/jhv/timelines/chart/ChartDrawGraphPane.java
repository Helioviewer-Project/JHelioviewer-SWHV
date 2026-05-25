package org.helioviewer.jhv.timelines.chart;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.event.MouseInputListener;

import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.events.JHVEventCache;
import org.helioviewer.jhv.export.ExportMovie;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.timelines.TimelineLayers;
import org.helioviewer.jhv.timelines.draw.ClickableDrawable;
import org.helioviewer.jhv.timelines.draw.DrawConstants;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.draw.GraphGeometry;
import org.helioviewer.jhv.timelines.draw.GraphGeometry.YAxisHit;
import org.helioviewer.jhv.timelines.draw.TimeAxis;

@SuppressWarnings("serial")
final class ChartDrawGraphPane extends JComponent implements MouseInputListener, MouseWheelListener, ComponentListener, DrawController.Listener {

    private enum DragMode {
        MOVIELINE, CHART, NODRAG
    }

    private Point mousePressedPosition;
    private Point mouseDragPosition;

    private BufferedImage screenImage;

    private final TimelineLabelPainter labelPainter = new TimelineLabelPainter();
    private Point mousePosition;
    private int lastWidth = -1;
    private int lastHeight = -1;

    private boolean redrawGraphArea;

    private DragMode dragMode = DragMode.NODRAG;

    ChartDrawGraphPane() {
        setPreferredSize(new Dimension(-1, 50));
        setOpaque(true);
        setDoubleBuffered(false);

        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        addComponentListener(this);
        DrawController.addDrawListener(this);
        DrawController.setGraphInformation(new Rectangle(getWidth(), getHeight()));
    }

    @Override
    public void removeNotify() {
        DrawController.removeDrawListener(this);
        super.removeNotify();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            ExportMovie.EVEImage = screenImage;
            DrawController.start();
        } else {
            ExportMovie.EVEImage = null;
            DrawController.stop();
        }
    }

    private boolean toggleAxisHighlight(GraphGeometry geometry) {
        YAxisHit hit = mousePosition == null ? null : geometry.yAxisHit(mousePosition);

        final boolean[] toggled = {false};
        TimelineLayers.forEachYAxis((tl, axisIndex) -> {
            boolean highlighted = hit != null && hit.targets(axisIndex);
            toggled[0] = toggled[0] || tl.getYAxis().isHighlighted() != highlighted;
            tl.getYAxis().setHighlighted(highlighted);
        });
        return toggled[0];
    }

    @Override
    protected void paintComponent(Graphics g1) {
        super.paintComponent(g1);
        GraphGeometry geometry = DrawController.getGeometry();

        boolean axisHighlightChanged = toggleAxisHighlight(geometry);
        if (redrawGraphArea || axisHighlightChanged) {
            redrawGraphArea = false;
            redrawGraph(geometry);
        }

        Graphics2D g = (Graphics2D) g1;
        if (screenImage != null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(DrawConstants.font);
            g.drawImage(screenImage, 0, 0, getWidth(), getHeight(), null);
            drawMovieLine(g);
            labelPainter.drawMouseValues(g, geometry, DrawController.selectedAxis, mousePosition);
        }
    }

    private void redrawGraph(GraphGeometry geometry) {
        Rectangle graphArea = geometry.area();
        Rectangle graphSize = geometry.size();
        double sx = Display.pixelScale[0], sy = Display.pixelScale[1];
        int width = (int) (sx * graphSize.getWidth() + .5);
        int height = (int) (sy * graphSize.getHeight() + .5);

        if (width > 0 && height > 0) {
            if (width != lastWidth || height != lastHeight) {
                screenImage = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(width, height, Transparency.OPAQUE);
                ExportMovie.EVEImage = screenImage;

                lastWidth = width;
                lastHeight = height;
            }

            Graphics2D fullG = screenImage.createGraphics();
            drawBackground(fullG, screenImage.getWidth(), screenImage.getHeight());

            fullG.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            fullG.setFont(DrawConstants.font);
            fullG.setTransform(AffineTransform.getScaleInstance(sx, sy));

            Graphics2D plotG = (Graphics2D) fullG.create();
            plotG.setClip(graphArea);
            TimeAxis xAxis = DrawController.selectedAxis;
            TimelineLayers.draw(plotG, graphArea, xAxis, mousePosition);
            labelPainter.drawStaticLabels(fullG, geometry, xAxis);

            plotG.dispose();
            fullG.dispose();
        }
    }

    private static void drawBackground(Graphics2D g, int width, int height) {
        g.setColor(UIGlobals.TL_SELECTED_INTERVAL_BACKGROUND_COLOR);
        g.fillRect(0, 0, width, height);
    }

    private static void drawMovieLine(Graphics2D g) {
        int movieLinePosition = DrawController.getMovieLinePosition();
        ExportMovie.EVEMovieLinePosition = movieLinePosition;
        if (movieLinePosition < 0) {
            return;
        }
        g.setColor(UIGlobals.TL_MOVIE_FRAME_COLOR);
        g.drawLine(movieLinePosition, 0, movieLinePosition, DrawController.getGeometry().size().height);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        Point p = e.getPoint();
        if (e.getClickCount() == 2) {
            DrawController.resetAxis(p);
            return;
        }

        ClickableDrawable element = TimelineLayers.getDrawableUnderMouse();
        if (element != null) {
            element.clicked(e.getLocationOnScreen(), DrawController.getGeometry().xMapper(DrawController.selectedAxis).toValue(p.x));
        } else {
            DrawController.setMovieFrame(p);
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {
        JHVEventCache.highlight(null);
        mousePosition = null;
        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Point p = e.getPoint();
        mousePressedPosition = p;
        if (overMovieLine(p)) {
            // setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
            dragMode = DragMode.MOVIELINE;
        } else {
            setCursor(UIGlobals.closedHandCursor);
            dragMode = DragMode.CHART;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Point p = e.getPoint();

        switch (dragMode) {
            case CHART -> {
                setCursor(UIGlobals.openHandCursor);
                if (mousePressedPosition != null && mouseDragPosition != null) {
                    DrawController.moveX(mousePressedPosition.x - p.x);
                    DrawController.moveAllAxes(p.y - mousePressedPosition.y);
                }
            }
            case MOVIELINE -> DrawController.setMovieFrame(p);
            case NODRAG -> {}
        }
        dragMode = DragMode.NODRAG;
        mousePressedPosition = null;
        mouseDragPosition = null;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point p = e.getPoint();
        mouseDragPosition = p;
        if (mousePressedPosition != null) {
            switch (dragMode) {
                case CHART -> {
                    setCursor(UIGlobals.closedHandCursor);
                    DrawController.moveX(mousePressedPosition.x - p.x);
                    DrawController.moveY(p, p.y - mousePressedPosition.y);
                }
                case MOVIELINE -> DrawController.setMovieFrame(p);
                case NODRAG -> {}
            }
        }
        mousePressedPosition = p;
    }

    private static boolean overMovieLine(Point p) {
        int movieLinePosition = DrawController.getMovieLinePosition();
        return movieLinePosition >= 0 && movieLinePosition - 3 <= p.x && p.x <= movieLinePosition + 3;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mousePosition = e.getPoint();
        if (overMovieLine(mousePosition)) {
            setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
        } else if (TimelineLayers.getDrawableUnderMouse() != null) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else if (DrawController.getGeometry().area().contains(mousePosition)) {
            setCursor(UIGlobals.openHandCursor);
        } else {
            setCursor(Cursor.getDefaultCursor());
        }

        if (TimelineLayers.highLightChanged(mousePosition)) {
            drawRequest();
        } else {
            repaint(); // for timeline values
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
            int scrollDistance = e.getWheelRotation() * e.getScrollAmount();
            DrawController.zoomXY(e.getPoint(), scrollDistance, e.isShiftDown(), e.isAltDown(), e.isControlDown());
        }
    }

    @Override
    public void componentHidden(ComponentEvent e) {}

    @Override
    public void componentMoved(ComponentEvent e) {}

    @Override
    public void componentResized(ComponentEvent e) {
        DrawController.setGraphInformation(new Rectangle(getWidth(), getHeight()));
    }

    @Override
    public void componentShown(ComponentEvent e) {}

    @Override
    public void drawRequest() {
        redrawGraphArea = true;
        repaint();
    }

    @Override
    public void drawMovieLineRequest() {
        repaint();
    }

}
