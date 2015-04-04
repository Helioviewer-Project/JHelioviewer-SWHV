package org.helioviewer.gl3d.camera;

import java.awt.Point;
import java.awt.event.MouseEvent;

import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.viewport.Viewport;
import org.helioviewer.viewmodel.viewportimagesize.ViewportImageSize;

/**
 * Standard panning interaction, moves the camera proportionally to the mouse
 * movement when dragging
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DPanInteraction extends GL3DDefaultInteraction {

    private Point lastMousePoint;

    protected GL3DPanInteraction(GL3DSolarRotationTrackingTrackballCamera camera) {
        super(camera);
    }

    @Override
    public void mousePressed(MouseEvent e, GL3DCamera camera) {
        this.lastMousePoint = e.getPoint();
    }

    @Override
    public void mouseDragged(MouseEvent e, GL3DCamera camera) {
        int x = e.getPoint().x - this.lastMousePoint.x;
        int y = e.getPoint().y - this.lastMousePoint.y;

        JHVJP2View jp2view = Displayer.getLayersModel().getActiveView();

        Region region;
        Viewport viewport;
        if (jp2view != null && (region = jp2view.getRegion()) != null && (viewport = jp2view.getViewport()) != null) {

            ViewportImageSize vis = ViewHelper.calculateViewportImageSize(viewport, region);
            Vector2dDouble imageDisplacement = ViewHelper.convertScreenToImageDisplacement(x, y, region, vis);
            camera.translation.x += imageDisplacement.getX();
            camera.translation.y += imageDisplacement.getY();
        } else {
            camera.translation.x += x / 100.0 * Constants.SunRadius;
            camera.translation.y -= y / 100.0 * Constants.SunRadius;
        }
        this.lastMousePoint = e.getPoint();
        camera.updateCameraTransformation();

        Displayer.display();
    }

    @Override
    public void mouseReleased(MouseEvent e, GL3DCamera camera) {
        Displayer.display();
    }

}
