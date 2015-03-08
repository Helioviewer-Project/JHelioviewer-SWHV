package org.helioviewer.gl3d.model.image;

import java.awt.Point;

import javax.media.opengl.GL2;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.math.GL3DMat4d;
import org.helioviewer.gl3d.math.GL3DQuatd;
import org.helioviewer.gl3d.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.GL3DGroup;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.opengl.GL3DImageTextureView;
import org.helioviewer.viewmodel.view.opengl.GL3DView;

/**
 * This is the scene graph equivalent of an image layer sub view chain attached
 * to the GL3DLayeredView. It represents exactly one image layer in the view
 * chain
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DImageLayer extends GL3DGroup {

    private GL3DImageSphere sphere;
    private static int nextLayerId = 0;
    private final int layerId;

    public int getLayerId() {
        return layerId;
    }

    protected GL3DView mainLayerView;
    protected GL3DImageTextureView imageTextureView;
    protected MetaDataView metaDataView;
    protected RegionView regionView;
    public double minZ = -Constants.SunRadius;
    public double maxZ = Constants.SunRadius;

    private final int resolution = 6;
    private final double[][] pointlist = new double[(resolution + 1) * 2 * 2][2];

    public GL3DImageLayer(String name, GL3DView mainLayerView) {
        super(name);
        layerId = nextLayerId++;

        this.mainLayerView = mainLayerView;
        this.imageTextureView = this.mainLayerView.getAdapter(GL3DImageTextureView.class);
        this.metaDataView = this.mainLayerView.getAdapter(MetaDataView.class);
        this.regionView = this.mainLayerView.getAdapter(RegionView.class);

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
            }
        }
    }

    private void createImageMeshNodes(GL2 gl) {
        sphere = new GL3DImageSphere(imageTextureView, this, true, true, false);
        this.addNode(sphere);
    }

    protected GL3DImageSphere getImageSphere() {
        return this.sphere;
    }

    @Override
    public void shapeInit(GL3DState state) {
        this.createImageMeshNodes(state.gl);

        super.shapeInit(state);

        this.markAsChanged();
        updateROI(state);

        state.getActiveCamera().updateCameraTransformation();
    }

    @Override
    public void shapeUpdate(GL3DState state) {
        super.shapeUpdate(state);
        this.updateROI(state);
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

    public void setCoronaVisibility(boolean visible) {
        this.getImageSphere().setCoronaVisibility(visible);
    }

}
