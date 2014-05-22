package org.helioviewer.gl3d.model.image;

import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.camera.GL3DCameraListener;
import org.helioviewer.gl3d.model.GL3DHitReferenceShape;
import org.helioviewer.gl3d.scenegraph.GL3DNode;
import org.helioviewer.gl3d.scenegraph.GL3DOrientedGroup;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
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

    protected GL gl;
    protected GL3DImageFragmentShaderProgram coronaFragmentShader = null;
    protected GL3DImageFragmentShaderProgram sphereFragmentShader = null;

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
    public void shapeDraw(GL3DState state) {
        super.shapeDraw(state);
    }

    @Override
    public void shapeInit(GL3DState state) {
        this.createImageMeshNodes(state.gl);

        this.accellerationShape = new GL3DHitReferenceShape(true, 0.);
        this.addNode(this.accellerationShape);

        super.shapeInit(state);
        this.doUpdateROI = true;
        this.markAsChanged();
        updateROI(state.getActiveCamera());

    }

    protected abstract void createImageMeshNodes(GL gl);

    protected abstract GL3DImageMesh getImageSphere();

    @Override
    public void shapeUpdate(GL3DState state) {
        super.shapeUpdate(state);
        if (doUpdateROI) {
            this.updateROI(state.getActiveCamera());
            doUpdateROI = false;
            this.accellerationShape.setUnchanged();
        }
    }

    @Override
    public void cameraMoved(GL3DCamera camera) {
        doUpdateROI = true;
        if (this.accellerationShape != null)
            this.accellerationShape.markAsChanged();
    }


    @Override
    public void cameraMoving(GL3DCamera camera) {
    }

    @Override
    public CoordinateSystem getCoordinateSystem() {
        return this.coordinateSystemView.getCoordinateSystem();
    }

    @Override
    public CoordinateVector getOrientation() {
        return this.coordinateSystemView.getOrientation();
    }

    private void updateROI(GL3DCamera activeCamera) {

        MetaData metaData = metaDataView.getMetaData();

        double llx = metaData.getPhysicalLowerLeft().getX();
        double lly = metaData.getPhysicalLowerLeft().getY();
        double urx = metaData.getPhysicalUpperRight().getX();
        double ury = metaData.getPhysicalUpperRight().getY();

        GL3DRayTracer rayTracer = new GL3DRayTracer(this.accellerationShape, activeCamera);

        // Shoot Rays in the corners of the viewport
        int width = (int) activeCamera.getWidth();
        int height = (int) activeCamera.getHeight();
        List<GL3DRay> regionTestRays = new ArrayList<GL3DRay>();

        // frame.setVisible(true);
        // frame1.setVisible(true);
        for (int i = 0; i <= 1; i++) {
            for (int j = 0; j <= 1; j++) {
                regionTestRays.add(rayTracer.cast(i * (width / 1), j * (height / 1)));
            }
        }

        double minPhysicalX = Double.MAX_VALUE;
        double minPhysicalY = Double.MAX_VALUE;
        double minPhysicalZ = Double.MAX_VALUE;
        double maxPhysicalX = -Double.MAX_VALUE;
        double maxPhysicalY = -Double.MAX_VALUE;
        double maxPhysicalZ = -Double.MAX_VALUE;

        GL3DMat4d phiRotation = GL3DMat4d.rotation(this.imageTextureView.phi, new GL3DVec3d(0, 1, 0));
        phiRotation.rotate(-this.imageTextureView.theta, new GL3DVec3d(0, 0, 1));

        for (GL3DRay ray : regionTestRays) {
            GL3DVec3d hitPoint = ray.getHitPoint();
            if (hitPoint != null) {
                hitPoint = this.wmI.multiply(hitPoint);
                double x = phiRotation.m[0] * hitPoint.x + phiRotation.m[4] * hitPoint.y + phiRotation.m[8] * hitPoint.z + phiRotation.m[12];
                double y = phiRotation.m[1] * hitPoint.x + phiRotation.m[5] * hitPoint.y + phiRotation.m[9] * hitPoint.z + phiRotation.m[13];
                double z = phiRotation.m[2] * hitPoint.x + phiRotation.m[6] * hitPoint.y + phiRotation.m[10] * hitPoint.z + phiRotation.m[14];
                minPhysicalX = Math.min(minPhysicalX, x);
                minPhysicalY = Math.min(minPhysicalY, y);
                minPhysicalZ = Math.min(minPhysicalZ, z);
                maxPhysicalX = Math.max(maxPhysicalX, x);
                maxPhysicalY = Math.max(maxPhysicalY, y);
                maxPhysicalZ = Math.max(maxPhysicalZ, z);
            }
        }

        minPhysicalX = Math.max(minPhysicalX, llx);
        minPhysicalY = Math.max(minPhysicalY, lly);
        maxPhysicalX = Math.min(maxPhysicalX, urx);
        maxPhysicalY = Math.min(maxPhysicalY, ury);

        minPhysicalX -= Math.abs(minPhysicalX) * 0.5;
        minPhysicalY -= Math.abs(minPhysicalY) * 0.5;
        maxPhysicalX += Math.abs(maxPhysicalX) * 0.5;
        maxPhysicalY += Math.abs(maxPhysicalY) * 0.5;

        minPhysicalX = Math.max(minPhysicalX, llx);
        minPhysicalY = Math.max(minPhysicalY, lly);
        maxPhysicalX = Math.min(maxPhysicalX, urx);
        maxPhysicalY = Math.min(maxPhysicalY, ury);

        double regionWidth = maxPhysicalX - minPhysicalX;
        double regionHeight = maxPhysicalY - minPhysicalY;

        if (regionWidth > 0 && regionHeight > 0) {
            Region newRegion = StaticRegion.createAdaptedRegion(minPhysicalX, minPhysicalY, regionWidth, regionHeight);
            this.regionView.setRegion(newRegion, new ChangeEvent());
        } else {
            Log.error("Illegal Region calculated! " + regionWidth + ":" + regionHeight + ". x = " + minPhysicalX + " - " + maxPhysicalX + ", y = " + minPhysicalY + " - " + maxPhysicalY);
        }
    }

    public void setCoronaVisibility(boolean visible) {

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
}
