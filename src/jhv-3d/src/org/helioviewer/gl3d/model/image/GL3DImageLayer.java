package org.helioviewer.gl3d.model.image;

import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;

import org.helioviewer.base.logging.Log;
import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.camera.GL3DCameraListener;
import org.helioviewer.gl3d.model.GL3DHitReferenceShape;
import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.gl3d.scenegraph.GL3DNode;
import org.helioviewer.gl3d.scenegraph.GL3DOrientedGroup;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRay;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRayTracer;
import org.helioviewer.gl3d.view.GL3DCoordinateSystemView;
import org.helioviewer.gl3d.view.GL3DImageTextureView;
import org.helioviewer.gl3d.view.GL3DView;
import org.helioviewer.gl3d.wcs.CoordinateConversion;
import org.helioviewer.gl3d.wcs.CoordinateSystem;
import org.helioviewer.gl3d.wcs.CoordinateVector;
import org.helioviewer.gl3d.wcs.HeliocentricCartesianCoordinateSystem;
import org.helioviewer.jhv.internal_plugins.filter.opacity.DynamicOpacityFilter;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.filter.Filter;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.opengl.GLFilterView;

/**
 * This is the scene graph equivalent of an image layer sub view chain attached
 * to the GL3DLayeredView. It represents exactly one image layer in the view
 * chain
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public abstract class GL3DImageLayer extends GL3DOrientedGroup implements GL3DCameraListener {
    private static int nextLayerId = 0;
    private final int layerId;

    public int getLayerId() {
        return layerId;
    }

    protected GL3DView mainLayerView;
    protected GL3DImageTextureView imageTextureView;
    protected GL3DCoordinateSystemView coordinateSystemView;
    protected MetaDataView metaDataView;
    protected RegionView regionView;

    protected GL3DNode accellerationShape;

    protected boolean doUpdateROI = true;

    private DynamicOpacityFilter opacityFilter;

    private double lastViewAngle = 0.0;

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

        initOpacityFilter(mainLayerView);

        getCoordinateSystem().addListener(this);
        this.doUpdateROI = true;
        this.markAsChanged();
    }

    private void initOpacityFilter(View mainLayerView) {
        // Get opacity filter from view chain
        View filterView = mainLayerView;
        while ((filterView = filterView.getAdapter(GLFilterView.class)) != null && opacityFilter == null) {
            Filter filter = ((GLFilterView) filterView).getFilter();
            if (DynamicOpacityFilter.class.isInstance(filter))
                opacityFilter = (DynamicOpacityFilter) filter;
        }

        if (opacityFilter == null)
            throw new IllegalStateException("Cannot create GL3DImageLayer when no DynamicOpacityFilter is present in ViewChain");
    }

    public void shapeInit(GL3DState state) {
        this.createImageMeshNodes(state.gl);

        this.accellerationShape = new GL3DHitReferenceShape();
        this.addNode(this.accellerationShape);

        super.shapeInit(state);

        this.doUpdateROI = true;
        this.markAsChanged();
        updateROI(state.getActiveCamera());
    }

    protected abstract void createImageMeshNodes(GL gl);

    protected abstract GL3DImageMesh getImageCorona();

    protected abstract GL3DImageMesh getImageSphere();

    public void shapeUpdate(GL3DState state) {
        super.shapeUpdate(state);
        if (doUpdateROI) {
            // Log.debug("GL3DImageLayer: '"+getName()+" is updating its ROI");
            this.updateROI(state.getActiveCamera());
            doUpdateROI = false;
            this.accellerationShape.setUnchanged();
        }
    }

    public void cameraMoved(GL3DCamera camera) {
        doUpdateROI = true;
        this.accellerationShape.markAsChanged();
        //markAsChanged();

        cameraMoving(camera);
    }

    public double getLastViewAngle() {
        return lastViewAngle;
    }

    public void cameraMoving(GL3DCamera camera) {
        // try {
        // Calculate camera direction vector
        GL3DMat4d camTrans = camera.getRotation().toMatrix().inverse();
        GL3DVec4d camDirection = new GL3DVec4d(0, 0, 1, 1);
        camDirection = camTrans.multiply(camDirection);
        camDirection.w = 0;
        camDirection.normalize();

        // Convert layer orientation to heliocentric coordinate system
        CoordinateVector orientation = coordinateSystemView.getOrientation();
        CoordinateSystem targetSystem = new HeliocentricCartesianCoordinateSystem();
        CoordinateConversion converter = orientation.getCoordinateSystem().getConversion(targetSystem);
        orientation = converter.convert(orientation);

        GL3DVec4d layerDirection = new GL3DVec4d(orientation.getValue(0), orientation.getValue(1), orientation.getValue(2), 0);
        layerDirection.normalize();

        // Calculate view angle
        double cos = camDirection.dot(layerDirection);
        lastViewAngle = Math.acos(cos) / 3.141592654 * 180.0;

        // angle can be NaN!
        if (Double.isNaN(lastViewAngle))
            lastViewAngle = 0.0;

        //opacityFilter.setOpacity((float) ((180.0 - lastViewAngle) / 180.0));

    }

    public CoordinateSystem getCoordinateSystem() {
        return this.coordinateSystemView.getCoordinateSystem();
    }

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

        // Shoot Rays in the corners of the viewport
        int width = (int) activeCamera.getWidth();
        int height = (int) activeCamera.getHeight();
        List<GL3DRay> regionTestRays = new ArrayList<GL3DRay>();
        regionTestRays.add(rayTracer.cast(0, height)); // Lower Left
        regionTestRays.add(rayTracer.cast(width, 0)); // Upper Right
        regionTestRays.add(rayTracer.cast(0, 0)); // Upper Left
        regionTestRays.add(rayTracer.cast(width, height)); // Lower Right

        regionTestRays.add(rayTracer.cast(width, height / 2)); // Right
        regionTestRays.add(rayTracer.cast(0, height / 2)); // Left
        regionTestRays.add(rayTracer.cast(width / 2, 0)); // top
        regionTestRays.add(rayTracer.cast(width / 2, height)); // bottom

        double minPhysicalX = Double.MAX_VALUE;
        double minPhysicalY = Double.MAX_VALUE;
        double maxPhysicalX = -Double.MAX_VALUE;
        double maxPhysicalY = -Double.MAX_VALUE;

        for (GL3DRay ray : regionTestRays) {
            GL3DVec3d hitPoint = ray.getHitPoint();
            if (hitPoint != null) {

                hitPoint = this.wmI.multiply(hitPoint);

                double x = hitPoint.x;
                double y = hitPoint.y;
                minPhysicalX = Math.min(minPhysicalX, x);
                minPhysicalY = Math.min(minPhysicalY, y);
                maxPhysicalX = Math.max(maxPhysicalX, x);
                maxPhysicalY = Math.max(maxPhysicalY, y);
                // Log.debug("GL3DImageLayer: Hitpoint: "+hitPoint+" - "+ray.isOnSun);
            }
        }

        // Restrict maximal region to physically available region
        minPhysicalX = Math.max(minPhysicalX, metaData.getPhysicalLowerLeft().getX());
        minPhysicalY = Math.max(minPhysicalY, metaData.getPhysicalLowerLeft().getY());
        maxPhysicalX = Math.min(maxPhysicalX, metaData.getPhysicalUpperRight().getX());
        maxPhysicalY = Math.min(maxPhysicalY, metaData.getPhysicalUpperRight().getY());

        double regionWidth = maxPhysicalX - minPhysicalX;
        double regionHeight = maxPhysicalY - minPhysicalY;
        if (regionWidth > 0 && regionHeight > 0) {
            Region newRegion = StaticRegion.createAdaptedRegion(minPhysicalX, minPhysicalY, regionWidth, regionHeight);
            // Log.debug("GL3DImageLayer: '"+getName()+" set its region");
            this.regionView.setRegion(newRegion, new ChangeEvent());
        } else {
            Log.error("Illegal Region calculated! " + regionWidth + ":" + regionHeight + ". x = " + minPhysicalX + " - " + maxPhysicalX + ", y = " + minPhysicalY + " - " + maxPhysicalY);
        }

    }

    public void setCoronaVisibility(boolean visible) {
        GL3DNode node = this.first;
        while (node != null) {
            if (node instanceof GL3DImageCorona) {
                node.getDrawBits().set(Bit.Hidden, !visible);
            }

            node = node.getNext();
        }
    }

    protected GL3DImageTextureView getImageTextureView() {
        return this.imageTextureView;
    }
}
