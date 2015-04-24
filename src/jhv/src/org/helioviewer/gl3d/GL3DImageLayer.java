package org.helioviewer.gl3d;

import java.awt.Component;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import javax.media.opengl.GL2;

import org.helioviewer.base.Pair;
import org.helioviewer.base.math.GL3DMat4d;
import org.helioviewer.base.math.GL3DQuatd;
import org.helioviewer.base.math.GL3DVec2d;
import org.helioviewer.base.math.GL3DVec3d;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.filters.FilterPanelContainer;
import org.helioviewer.jhv.plugin.renderable.Renderable;
import org.helioviewer.jhv.plugin.renderable.RenderableType;
import org.helioviewer.jhv.renderable.RenderableImageType;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.AbstractView;
import org.helioviewer.viewmodel.view.opengl.GLInfo;
import org.helioviewer.viewmodel.view.opengl.GLSLShader;
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
public class GL3DImageLayer implements Renderable {

    private static boolean showCorona = true;
    private static int nextLayerId = 0;
    private final int layerId;

    public int getLayerId() {
        return layerId;
    }

    public double minZ = -Constants.SunRadius;
    public double maxZ = Constants.SunRadius;

    private final int resolution = 3;
    private final GL3DVec2d[] pointlist = new GL3DVec2d[(resolution + 1) * 2 * 2];
    private int positionBufferID;
    private int indexBufferID;
    private int indexBufferSize;

    private int positionBufferSize;
    private final AbstractView mainLayerView;
    private final RenderableType type;
    private boolean isVisible = true;

    public GL3DImageLayer(String name, AbstractView view) {
        this.type = new RenderableImageType(view.getName());
        layerId = nextLayerId++;
        this.mainLayerView = view;

        int count = 0;
        for (int i = 0; i <= this.resolution; i++) {
            for (int j = 0; j <= 1; j++) {
                this.pointlist[count] = new GL3DVec2d(2. * (1. * i / this.resolution - 0.5), -2. * (j - 0.5));
                count++;
            }
        }
        for (int i = 0; i <= 1; i++) {
            for (int j = 0; j <= this.resolution; j++) {
                this.pointlist[count] = new GL3DVec2d(2. * (i / 1. - 0.5), -2. * (1. * j / this.resolution - 0.5));
                count++;
            }
        }

        Displayer.getRenderablecontainer().addBeforeRenderable(this);
        mainLayerView.setOpacity((float) (1. / (1. + Displayer.getLayersModel().getNumLayers())));
    }

    @Override
    public void init(GL3DState state) {
        Pair<FloatBuffer, IntBuffer> bufferPair = makeIcosphere(2);
        FloatBuffer positionBuffer = bufferPair.a;
        IntBuffer indexBuffer = bufferPair.b;

        this.positionBufferSize = positionBuffer.capacity();
        positionBufferID = generate(state);

        state.gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, positionBufferID);
        state.gl.glBufferData(GL2.GL_ARRAY_BUFFER, this.positionBufferSize * Buffers.SIZEOF_FLOAT, positionBuffer, GL2.GL_STATIC_DRAW);
        state.gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);

        indexBufferID = generate(state);
        indexBufferSize = indexBuffer.capacity();
        state.gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBufferID);
        state.gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.capacity() * Buffers.SIZEOF_INT, indexBuffer, GL2.GL_STATIC_DRAW);
        GL3DState.getActiveCamera().updateCameraTransformation();
        state.gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);

    }

    private void updateROI(GL3DState state) {
        MetaData metaData = getMainLayerView().getMetaData();
        if (metaData == null)
            return;

        double minPhysicalX = Double.MAX_VALUE;
        double minPhysicalY = Double.MAX_VALUE;
        double maxPhysicalX = Double.MIN_VALUE;
        double maxPhysicalY = Double.MIN_VALUE;

        GL3DCamera activeCamera = GL3DState.getActiveCamera();
        GL3DQuatd camdiff = this.getCameraDifferenceRotationQuatd(activeCamera, this.getMainLayerView().getImageData());

        for (int i = 0; i < pointlist.length; i++) {
            GL3DVec3d hitPoint;
            hitPoint = activeCamera.getVectorFromSphereOrPlane(pointlist[i], camdiff);
            if (hitPoint != null) {
                minPhysicalX = Math.min(minPhysicalX, hitPoint.x);
                minPhysicalY = Math.min(minPhysicalY, hitPoint.y);
                maxPhysicalX = Math.max(maxPhysicalX, hitPoint.x);
                maxPhysicalY = Math.max(maxPhysicalY, hitPoint.y);
            }
        }

        double widthxAdd = Math.abs((maxPhysicalX - minPhysicalX) * 0.0);
        double widthyAdd = Math.abs((maxPhysicalY - minPhysicalY) * 0.0);
        minPhysicalX = minPhysicalX - widthxAdd;
        maxPhysicalX = maxPhysicalX + widthxAdd;
        minPhysicalY = minPhysicalY - widthyAdd;
        maxPhysicalY = maxPhysicalY + widthyAdd;

        double metLLX = metaData.getPhysicalLowerLeft().x;
        double metLLY = metaData.getPhysicalLowerLeft().y;
        double metURX = metaData.getPhysicalUpperRight().x;
        double metURY = metaData.getPhysicalUpperRight().y;

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
        this.getMainLayerView().setRegion(newRegion);

        Viewport layerViewport = new ViewportAdapter(new StaticViewport(state.getViewportWidth(), state.getViewportHeight()));
        this.getMainLayerView().setViewport(layerViewport);
    }

    public GL3DQuatd getCameraDifferenceRotationQuatd(GL3DCamera camera, ImageData imageData) {
        if (imageData == null)
            return new GL3DQuatd();
        HelioviewerMetaData md = (HelioviewerMetaData) (imageData.getMETADATA());

        GL3DQuatd cameraDifferenceRotation = camera.getRotation().copy();
        cameraDifferenceRotation.rotateWithConjugate(md.getLocalRotation());

        return cameraDifferenceRotation;
    }

    @Override
    public void render(GL3DState state) {
        if (!this.isVisible)
            return;
        GL2 gl = state.gl;
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        GLSLShader.bind(gl);
        {
            gl.glEnable(GL2.GL_CULL_FACE);
            {
                gl.glCullFace(GL2.GL_BACK);

                gl.glEnable(GL2.GL_BLEND);
                AbstractView jp2view = this.mainLayerView;

                jp2view.applyFilters(gl);

                GLSLShader.setViewport(GLInfo.pixelScale[0] * state.getViewportWidth(), GLInfo.pixelScale[1] * state.getViewportHeight());
                if (!GL3DImageLayer.showCorona) {
                    GLSLShader.setOuterCutOffRadius(1.);
                }
                GLSLShader.filter(gl);
                GL3DCamera camera = GL3DState.getActiveCamera();
                GL3DMat4d vpmi = camera.orthoMatrixInverse.copy();
                vpmi.translate(new GL3DVec3d(-camera.getTranslation().x, -camera.getTranslation().y, 0.));
                GLSLShader.bindMatrix(gl, vpmi.getFloatArray(), "cameraTransformationInverse");
                GLSLShader.bindQuat(gl, getCameraDifferenceRotationQuatd(camera, this.mainLayerView.getImageData()), "cameraDifferenceRotationQuat");

                if (this.mainLayerView.getBaseDifferenceMode()) {
                    GLSLShader.bindQuat(gl, getCameraDifferenceRotationQuatd(camera, this.mainLayerView.getBaseDifferenceImageData()), "diffcameraDifferenceRotationQuat");
                } else if (this.mainLayerView.getDifferenceMode()) {
                    GLSLShader.bindQuat(gl, getCameraDifferenceRotationQuatd(camera, this.mainLayerView.getPreviousImageData()), "diffcameraDifferenceRotationQuat");
                }

                enablePositionVBO(state);
                enableIndexVBO(state);
                {
                    gl.glVertexPointer(3, GL2.GL_FLOAT, 3 * Buffers.SIZEOF_FLOAT, 0);
                    GLSLShader.bindIsDisc(gl, 0);

                    gl.glDepthRange(1.f, 1.f);
                    gl.glDrawElements(GL2.GL_TRIANGLES, 6, GL2.GL_UNSIGNED_INT, (this.indexBufferSize - 6) * Buffers.SIZEOF_INT);

                    gl.glDepthRange(0.f, 1.f);

                    GLSLShader.bindIsDisc(gl, 1);
                    gl.glDrawElements(GL2.GL_TRIANGLES, this.indexBufferSize - 6, GL2.GL_UNSIGNED_INT, 0);
                }
                disableIndexVBO(state);
                disablePositionVBO(state);
                GLSLShader.unbind(gl);

                gl.glColorMask(true, true, true, true);
            }
            gl.glDisable(GL2.GL_CULL_FACE);
        }
        GLSLShader.unbind(gl);
        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glDisable(GL2.GL_BLEND);

        updateROI(state);
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
    public void remove(GL3DState state) {
        disablePositionVBO(state);
        disableIndexVBO(state);
        deletePositionVBO(state);
        deleteIndexVBO(state);
        Displayer.getLayersModel().removeLayer(getMainLayerView());
    }

    private static Pair<FloatBuffer, IntBuffer> makeIcosphere(int level) {
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
        float r = 40.f;
        vertices.add(-r);
        vertices.add(r);
        vertices.add(0f);

        vertices.add(r);
        vertices.add(r);
        vertices.add(0f);

        vertices.add(r);
        vertices.add(-r);
        vertices.add(0f);

        vertices.add(-r);
        vertices.add(-r);
        vertices.add(0f);

        faceIndices.add(beginPositionNumberCorona + 0);
        faceIndices.add(beginPositionNumberCorona + 2);
        faceIndices.add(beginPositionNumberCorona + 1);

        faceIndices.add(beginPositionNumberCorona + 2);
        faceIndices.add(beginPositionNumberCorona + 0);
        faceIndices.add(beginPositionNumberCorona + 3);
        FloatBuffer positionBuffer = FloatBuffer.allocate(vertices.size());
        for (Float vert : vertices) {
            if (vert == 0f)
                vert = Math.nextAfter(vert, vert + 1.0f);
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

    private static void subdivide(int vx, int vy, int vz, ArrayList<Float> vertexList, ArrayList<Integer> faceList, int level) {
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

    public static void toggleCorona() {
        showCorona = !showCorona;
    }

    @Override
    public RenderableType getType() {
        return this.type;
    }

    @Override
    public Component getOptionsPanel() {
        FilterPanelContainer fpc = FilterPanelContainer.getSingletonInstance();
        fpc.getFilterTabPanelManager().setActivejp2(mainLayerView);
        return fpc.getFilterPanelContainer();
    }

    @Override
    public boolean isVisible() {
        return isVisible;
    }

    @Override
    public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

    @Override
    public String getName() {
        return getMainLayerView().getName();
    }

    @Override
    public String getTimeString() {
        return getMainLayerView().getMetaData().getDateTime().getCachedDate();
    }

    public AbstractView getMainLayerView() {
        return mainLayerView;
    }

}
