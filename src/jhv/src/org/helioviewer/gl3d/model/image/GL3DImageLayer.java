package org.helioviewer.gl3d.model.image;

import java.awt.Point;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import javax.media.opengl.GL2;

import org.helioviewer.base.math.Pair;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.math.GL3DMat4d;
import org.helioviewer.gl3d.math.GL3DQuatd;
import org.helioviewer.gl3d.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.GL3DShape;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.jhv.gui.states.StateController;
import org.helioviewer.jhv.gui.states.ViewStateEnum;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.ImageTextureRecapturedReason;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.opengl.GL3DImageTextureView;
import org.helioviewer.viewmodel.view.opengl.GL3DView;
import org.helioviewer.viewmodel.view.opengl.shader.GLSLShader;
import org.helioviewer.viewmodel.viewport.StaticViewport;
import org.helioviewer.viewmodel.viewport.Viewport;
import org.helioviewer.viewmodel.viewport.ViewportAdapter;

import com.jogamp.common.nio.Buffers;

/**
 * This is the scene graph equivalent of an image layer sub view chain attached
 * to the GL3DLayeredView. It represents exactly one image layer in the view
 * chain
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DImageLayer extends GL3DShape {

    private static int nextLayerId = 0;
    private final int layerId;

    public int getLayerId() {
        return layerId;
    }

    protected GL3DView mainLayerView;
    protected GL3DImageTextureView imageTextureView;
    protected MetaDataView metaDataView;
    protected JHVJPXView regionView;
    public double minZ = -Constants.SunRadius;
    public double maxZ = Constants.SunRadius;

    private final int resolution = 6;
    private final double[][] pointlist = new double[(resolution + 1) * 2 * 2][2];
    private final boolean showSphere;
    private boolean showCorona;
    private int positionBufferID;
    private int indexBufferID;
    private int indexBufferSize;

    private int positionBufferSize;

    public GL3DImageLayer(String name, GL3DView mainLayerView, boolean showSphere, boolean showCorona, boolean restoreColorMask) {
        super(name);
        layerId = nextLayerId++;

        this.mainLayerView = mainLayerView;
        this.imageTextureView = this.mainLayerView.getAdapter(GL3DImageTextureView.class);
        this.metaDataView = this.mainLayerView.getAdapter(MetaDataView.class);
        this.regionView = this.mainLayerView.getAdapter(JHVJPXView.class);

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

        imageTextureView.addViewListener(new ViewListener() {
            @Override
            public void viewChanged(View sender, ChangeEvent aEvent) {
                ImageTextureRecapturedReason reason = aEvent.getLastChangedReasonByType(ImageTextureRecapturedReason.class);
                if (reason != null) {
                    markAsChanged();
                }
            }
        });
        this.markAsChanged();
        this.showSphere = showSphere;
        this.showCorona = showCorona;
    }

    @Override
    public void shapeInit(GL3DState state) {
        Pair<FloatBuffer, IntBuffer> bufferPair = this.makeIcosphere(3);
        FloatBuffer positionBuffer = bufferPair.a;
        IntBuffer indexBuffer = bufferPair.b;

        this.positionBufferSize = positionBuffer.capacity();
        positionBufferID = generate(state);

        state.gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, positionBufferID);
        state.gl.glBufferData(GL2.GL_ARRAY_BUFFER, this.positionBufferSize * Buffers.SIZEOF_FLOAT, positionBuffer, GL2.GL_STATIC_DRAW);

        indexBufferID = generate(state);
        indexBufferSize = indexBuffer.capacity();
        state.gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBufferID);
        state.gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.capacity() * Buffers.SIZEOF_INT, indexBuffer, GL2.GL_STATIC_DRAW);
        this.imageTextureView.forceUpdate();

        this.markAsChanged();
        updateROI(state);

        state.getActiveCamera().updateCameraTransformation();
    }

    @Override
    public void shapeUpdate(GL3DState state) {
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
        Viewport layerViewport = new ViewportAdapter(new StaticViewport(state.getViewportWidth(), state.getViewportHeight()));
        this.regionView.setViewport(layerViewport, null);
        this.markAsChanged();

    }

    protected GL3DImageTextureView getImageTextureView() {
        return this.imageTextureView;
    }

    @Override
    public void shapeDraw(GL3DState state) {
        GL2 gl = state.gl;
        GLSLShader.bind(gl);
        GLSLShader.bindVars(gl);

        gl.glEnable(GL2.GL_CULL_FACE);
        {
            gl.glCullFace(GL2.GL_BACK);

            gl.glEnable(GL2.GL_BLEND);
            JHVJP2View jp2view = this.imageTextureView.getAdapter(JHVJP2View.class);

            if (jp2view != null) {
                jp2view.applyFilters(gl);
            }
            GLSLShader.filter(gl);
            GLSLShader.bind(gl);

            enablePositionVBO(state);
            enableIndexVBO(state);
            {
                gl.glVertexPointer(3, GL2.GL_FLOAT, 3 * Buffers.SIZEOF_FLOAT, 0);
                if (this.showCorona) {
                    gl.glDepthRange(1.f, 1.f);
                    gl.glDrawElements(GL2.GL_TRIANGLES, 6, GL2.GL_UNSIGNED_INT, (this.indexBufferSize - 6) * Buffers.SIZEOF_INT);
                    gl.glDepthRange(0.f, 1.f);
                }
                if (this.showSphere && StateController.getInstance().getCurrentState() == ViewStateEnum.View3D.getState()) {
                    gl.glDrawElements(GL2.GL_TRIANGLES, this.indexBufferSize - 6, GL2.GL_UNSIGNED_INT, 0);
                }
            }
            disableIndexVBO(state);
            disablePositionVBO(state);
            GLSLShader.unbind(gl);

            gl.glColorMask(true, true, true, true);
        }
        gl.glDisable(GL2.GL_CULL_FACE);
        this.updateROI(state);
    }

    private int generate(GL3DState state) {
        int[] tmpId = new int[1];
        state.gl.glGenBuffers(1, tmpId, 0);
        return tmpId[0];
    }

    private void enableIndexVBO(GL3DState state) {
        state.gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBufferID);
    }

    private void disableIndexVBO(GL3DState state) {
        state.gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    private void enablePositionVBO(GL3DState state) {
        state.gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        state.gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, positionBufferID);
    }

    private void disablePositionVBO(GL3DState state) {
        state.gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        state.gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
    }

    private void deletePositionVBO(GL3DState state) {
        state.gl.glDeleteBuffers(1, new int[] { this.positionBufferID }, 0);
    }

    private void deleteIndexVBO(GL3DState state) {
        state.gl.glDeleteBuffers(1, new int[] { this.indexBufferID }, 0);
    }

    @Override
    public void shapeDelete(GL3DState state) {
        disablePositionVBO(state);
        disableIndexVBO(state);
        deletePositionVBO(state);
        deleteIndexVBO(state);
    }

    private Pair<FloatBuffer, IntBuffer> makeIcosphere(int level) {
        float t = (float) ((Math.sqrt(5) - 1) / 2);
        float[][] icosahedronVertexList = new float[][] { new float[] { -1, -t, 0 }, new float[] { 0, 1, t }, new float[] { 0, 1, -t }, new float[] { 1, t, 0 }, new float[] { 1, -t, 0 }, new float[] { 0, -1, -t }, new float[] { 0, -1, t }, new float[] { t, 0, 1 }, new float[] { -t, 0, 1 }, new float[] { t, 0, -1 }, new float[] { -t, 0, -1 }, new float[] { -1, t, 0 }, };
        for (float[] v : icosahedronVertexList) {
            float length = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
            v[0] /= length;
            v[1] /= length;
            v[2] /= length;
        }
        int[][] icosahedronFaceList = new int[][] { { 3, 7, 1 }, { 4, 7, 3 }, { 6, 7, 4 }, { 8, 7, 6 }, { 7, 8, 1 }, { 9, 4, 3 }, { 2, 9, 3 }, { 2, 3, 1 }, { 11, 2, 1 }, { 10, 2, 11 }, { 10, 9, 2 }, { 9, 5, 4 }, { 6, 4, 5 }, { 0, 6, 5 }, { 0, 11, 8 }, { 11, 1, 8 }, { 10, 0, 5 }, { 10, 5, 9 }, { 0, 8, 6 }, { 0, 10, 11 }, };
        ArrayList<Float> vertices = new ArrayList<Float>();
        ArrayList<Integer> faceIndices = new ArrayList<Integer>();
        for (float[] v : icosahedronVertexList) {
            vertices.add(v[0]);
            vertices.add(v[2]);
            vertices.add(v[1]);
        }
        for (int[] f : icosahedronFaceList) {
            subdivide(f[0], f[1], f[2], vertices, faceIndices, level);
        }
        int beginPositionNumberCorona = vertices.size() / 3;
        vertices.add(-40f);
        vertices.add(40f);
        vertices.add(0f);

        vertices.add(40f);
        vertices.add(40f);
        vertices.add(0f);

        vertices.add(40f);
        vertices.add(-40f);
        vertices.add(0f);

        vertices.add(-40f);
        vertices.add(-40f);
        vertices.add(0f);

        faceIndices.add(beginPositionNumberCorona + 0);
        faceIndices.add(beginPositionNumberCorona + 2);
        faceIndices.add(beginPositionNumberCorona + 1);

        faceIndices.add(beginPositionNumberCorona + 2);
        faceIndices.add(beginPositionNumberCorona + 0);
        faceIndices.add(beginPositionNumberCorona + 3);
        FloatBuffer positionBuffer = FloatBuffer.allocate(vertices.size());
        for (Float vert : vertices) {
            positionBuffer.put(vert);
        }
        positionBuffer.flip();
        IntBuffer indexBuffer = IntBuffer.allocate(faceIndices.size());

        for (int i : faceIndices) {
            indexBuffer.put(i);
        }
        indexBuffer.flip();

        return new Pair<FloatBuffer, IntBuffer>(positionBuffer, indexBuffer);
    }

    private void subdivide(int vx, int vy, int vz, ArrayList<Float> vertexList, ArrayList<Integer> faceList, int level) {
        if (level != 0) {
            float x1 = (vertexList.get(3 * vx) + vertexList.get(3 * vy));
            float y1 = (vertexList.get(3 * vx + 1) + vertexList.get(3 * vy + 1));
            float z1 = (vertexList.get(3 * vx + 2) + vertexList.get(3 * vy + 2));
            float length = (float) Math.sqrt(x1 * x1 + y1 * y1 + z1 * z1);
            x1 /= length;
            y1 /= length;
            z1 /= length;
            int firstIndex = vertexList.size() / 3;
            vertexList.add(x1);
            vertexList.add(y1);
            vertexList.add(z1);

            float x2 = (vertexList.get(3 * vz) + vertexList.get(3 * vy));
            float y2 = (vertexList.get(3 * vz + 1) + vertexList.get(3 * vy + 1));
            float z2 = (vertexList.get(3 * vz + 2) + vertexList.get(3 * vy + 2));
            length = (float) Math.sqrt(x2 * x2 + y2 * y2 + z2 * z2);
            x2 /= length;
            y2 /= length;
            z2 /= length;
            int secondIndex = vertexList.size() / 3;
            vertexList.add(x2);
            vertexList.add(y2);
            vertexList.add(z2);

            float x3 = (vertexList.get(3 * vx) + vertexList.get(3 * vz));
            float y3 = (vertexList.get(3 * vx + 1) + vertexList.get(3 * vz + 1));
            float z3 = (vertexList.get(3 * vx + 2) + vertexList.get(3 * vz + 2));
            length = (float) Math.sqrt(x3 * x3 + y3 * y3 + z3 * z3);
            x3 /= length;
            y3 /= length;
            z3 /= length;
            int thirdIndex = vertexList.size() / 3;
            vertexList.add(x3);
            vertexList.add(y3);
            vertexList.add(z3);

            subdivide(vx, firstIndex, thirdIndex, vertexList, faceList, level - 1);
            subdivide(firstIndex, vy, secondIndex, vertexList, faceList, level - 1);
            subdivide(thirdIndex, secondIndex, vz, vertexList, faceList, level - 1);
            subdivide(firstIndex, secondIndex, thirdIndex, vertexList, faceList, level - 1);
        } else {
            faceList.add(vx);
            faceList.add(vy);
            faceList.add(vz);
        }
    }

    public void setCoronaVisibility(boolean visible) {
        this.showCorona = visible;
    }
}
