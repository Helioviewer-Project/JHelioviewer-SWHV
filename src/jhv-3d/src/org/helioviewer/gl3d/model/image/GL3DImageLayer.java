package org.helioviewer.gl3d.model.image;

import java.awt.Graphics;
import java.awt.Point;
import java.util.ArrayList;

import javax.media.opengl.GL;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.camera.GL3DCameraListener;
import org.helioviewer.gl3d.model.GL3DHitReferenceShape;
import org.helioviewer.gl3d.scenegraph.GL3DNode;
import org.helioviewer.gl3d.scenegraph.GL3DOrientedGroup;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRay;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRayTracer;
import org.helioviewer.gl3d.shader.GL3DImageFragmentShaderProgram;
import org.helioviewer.gl3d.view.GL3DCoordinateSystemView;
import org.helioviewer.gl3d.view.GL3DImageTextureView;
import org.helioviewer.gl3d.view.GL3DView;
import org.helioviewer.gl3d.wcs.CoordinateSystem;
import org.helioviewer.gl3d.wcs.CoordinateVector;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.RegionView;

/**
 * This is the scene graph equivalent of an image layer sub view chain attached
 * to the GL3DLayeredView. It represents exactly one image layer in the view
 * chain
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public abstract class GL3DImageLayer extends GL3DOrientedGroup implements GL3DCameraListener {
    private static int nextLayerId = 0;
    private final int layerId;
    private GL3DVec4d direction = new GL3DVec4d(0, 0, 1, 0);

    public int getLayerId() {
        return layerId;
    }

    protected GL3DView mainLayerView;
    protected GL3DImageTextureView imageTextureView;
    protected GL3DCoordinateSystemView coordinateSystemView;
    protected MetaDataView metaDataView;
    protected RegionView regionView;
    protected GL3DImageLayers layerGroup;
    public double minZ = -Constants.SunRadius;
    public double maxZ = Constants.SunRadius;

    protected GL3DNode accellerationShape;

    protected boolean doUpdateROI = true;

    private final ArrayList<Point> points = new ArrayList<Point>();

    private final double lastViewAngle = 0.0;

    protected GL gl;
    protected GL3DImageFragmentShaderProgram sphereFragmentShader = null;
    private GL3DHitReferenceShape accellerationShapeS;

    public GL3DImageLayer(String name, GL3DView mainLayerView) {
        super(name);

        layerId = nextLayerId++;

        this.mainLayerView = mainLayerView;
        if (this.mainLayerView == null) {
            throw new NullPointerException("Cannot create GL3DImageLayer from null Layer");
        }

        this.imageTextureView = this.mainLayerView.getAdapter(GL3DImageTextureView.class);
        if (this.imageTextureView == null) {
            throw new IllegalStateException("Cannot create GL3DImageLayer when no GL3DImageTextureView is present in Layer");
        }

        this.coordinateSystemView = this.mainLayerView.getAdapter(GL3DCoordinateSystemView.class);
        if (this.coordinateSystemView == null) {
            throw new IllegalStateException("Cannot create GL3DImageLayer when no GL3DCoordinateSystemView is present in Layer");
        }

        this.metaDataView = this.mainLayerView.getAdapter(MetaDataView.class);
        if (this.metaDataView == null) {
            throw new IllegalStateException("Cannot create GL3DImageLayer when no MetaDataView is present in Layer");
        }
        this.regionView = this.mainLayerView.getAdapter(RegionView.class);
        if (this.regionView == null) {
            throw new IllegalStateException("Cannot create GL3DImageLayer when no RegionView is present in Layer");
        }

        getCoordinateSystem().addListener(this);
        this.doUpdateROI = true;
        this.markAsChanged();
    }

    @Override
    public void shapeInit(GL3DState state) {
        this.createImageMeshNodes(state.gl);

        this.accellerationShape = new GL3DHitReferenceShape(false);
        this.accellerationShapeS = new GL3DHitReferenceShape(true);

        this.addNode(this.accellerationShape);

        super.shapeInit(state);

        this.doUpdateROI = true;
        this.markAsChanged();
        updateROI(state.getActiveCamera());

        state.getActiveCamera().updateCameraTransformation();
    }

    protected abstract void createImageMeshNodes(GL gl);

    protected abstract GL3DImageMesh getImageSphere();

    @Override
    public void shapeUpdate(GL3DState state) {
        super.shapeUpdate(state);
        if (doUpdateROI) {
            // Log.debug("GL3DImageLayer: '"+getName()+" is updating its ROI");
            this.updateROI(state.getActiveCamera());
            doUpdateROI = false;
            this.accellerationShape.setUnchanged();
        }
    }

    @Override
    public void cameraMoved(GL3DCamera camera) {
        doUpdateROI = true;
        this.accellerationShape.markAsChanged();
    }

    public double getLastViewAngle() {
        return lastViewAngle;
    }

    public void paint(Graphics g) {

        for (Point p : points) {
            g.fillRect(p.x - 1, p.y - 1, 2, 2);
        }
    }

    @Override
    public void cameraMoving(GL3DCamera camera) {

    }

    public GL3DVec4d getLayerDirection() {
        // Convert layer orientation to heliocentric coordinate system
        /*
         * CoordinateVector orientation = coordinateSystemView.getOrientation();
         * CoordinateSystem targetSystem = new
         * HeliocentricCartesianCoordinateSystem(); CoordinateConversion
         * converter =
         * orientation.getCoordinateSystem().getConversion(targetSystem);
         * orientation = converter.convert(orientation);
         * 
         * GL3DVec3d layerDirection = new GL3DVec3d(orientation.getValue(0),
         * orientation.getValue(1), orientation.getValue(2));
         */

        //GL3DVec4d n = new GL3DVec4d(0, 0, 1, 1);
        //n = modelView().multiply(n);

        //GL3DVec3d layerDirection = new GL3DVec3d(n.x, n.y, n.z);

        //layerDirection.normalize();
        return direction;
    }

    public void setLayerDirection(GL3DVec4d direction) {
        this.direction = direction;
    }

    @Override
    public CoordinateSystem getCoordinateSystem() {
        return this.coordinateSystemView.getCoordinateSystem();
    }

    @Override
    public CoordinateVector getOrientation() {
        // Log.debug("GL3DImageLayer: Orientation: "+this.coordinateSystemView.getOrientation());
        return this.coordinateSystemView.getOrientation();
    }

    private void updateROI(GL3DCamera activeCamera) {
        MetaData metaData = metaDataView.getMetaData();

        if (metaData == null) {
            // No Image Data found
            return;
        }

        GL3DRayTracer rayTracer = new GL3DRayTracer(this.accellerationShape, activeCamera);
        GL3DRayTracer rayTracerS = new GL3DRayTracer(this.accellerationShapeS, activeCamera);

        // Shoot Rays in the corners of the viewport
        int width = (int) activeCamera.getWidth();
        int height = (int) activeCamera.getHeight();
        double minPhysicalX = Double.MAX_VALUE;
        double minPhysicalY = Double.MAX_VALUE;
        double maxPhysicalX = -Double.MAX_VALUE;
        double maxPhysicalY = -Double.MAX_VALUE;

        double res = 100.;
        for (int i = 0; i <= res; i++) {
            for (int j = 0; j <= 1; j++) {
                GL3DRay ray = rayTracer.cast((int) (i * width / res), (int) (j * height / 1.));
                GL3DVec3d hitPoint = ray.getHitPoint();
                if (hitPoint != null) {
                    hitPoint = ray.getHitPoint();
                    hitPoint = activeCamera.getLocalRotation().toMatrix().multiply(hitPoint);

                    minPhysicalX = Math.min(minPhysicalX, hitPoint.x);
                    minPhysicalY = Math.min(minPhysicalY, hitPoint.y);
                    maxPhysicalX = Math.max(maxPhysicalX, hitPoint.x);
                    maxPhysicalY = Math.max(maxPhysicalY, hitPoint.y);
                }
                GL3DRay rayS = rayTracerS.cast((int) (i * width / res), (int) (j * height / 1.));

                hitPoint = rayS.getHitPoint();
                if (hitPoint != null) {
                    hitPoint = ray.getHitPoint();

                    hitPoint = activeCamera.getLocalRotation().toMatrix().multiply(hitPoint);
                    minPhysicalX = Math.min(minPhysicalX, hitPoint.x);
                    minPhysicalY = Math.min(minPhysicalY, hitPoint.y);
                    maxPhysicalX = Math.max(maxPhysicalX, hitPoint.x);
                    maxPhysicalY = Math.max(maxPhysicalY, hitPoint.y);

                }
            }
        }
        for (int i = 0; i <= 1; i++) {
            for (int j = 0; j <= res; j++) {
                GL3DRay ray = rayTracer.cast((int) (i * width / 1.), (int) (j * height / res));
                GL3DVec3d hitPoint = ray.getHitPoint();
                if (hitPoint != null) {
                    hitPoint = ray.getHitPoint();
                    hitPoint = activeCamera.getLocalRotation().toMatrix().multiply(hitPoint);

                    minPhysicalX = Math.min(minPhysicalX, hitPoint.x);
                    minPhysicalY = Math.min(minPhysicalY, hitPoint.y);
                    maxPhysicalX = Math.max(maxPhysicalX, hitPoint.x);
                    maxPhysicalY = Math.max(maxPhysicalY, hitPoint.y);

                }
                GL3DRay rayS = rayTracerS.cast((int) (i * width / 1.), (int) (j * height / res));
                hitPoint = rayS.getHitPoint();
                if (hitPoint != null) {
                    hitPoint = ray.getHitPoint();

                    hitPoint = activeCamera.getLocalRotation().toMatrix().multiply(hitPoint);
                    minPhysicalX = Math.min(minPhysicalX, hitPoint.x);
                    minPhysicalY = Math.min(minPhysicalY, hitPoint.y);
                    maxPhysicalX = Math.max(maxPhysicalX, hitPoint.x);
                    maxPhysicalY = Math.max(maxPhysicalY, hitPoint.y);

                }
            }
        }

        if (minPhysicalX < metaData.getPhysicalLowerLeft().getX())
            minPhysicalX = metaData.getPhysicalLowerLeft().getX();
        if (minPhysicalY < metaData.getPhysicalLowerLeft().getY())
            minPhysicalY = metaData.getPhysicalLowerLeft().getY();
        if (maxPhysicalX > metaData.getPhysicalUpperRight().getX())
            maxPhysicalX = metaData.getPhysicalUpperRight().getX();
        if (maxPhysicalY > metaData.getPhysicalUpperRight().getY())
            maxPhysicalY = metaData.getPhysicalUpperRight().getY();

        double regionWidth = maxPhysicalX - minPhysicalX;
        double regionHeight = maxPhysicalY - minPhysicalY;
        if (regionWidth > 0 && regionHeight > 0) {
            Region newRegion = StaticRegion.createAdaptedRegion(minPhysicalX, minPhysicalY, regionWidth, regionHeight);
            this.regionView.setRegion(newRegion, new ChangeEvent());
        } else {
            Region newRegion = StaticRegion.createAdaptedRegion(metaData.getPhysicalLowerLeft().getX(), metaData.getPhysicalLowerLeft().getY(), metaData.getPhysicalUpperRight().getX() - metaData.getPhysicalLowerLeft().getX(), metaData.getPhysicalUpperRight().getY() - metaData.getPhysicalLowerLeft().getY());
            this.regionView.setRegion(newRegion, new ChangeEvent());
            //Log.error("Illegal Region calculated! " + regionWidth + ":" + regionHeight + ". x = " + minPhysicalX + " - " + maxPhysicalX + ", y = " + minPhysicalY + " - " + maxPhysicalY);
        }
        this.markAsChanged();
    }

    protected GL3DImageTextureView getImageTextureView() {
        return this.imageTextureView;
    }

    public void setLayerGroup(GL3DImageLayers layers) {
        layerGroup = layers;
    }

    public GL3DImageLayers getLayerGroup() {
        return layerGroup;
    }

    public GL3DImageFragmentShaderProgram getSphereFragmentShader() {
        return sphereFragmentShader;
    }
}
