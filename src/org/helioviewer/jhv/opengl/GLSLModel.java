package org.helioviewer.jhv.opengl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Comparator;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.display.ViewportMath;
import org.helioviewer.jhv.opengl.model.ModelMaterial;
import org.helioviewer.jhv.opengl.model.ModelMesh;
import org.helioviewer.jhv.opengl.model.ModelNode;
import org.helioviewer.jhv.opengl.model.ModelSampler;
import org.helioviewer.jhv.opengl.model.ModelScene;
import org.helioviewer.jhv.opengl.model.ModelTexture;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class GLSLModel {

    private static final float DEFAULT_POINT_SIZE = 0.02f;
    private static final double DEFAULT_LINE_WIDTH = GLSLLine.LINEWIDTH_BASIC;

    private final ModelScene scene;
    private final GLSLMesh[] triangleMeshes;
    private final GLSLLine[] lineMeshes;
    private final GLSLShape[] pointMeshes;
    private final DirectBufVertex[] lineVertices;
    private final DirectBufVertex[] pointVertices;
    private final ArrayList<Instance> opaqueInstances = new ArrayList<>();
    private final ArrayList<Instance> drawingInstances = new ArrayList<>();
    private final ArrayList<Instance> blendedInstances = new ArrayList<>();

    private GLTexture[] textures;
    private boolean initialized;

    public GLSLModel(ModelScene _scene) {
        scene = _scene;
        int meshCount = scene.meshes().size();
        triangleMeshes = new GLSLMesh[meshCount];
        lineMeshes = new GLSLLine[meshCount];
        pointMeshes = new GLSLShape[meshCount];
        lineVertices = new DirectBufVertex[meshCount];
        pointVertices = new DirectBufVertex[meshCount];

        for (int i = 0; i < meshCount; i++) {
            ModelMesh mesh = scene.meshes().get(i);
            ModelMaterial material = getMaterial(mesh);
            validateTexture(material);
            switch (mesh.primitive()) {
                case TRIANGLES -> triangleMeshes[i] = new GLSLMesh(mesh, material);
                case LINES -> {
                    requireUntextured(mesh, material);
                    lineMeshes[i] = new GLSLLine(false);
                    lineVertices[i] = createLineVertices(mesh, material);
                }
                case POINTS -> {
                    requireUntextured(mesh, material);
                    pointMeshes[i] = new GLSLShape(false);
                    pointVertices[i] = createPointVertices(mesh, material);
                }
            }
        }
        flattenNode(scene.root(), new Matrix4f());
    }

    private ModelMaterial getMaterial(ModelMesh mesh) {
        int index = mesh.materialIndex();
        if (index < 0 || index >= scene.materials().size())
            throw new IllegalArgumentException("Invalid material index " + index + " in mesh " + mesh.name());
        return scene.materials().get(index);
    }

    private void validateTexture(ModelMaterial material) {
        int index = material.baseColorTexture();
        if (index < ModelMaterial.NO_TEXTURE || index >= scene.textures().size())
            throw new IllegalArgumentException("Invalid base-color texture index: " + index);
    }

    private static void requireUntextured(ModelMesh mesh, ModelMaterial material) {
        if (material.baseColorTexture() != ModelMaterial.NO_TEXTURE)
            throw new IllegalArgumentException("Textures are not supported on " + mesh.primitive().name().toLowerCase() + " mesh " + mesh.name());
    }

    private void flattenNode(ModelNode node, Matrix4fc parentTransform) {
        Matrix4f worldTransform = new Matrix4f(parentTransform).mul(node.transform());
        IntBuffer meshIndices = node.meshIndices();
        while (meshIndices.hasRemaining()) {
            int meshIndex = meshIndices.get();
            if (meshIndex < 0 || meshIndex >= scene.meshes().size())
                throw new IllegalArgumentException("Invalid mesh index " + meshIndex + " in node " + node.name());

            Instance instance = new Instance(meshIndex, new Matrix4f(worldTransform));
            GLSLMesh triangleMesh = triangleMeshes[meshIndex];
            if (triangleMesh == null)
                drawingInstances.add(instance);
            else if (triangleMesh.material().alphaMode() == ModelMaterial.AlphaMode.BLEND)
                blendedInstances.add(instance);
            else
                opaqueInstances.add(instance);
        }
        for (ModelNode child : node.children())
            flattenNode(child, worldTransform);
    }

    public void init() {
        if (initialized)
            return;
        initialized = true;
        try {
            initTextures();
            for (int i = 0; i < triangleMeshes.length; i++) {
                if (triangleMeshes[i] != null)
                    triangleMeshes[i].init();
                if (lineMeshes[i] != null) {
                    lineMeshes[i].init();
                    lineMeshes[i].setVertexRepeatable(lineVertices[i]);
                }
                if (pointMeshes[i] != null) {
                    pointMeshes[i].init();
                    pointMeshes[i].setVertexRepeatable(pointVertices[i]);
                }
            }
        } catch (RuntimeException | Error e) {
            dispose();
            throw e;
        }
    }

    private void initTextures() {
        textures = new GLTexture[scene.textures().size()];
        for (int i = 0; i < textures.length; i++) {
            ModelTexture data = scene.textures().get(i);
            if (data.width() > GL.maxTextureSize || data.height() > GL.maxTextureSize)
                throw new IllegalArgumentException("Texture exceeds the OpenGL size limit: " + data.width() + 'x' + data.height());
            ModelSampler sampler = data.sampler();
            GLTexture texture = new GLTexture(GL.TEXTURE_2D, GLTexture.Unit.THREE);
            textures[i] = texture;
            texture.bind();
            GLTexture.copyByteImage(data.width(), data.height(), minFilter(sampler.minFilter()), magFilter(sampler.magFilter()),
                    wrap(sampler.wrapS()), wrap(sampler.wrapT()), usesMipmaps(sampler.minFilter()), data.rgba());
        }
    }

    public void render(MapView mv, Viewport vp) {
        if (!initialized)
            return;

        for (Instance instance : opaqueInstances)
            renderTriangle(instance);

        double pointFactor = ViewportMath.getPixelFactor(vp, mv.cameraWidth(vp));
        for (Instance instance : drawingInstances)
            renderDrawing(instance, vp, pointFactor);

        blendedInstances.sort(Comparator.comparingDouble(this::viewZ));
        GL.glDepthMask(false);
        try {
            for (Instance instance : blendedInstances)
                renderTriangle(instance);
        } finally {
            GL.glDepthMask(true);
        }
    }

    private double viewZ(Instance instance) {
        GLSLMesh mesh = triangleMeshes[instance.meshIndex];
        return Transform.viewZ(instance.transform, mesh.centerX(), mesh.centerY(), mesh.centerZ());
    }

    private void renderDrawing(Instance instance, Viewport vp, double pointFactor) {
        Transform.pushView();
        try {
            Transform.multiplyView(instance.transform);
            GLSLLine line = lineMeshes[instance.meshIndex];
            if (line != null)
                line.renderLine(vp, DEFAULT_LINE_WIDTH);
            else
                pointMeshes[instance.meshIndex].renderPoints(pointFactor);
        } finally {
            Transform.popView();
        }
    }

    private void renderTriangle(Instance instance) {
        GLSLMesh mesh = triangleMeshes[instance.meshIndex];
        ModelMaterial material = mesh.material();
        boolean reverseWinding = instance.transform.determinant3x3() < 0;
        if (material.doubleSided())
            GL.glDisable(GL.CULL_FACE);
        else if (reverseWinding)
            GL.glFrontFace(GL.CW);

        try {
            Transform.pushView();
            try {
                Transform.multiplyView(instance.transform);
                mesh.render(textures);
            } finally {
                Transform.popView();
            }
        } finally {
            if (material.doubleSided())
                GL.glEnable(GL.CULL_FACE);
            else if (reverseWinding)
                GL.glFrontFace(GL.CCW);
        }
    }

    public void dispose() {
        if (!initialized)
            return;
        initialized = false;

        for (GLSLMesh mesh : triangleMeshes) {
            if (mesh != null)
                mesh.dispose();
        }
        for (GLSLLine line : lineMeshes) {
            if (line != null)
                line.dispose();
        }
        for (GLSLShape points : pointMeshes) {
            if (points != null)
                points.dispose();
        }
        if (textures != null) {
            for (GLTexture texture : textures) {
                if (texture != null)
                    texture.delete();
            }
            textures = null;
        }
    }

    private static DirectBufVertex createPointVertices(ModelMesh mesh, ModelMaterial material) {
        IntBuffer indices = mesh.indices();
        BufVertex vertices = new BufVertex(Math.multiplyExact(indices.remaining(), GLSLShape.stride));
        FloatBuffer positions = mesh.positions();
        ByteBuffer colors = mesh.colors();
        byte[] color = new byte[4];
        while (indices.hasRemaining()) {
            int index = indices.get();
            setColor(colors, material, index, color);
            putVertex(positions, index, DEFAULT_POINT_SIZE, color, vertices);
        }
        return new DirectBufVertex(vertices);
    }

    private static DirectBufVertex createLineVertices(ModelMesh mesh, ModelMaterial material) {
        IntBuffer indices = mesh.indices();
        IntBuffer offsets = mesh.lineOffsets();
        int lineCount = offsets.remaining() - 1;
        int vertexCount = Math.addExact(indices.remaining(), Math.multiplyExact(2, lineCount));
        BufVertex vertices = new BufVertex(Math.multiplyExact(vertexCount, GLSLLine.stride));
        FloatBuffer positions = mesh.positions();
        ByteBuffer colors = mesh.colors();
        byte[] color = new byte[4];

        // MASK is only approximate for lines: setColor() applies the cutoff per vertex, while GLSLLine interpolates the resulting colors and
        // also uses zero-alpha colors as topology sentinels. Exact masking requires applying the cutoff in the line fragment shader.
        for (int line = 0; line < lineCount; line++) {
            int start = offsets.get(line);
            int end = offsets.get(line + 1);
            int firstIndex = indices.get(start);
            putVertex(positions, firstIndex, 1, Colors.Null, vertices);
            for (int i = start; i < end; i++) {
                int index = indices.get(i);
                setColor(colors, material, index, color);
                putVertex(positions, index, 1, color, vertices);
            }
            vertices.repeatVertex(Colors.Null);
        }
        return new DirectBufVertex(vertices);
    }

    private static void putVertex(FloatBuffer positions, int index, float size, byte[] color, BufVertex vertices) {
        vertices.putVertex(positions.get(3 * index), positions.get(3 * index + 1), positions.get(3 * index + 2), size, color);
    }

    private static void setColor(ByteBuffer colors, ModelMaterial material, int index, byte[] result) {
        float red = (colors.get(4 * index) & 0xff) / 255f * material.red();
        float green = (colors.get(4 * index + 1) & 0xff) / 255f * material.green();
        float blue = (colors.get(4 * index + 2) & 0xff) / 255f * material.blue();
        float alpha = (colors.get(4 * index + 3) & 0xff) / 255f * material.alpha();

        switch (material.alphaMode()) {
            case OPAQUE -> alpha = 1;
            case MASK -> alpha = alpha < material.alphaCutoff() ? 0 : 1;
            case BLEND -> {}
        }
        red = Math.clamp(red, 0, 1) * alpha;
        green = Math.clamp(green, 0, 1) * alpha;
        blue = Math.clamp(blue, 0, 1) * alpha;
        result[0] = (byte) Math.round(255 * red);
        result[1] = (byte) Math.round(255 * green);
        result[2] = (byte) Math.round(255 * blue);
        result[3] = (byte) Math.round(255 * Math.clamp(alpha, 0, 1));
    }

    private static int minFilter(ModelSampler.MinFilter filter) {
        return switch (filter) {
            case NEAREST -> GL.NEAREST;
            case LINEAR -> GL.LINEAR;
            case NEAREST_MIPMAP_NEAREST -> GL.NEAREST_MIPMAP_NEAREST;
            case LINEAR_MIPMAP_NEAREST -> GL.LINEAR_MIPMAP_NEAREST;
            case NEAREST_MIPMAP_LINEAR -> GL.NEAREST_MIPMAP_LINEAR;
            case LINEAR_MIPMAP_LINEAR -> GL.LINEAR_MIPMAP_LINEAR;
        };
    }

    private static int magFilter(ModelSampler.MagFilter filter) {
        return switch (filter) {
            case NEAREST -> GL.NEAREST;
            case LINEAR -> GL.LINEAR;
        };
    }

    private static int wrap(ModelSampler.Wrap wrap) {
        return switch (wrap) {
            case CLAMP_TO_EDGE -> GL.CLAMP_TO_EDGE;
            case MIRRORED_REPEAT -> GL.MIRRORED_REPEAT;
            case REPEAT -> GL.REPEAT;
        };
    }

    private static boolean usesMipmaps(ModelSampler.MinFilter filter) {
        return filter != ModelSampler.MinFilter.NEAREST && filter != ModelSampler.MinFilter.LINEAR;
    }

    private record Instance(int meshIndex, Matrix4f transform) {}

}
