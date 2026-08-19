package org.helioviewer.jhv.opengl.model;

import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.time.JHVTime;

public record ModelScene(String name, @Nullable JHVTime time, ModelNode root, List<ModelMesh> meshes, List<ModelMaterial> materials,
                         List<ModelTexture> textures) {

    public ModelScene {
        meshes = List.copyOf(meshes);
        materials = List.copyOf(materials);
        textures = List.copyOf(textures);
    }

}
