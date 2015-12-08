package org.helioviewer.jhv.gui.components.statusplugins;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import org.helioviewer.jhv.base.math.Vec2;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.StatusPanel;
import org.helioviewer.jhv.gui.controller.InputControllerPlugin;

@SuppressWarnings("serial")
public class PositionStatusPanel extends StatusPanel.StatusPlugin implements MouseMotionListener, InputControllerPlugin {

    private Point lastPosition;
    private static String rhoFormat = " | \u03c1 : %.2f R\u2299";
    private static String emptyPos = "(\u03C6, \u03B8) : (--\u00B0, --\u00B0)";

    private static Camera camera;

    public PositionStatusPanel() {
        setText(emptyPos + String.format(rhoFormat, 0.));
    }

    private void updatePosition(Point position) {
        if (position == lastPosition)
            return;

        Viewport vp = Displayer.getActiveViewport();
        Vec2 coord = ImageViewerGui.getRenderableGrid().gridPoint(camera, vp, position);
        double radius = CameraHelper.getRadiusFromSphereAlt(camera, vp, position);

        if (coord == null) {
            setText(emptyPos + String.format(rhoFormat, radius));
        } else {
            setText(String.format("(\u03C6, \u03B8) : (%.2f\u00B0,%.2f\u00B0)", coord.x, coord.y) + String.format(rhoFormat, radius));
        }
        lastPosition = position;
    }

    @Override
    public void setCamera(Camera _camera) {
        camera = _camera;
    }

    @Override
    public void setComponent(Component _component) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        updatePosition(e.getPoint());
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        updatePosition(e.getPoint());
    }

}
