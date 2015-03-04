package org.helioviewer.gl3d.model.image;

import java.awt.Point;

import javax.media.opengl.GL2;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.camera.GL3DCameraListener;
import org.helioviewer.gl3d.scenegraph.GL3DGroup;
import org.helioviewer.gl3d.scenegraph.GL3DShape;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DQuatd;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.opengl.GL3DImageTextureView;
import org.helioviewer.viewmodel.view.opengl.GL3DView;
import org.helioviewer.viewmodel.view.opengl.shader.GL3DImageFragmentShaderProgram;

/**
 * This is the scene graph equivalent of an image layer sub view chain attached
 * to the GL3DLayeredView. It represents exactly one image layer in the view
 * chain
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public abstract class GL3DImageLayer extends GL3DGroup implements GL3DCameraListener {
    private static int nextLayerId = 0;
    private final int layerId;
    private GL3DVec4d direction = new GL3DVec4d(0, 0, 1, 0);

    public int getLayerId() {
        return layerId;
    }

    protected GL3DView mainLayerView;
    protected GL3DImageTextureView imageTextureView;
    protected MetaDataView metaDataView;
    protected RegionView regionView;
    protected GL3DImageLayers layerGroup;
    public double minZ = -Constants.SunRadius;
    public double maxZ = Constants.SunRadius;

    protected boolean doUpdateROI = true;

    private final double lastViewAngle = 0.0;

    protected GL3DImageFragmentShaderProgram sphereFragmentShader = null;

    private int resolution;
    private final double[][] pointlist = new double[(resolution + 1) * 2 * 2][2];

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

        this.metaDataView = this.mainLayerView.getAdapter(MetaDataView.class);
        if (this.metaDataView == null) {
            throw new IllegalStateException("Cannot create GL3DImageLayer when no MetaDataView is present in Layer");
        }
        this.regionView = this.mainLayerView.getAdapter(RegionView.class);
        if (this.regionView == null) {
            throw new IllegalStateException("Cannot create GL3DImageLayer when no RegionView is present in Layer");
        }

        this.doUpdateROI = true;
        this.markAsChanged();
        int count = 0;
        for (int i = 0; i <= this.resolution; i++) {
            for (int j = 0; j <= 1; j++) {
                this.pointlist[count][0] = 1. * i / this.resolution;
                this.pointlist[count][1] = j;
                count++;
            }
        }
        for (int i = 0; i <= 1; i++) {
            for (int j = 0; j <= this.resolution; j++) {
                this.pointlist[count][0] = i / 1.;
                this.pointlist[count][1] = 1. * j / this.resolution;
                count++;
                System.out.println(count);
            }
        }
    }

    @Override
    public void shapeInit(GL3DState state) {
        this.createImageMeshNodes(state.gl);

        super.shapeInit(state);

        this.doUpdateROI = true;
        this.markAsChanged();
        updateROI(state);

        state.getActiveCamera().updateCameraTransformation();
    }

    protected abstract void createImageMeshNodes(GL2 gl);

    protected abstract GL3DImageMesh getImageSphere();

    @Override
    public void shapeUpdate(GL3DState state) {
        super.shapeUpdate(state);
        // Log.debug("GL3DImageLayer: '"+getName()+" is updating its ROI");
        this.updateROI(state);
        doUpdateROI = false;
    }

    @Override
    public void cameraMoved(GL3DCamera camera) {
        doUpdateROI = true;
    }

    public double getLastViewAngle() {
        return lastViewAngle;
    }

    @Override
    public void cameraMoving(GL3DCamera camera) {

    }

    public GL3DVec4d getLayerDirection() {
        return direction;
    }

    public void setLayerDirection(GL3DVec4d direction) {
        this.direction = direction;
    }

    public void updateROI(GL3DState state) {
        MetaData metaData = metaDataView.getMetaData();
        GL3DCamera activeCamera = state.getActiveCamera();
        HelioviewerMetaData hvmd = null;
        if (metaData instanceof HelioviewerMetaData) {
            hvmd = (HelioviewerMetaData) metaData;
        }
        if (metaData == null || activeCamera == null) {
            return;
        }

        double phi = hvmd.getPhi();
        double theta = hvmd.getTheta();
        //this.accellerationShape.setPhi(phi);
        //this.accellerationShape.setTheta(theta);

        GL3DQuatd rth = GL3DQuatd.createRotation(theta, GL3DVec3d.XAxis);
        rth.rotate(GL3DQuatd.createRotation(phi, GL3DVec3d.YAxis));
        GL3DMat4d rt = rth.toMatrix();

        int width = state.getViewportWidth();
        int height = state.getViewportHeight();
        double minPhysicalX = Double.MAX_VALUE;
        double minPhysicalY = Double.MAX_VALUE;
        double maxPhysicalX = -Double.MAX_VALUE;
        double maxPhysicalY = -Double.MAX_VALUE;

        for (int i = 0; i < pointlist.length; i++) {
            for (final boolean on : new boolean[] { false, true }) {
                GL3DVec3d hitPoint;
                if (on) {
                    hitPoint = activeCamera.getVectorFromSphere(new Point((int) (pointlist[i][0] * width), (int) (pointlist[i][1] * height)));
                } else {
                    hitPoint = activeCamera.getVectorFromPlane(new Point((int) (pointlist[i][0] * width), (int) (pointlist[i][1] * height)));
                }
                if (hitPoint != null) {
                    hitPoint = rt.multiply(hitPoint);
                    minPhysicalX = Math.min(minPhysicalX, hitPoint.x);
                    minPhysicalY = Math.min(minPhysicalY, hitPoint.y);
                    maxPhysicalX = Math.max(maxPhysicalX, hitPoint.x);
                    maxPhysicalY = Math.max(maxPhysicalY, hitPoint.y);
                }
            }
        }
        double widthxAdd = Math.abs((maxPhysicalX - minPhysicalX) * 0.1);
        double widthyAdd = Math.abs((maxPhysicalY - minPhysicalY) * 0.1);
        minPhysicalX = minPhysicalX - widthxAdd;
        maxPhysicalX = maxPhysicalX + widthxAdd;
        minPhysicalY = minPhysicalY - widthyAdd;
        maxPhysicalY = maxPhysicalY + widthyAdd;

        double metLLX = metaData.getPhysicalLowerLeft().getX();
        double metLLY = metaData.getPhysicalLowerLeft().getY();
        double metURX = metaData.getPhysicalUpperRight().getX();
        double metURY = metaData.getPhysicalUpperRight().getY();

        if (minPhysicalX < metLLX)
            minPhysicalX = metLLX;
        if (minPhysicalY < metLLY)
            minPhysicalY = metLLY;
        if (maxPhysicalX > metURX)
            maxPhysicalX = metURX;
        if (maxPhysicalY > metURY)
            maxPhysicalY = metURY;

        double regionWidth = maxPhysicalX - minPhysicalX;
        double regionHeight = maxPhysicalY - minPhysicalY;
        Region newRegion;
        if (regionWidth > 0 && regionHeight > 0) {
            newRegion = StaticRegion.createAdaptedRegion(minPhysicalX, minPhysicalY, regionWidth, regionHeight);
        } else {
            newRegion = StaticRegion.createAdaptedRegion(metLLX, metLLY, metURX - metLLX, metURY - metLLY);
        }
        this.regionView.setRegion(newRegion, new ChangeEvent());

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

    protected GL3DShape getImageCorona() {
        return null;
    }

    public void setCoronaVisibility(boolean visible) {
    }

    public GL3DVec3d convertViewportToPlane(GL3DVec3d viewportCoordinates, GL3DMat4d projectionMatrix) {
        GL3DMat4d vpm = projectionMatrix.inverse();
        GL3DVec3d planeCoordinates = vpm.multiply(viewportCoordinates);
        return planeCoordinates;
    }

    public GL3DVec3d convertViewportToView(GL3DVec3d viewportCoordinates, GL3DMat4d projectionMatrix, GL3DMat4d translationMatrix, GL3DState state) {
        GL3DMat4d vpm = projectionMatrix.inverse();
        GL3DMat4d tli = translationMatrix.inverse();

        GL3DVec4d centeredViewportCoordinates = new GL3DVec4d(2. * (viewportCoordinates.x / state.getViewportWidth()), 2. * (viewportCoordinates.y / state.getViewportHeight()), 0., 0.);
        GL3DVec4d solarCoordinates = vpm.multiply(centeredViewportCoordinates);
        solarCoordinates.w = 1.;
        solarCoordinates = tli.multiply(solarCoordinates);
        solarCoordinates.w = 0.;

        double solarCoordinates3Dz = Math.sqrt(1 - solarCoordinates.dot(solarCoordinates));
        if (solarCoordinates3Dz == Double.NaN) {
            solarCoordinates3Dz = 0.;
        }
        GL3DVec3d solarCoordinates3D = new GL3DVec3d(solarCoordinates.x, solarCoordinates.y, solarCoordinates3Dz);
        return solarCoordinates3D;
    }
}
