package org.helioviewer.jhv.opengl.model;

import java.nio.IntBuffer;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public record ModelNode(String name, Matrix4fc transform, IntBuffer meshIndices, List<ModelNode> children) {

    public ModelNode {
        transform = new Matrix4f(transform);
        meshIndices = meshIndices.slice().asReadOnlyBuffer();
        children = List.copyOf(children);
    }

    @Override
    public IntBuffer meshIndices() {
        return meshIndices.duplicate();
    }

}
