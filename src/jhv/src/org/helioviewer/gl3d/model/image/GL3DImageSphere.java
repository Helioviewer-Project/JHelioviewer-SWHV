package org.helioviewer.gl3d.model.image;

import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL2;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.GL3DBuffer;
import org.helioviewer.gl3d.scenegraph.GL3DShape;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.ImageTextureRecapturedReason;
import org.helioviewer.viewmodel.metadata.HelioviewerOcculterMetaData;
import org.helioviewer.viewmodel.metadata.HelioviewerPositionedMetaData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.opengl.GL3DImageTextureView;
import org.helioviewer.viewmodel.view.opengl.GLFilterView;
import org.helioviewer.viewmodel.view.opengl.shader.ShaderFactory;

/**
 * Maps the solar disc part of an image layer onto an adaptive mesh that either
 * covers the entire solar disc or the just the part that is visible in the view
 * frustum.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DImageSphere extends GL3DShape {

    private final GL3DImageLayer layer;
    private boolean showSphere;
    private boolean showCorona;
    private final boolean restoreColorMask;
    private final GL3DImageTextureView imageTextureView;
    private Region capturedRegion;
    private boolean reshapeRequested = false;
    private GL3DBuffer positionVBO;
    private GL3DBuffer indexVBO;
    private GL3DMeshPrimitive primitive;
    private List<GL3DVec3d> positions;
    private List<Integer> indices;

    public GL3DImageSphere(GL3DImageTextureView imageTextureView, GL3DImageLayer imageLayer, boolean showSphere, boolean showCorona, boolean restoreColorMask) {
        super("Sphere");
        this.imageTextureView = imageTextureView;

        imageTextureView.addViewListener(new ViewListener() {
            @Override
            public void viewChanged(View sender, ChangeEvent aEvent) {
                ImageTextureRecapturedReason reason = aEvent.getLastChangedReasonByType(ImageTextureRecapturedReason.class);
                if (reason != null) {
                    reshapeRequested = true;
                    capturedRegion = reason.getCapturedRegion();
                    markAsChanged();
                    // Log.debug("GL3DImageMesh.reshape: "+getName()+" Reason="+reason+", Event="+aEvent);
                }
            }
        });
        this.reshapeRequested = true;
        this.markAsChanged();
        this.restoreColorMask = restoreColorMask;
        layer = imageLayer;
        this.showSphere = showSphere;
        this.showCorona = showCorona;
    }

    @Override
    public void shapeDraw(GL3DState state) {
        GL2 gl = state.gl;
        gl.glEnable(GL2.GL_CULL_FACE);
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glEnable(GL2.GL_BLEND);
        GLFilterView glfilter = this.imageTextureView.getAdapter(GLFilterView.class);

        ShaderFactory.bindVertexShader(gl);
        ShaderFactory.bindFragmentShader(gl);
        if (glfilter != null) {
            glfilter.renderGL(gl, true);
        }
        ShaderFactory.filter(gl);

        // If Mesh does not have any data, do not draw!
        if (this.positions.size() < 1) {
            return;
        }

        this.positionVBO.enable(state);
        this.indexVBO.enable(state);

        GL3DMeshPrimitive primitive = this.primitive;
        gl.glDrawElements(primitive.id, this.indexVBO.numberOfElements, this.indexVBO.dataType.id, 0);
        this.positionVBO.disable(state);

        this.indexVBO.disable(state);
        if (restoreColorMask) {
            gl.glColorMask(true, true, true, true);
        }
    }

    public GL3DMeshPrimitive createMesh(GL3DState state, List<GL3DVec3d> positions, List<Integer> indices) {
        int resolutionX = 50;
        int resolutionY = 50;
        int numberOfPositions = 0;
        MetaData metaData = this.layer.metaDataView.getMetaData();
        if (metaData instanceof HelioviewerOcculterMetaData) {
            HelioviewerOcculterMetaData md = (HelioviewerOcculterMetaData) metaData;
            showSphere = false;
        }
        if (showSphere) {
            for (int latNumber = 0; latNumber <= resolutionX; latNumber++) {
                double theta = latNumber * Math.PI / resolutionX;
                double sinTheta = Math.sin(theta);
                double cosTheta = Math.cos(theta);
                for (int longNumber = 0; longNumber <= resolutionY; longNumber++) {
                    double phi = longNumber * 2 * Math.PI / resolutionY;
                    double sinPhi = Math.sin(phi);
                    double cosPhi = Math.cos(phi);

                    double x = cosPhi * sinTheta;
                    double y = cosTheta;
                    double z = sinPhi * sinTheta;
                    positions.add(new GL3DVec3d(Constants.SunRadius * x, Constants.SunRadius * y, Constants.SunRadius * z));
                    numberOfPositions++;
                }
            }

            for (int latNumber = 0; latNumber < resolutionX; latNumber++) {
                for (int longNumber = 0; longNumber < resolutionY; longNumber++) {
                    int first = (latNumber * (resolutionY + 1)) + longNumber;
                    int second = first + resolutionY + 1;
                    indices.add(first);
                    indices.add(first + 1);
                    indices.add(second + 1);
                    indices.add(first);
                    indices.add(second + 1);
                    indices.add(second);
                }
            }
        }
        if (metaData instanceof HelioviewerPositionedMetaData) {
            HelioviewerPositionedMetaData md = (HelioviewerPositionedMetaData) metaData;
            if (md.getInstrument().contains("HMI")) {
                showCorona = false;
            }
        }
        if (showCorona) {
            int beginPositionNumberCorona = numberOfPositions;
            positions.add(new GL3DVec3d(-40., 40., 0.));
            numberOfPositions++;
            positions.add(new GL3DVec3d(40., 40., 0.));
            numberOfPositions++;
            positions.add(new GL3DVec3d(40., -40., 0.));
            numberOfPositions++;
            positions.add(new GL3DVec3d(-40., -40., 0.));
            numberOfPositions++;

            indices.add(beginPositionNumberCorona + 0);
            indices.add(beginPositionNumberCorona + 2);
            indices.add(beginPositionNumberCorona + 1);

            indices.add(beginPositionNumberCorona + 2);
            indices.add(beginPositionNumberCorona + 0);
            indices.add(beginPositionNumberCorona + 3);
        }

        return GL3DMeshPrimitive.TRIANGLES;
    }

    public GL3DImageTextureView getImageTextureView() {
        return imageTextureView;
    }

    @Override
    public void shapeInit(GL3DState state) {
        positions = new ArrayList<GL3DVec3d>();

        indices = new ArrayList<Integer>();

        this.primitive = this.createMesh(state, positions, indices);

        this.positionVBO = GL3DBuffer.createPositionBuffer(state, positions);

        this.indexVBO = GL3DBuffer.createIndexBuffer(state, indices);

        this.imageTextureView.forceUpdate();

    }

    protected void recreateMesh(GL3DState state) {
        this.shapeDelete(state);
        this.shapeInit(state);
    }

    private void renderWireframe(GL3DState state, GL3DMeshPrimitive primitive) {
        GL2 gl = state.gl;
        gl.glDisable(GL2.GL_TEXTURE_2D);

        if (primitive == GL3DMeshPrimitive.QUADS) {
            for (int i = 0; i < this.indices.size(); i++) {
                if (i % 4 == 0)
                    gl.glBegin(GL2.GL_LINE_LOOP);
                int index = this.indices.get(i);
                GL3DVec3d position = this.positions.get(index);
                gl.glVertex3d(position.x, position.y, position.z);
                if ((i) % 4 == 3)
                    gl.glEnd();
            }

        } else if (primitive == GL3DMeshPrimitive.TRIANGLES) {
            for (int i = 0; i < this.indices.size(); i++) {
                if (i % 3 == 0)
                    gl.glBegin(GL2.GL_LINE_LOOP);
                int index = this.indices.get(i);
                GL3DVec3d position = this.positions.get(index);
                gl.glVertex3d(position.x, position.y, position.z);
                if ((i) % 3 == 2)
                    gl.glEnd();
            }

        } else if (primitive == GL3DMeshPrimitive.LINES) {
            gl.glBegin(GL2.GL_LINES);
            GL3DVec3d lastPosition = null;
            for (int i = 0; i < this.indices.size(); i++) {
                int index = this.indices.get(i);
                GL3DVec3d position = this.positions.get(index);
                if (lastPosition != null) {
                    gl.glVertex3d(lastPosition.x, lastPosition.y, lastPosition.z);
                    gl.glVertex3d(position.x, position.y, position.z);
                }
                lastPosition = position;
            }
            gl.glEnd();

        } else {
            gl.glBegin(GL2.GL_LINE_LOOP);
            for (int i = 0; i < this.indices.size(); i++) {
                int index = this.indices.get(i);
                GL3DVec3d position = this.positions.get(index);
                gl.glVertex3d(position.x, position.y, position.z);
            }
            gl.glEnd();
        }

        gl.glEnable(GL2.GL_TEXTURE_2D);
    }

    @Override
    public void shapeUpdate(GL3DState state) {
    }

    @Override
    public void shapeDelete(GL3DState state) {
        this.positionVBO.disable(state);
        this.indexVBO.disable(state);

        this.positionVBO.delete(state);
        this.indexVBO.delete(state);

        positions.clear();
        indices.clear();
    }

    public enum GL3DMeshPrimitive {
        TRIANGLES(GL2.GL_TRIANGLES), TRIANGLE_STRIP(GL2.GL_TRIANGLE_STRIP), TRIANGLE_FAN(GL2.GL_TRIANGLE_FAN), POINTS(GL2.GL_POINTS), QUADS(GL2.GL_QUADS), LINES(GL2.GL_LINES), LINE_LOOP(GL2.GL_LINE_LOOP), LINE_STRIP(GL2.GL_LINE_STRIP);
        protected int id;

        private GL3DMeshPrimitive(int id) {
            this.id = id;
        }
    }
}
