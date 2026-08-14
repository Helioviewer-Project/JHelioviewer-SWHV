package org.helioviewer.jhv.opengl.model;

import java.util.List;

public record ModelScene(String name, ModelNode root, List<ModelMesh> meshes, List<ModelMaterial> materials, List<ModelTexture> textures) {

    public ModelScene {
        meshes = List.copyOf(meshes);
        materials = List.copyOf(materials);
        textures = List.copyOf(textures);
    }

}
