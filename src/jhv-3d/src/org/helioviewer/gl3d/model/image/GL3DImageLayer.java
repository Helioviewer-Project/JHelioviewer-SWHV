package org.helioviewer.gl3d.model.image;

import java.awt.Point;
import java.util.ArrayList;

import javax.media.opengl.GL2;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.camera.GL3DCameraListener;
import org.helioviewer.gl3d.model.GL3DHitReferenceShape;
import org.helioviewer.gl3d.scenegraph.GL3DGroup;
import org.helioviewer.gl3d.scenegraph.GL3DShape;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DQuatd;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRay;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRayTracer;
import org.helioviewer.gl3d.shader.GL3DImageFragmentShaderProgram;
import org.helioviewer.gl3d.view.GL3DImageTextureView;
import org.helioviewer.gl3d.view.GL3DView;
import org.helioviewer.jhv.gui.states.StateController;
import org.helioviewer.jhv.gui.states.ViewStateEnum;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;

/**
 * This is the scene graph equivalent of an image layer sub view chain attached
 * to the GL3DLayeredView. It represents exactly one image layer in the view
 * chain
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public abstract class GL3DImageLayer extends GL3DGroup implements GL3DCameraListener, ViewListener {
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

    protected GL3DHitReferenceShape accellerationShape;

    protected boolean doUpdateROI = true;

    private final ArrayList<Point> points = new ArrayList<Point>();

    private final double lastViewAngle = 0.0;

    protected GL2 gl;
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

        this.metaDataView = this.mainLayerView.getAdapter(MetaDataView.class);
        if (this.metaDataView == null) {
            throw new IllegalStateException("Cannot create GL3DImageLayer when no MetaDataView is present in Layer");
        }
        this.regionView = this.mainLayerView.getAdapter(RegionView.class);
        if (this.regionView == null) {
            throw new IllegalStateException("Cannot create GL3DImageLayer when no RegionView is present in Layer");
        }

        this.accellerationShape = new GL3DHitReferenceShape(false);

        this.doUpdateROI = true;
        this.markAsChanged();
    }

    @Override
    public void shapeInit(GL3DState state) {
        this.createImageMeshNodes(state.gl);

        this.addNode(this.accellerationShape);

        super.shapeInit(state);

        this.doUpdateROI = true;
        this.markAsChanged();
        updateROI(state.getActiveCamera());

        state.getActiveCamera().updateCameraTransformation();
    }

    protected abstract void createImageMeshNodes(GL2 gl);

    protected abstract GL3DImageMesh getImageSphere();

    @Override
    public void shapeUpdate(GL3DState state) {
        super.shapeUpdate(state);
        // Log.debug("GL3DImageLayer: '"+getName()+" is updating its ROI");
        this.updateROI(state.getActiveCamera());
        doUpdateROI = false;
        this.accellerationShape.setUnchanged();
    }

    @Override
    public void cameraMoved(GL3DCamera camera) {
        doUpdateROI = true;
        this.accellerationShape.markAsChanged();
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

    public void updateROI(GL3DCamera activeCamera) {
        MetaData metaData = metaDataView.getMetaData();
        HelioviewerMetaData hvmd = null;
        if (metaData instanceof HelioviewerMetaData) {
            hvmd = (HelioviewerMetaData) metaData;
        }
        if (metaData == null || activeCamera == null) {
            return;
        }
        double phi = 0;
        double theta = 0;
        phi = hvmd.getPhi();
        theta = hvmd.getTheta();
        this.accellerationShape.setPhi(phi);
        this.accellerationShape.setTheta(theta);
        //GL3DQuatd rth = GL3DQuatd.createRotation(phi, new GL3DVec3d(0, 1, 0));
        //rth.rotate(GL3DQuatd.createRotation(theta, new GL3DVec3d(1, 0, 0)));

        GL3DQuatd rth = GL3DQuatd.createRotation(theta, new GL3DVec3d(1, 0, 0));
        rth.rotate(GL3DQuatd.createRotation(phi, new GL3DVec3d(0, 1, 0)));
        GL3DMat4d rt = rth.toMatrix();
        GL3DRayTracer rayTracer = new GL3DRayTracer(this.accellerationShape, activeCamera);

        int width = (int) activeCamera.getWidth();
        int height = (int) activeCamera.getHeight();
        double minPhysicalX = Double.MAX_VALUE;
        double minPhysicalY = Double.MAX_VALUE;
        double maxPhysicalX = -Double.MAX_VALUE;
        double maxPhysicalY = -Double.MAX_VALUE;

        double res = 10.;
        boolean addpoints = false;

        for (int i = 0; i <= res; i++) {
            for (int j = 0; j <= 1; j++) {
                for (final boolean on : new boolean[] { false, true }) {
                    this.accellerationShape.setHitCoronaPlane(on);
                    GL3DRay ray = rayTracer.cast((int) (i * width / res), (int) (j * height / 1.));
                    GL3DVec3d hitPoint = ray.getHitPoint();
                    if (hitPoint != null) {
                        hitPoint = ray.getHitPoint();
                        hitPoint = rt.multiply(hitPoint);
                        minPhysicalX = Math.min(minPhysicalX, hitPoint.x);
                        minPhysicalY = Math.min(minPhysicalY, hitPoint.y);
                        maxPhysicalX = Math.max(maxPhysicalX, hitPoint.x);
                        maxPhysicalY = Math.max(maxPhysicalY, hitPoint.y);
                    }
                }
            }
        }
        for (int i = 0; i <= 1; i++) {
            for (int j = 0; j <= res; j++) {
                for (final boolean on : new boolean[] { false, true }) {
                    this.accellerationShape.setHitCoronaPlane(on);
                    GL3DRay ray = rayTracer.cast((int) (i * width / 1.), (int) (j * height / res));
                    GL3DVec3d hitPoint = ray.getHitPoint();
                    if (hitPoint != null) {
                        hitPoint = ray.getHitPoint();
                        hitPoint = rt.multiply(hitPoint);

                        minPhysicalX = Math.min(minPhysicalX, hitPoint.x);
                        minPhysicalY = Math.min(minPhysicalY, hitPoint.y);
                        maxPhysicalX = Math.max(maxPhysicalX, hitPoint.x);
                        maxPhysicalY = Math.max(maxPhysicalY, hitPoint.y);
                    }
                }
            }
        }

        double widthxAdd = Math.abs((maxPhysicalX - minPhysicalX) * 0.1);
        double widthyAdd = Math.abs((maxPhysicalY - minPhysicalY) * 0.1);
        minPhysicalX = minPhysicalX - widthxAdd;
        maxPhysicalX = maxPhysicalX + widthxAdd;
        minPhysicalY = minPhysicalY - widthyAdd;
        maxPhysicalY = maxPhysicalY + widthyAdd;

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
        Region newRegion;
        if (regionWidth > 0 && regionHeight > 0) {
            newRegion = StaticRegion.createAdaptedRegion(minPhysicalX, minPhysicalY, regionWidth, regionHeight);
        } else {
            newRegion = StaticRegion.createAdaptedRegion(metaData.getPhysicalLowerLeft().getX(), metaData.getPhysicalLowerLeft().getY(), metaData.getPhysicalUpperRight().getX() - metaData.getPhysicalLowerLeft().getX(), metaData.getPhysicalUpperRight().getY() - metaData.getPhysicalLowerLeft().getY());
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

    @Override
    public void viewChanged(View sender, ChangeEvent aEvent) {
        if (StateController.getInstance().getCurrentState().getType() == ViewStateEnum.View3D) {
            this.updateROI(GL3DState.get().getActiveCamera());
        }
    }

    protected GL3DShape getImageCorona() {
        return null;
    }

    public void setCoronaVisibility(boolean visible) {
    }
}
