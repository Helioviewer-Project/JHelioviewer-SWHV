package org.helioviewer.gl3d.scenegraph;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.media.opengl.GL2;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec2d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4f;

/**
 * A {@link GL3DMesh} represents a Mesh object within the scene graph. A Mesh
 * consists of vertices, including attributes such as color, normal, texture
 * coordinates, and indices, that connect vertices to faces. An implementation
 * must provide these attributes and indices which will be converted to
 * {@link GL3DBuffer}.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public abstract class GL3DMesh extends GL3DShape {
    private GL3DBuffer positionVBO;
    private GL3DBuffer normalVBO;
    private GL3DBuffer colorVBO;
    private GL3DBuffer texcoordVBO;
    private GL3DBuffer indexVBO;

    private GL3DMeshPrimitive primitive;

    private List<GL3DVec3d> positions;
    private List<GL3DVec3d> normals;
    private List<GL3DVec4d> colors;
    private List<GL3DVec2d> textCoords;
    private List<Integer> indices;

    private List<GL3DTriangle> triangles;

    private final float[] diffuseMaterial;

    private final float[] specularMaterial;

    private final ReentrantLock meshLock = new ReentrantLock();

    public GL3DMesh(String name) {
        this(name, new GL3DVec4f(1, 1, 1, 1));
    }

    public GL3DMesh(String name, GL3DVec4f diffuseMaterial) {
        this(name, diffuseMaterial, new GL3DVec4f(0.1f, 0.1f, 0.1f, 1.0f));

    }

    public GL3DMesh(String name, GL3DVec4f diffuseMaterial, GL3DVec4f specularMaterial) {
        super(name);

        this.diffuseMaterial = new float[] { diffuseMaterial.x, diffuseMaterial.y, diffuseMaterial.z, diffuseMaterial.w };
        this.specularMaterial = new float[] { specularMaterial.x, specularMaterial.y, specularMaterial.z, specularMaterial.w };
    }

    public void setMaterialAlpha(float alpha) {
        this.diffuseMaterial[3] = alpha;
        this.specularMaterial[3] = alpha;
    }

    @Override
    public void shapeInit(GL3DState state) {
        meshLock.lock();

        positions = new ArrayList<GL3DVec3d>();
        normals = new ArrayList<GL3DVec3d>();
        colors = new ArrayList<GL3DVec4d>();
        textCoords = new ArrayList<GL3DVec2d>();
        indices = new ArrayList<Integer>();

        this.primitive = this.createMesh(state, positions, normals, textCoords, indices, colors);

        this.positionVBO = GL3DBuffer.createPositionBuffer(state, positions);
        this.normalVBO = GL3DBuffer.createNormalBuffer(state, normals);
        this.colorVBO = GL3DBuffer.createColorBuffer(state, colors);
        this.texcoordVBO = GL3DBuffer.create2DTextureCoordinateBuffer(state, textCoords);
        this.indexVBO = GL3DBuffer.createIndexBuffer(state, indices);

        this.setTriangles(buildTriangles());
        meshLock.unlock();
    }

    protected void recreateMesh(GL3DState state) {
        this.shapeDelete(state);
        this.shapeInit(state);
    }

    @Override
    public void shapeDraw(GL3DState state) {
        meshLock.lock();

        // If Mesh does not have any data, do not draw!
        if (this.positions.size() < 1) {
            meshLock.unlock();
            // Log.debug("Mesh '"+this+"' is not initialised, abortingDraw");
            return;
        }

        if (isDrawBitOn(Bit.Wireframe)) {
            this.renderWireframe(state, this.primitive);
        } else {
            this.positionVBO.enable(state);
            if (this.normals.isEmpty()) {
                this.normalVBO.disable(state);
            } else {
                this.normalVBO.enable(state);
            }
            this.indexVBO.enable(state);

            if (this.colors.size() < 1) {
                this.colorVBO.disable(state);
                state.gl.glColor4fv(this.diffuseMaterial, 0);
                // Log.debug("GL3DMesh: "+getName()+" Using diffuseMaterial as Color "+this.diffuseMaterial[3]);
            } else {
                // state.gl.glColor4d(1, 1, 1, 1);
                // Log.debug("GL3DMesh: "+getName()+" Using VBO AND diffuseMaterial as Color");
                state.gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT_AND_DIFFUSE, this.diffuseMaterial, 0);
                state.gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, this.specularMaterial, 0);
                this.colorVBO.enable(state);
            }

            GL3DMeshPrimitive primitive = this.primitive;
            if (textCoords.size() > 0) {
                // state.gl.glColor4d(1, 1, 1, 1);
                this.texcoordVBO.enable(state);
                state.gl.glEnable(GL2.GL_TEXTURE_2D);
            } else {
                state.gl.glDisable(GL2.GL_TEXTURE_2D);
            }
            // GL3DState.get().checkGLErrors("GL3DImageMesh.beforeDrawCall "+getName());

            state.gl.glDrawElements(primitive.id, this.indexVBO.numberOfElements, this.indexVBO.dataType.id, 0);
            // GL3DState.get().checkGLErrors("GL3DImageMesh.afterDrawCall " + getName() + " IndexVBO: " + this.indexVBO.id);
            this.positionVBO.disable(state);
            this.colorVBO.disable(state);
            this.normalVBO.disable(state);
            this.texcoordVBO.disable(state);
            this.indexVBO.disable(state);
            // GL3DState.get().checkGLErrors("GL3DImageMesh.afterDisableVBOs " + getName());
        }

        if (isDrawBitOn(Bit.Normals)) {
            renderNormals(state);
        }

        meshLock.unlock();
    }

    private void renderWireframe(GL3DState state, GL3DMeshPrimitive primitive) {
        GL2 gl = state.gl;
        gl.glDisable(GL2.GL_LIGHTING);
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

        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_TEXTURE_2D);
    }

    private void renderNormals(GL3DState state) {
        GL2 gl = state.gl;
        gl.glColor3d(1, 0.5, 0);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glBegin(GL2.GL_LINES);
        for (int i = 0; i < this.normals.size(); i++) {
            GL3DVec3d position = this.positions.get(i);
            GL3DVec3d normal = this.normals.get(i);

            gl.glVertex3d(position.x, position.y, position.z);
            gl.glVertex3d(position.x + normal.x * Constants.SunRadius / 10, position.y + normal.y * Constants.SunRadius / 10, position.z + normal.z * Constants.SunRadius / 10);
        }
        gl.glEnd();
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_TEXTURE_2D);
    }

    @Override
    public void shapeUpdate(GL3DState state) {
    }

    @Override
    public void shapeDelete(GL3DState state) {
        meshLock.lock();

        this.positionVBO.disable(state);
        this.colorVBO.disable(state);
        this.normalVBO.disable(state);
        this.texcoordVBO.disable(state);
        this.indexVBO.disable(state);

        this.positionVBO.delete(state);
        this.normalVBO.delete(state);
        this.colorVBO.delete(state);
        this.texcoordVBO.delete(state);
        this.indexVBO.delete(state);

        positions.clear();
        normals.clear();
        colors.clear();
        textCoords.clear();
        indices.clear();

        meshLock.unlock();
    }

    private List<GL3DTriangle> buildTriangles() {
        List<GL3DTriangle> triangles = new ArrayList<GL3DTriangle>();

        if (this.primitive == GL3DMeshPrimitive.TRIANGLES) {
            for (int i = 0; i < this.indices.size(); i += 3) {
                GL3DVec3d a = this.positions.get(this.indices.get(i));
                GL3DVec3d b = this.positions.get(this.indices.get(i + 1));
                GL3DVec3d c = this.positions.get(this.indices.get(i + 2));
                triangles.add(new GL3DTriangle(a, b, c));
            }
        } else if (this.primitive == GL3DMeshPrimitive.QUADS) {
            for (int i = 0; i < this.indices.size(); i += 4) {
                GL3DVec3d a = this.positions.get(this.indices.get(i));
                GL3DVec3d b = this.positions.get(this.indices.get(i + 1));
                GL3DVec3d c = this.positions.get(this.indices.get(i + 2));
                GL3DVec3d d = this.positions.get(this.indices.get(i + 3));
                triangles.add(new GL3DTriangle(a, b, c));
                triangles.add(new GL3DTriangle(a, c, d));
            }
        } else if (this.primitive == GL3DMeshPrimitive.TRIANGLE_FAN) {
            GL3DVec3d a = this.positions.get(this.indices.get(0));
            GL3DVec3d first = this.positions.get(this.indices.get(1));

            for (int i = 2; i < this.indices.size(); i++) {
                GL3DVec3d next = this.positions.get(this.indices.get(i));

                triangles.add(new GL3DTriangle(a, first, next));
                first = next;
            }
        } else if (this.primitive == GL3DMeshPrimitive.TRIANGLE_STRIP) {
            GL3DVec3d first = this.positions.get(this.indices.get(0));
            GL3DVec3d second = this.positions.get(this.indices.get(1));

            for (int i = 2; i < this.indices.size(); i++) {
                GL3DVec3d third = this.positions.get(this.indices.get(i));

                triangles.add(new GL3DTriangle(first, second, third));
                first = second;
                second = third;
            }
        }

        return triangles;
    }

    private void calcMinMax(GL3DVec3d minV, GL3DVec3d maxV) {
        minV.set(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
        maxV.set(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE);

        for (GL3DVec3d p : this.positions) {
            minV.setMin(p);
            maxV.setMax(p);
        }
    }

    public abstract GL3DMeshPrimitive createMesh(GL3DState state, List<GL3DVec3d> positions, List<GL3DVec3d> normals, List<GL3DVec2d> textCoords, List<Integer> indices, List<GL3DVec4d> colors);

    public List<GL3DTriangle> getTriangles() {
        return triangles;
    }

    public void setTriangles(List<GL3DTriangle> triangles) {
        this.triangles = triangles;
    }

    public enum GL3DMeshPrimitive {
        TRIANGLES(GL2.GL_TRIANGLES), TRIANGLE_STRIP(GL2.GL_TRIANGLE_STRIP), TRIANGLE_FAN(GL2.GL_TRIANGLE_FAN), POINTS(GL2.GL_POINTS), QUADS(GL2.GL_QUADS), LINES(GL2.GL_LINES), LINE_LOOP(GL2.GL_LINE_LOOP), LINE_STRIP(GL2.GL_LINE_STRIP);
        protected int id;

        private GL3DMeshPrimitive(int id) {
            this.id = id;
        }
    }

}
