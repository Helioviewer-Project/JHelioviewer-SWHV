package org.helioviewer.gl3d.camera;

import java.awt.Point;
import java.awt.event.MouseEvent;

import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.view.opengl.GL3DSceneGraphView;

/**
 * Standard panning interaction, moves the camera proportionally to the mouse
 * movement when dragging
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DPanInteraction extends GL3DDefaultInteraction {

    private Point lastMousePoint;

    protected GL3DPanInteraction(GL3DSolarRotationTrackingTrackballCamera camera, GL3DSceneGraphView sceneGraph) {
        super(camera, sceneGraph);
    }

    public void mousePressed(MouseEvent e, GL3DCamera camera) {
        this.lastMousePoint = e.getPoint();
    }

    public void mouseDragged(MouseEvent e, GL3DCamera camera) {
        int x = (e.getPoint().x - this.lastMousePoint.x);
        int y = (e.getPoint().y - this.lastMousePoint.y);
        if (sceneGraphView.getAdapter(RegionView.class).getRegion() != null) {
            Vector2dDouble imageDisplacement = ViewHelper.convertScreenToImageDisplacement(x, y, sceneGraphView.getAdapter(RegionView.class).getRegion(), ViewHelper.calculateViewportImageSize(sceneGraphView));
            camera.translation.x += imageDisplacement.getX();
            camera.translation.y += imageDisplacement.getY();
        } else {
            camera.translation.x += x / 100.0 * Constants.SunRadius;
            camera.translation.y -= y / 100.0 * Constants.SunRadius;
        }
        this.lastMousePoint = e.getPoint();
        camera.updateCameraTransformation();

        camera.fireCameraMoving();
        Displayer.getSingletonInstance().display();
    }

    @Override
    public void mouseReleased(MouseEvent e, GL3DCamera camera) {
        camera.fireCameraMoved();
        Displayer.getSingletonInstance().display();
    }

}
