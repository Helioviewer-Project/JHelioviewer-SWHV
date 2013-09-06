package org.helioviewer.gl3d.scenegraph;

import java.util.LinkedList;
import java.util.List;

import org.helioviewer.base.logging.Log;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec2d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4f;

public class GL3DMergeMesh extends GL3DMesh {
    private List<GL3DMesh> meshes;

    public GL3DMergeMesh(String name) {
        super(name, new GL3DVec4f(1f, 1f, 1f, 1f));
        this.meshes = new LinkedList<GL3DMesh>();
    }

    public void shapeDraw(GL3DState state) {
        // state.gl.glDisable(GL.GL_LIGHTING);
        super.shapeDraw(state);
    }

    public GL3DMeshPrimitive createMesh(GL3DState state, List<GL3DVec3d> positions, List<GL3DVec3d> normals, List<GL3DVec2d> textCoords, List<Integer> indices, List<GL3DVec4d> colors) {
        GL3DMeshPrimitive primitive = null;
        int lastHighestIndex = 0;

        for (GL3DMesh mesh : this.meshes) {
            List<GL3DVec3d> _positions = new LinkedList<GL3DVec3d>();
            List<GL3DVec3d> _normals = new LinkedList<GL3DVec3d>();
            List<GL3DVec2d> _textCoords = new LinkedList<GL3DVec2d>();
            List<GL3DVec4d> _colors = new LinkedList<GL3DVec4d>();
            List<Integer> _indices = new LinkedList<Integer>();

            GL3DMeshPrimitive _primitive = mesh.createMesh(state, _positions, _normals, _textCoords, _indices, _colors);
            if (primitive == null) {
                primitive = _primitive;
            }

            if (primitive != _primitive) {
                Log.warn("GL3DMergeMesh: Cannot Merge meshes of different GL3DMeshPrimitives. Primitive so far: " + primitive + ". New Primitive: " + _primitive + ". Ommitting this mesh...");
                break;
            }
            for (GL3DVec3d p : _positions) {
                positions.add(mesh.modelView().multiply(p));
            }
            // positions.addAll(_positions);
            normals.addAll(_normals);
            textCoords.addAll(_textCoords);
            colors.addAll(_colors);

            for (Integer index : _indices) {
                indices.add((index + lastHighestIndex));
            }

            // Increase used indices
            lastHighestIndex += _positions.size();
            // mesh = null;
            // _positions = null;
            // _colors = null;
            // _normals = null;
            // _textCoords = null;
            // _indices = null;
        }

        if (primitive == null) {
            Log.error("GL3DMergeMesh: MergeMesh must contain at least 1 mesh!");
        } else {
            Log.debug("GL3DMergeMesh: Merged " + this.meshes.size() + " meshes into one " + primitive);
        }

        Log.debug("GL3DMergeMesh: Mesh has " + colors.size() + " Colors");

        return primitive;
    }

    public void addMesh(GL3DMesh mesh) {
        this.meshes.add(mesh);
    }
}
