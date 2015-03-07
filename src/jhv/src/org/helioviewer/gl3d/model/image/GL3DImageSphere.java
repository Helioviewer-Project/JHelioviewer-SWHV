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
    private GL3DBuffer positionVBO;
    private GL3DBuffer indexVBO;
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
                    markAsChanged();
                }
            }
        });
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

        gl.glDrawElements(GL2.GL_TRIANGLES, this.indexVBO.numberOfElements, this.indexVBO.dataType.id, 0);
        this.positionVBO.disable(state);

        this.indexVBO.disable(state);
        if (restoreColorMask) {
            gl.glColorMask(true, true, true, true);
        }
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

    public GL3DImageTextureView getImageTextureView() {
        return imageTextureView;
    }

    @Override
    public void shapeInit(GL3DState state) {
        positions = new ArrayList<GL3DVec3d>();

        indices = new ArrayList<Integer>();

        this.createMesh(state, positions, indices);

        this.positionVBO = GL3DBuffer.createPositionBuffer(state, positions);

        this.indexVBO = GL3DBuffer.createIndexBuffer(state, indices);

        this.imageTextureView.forceUpdate();

    }

    public void createMesh(GL3DState state, List<GL3DVec3d> positions, List<Integer> indices) {
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
    }
}
