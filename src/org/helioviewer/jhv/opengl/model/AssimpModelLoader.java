package org.helioviewer.jhv.opengl.model;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.io.DataUri;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.metadata.HeliocentricCartesianMetaData;
import org.helioviewer.jhv.opengl.GL;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeUtils;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIColor4D;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMatrix4x4;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AINode;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIString;
import org.lwjgl.assimp.AITexel;
import org.lwjgl.assimp.AITexture;
import org.lwjgl.assimp.AIUVTransform;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.stb.STBImage;

public final class AssimpModelLoader {

    private static final int IMPORT_FLAGS = Assimp.aiProcess_Triangulate | Assimp.aiProcess_ValidateDataStructure |
            Assimp.aiProcess_SortByPType | Assimp.aiProcess_EmbedTextures | Assimp.aiProcess_RemoveRedundantMaterials;

    private final AIScene source;
    private final ArrayList<ModelTexture> textures = new ArrayList<>();
    private final Map<TextureKey, Integer> textureIndices = new HashMap<>();

    public static ModelScene load(DataUri data) throws IOException {
        if (data.format() != DataUri.Format.GLTF)
            throw new IOException("Not a glTF model: " + data.sourceUri());

        AIScene scene;
        try (AssimpFileIO fileIO = new AssimpFileIO(data)) {
            scene = fileIO.importScene(IMPORT_FLAGS);
        }
        try {
            return new AssimpModelLoader(scene).convert(data);
        } finally {
            Assimp.aiReleaseImport(scene);
        }
    }

    private AssimpModelLoader(AIScene _source) {
        source = _source;
    }

    private ModelScene convert(DataUri data) throws IOException {
        if ((source.mFlags() & Assimp.AI_SCENE_FLAGS_INCOMPLETE) != 0)
            throw new IOException("Assimp returned an incomplete scene");
        AINode root = source.mRootNode();
        if (root == null)
            throw new IOException("Assimp scene has no root node");
        if (source.mNumAnimations() != 0)
            throw new IOException("Animated model scenes are not supported");
        if (source.mNumSkeletons() != 0)
            throw new IOException("Model skeletons are not supported");

        AssimpMetaData metadata = new AssimpMetaData(data.sourceUri(), source.mMetaData());
        JHVTime time = TimeUtils.START;
        Quat observerRotation = null;
        if (metadata.contains("WCSNAME")) {
            time = HeliocentricCartesianMetaData.observationTime(metadata);
            observerRotation = HeliocentricCartesianMetaData.observerRotation(metadata);
        }
        Matrix4f coordinateTransform = coordinateTransform(observerRotation);

        List<MaterialData> materialData = convertMaterials();
        List<ModelMesh> sourceMeshes = convertMeshes(materialData);
        ArrayList<ModelMesh> meshes = new ArrayList<>();
        convertNode(root, coordinateTransform, sourceMeshes, meshes);
        if (meshes.isEmpty())
            throw new IOException("Model scene graph contains no meshes");

        String name = source.mName().dataString();
        return new ModelScene(name.isEmpty() ? data.baseName() : name, time, meshes,
                materialData.stream().map(MaterialData::material).toList(), textures);
    }

    private static Matrix4f coordinateTransform(@Nullable Quat worldToObserver) {
        if (worldToObserver == null)
            return new Matrix4f();

        Vec3 sourceX = worldToObserver.rotateInverseVector(Vec3.XAxis);
        Vec3 sourceY = worldToObserver.rotateInverseVector(Vec3.YAxis);
        Vec3 sourceZ = worldToObserver.rotateInverseVector(Vec3.ZAxis);
        return new Matrix4f(
                (float) sourceX.x, (float) sourceX.y, (float) sourceX.z, 0,
                (float) sourceY.x, (float) sourceY.y, (float) sourceY.z, 0,
                (float) sourceZ.x, (float) sourceZ.y, (float) sourceZ.z, 0,
                0, 0, 0, 1);
    }

    private List<MaterialData> convertMaterials() throws IOException {
        PointerBuffer sourceMaterials = source.mMaterials();
        if (source.mNumMaterials() == 0)
            throw new IOException("Model scene has no materials");
        if (sourceMaterials == null)
            throw new IOException("Assimp scene has no material array");

        ArrayList<MaterialData> result = new ArrayList<>(source.mNumMaterials());
        for (int i = 0; i < source.mNumMaterials(); i++)
            result.add(convertMaterial(AIMaterial.create(sourceMaterials.get(i)), i));
        return List.copyOf(result);
    }

    private MaterialData convertMaterial(AIMaterial material, int materialIndex) throws IOException {
        AIColor4D color = AIColor4D.create();
        if (Assimp.aiGetMaterialColor(material, Assimp.AI_MATKEY_BASE_COLOR, 0, 0, color) != Assimp.aiReturn_SUCCESS &&
                Assimp.aiGetMaterialColor(material, Assimp.AI_MATKEY_COLOR_DIFFUSE, 0, 0, color) != Assimp.aiReturn_SUCCESS)
            color.set(1, 1, 1, 1);

        float alpha = getFloat(material, Assimp.AI_MATKEY_OPACITY, 0, 0, color.a());
        ModelMaterial.AlphaMode alphaMode = alphaMode(material);
        float alphaCutoff = getFloat(material, Assimp.AI_MATKEY_GLTF_ALPHACUTOFF, 0, 0, 0.5f);
        boolean doubleSided = getInt(material, Assimp.AI_MATKEY_TWOSIDED, 0, 0, 0) != 0;
        boolean unlit = getInt(material, Assimp.AI_MATKEY_SHADING_MODEL, 0, 0, -1) == Assimp.aiShadingMode_Unlit;
        if (getInt(material, Assimp.AI_MATKEY_BLEND_FUNC, 0, 0, Assimp.aiBlendMode_Default) != Assimp.aiBlendMode_Default)
            throw new IOException("Material " + materialIndex + " uses unsupported additive blending");
        if (Assimp.aiGetMaterialTextureCount(material, Assimp.aiTextureType_OPACITY) != 0)
            throw new IOException("Material " + materialIndex + " uses a separate opacity texture");

        TextureInfo texture = baseColorTexture(material, materialIndex);
        return new MaterialData(new ModelMaterial(color.r(), color.g(), color.b(), alpha, texture.textureIndex(), alphaMode, alphaCutoff,
                doubleSided, unlit), texture.uvIndex());
    }

    private TextureInfo baseColorTexture(AIMaterial material, int materialIndex) throws IOException {
        int textureType = Assimp.aiTextureType_BASE_COLOR;
        int count = Assimp.aiGetMaterialTextureCount(material, textureType);
        if (count == 0) {
            textureType = Assimp.aiTextureType_DIFFUSE;
            count = Assimp.aiGetMaterialTextureCount(material, textureType);
        }
        if (count == 0)
            return new TextureInfo(ModelMaterial.NO_TEXTURE, 0);
        if (count != 1)
            throw new IOException("Material " + materialIndex + " has more than one base-color texture");

        AIString path = AIString.create();
        int[] mapping = {Assimp.aiTextureMapping_UV};
        int[] uvIndex = {0};
        float[] blend = {1};
        int[] operation = {Assimp.aiTextureOp_Multiply};
        int[] mapModes = {Assimp.aiTextureMapMode_Wrap, Assimp.aiTextureMapMode_Wrap, Assimp.aiTextureMapMode_Wrap};
        int[] flags = {0};
        if (Assimp.aiGetMaterialTexture(material, textureType, 0, path, mapping, uvIndex, blend, operation, mapModes, flags) !=
                Assimp.aiReturn_SUCCESS)
            throw new IOException("Could not read the base-color texture of material " + materialIndex);
        if (mapping[0] != Assimp.aiTextureMapping_UV || operation[0] != Assimp.aiTextureOp_Multiply || blend[0] != 1 || flags[0] != 0)
            throw new IOException("Material " + materialIndex + " uses unsupported texture mapping");
        rejectUVTransform(material, textureType, materialIndex);

        ModelSampler sampler = new ModelSampler(
                minFilter(getInt(material, Assimp._AI_MATKEY_GLTF_MAPPINGFILTER_MIN_BASE, textureType, 0, 9987)),
                magFilter(getInt(material, Assimp._AI_MATKEY_GLTF_MAPPINGFILTER_MAG_BASE, textureType, 0, 9729)),
                wrap(mapModes[0]), wrap(mapModes[1]));
        String texturePath = path.dataString();
        TextureKey key = new TextureKey(texturePath, sampler);
        Integer index = textureIndices.get(key);
        if (index == null) {
            index = textures.size();
            textures.add(loadTexture(texturePath, sampler));
            textureIndices.put(key, index);
        }
        return new TextureInfo(index, uvIndex[0]);
    }

    private static void rejectUVTransform(AIMaterial material, int textureType, int materialIndex) throws IOException {
        AIUVTransform transform = AIUVTransform.create();
        if (Assimp.aiGetMaterialUVTransform(material, Assimp._AI_MATKEY_UVTRANSFORM_BASE, textureType, 0, transform) != Assimp.aiReturn_SUCCESS)
            return;
        if (transform.mTranslation().x() != 0 || transform.mTranslation().y() != 0 || transform.mScaling().x() != 1 ||
                transform.mScaling().y() != 1 || transform.mRotation() != 0)
            throw new IOException("Material " + materialIndex + " uses an unsupported texture-coordinate transform");
    }

    private ModelTexture loadTexture(String path, ModelSampler sampler) throws IOException {
        AITexture texture = Assimp.aiGetEmbeddedTexture(source, path);
        if (texture == null)
            throw new IOException("Assimp did not embed texture " + path);

        String name = texture.mFilename().dataString();
        if (name.isEmpty())
            name = path;
        if (texture.mHeight() == 0)
            return decodeTexture(name, texture.pcDataCompressed(), sampler);

        int width = texture.mWidth();
        int height = texture.mHeight();
        ByteBuffer rgba = BufferUtils.newByteBuffer(Math.multiplyExact(Math.multiplyExact(width, height), 4));
        AITexel.Buffer texels = texture.pcData();
        for (int i = 0; i < texels.capacity(); i++) {
            AITexel texel = texels.get(i);
            rgba.put(texel.r()).put(texel.g()).put(texel.b()).put(texel.a());
        }
        return new ModelTexture(name, width, height, rgba.flip(), sampler);
    }

    private static ModelTexture decodeTexture(String name, ByteBuffer encoded, ModelSampler sampler) throws IOException {
        int[] width = new int[1];
        int[] height = new int[1];
        int[] components = new int[1];
        ByteBuffer decoded = STBImage.stbi_load_from_memory(encoded, width, height, components, 4);
        if (decoded == null)
            throw new IOException("Could not decode texture " + name + ": " + STBImage.stbi_failure_reason());
        try {
            ByteBuffer rgba = BufferUtils.newByteBuffer(decoded.remaining());
            int rowSize = width[0] * 4;
            for (int y = height[0] - 1; y >= 0; y--)
                BufferUtils.putRange(rgba, decoded, y * rowSize, rowSize);
            return new ModelTexture(name, width[0], height[0], rgba.flip(), sampler);
        } finally {
            STBImage.stbi_image_free(decoded);
        }
    }

    private List<ModelMesh> convertMeshes(List<MaterialData> materials) throws IOException {
        PointerBuffer sourceMeshes = source.mMeshes();
        if (source.mNumMeshes() == 0)
            throw new IOException("Model scene has no meshes");
        if (sourceMeshes == null)
            throw new IOException("Assimp scene has no mesh array");

        ArrayList<ModelMesh> result = new ArrayList<>(source.mNumMeshes());
        for (int i = 0; i < source.mNumMeshes(); i++)
            result.add(convertMesh(AIMesh.create(sourceMeshes.get(i)), i, materials));
        return List.copyOf(result);
    }

    private ModelMesh convertMesh(AIMesh mesh, int meshIndex, List<MaterialData> materials) throws IOException {
        if (mesh.mNumBones() != 0)
            throw new IOException("Mesh " + meshIndex + " uses unsupported skinning");
        if (mesh.mNumAnimMeshes() != 0)
            throw new IOException("Mesh " + meshIndex + " uses unsupported morph targets");

        ModelMesh.Primitive primitive = primitive(mesh.mPrimitiveTypes(), meshIndex);
        int materialIndex = mesh.mMaterialIndex();
        if (materialIndex < 0 || materialIndex >= materials.size())
            throw new IOException("Mesh " + meshIndex + " has invalid material index " + materialIndex);
        MaterialData material = materials.get(materialIndex);
        if (primitive != ModelMesh.Primitive.TRIANGLES && material.material().baseColorTexture() != ModelMaterial.NO_TEXTURE) {
            String type = primitive == ModelMesh.Primitive.LINES ? "line" : "point";
            throw new IOException("Textures are not supported on " + type + " mesh " + meshIndex);
        }

        FloatBuffer positions = copyPositions(mesh);
        FloatBuffer normals = primitive == ModelMesh.Primitive.TRIANGLES && !material.material().unlit() ?
                copyNormals(mesh, meshIndex) : null;
        ByteBuffer colors = copyColors(mesh);
        FloatBuffer texCoords = copyTexCoords(mesh, material, meshIndex);
        IndexData indexData = copyIndices(mesh, primitive, meshIndex);
        String name = mesh.mName().dataString();
        return new ModelMesh(name.isEmpty() ? "mesh-" + meshIndex : name, primitive, positions, normals, colors, texCoords,
                indexData.indices(), indexData.lineOffsets(), materialIndex);
    }

    private static FloatBuffer copyPositions(AIMesh mesh) {
        int vertexCount = mesh.mNumVertices();
        FloatBuffer result = BufferUtils.newFloatBuffer(Math.multiplyExact(vertexCount, 3));
        AIVector3D.Buffer vertices = mesh.mVertices();
        for (int i = 0; i < vertexCount; i++) {
            AIVector3D vertex = vertices.get(i);
            result.put(vertex.x()).put(vertex.y()).put(vertex.z());
        }
        return result.flip();
    }

    private static FloatBuffer copyNormals(AIMesh mesh, int meshIndex) throws IOException {
        AIVector3D.Buffer normals = mesh.mNormals();
        if (normals == null)
            throw new IOException("Lit triangle mesh " + meshIndex + " has no normals");

        FloatBuffer result = BufferUtils.newFloatBuffer(Math.multiplyExact(mesh.mNumVertices(), 3));
        for (int i = 0; i < mesh.mNumVertices(); i++) {
            AIVector3D normal = normals.get(i);
            result.put(normal.x()).put(normal.y()).put(normal.z());
        }
        return result.flip();
    }

    private static ByteBuffer copyColors(AIMesh mesh) {
        int vertexCount = mesh.mNumVertices();
        ByteBuffer result = BufferUtils.newByteBuffer(Math.multiplyExact(vertexCount, 4));
        AIColor4D.Buffer colors = mesh.mColors(0);
        if (colors == null) {
            for (int i = 0; i < vertexCount; i++)
                result.putInt(-1);
        } else {
            for (int i = 0; i < vertexCount; i++) {
                AIColor4D color = colors.get(i);
                result.put(toByte(color.r())).put(toByte(color.g())).put(toByte(color.b())).put(toByte(color.a()));
            }
        }
        return result.flip();
    }

    private static FloatBuffer copyTexCoords(AIMesh mesh, MaterialData material, int meshIndex) throws IOException {
        if (material.material().baseColorTexture() == ModelMaterial.NO_TEXTURE)
            return null;

        int channel = material.uvIndex();
        if (channel < 0 || channel >= Assimp.AI_MAX_NUMBER_OF_TEXTURECOORDS)
            throw new IOException("Mesh " + meshIndex + " uses invalid texture-coordinate channel " + channel);
        AIVector3D.Buffer coords = mesh.mTextureCoords(channel);
        if (coords == null)
            throw new IOException("Textured mesh " + meshIndex + " has no texture-coordinate channel " + channel);
        if (mesh.mNumUVComponents(channel) < 2)
            throw new IOException("Mesh " + meshIndex + " has one-dimensional texture coordinates");

        FloatBuffer result = BufferUtils.newFloatBuffer(Math.multiplyExact(mesh.mNumVertices(), 2));
        for (int i = 0; i < mesh.mNumVertices(); i++) {
            AIVector3D coord = coords.get(i);
            result.put(coord.x()).put(coord.y());
        }
        return result.flip();
    }

    private static IndexData copyIndices(AIMesh mesh, ModelMesh.Primitive primitive, int meshIndex) throws IOException {
        if (primitive == ModelMesh.Primitive.LINES)
            return copyLineIndices(mesh, meshIndex);

        int indicesPerFace = switch (primitive) {
            case POINTS -> 1;
            case TRIANGLES -> 3;
            case LINES -> throw new AssertionError();
        };
        int faceCount = mesh.mNumFaces();
        if (faceCount == 0)
            throw new IOException("Mesh " + meshIndex + " has no primitives");
        IntBuffer indices = BufferUtils.newIntBuffer(Math.multiplyExact(faceCount, indicesPerFace));

        AIFace.Buffer faces = mesh.mFaces();
        for (int i = 0; i < faceCount; i++) {
            AIFace face = faces.get(i);
            if (face.mNumIndices() != indicesPerFace)
                throw new IOException("Mesh " + meshIndex + " contains an unexpected " + face.mNumIndices() + "-vertex primitive");
            indices.put(face.mIndices());
        }
        return new IndexData(indices.flip(), BufferUtils.newIntBuffer(0));
    }

    private static IndexData copyLineIndices(AIMesh mesh, int meshIndex) throws IOException {
        int edgeCount = mesh.mNumFaces();
        if (edgeCount == 0)
            throw new IOException("Mesh " + meshIndex + " has no primitives");

        int[] starts = new int[edgeCount];
        int[] ends = new int[edgeCount];
        ArrayList<ArrayList<Integer>> incidentEdges = new ArrayList<>(mesh.mNumVertices());
        for (int i = 0; i < mesh.mNumVertices(); i++)
            incidentEdges.add(new ArrayList<>());

        AIFace.Buffer faces = mesh.mFaces();
        for (int edge = 0; edge < edgeCount; edge++) {
            AIFace face = faces.get(edge);
            if (face.mNumIndices() != 2)
                throw new IOException("Mesh " + meshIndex + " contains an unexpected " + face.mNumIndices() + "-vertex primitive");
            IntBuffer faceIndices = face.mIndices();
            int start = faceIndices.get(0);
            int end = faceIndices.get(1);
            if (start < 0 || start >= mesh.mNumVertices() || end < 0 || end >= mesh.mNumVertices())
                throw new IOException("Mesh " + meshIndex + " contains an invalid line index");
            starts[edge] = start;
            ends[edge] = end;
            incidentEdges.get(start).add(edge);
            incidentEdges.get(end).add(edge);
        }

        IntBuffer indices = BufferUtils.newIntBuffer(Math.multiplyExact(edgeCount, 2));
        IntBuffer lineOffsets = BufferUtils.newIntBuffer(Math.addExact(edgeCount, 1));
        boolean[] visited = new boolean[edgeCount];
        lineOffsets.put(0);

        // Assimp represents every line mode as separate edges. Rebuild paths so the JHV line renderer can form joins and loops.
        for (int vertex = 0; vertex < incidentEdges.size(); vertex++) {
            if (incidentEdges.get(vertex).size() == 2)
                continue;
            for (int edge : incidentEdges.get(vertex)) {
                if (!visited[edge])
                    appendLine(vertex, edge, starts, ends, incidentEdges, visited, indices, lineOffsets);
            }
        }
        for (int edge = 0; edge < edgeCount; edge++) {
            if (!visited[edge])
                appendLine(starts[edge], edge, starts, ends, incidentEdges, visited, indices, lineOffsets);
        }
        return new IndexData(indices.flip(), lineOffsets.flip());
    }

    private static void appendLine(int start, int firstEdge, int[] starts, int[] ends, List<? extends List<Integer>> incidentEdges,
                                   boolean[] visited, IntBuffer indices, IntBuffer lineOffsets) {
        indices.put(start);
        int vertex = start;
        int edge = firstEdge;
        while (true) {
            visited[edge] = true;
            vertex = starts[edge] == vertex ? ends[edge] : starts[edge];
            indices.put(vertex);
            List<Integer> nextEdges = incidentEdges.get(vertex);
            if (nextEdges.size() != 2)
                break;

            int nextEdge = nextEdges.get(0) == edge ? nextEdges.get(1) : nextEdges.get(0);
            if (visited[nextEdge])
                break;
            edge = nextEdge;
        }
        lineOffsets.put(indices.position());
    }

    private void convertNode(AINode node, Matrix4fc parentTransform, List<ModelMesh> sourceMeshes, List<ModelMesh> meshes) throws IOException {
        Matrix4f transform = new Matrix4f(parentTransform).mul(matrix(node.mTransformation()));
        IntBuffer meshIndices = node.mMeshes();
        if (node.mNumMeshes() != 0 && meshIndices == null)
            throw new IOException("Node " + node.mName().dataString() + " has no mesh-index array");
        for (int i = 0; i < node.mNumMeshes(); i++) {
            int meshIndex = meshIndices.get(i);
            if (meshIndex < 0 || meshIndex >= sourceMeshes.size())
                throw new IOException("Node " + node.mName().dataString() + " uses invalid mesh index " + meshIndex);
            meshes.add(transformMesh(sourceMeshes.get(meshIndex), transform));
        }

        PointerBuffer children = node.mChildren();
        if (node.mNumChildren() != 0 && children == null)
            throw new IOException("Node " + node.mName().dataString() + " has no child array");
        for (int i = 0; i < node.mNumChildren(); i++)
            convertNode(AINode.create(children.get(i)), transform, sourceMeshes, meshes);
    }

    private static ModelMesh transformMesh(ModelMesh mesh, Matrix4fc transform) throws IOException {
        FloatBuffer sourcePositions = mesh.positions();
        FloatBuffer positions = BufferUtils.newFloatBuffer(sourcePositions.remaining());
        while (sourcePositions.hasRemaining()) {
            float x = sourcePositions.get();
            float y = sourcePositions.get();
            float z = sourcePositions.get();
            positions.put(transform.m00() * x + transform.m10() * y + transform.m20() * z + transform.m30())
                    .put(transform.m01() * x + transform.m11() * y + transform.m21() * z + transform.m31())
                    .put(transform.m02() * x + transform.m12() * y + transform.m22() * z + transform.m32());
        }

        FloatBuffer normals = transformNormals(mesh, transform);
        IntBuffer indices = mesh.indices();
        if (mesh.primitive() == ModelMesh.Primitive.TRIANGLES && transform.determinant3x3() < 0)
            indices = reverseTriangles(indices);

        return new ModelMesh(mesh.name(), mesh.primitive(), positions.flip(), normals, mesh.colors(), mesh.texCoords(), indices,
                mesh.lineOffsets(), mesh.materialIndex());
    }

    private static FloatBuffer transformNormals(ModelMesh mesh, Matrix4fc transform) throws IOException {
        FloatBuffer sourceNormals = mesh.normals();
        if (sourceNormals == null)
            return null;
        if (transform.determinant3x3() == 0)
            throw new IOException("Triangle mesh " + mesh.name() + " has a singular node transform");

        Matrix3f normalMatrix = transform.normal(new Matrix3f());
        FloatBuffer normals = BufferUtils.newFloatBuffer(sourceNormals.remaining());
        while (sourceNormals.hasRemaining()) {
            float x = sourceNormals.get();
            float y = sourceNormals.get();
            float z = sourceNormals.get();
            float nx = normalMatrix.m00() * x + normalMatrix.m10() * y + normalMatrix.m20() * z;
            float ny = normalMatrix.m01() * x + normalMatrix.m11() * y + normalMatrix.m21() * z;
            float nz = normalMatrix.m02() * x + normalMatrix.m12() * y + normalMatrix.m22() * z;
            float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (length != 0) {
                nx /= length;
                ny /= length;
                nz /= length;
            }
            normals.put(nx).put(ny).put(nz);
        }
        return normals.flip();
    }

    private static IntBuffer reverseTriangles(IntBuffer source) {
        IntBuffer indices = BufferUtils.newIntBuffer(source.remaining());
        while (source.hasRemaining()) {
            int first = source.get();
            int second = source.get();
            int third = source.get();
            indices.put(first).put(third).put(second);
        }
        return indices.flip();
    }

    private static Matrix4f matrix(AIMatrix4x4 matrix) {
        // Assimp names entries by row; the JOML constructor takes columns.
        return new Matrix4f(
                matrix.a1(), matrix.b1(), matrix.c1(), matrix.d1(),
                matrix.a2(), matrix.b2(), matrix.c2(), matrix.d2(),
                matrix.a3(), matrix.b3(), matrix.c3(), matrix.d3(),
                matrix.a4(), matrix.b4(), matrix.c4(), matrix.d4());
    }

    private static ModelMesh.Primitive primitive(int type, int meshIndex) throws IOException {
        return switch (type) {
            case Assimp.aiPrimitiveType_POINT -> ModelMesh.Primitive.POINTS;
            case Assimp.aiPrimitiveType_LINE -> ModelMesh.Primitive.LINES;
            case Assimp.aiPrimitiveType_TRIANGLE -> ModelMesh.Primitive.TRIANGLES;
            default -> throw new IOException("Mesh " + meshIndex + " has unsupported or mixed primitive types: " + type);
        };
    }

    private static ModelMaterial.AlphaMode alphaMode(AIMaterial material) throws IOException {
        AIString value = AIString.create();
        if (Assimp.aiGetMaterialString(material, Assimp.AI_MATKEY_GLTF_ALPHAMODE, 0, 0, value) != Assimp.aiReturn_SUCCESS)
            return ModelMaterial.AlphaMode.OPAQUE;
        return switch (value.dataString()) {
            case "OPAQUE" -> ModelMaterial.AlphaMode.OPAQUE;
            case "MASK" -> ModelMaterial.AlphaMode.MASK;
            case "BLEND" -> ModelMaterial.AlphaMode.BLEND;
            default -> throw new IOException("Unsupported alpha mode: " + value.dataString());
        };
    }

    private static float getFloat(AIMaterial material, String key, int type, int index, float fallback) {
        float[] value = {fallback};
        int[] count = {1};
        return Assimp.aiGetMaterialFloatArray(material, key, type, index, value, count) == Assimp.aiReturn_SUCCESS ? value[0] : fallback;
    }

    private static int getInt(AIMaterial material, String key, int type, int index, int fallback) {
        int[] value = {fallback};
        int[] count = {1};
        return Assimp.aiGetMaterialIntegerArray(material, key, type, index, value, count) == Assimp.aiReturn_SUCCESS ? value[0] : fallback;
    }

    private static ModelSampler.MinFilter minFilter(int filter) throws IOException {
        return switch (filter) {
            case GL.NEAREST -> ModelSampler.MinFilter.NEAREST;
            case GL.LINEAR -> ModelSampler.MinFilter.LINEAR;
            case GL.NEAREST_MIPMAP_NEAREST -> ModelSampler.MinFilter.NEAREST_MIPMAP_NEAREST;
            case GL.LINEAR_MIPMAP_NEAREST -> ModelSampler.MinFilter.LINEAR_MIPMAP_NEAREST;
            case GL.NEAREST_MIPMAP_LINEAR -> ModelSampler.MinFilter.NEAREST_MIPMAP_LINEAR;
            case GL.LINEAR_MIPMAP_LINEAR -> ModelSampler.MinFilter.LINEAR_MIPMAP_LINEAR;
            default -> throw new IOException("Unsupported texture minification filter: " + filter);
        };
    }

    private static ModelSampler.MagFilter magFilter(int filter) throws IOException {
        return switch (filter) {
            case GL.NEAREST -> ModelSampler.MagFilter.NEAREST;
            case GL.LINEAR -> ModelSampler.MagFilter.LINEAR;
            default -> throw new IOException("Unsupported texture magnification filter: " + filter);
        };
    }

    private static ModelSampler.Wrap wrap(int mode) throws IOException {
        return switch (mode) {
            case Assimp.aiTextureMapMode_Wrap -> ModelSampler.Wrap.REPEAT;
            case Assimp.aiTextureMapMode_Clamp -> ModelSampler.Wrap.CLAMP_TO_EDGE;
            case Assimp.aiTextureMapMode_Mirror -> ModelSampler.Wrap.MIRRORED_REPEAT;
            default -> throw new IOException("Unsupported texture wrap mode: " + mode);
        };
    }

    private static byte toByte(float value) {
        return (byte) Math.round(255 * Math.clamp(value, 0, 1));
    }

    private record IndexData(IntBuffer indices, IntBuffer lineOffsets) {}

    private record MaterialData(ModelMaterial material, int uvIndex) {}

    private record TextureInfo(int textureIndex, int uvIndex) {}

    private record TextureKey(String path, ModelSampler sampler) {}

}
