package org.helioviewer.jhv.opengl.model;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import javax.imageio.ImageIO;

import org.helioviewer.jhv.io.NetFileCache;
import org.helioviewer.jhv.layers.ModelLayer;
import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONObject;

public final class AssimpModelLoaderTest {

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("jhv-assimp-");
        try {
            Path model = directory.resolve("model.gltf");
            String document = modelJson();
            byte[] json = document.getBytes(StandardCharsets.UTF_8);
            Files.write(model, json);
            writeGeometry(directory.resolve("geometry.bin"));
            writeTexture(directory.resolve("texture.png"));

            checkScene(AssimpModelLoader.load(NetFileCache.get(model.toUri())));
            checkLineReconstruction(directory);
            checkTexturedDrawingsRejected(directory, document);
            checkLitNormalsRequired(directory, document);
            checkUnlitNormalsDiscarded(directory, document);
            checkPositionMetadata(directory, document);
            checkLayer(model);

            Path compressed = directory.resolve("model.gltf.gz");
            try (OutputStream output = new GZIPOutputStream(Files.newOutputStream(compressed))) {
                output.write(json);
            }
            checkScene(AssimpModelLoader.load(NetFileCache.get(compressed.toUri())));
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList())
                    Files.delete(path);
            }
        }
    }

    private static void checkLineReconstruction(Path directory) throws Exception {
        Path binary = directory.resolve("line-graph.bin");
        ByteBuffer data = ByteBuffer.allocate(108).order(ByteOrder.LITTLE_ENDIAN);
        for (int vertex = 0; vertex < 7; vertex++)
            data.putFloat(vertex).putFloat(0).putFloat(0);
        for (int index : new int[]{0, 1, 1, 2, 2, 0, 3, 4, 4, 5, 4, 6})
            data.putShort((short) index);
        Files.write(binary, data.array());

        Path model = directory.resolve("line-graph.gltf");
        Files.writeString(model, """
                {
                  "asset": {"version": "2.0"},
                  "scene": 0,
                  "scenes": [{"nodes": [0]}],
                  "nodes": [{"mesh": 0}],
                  "buffers": [{"byteLength": 108, "uri": "line-graph.bin"}],
                  "bufferViews": [
                    {"buffer": 0, "byteOffset": 0, "byteLength": 84, "target": 34962},
                    {"buffer": 0, "byteOffset": 84, "byteLength": 24, "target": 34963}
                  ],
                  "accessors": [
                    {"bufferView": 0, "componentType": 5126, "count": 7, "type": "VEC3",
                     "min": [0, 0, 0], "max": [6, 0, 0]},
                    {"bufferView": 1, "componentType": 5123, "count": 12, "type": "SCALAR"}
                  ],
                  "materials": [{}],
                  "meshes": [{"primitives": [
                    {"attributes": {"POSITION": 0}, "indices": 1, "material": 0, "mode": 1}
                  ]}]
                }
                """);

        ModelMesh lines = meshes(AssimpModelLoader.load(NetFileCache.get(model.toUri())), ModelMesh.Primitive.LINES).getFirst();
        IntBuffer indices = lines.indices();
        IntBuffer offsets = lines.lineOffsets();
        check(offsets.remaining() == 5, "loop and branch path count");

        Set<Long> edges = new HashSet<>();
        int closedPaths = 0;
        for (int path = 0; path < offsets.remaining() - 1; path++) {
            int start = offsets.get(path);
            int end = offsets.get(path + 1);
            if (indices.get(start) == indices.get(end - 1))
                closedPaths++;
            for (int i = start + 1; i < end; i++) {
                int first = indices.get(i - 1);
                int second = indices.get(i);
                check(edges.add(undirectedEdge(first, second)), "duplicate reconstructed edge");
            }
        }
        check(closedPaths == 1, "closed loop reconstruction");
        check(edges.equals(Set.of(undirectedEdge(0, 1), undirectedEdge(1, 2), undirectedEdge(2, 0),
                undirectedEdge(3, 4), undirectedEdge(4, 5), undirectedEdge(4, 6))), "reconstructed line edges");
    }

    private static long undirectedEdge(int first, int second) {
        return (long) Math.min(first, second) << 32 | Integer.toUnsignedLong(Math.max(first, second));
    }

    private static void checkPositionMetadata(Path directory, String document) throws Exception {
        String scene = "\"scenes\": [{\"name\": \"URI loader test\", \"nodes\": [0]}]";
        String positionedScene = "\"scenes\": [{\"name\": \"URI loader test\", \"nodes\": [0], \"extras\": {" +
                "\"DATE-OBS\": \"2025-10-09T18:19:52\", \"WCSNAME\": \"Heliocentric-cartesian\", " +
                "\"CTYPE1\": \"SOLX\", \"CTYPE2\": \"SOLY\", \"CTYPE3\": \"SOLZ\", " +
                "\"CUNIT1\": \"solRad\", \"CUNIT2\": \"solRad\", \"CUNIT3\": \"solRad\", " +
                "\"RSUN_REF\": 695700000.0, \"DSUN_OBS\": 149000000000.0, " +
                "\"CRLN_OBS\": 90, \"CRLT_OBS\": -30}}]";
        check(document.contains(scene), "scene metadata insertion point");

        Path positioned = directory.resolve("positioned.gltf");
        Path glb = directory.resolve("positioned.glb");
        Path dated = directory.resolve("dated.gltf");
        try {
            String positionedDocument = document.replace(scene, positionedScene);
            Files.writeString(positioned, positionedDocument);
            checkPositionedScene(AssimpModelLoader.load(NetFileCache.get(positioned.toUri())));
            check(new ModelLayer(positioned.toUri()).getTimeString().equals("2025-10-09T18:19:52.000"), "layer observation time");

            writeGlb(glb, positionedDocument, Files.readAllBytes(directory.resolve("geometry.bin")));
            checkPositionedScene(AssimpModelLoader.load(NetFileCache.get(glb.toUri())));

            String datedScene = "\"scenes\": [{\"name\": \"URI loader test\", \"nodes\": [0], \"extras\": {" +
                    "\"DATE-OBS\": \"2025-10-09T18:19:52\"}}]";
            Files.writeString(dated, document.replace(scene, datedScene));
            check(AssimpModelLoader.load(NetFileCache.get(dated.toUri())).time().equals(TimeUtils.START),
                    "metadata without WCSNAME ignored");
            checkPositionRejected(directory, document, scene, positionedScene.replace("\"DATE-OBS\"", "\"DATE_OBS\""),
                    "missing DATE-OBS in solar metadata");
            checkPositionRejected(directory, document, scene, positionedScene.replace("\"CTYPE1\": \"SOLX\"", "\"CTYPE1\": \"SOLZ\""),
                    "CTYPE1 must be SOLX");
            checkPositionRejected(directory, document, scene, positionedScene.replace("\"CUNIT1\": \"solRad\"", "\"CUNIT1\": \"km\""),
                    "CUNIT1 must be solRad");
            checkPositionRejected(directory, document, scene, positionedScene.replace(", \"CRLT_OBS\": -30", ""),
                    "missing CRLT_OBS in solar metadata");
            checkPositionRejected(directory, document, scene, positionedScene.replace("149000000000.0", "-1"),
                    "DSUN_OBS must be positive");
        } finally {
            Files.deleteIfExists(dated);
            Files.deleteIfExists(glb);
            Files.deleteIfExists(positioned);
        }
    }

    private static void checkPositionedScene(ModelScene scene) {
        check(scene.time().toString().equals("2025-10-09T18:19:52.000"), "scene observation time");

        List<ModelMesh> triangles = meshes(scene, ModelMesh.Primitive.TRIANGLES);
        FloatBuffer positions = triangles.getFirst().positions();
        float cos30 = (float) (Math.sqrt(3) / 2);
        checkPosition(positions, 0, 0, 0, 0);
        checkPosition(positions, 1, 0, 0, -1);
        checkPosition(positions, 2, 0.5f + cos30, cos30 - 0.5f, 0);
        checkPosition(triangles.get(2).positions(), 0, 0, 0, -2);
    }

    private static void checkPositionRejected(Path directory, String document, String scene, String positionedScene,
            String expectedMessage) throws Exception {
        checkLoadRejected(directory.resolve("invalid-position.gltf"), document.replace(scene, positionedScene), expectedMessage);
    }

    private static void writeGlb(Path path, String document, byte[] binary) throws Exception {
        String buffer = "\"buffers\": [{\"byteLength\": 124, \"uri\": \"geometry.bin\"}]";
        check(document.contains(buffer), "GLB buffer insertion point");
        byte[] json = document.replace(buffer, "\"buffers\": [{\"byteLength\": 124}]")
                .getBytes(StandardCharsets.UTF_8);
        int jsonLength = json.length + 3 & ~3;
        int binaryLength = binary.length + 3 & ~3;
        int totalLength = 28 + jsonLength + binaryLength;
        ByteBuffer glb = ByteBuffer.allocate(totalLength).order(ByteOrder.LITTLE_ENDIAN);
        glb.putInt(0x46546c67).putInt(2).putInt(totalLength);
        glb.putInt(jsonLength).putInt(0x4e4f534a).put(json);
        while ((glb.position() & 3) != 0)
            glb.put((byte) ' ');
        glb.putInt(binaryLength).putInt(0x004e4942).put(binary);
        while (glb.hasRemaining())
            glb.put((byte) 0);
        Files.write(path, glb.array());
    }

    private static void checkTexturedDrawingsRejected(Path directory, String document) throws Exception {
        String line = "\"indices\": 4, \"material\": 1, \"mode\": 3";
        String point = "\"indices\": 5, \"material\": 2, \"mode\": 0";
        check(document.contains(line) && document.contains(point), "drawing primitives");
        checkLoadRejected(directory.resolve("textured-line.gltf"), document.replace(line,
                "\"indices\": 4, \"material\": 0, \"mode\": 3"), "Textures are not supported on line mesh");
        checkLoadRejected(directory.resolve("textured-point.gltf"), document.replace(point,
                "\"indices\": 5, \"material\": 0, \"mode\": 0"), "Textures are not supported on point mesh");
    }

    private static void checkLitNormalsRequired(Path directory, String document) throws Exception {
        String unlit = "\"extensions\": {\"KHR_materials_unlit\": {}}";
        check(document.contains(unlit), "unlit triangle material");
        checkLoadRejected(directory.resolve("missing-normals.gltf"), document.replace(unlit, "\"extras\": {}"),
                "Lit triangle mesh 0 has no normals");
    }

    private static void checkUnlitNormalsDiscarded(Path directory, String document) throws Exception {
        String attributes = "\"POSITION\": 0, \"TEXCOORD_0\": 1";
        check(document.contains(attributes), "unlit triangle attributes");
        Path path = directory.resolve("unlit-normals.gltf");
        Files.writeString(path, document.replace(attributes, "\"POSITION\": 0, \"NORMAL\": 6, \"TEXCOORD_0\": 1"));
        ModelMesh mesh = meshes(AssimpModelLoader.load(NetFileCache.get(path.toUri())), ModelMesh.Primitive.TRIANGLES).getFirst();
        check(mesh.normals() == null, "unlit triangle normals discarded");
    }

    private static void checkLoadRejected(Path path, String document, String expectedMessage) throws Exception {
        Files.writeString(path, document);
        checkLoadRejected(path, expectedMessage);
    }

    private static void checkLoadRejected(Path path, String expectedMessage) throws Exception {
        try {
            AssimpModelLoader.load(NetFileCache.get(path.toUri()));
            throw new AssertionError("model was accepted: " + path.getFileName());
        } catch (IOException e) {
            check(e.getMessage().contains(expectedMessage), "unexpected rejection: " + e.getMessage());
        }
    }

    private static void checkLayer(Path model) throws Exception {
        ModelLayer layer = new ModelLayer(model.toUri());
        check(layer.getName().equals("URI loader test"), "layer name");
        check(layer.getTimeString().equals(TimeUtils.START.toString()), "default layer time");
        check(layer.isEnabled(), "layer enabled");
        check(layer.isLocal(), "local layer");

        JSONObject state = new JSONObject();
        layer.serialize(state);
        check(state.getString("uri").equals(model.toUri().toString()), "layer URI state");

        ModelLayer restored = new ModelLayer(state);
        check(restored.getName().equals(layer.getName()), "restored layer");
    }

    private static void checkScene(ModelScene scene) {
        check(scene.name().equals("URI loader test"), "scene name");
        check(scene.time().equals(TimeUtils.START), "default scene time");
        check(scene.meshes().size() == 8, "two baked copies of four mesh primitives");
        check(scene.materials().size() == 3, "materials");
        check(scene.textures().size() == 1, "texture");

        List<ModelMesh> triangleMeshes = meshes(scene, ModelMesh.Primitive.TRIANGLES);
        ModelMesh triangles = triangleMeshes.getFirst();
        check(triangles.vertexCount() == 3, "triangle vertex count");
        check(triangles.normals() == null, "unlit triangle normals omitted");
        check(triangles.texCoords() != null && triangles.texCoords().remaining() == 6, "texture coordinates");
        check(close(triangles.texCoords().get(0), 0) && close(triangles.texCoords().get(1), 1),
                "Assimp texture-coordinate origin");
        check(Byte.toUnsignedInt(triangles.colors().get(1)) == 128, "vertex colors");

        ModelMaterial surface = scene.materials().get(triangles.materialIndex());
        check(close(surface.red(), 0.5f) && close(surface.alpha(), 0.8f), "base color");
        check(surface.alphaMode() == ModelMaterial.AlphaMode.MASK && close(surface.alphaCutoff(), 0.25f), "alpha mask");
        check(surface.doubleSided() && surface.unlit(), "material flags");
        check(surface.baseColorTexture() == 0, "base-color texture");

        ModelMesh litTriangles = triangleMeshes.get(1);
        check(litTriangles.normals() != null && litTriangles.normals().remaining() == 9, "lit triangle normals");

        ModelTexture texture = scene.textures().getFirst();
        check(texture.width() == 2 && texture.height() == 2, "texture dimensions");
        check(texture.sampler().equals(new ModelSampler(ModelSampler.MinFilter.NEAREST, ModelSampler.MagFilter.NEAREST,
                ModelSampler.Wrap.CLAMP_TO_EDGE, ModelSampler.Wrap.MIRRORED_REPEAT)), "texture sampler");
        checkPixel(texture.rgba(), 0, 0, 0, 255, 64);
        checkPixel(texture.rgba(), 2, 255, 0, 0, 255);

        ModelMesh lines = meshes(scene, ModelMesh.Primitive.LINES).getFirst();
        check(lines.indices().remaining() == 3, "line-strip indices");
        check(lines.texCoords() == null, "unused texture coordinates");
        checkTexturedDrawingRejected(scene, lines);
        IntBuffer offsets = lines.lineOffsets();
        check(offsets.remaining() == 2 && offsets.get(0) == 0 && offsets.get(1) == 3, "line-strip reconstruction");

        ModelMaterial drawing = scene.materials().get(lines.materialIndex());
        check(drawing.alphaMode() == ModelMaterial.AlphaMode.BLEND && close(drawing.alpha(), 0.5f), "blended material");

        ModelMesh points = meshes(scene, ModelMesh.Primitive.POINTS).getFirst();
        checkTexturedDrawingRejected(scene, points);
        ModelMaterial markers = scene.materials().get(points.materialIndex());
        check(markers.alphaMode() == ModelMaterial.AlphaMode.OPAQUE && close(markers.alpha(), 0.5f), "default opaque material");

        ModelMesh transformedTriangles = triangleMeshes.getLast();
        checkPosition(transformedTriangles.positions(), 0, 2, 0, 0);
        checkPosition(transformedTriangles.positions(), 1, 0, 0, 0);
        checkPosition(transformedTriangles.positions(), 2, 2, 3, 0.5f);
        IntBuffer transformedIndices = transformedTriangles.indices();
        check(transformedIndices.get(0) == 0 && transformedIndices.get(1) == 2 && transformedIndices.get(2) == 1,
                "mirrored triangle winding");
        checkNormalMatchesWinding(transformedTriangles);

        checkPosition(meshes(scene, ModelMesh.Primitive.LINES).getLast().positions(), 2, 2, 3, 0.5f);
        checkPosition(meshes(scene, ModelMesh.Primitive.POINTS).getLast().positions(), 1, 2, 3, 0.5f);
    }

    private static void checkTexturedDrawingRejected(ModelScene scene, ModelMesh mesh) {
        ModelMesh textured = new ModelMesh(mesh.name(), mesh.primitive(), mesh.positions(), mesh.normals(), mesh.colors(),
                mesh.texCoords(), mesh.indices(), mesh.lineOffsets(), 0);
        try {
            new ModelScene("invalid", TimeUtils.START, List.of(textured), scene.materials(), scene.textures());
            throw new AssertionError("textured drawing mesh was accepted: " + mesh.primitive());
        } catch (IllegalArgumentException e) {
            check(e.getMessage().contains("Only triangle meshes can have textures"), "unexpected rejection: " + e.getMessage());
        }
    }

    private static List<ModelMesh> meshes(ModelScene scene, ModelMesh.Primitive primitive) {
        return scene.meshes().stream().filter(mesh -> mesh.primitive() == primitive).toList();
    }

    private static void checkPosition(FloatBuffer positions, int vertex, float x, float y, float z) {
        check(close(positions.get(3 * vertex), x) && close(positions.get(3 * vertex + 1), y) && close(positions.get(3 * vertex + 2), z),
                "transformed vertex " + vertex);
    }

    private static void checkNormalMatchesWinding(ModelMesh mesh) {
        FloatBuffer positions = mesh.positions();
        FloatBuffer normals = mesh.normals();
        IntBuffer indices = mesh.indices();
        int a = indices.get(0);
        int b = indices.get(1);
        int c = indices.get(2);
        float abx = positions.get(3 * b) - positions.get(3 * a);
        float aby = positions.get(3 * b + 1) - positions.get(3 * a + 1);
        float abz = positions.get(3 * b + 2) - positions.get(3 * a + 2);
        float acx = positions.get(3 * c) - positions.get(3 * a);
        float acy = positions.get(3 * c + 1) - positions.get(3 * a + 1);
        float acz = positions.get(3 * c + 2) - positions.get(3 * a + 2);
        float nx = aby * acz - abz * acy;
        float ny = abz * acx - abx * acz;
        float nz = abx * acy - aby * acx;
        float dot = nx * normals.get(3 * a) + ny * normals.get(3 * a + 1) + nz * normals.get(3 * a + 2);
        float crossLength = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        check(dot > 0.999f * crossLength, "transformed normal follows mirrored winding and non-uniform scale");
    }

    private static void writeGeometry(Path path) throws Exception {
        ByteBuffer data = ByteBuffer.allocate(124).order(ByteOrder.LITTLE_ENDIAN);
        data.putFloat(0).putFloat(0).putFloat(0);
        data.putFloat(1).putFloat(0).putFloat(0);
        data.putFloat(0).putFloat(1).putFloat(1);
        data.putFloat(0).putFloat(0);
        data.putFloat(1).putFloat(0);
        data.putFloat(0).putFloat(1);
        data.put(new byte[]{(byte) 255, (byte) 128, 64, (byte) 255, 1, 2, 3, 4, 10, 20, 30, 40});
        data.putShort((short) 0).putShort((short) 1).putShort((short) 2);
        data.putShort((short) 0).putShort((short) 1).putShort((short) 2);
        data.putShort((short) 0).putShort((short) 2);
        data.putFloat(0).putFloat((float) (-1 / Math.sqrt(2))).putFloat((float) (1 / Math.sqrt(2)));
        data.putFloat(0).putFloat((float) (-1 / Math.sqrt(2))).putFloat((float) (1 / Math.sqrt(2)));
        data.putFloat(0).putFloat((float) (-1 / Math.sqrt(2))).putFloat((float) (1 / Math.sqrt(2)));
        Files.write(path, data.array());
    }

    private static void writeTexture(Path path) throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0xffff0000);
        image.setRGB(1, 0, 0x8000ff00);
        image.setRGB(0, 1, 0x400000ff);
        image.setRGB(1, 1, 0x00ffffff);
        check(ImageIO.write(image, "png", path.toFile()), "PNG writer");
    }

    private static String modelJson() {
        return """
                {
                  "asset": {"version": "2.0"},
                  "scene": 0,
                  "scenes": [{"name": "URI loader test", "nodes": [0]}],
                  "nodes": [
                    {"mesh": 0, "children": [1]},
                    {"translation": [2, 0, 0], "children": [2]},
                    {"mesh": 0, "scale": [-2, 3, 0.5]}
                  ],
                  "buffers": [{"byteLength": 124, "uri": "geometry.bin"}],
                  "bufferViews": [
                    {"buffer": 0, "byteOffset": 0, "byteLength": 36, "target": 34962},
                    {"buffer": 0, "byteOffset": 36, "byteLength": 24, "target": 34962},
                    {"buffer": 0, "byteOffset": 60, "byteLength": 12, "target": 34962},
                    {"buffer": 0, "byteOffset": 72, "byteLength": 6, "target": 34963},
                    {"buffer": 0, "byteOffset": 78, "byteLength": 6, "target": 34963},
                    {"buffer": 0, "byteOffset": 84, "byteLength": 4, "target": 34963},
                    {"buffer": 0, "byteOffset": 88, "byteLength": 36, "target": 34962}
                  ],
                  "accessors": [
                    {"bufferView": 0, "componentType": 5126, "count": 3, "type": "VEC3",
                     "min": [0, 0, 0], "max": [1, 1, 1]},
                    {"bufferView": 1, "componentType": 5126, "count": 3, "type": "VEC2"},
                    {"bufferView": 2, "componentType": 5121, "count": 3, "type": "VEC4", "normalized": true},
                    {"bufferView": 3, "componentType": 5123, "count": 3, "type": "SCALAR"},
                    {"bufferView": 4, "componentType": 5123, "count": 3, "type": "SCALAR"},
                    {"bufferView": 5, "componentType": 5123, "count": 2, "type": "SCALAR"},
                    {"bufferView": 6, "componentType": 5126, "count": 3, "type": "VEC3"}
                  ],
                  "extensionsUsed": ["KHR_materials_unlit"],
                  "images": [{"name": "test texture", "uri": "texture.png"}],
                  "samplers": [{"magFilter": 9728, "minFilter": 9728, "wrapS": 33071, "wrapT": 33648}],
                  "textures": [{"sampler": 0, "source": 0}],
                  "materials": [
                    {"pbrMetallicRoughness": {"baseColorFactor": [0.5, 0.6, 0.7, 0.8],
                                              "baseColorTexture": {"index": 0}},
                     "alphaMode": "MASK", "alphaCutoff": 0.25, "doubleSided": true,
                     "extensions": {"KHR_materials_unlit": {}}},
                    {"pbrMetallicRoughness": {"baseColorFactor": [0.1, 0.2, 0.3, 0.5]}, "alphaMode": "BLEND"},
                    {"pbrMetallicRoughness": {"baseColorFactor": [0.4, 0.3, 0.2, 0.5]}}
                  ],
                  "meshes": [{"primitives": [
                    {"attributes": {"POSITION": 0, "TEXCOORD_0": 1, "COLOR_0": 2}, "indices": 3, "material": 0, "mode": 4},
                    {"attributes": {"POSITION": 0, "NORMAL": 6}, "indices": 3, "material": 1, "mode": 4},
                    {"attributes": {"POSITION": 0}, "indices": 4, "material": 1, "mode": 3},
                    {"attributes": {"POSITION": 0}, "indices": 5, "material": 2, "mode": 0}
                  ]}]
                }
                """;
    }

    private static void check(boolean condition, String description) {
        if (!condition)
            throw new AssertionError(description);
    }

    private static boolean close(float actual, float expected) {
        return Math.abs(actual - expected) < 1e-6f;
    }

    private static void checkPixel(ByteBuffer rgba, int pixel, int red, int green, int blue, int alpha) {
        int offset = 4 * pixel;
        check(Byte.toUnsignedInt(rgba.get(offset)) == red && Byte.toUnsignedInt(rgba.get(offset + 1)) == green &&
                Byte.toUnsignedInt(rgba.get(offset + 2)) == blue && Byte.toUnsignedInt(rgba.get(offset + 3)) == alpha,
                "texture pixel " + pixel);
    }

    private AssimpModelLoaderTest() {}

}
