package org.helioviewer.jhv.opengl;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Locale;
import java.util.TimeZone;

import javax.imageio.ImageIO;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.layers.ModelLayer;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.angle.AngleRenderer;
import org.helioviewer.jhv.opengl.model.AssimpModelLoader;
import org.helioviewer.jhv.opengl.model.ModelMaterial;
import org.helioviewer.jhv.opengl.model.ModelMesh;
import org.helioviewer.jhv.opengl.model.ModelNode;
import org.helioviewer.jhv.opengl.model.ModelSampler;
import org.helioviewer.jhv.opengl.model.ModelScene;
import org.helioviewer.jhv.opengl.model.ModelTexture;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.json.JSONObject;

public final class AssimpModelLoaderTest {

    private static final int RENDER_SIZE = 512;

    public static void main(String[] args) throws Exception {
        Path path = Path.of("extra/test/data/model-loader.gltf");
        Path renderOutput = null;
        boolean defaultFixture = true;
        if (args.length == 1) {
            path = Path.of(args[0]);
            defaultFixture = false;
        } else if (args.length == 2 && args[0].equals("--render"))
            renderOutput = Path.of(args[1]);
        else if (args.length == 3 && args[1].equals("--render")) {
            path = Path.of(args[0]);
            renderOutput = Path.of(args[2]);
            defaultFixture = false;
        } else if (args.length != 0)
            throw new IllegalArgumentException("usage: AssimpModelLoaderTest [model] [--render output.png]");

        ModelScene scene = AssimpModelLoader.load(path);
        checkScene(scene);
        ModelLayer layer = new ModelLayer(path);
        checkLayer(layer, path);

        if (defaultFixture) {
            checkUntexturedUVsIgnored(path);
            checkRedundantMaterialsRemoved(path);
            checkPositionMetadata(path);
            Path glb = createGlb(path);
            try {
                checkScene(AssimpModelLoader.load(glb));
            } finally {
                Files.deleteIfExists(glb);
            }
        }

        if (renderOutput != null)
            render(layer, scene, renderOutput);
        System.out.println("AssimpModelLoaderTest passed");
    }

    private static void checkUntexturedUVsIgnored(Path gltf) throws IOException {
        String texture = ", \"baseColorTexture\": {\"index\": 1}";
        String document = Files.readString(gltf);
        check(document.contains(texture), "overlay texture reference");
        Path untextured = Files.createTempFile("model-loader-untextured-", ".gltf");
        try {
            Files.writeString(untextured, document.replace(texture, ""));
            check(AssimpModelLoader.load(untextured).meshes().get(1).texCoords() == null, "unused texture coordinates");
        } finally {
            Files.deleteIfExists(untextured);
        }
    }

    private static void checkRedundantMaterialsRemoved(Path gltf) throws IOException {
        String markerMaterial = "{\"name\": \"markers\", \"pbrMetallicRoughness\": {\"baseColorFactor\": [1, 0.3, 0.9, 1]}}";
        String duplicateMarkerMaterial = "{\"name\": \"duplicate-markers\", \"pbrMetallicRoughness\": {\"baseColorFactor\": [1, 0.3, 0.9, 1]}}";
        String unusedMaterial = "{\"name\": \"unused\", \"pbrMetallicRoughness\": {\"baseColorFactor\": [0.2, 0.4, 0.6, 1]}}";
        String markerPrimitive = "{\"attributes\": {\"POSITION\": 15, \"COLOR_0\": 16}, \"indices\": 17, \"material\": 4, \"mode\": 0}";
        String duplicateMarkerPrimitive = "{\"attributes\": {\"POSITION\": 15, \"COLOR_0\": 16}, \"indices\": 17, \"material\": 5, \"mode\": 0}";
        String document = Files.readString(gltf);
        check(document.contains(markerMaterial), "marker material");
        check(document.contains(markerPrimitive), "marker primitive");
        document = document.replace(markerMaterial, markerMaterial + ",\n    " + duplicateMarkerMaterial + ",\n    " + unusedMaterial);
        document = document.replace(markerPrimitive, markerPrimitive + ",\n      " + duplicateMarkerPrimitive);

        Path redundant = Files.createTempFile("model-loader-redundant-materials-", ".gltf");
        try {
            Files.writeString(redundant, document);
            ModelScene scene = AssimpModelLoader.load(redundant);
            check(scene.meshes().size() == 6, "redundant-material mesh count");
            check(scene.materials().size() == 5, "redundant materials were not removed");
            check(scene.meshes().get(4).materialIndex() == scene.meshes().get(5).materialIndex(), "duplicate material was not remapped");
        } finally {
            Files.deleteIfExists(redundant);
        }
    }

    private static void checkPositionMetadata(Path gltf) throws IOException {
        String scene = "\"scenes\": [{\"name\": \"loader-showcase\", \"nodes\": [0]}]";
        String extras = "\"scenes\": [{\"name\": \"loader-showcase\", \"nodes\": [0], \"extras\": {" +
                "\"DATE-OBS\": \"2025-10-09T18:19:52\", \"DSUN_OBS\": 150000000000, \"CRLN_OBS\": 37, \"CRLT_OBS\": -12, " +
                "\"RSUN_REF\": 695700000, \"CTYPE1\": \"SOLZ\", \"CTYPE2\": \"SOLX\", \"CTYPE3\": \"SOLY\", " +
                "\"CUNIT1\": \"m\", \"CUNIT2\": \"km\", \"CUNIT3\": \"Mm\"}}]";
        String document = Files.readString(gltf);
        check(document.contains(scene), "scene metadata insertion point");
        Path positioned = Files.createTempFile("model-loader-positioned-", ".gltf");
        Path glb = null;
        try {
            Files.writeString(positioned, document.replace(scene, extras));
            checkPositionTransform(AssimpModelLoader.load(positioned));
            ModelLayer layer = new ModelLayer(positioned);
            check(layer.getTimeString().equals("2025-10-09T18:19:52.000"), "model layer observation time");
            glb = createGlb(positioned);
            checkPositionTransform(AssimpModelLoader.load(glb));

            String bothCoordinatePairs = extras.replace("\"RSUN_REF\": 695700000, ",
                    "\"HGLN_OBS\": -80, \"HGLT_OBS\": 45, \"RSUN_REF\": 695700000, ");
            Files.writeString(positioned, document.replace(scene, bothCoordinatePairs));
            checkPositionTransform(AssimpModelLoader.load(positioned));

            Files.writeString(positioned, document.replace(scene, extras.replace("\"CRLT_OBS\": -12, ", "")));
            try {
                AssimpModelLoader.load(positioned);
                throw new AssertionError("incomplete positional metadata was accepted");
            } catch (IOException e) {
                check(e.getMessage().contains("CRLT_OBS"), "incomplete positional metadata error");
            }
        } finally {
            if (glb != null)
                Files.deleteIfExists(glb);
            Files.deleteIfExists(positioned);
        }
    }

    private static void checkPositionTransform(ModelScene scene) {
        check(scene.time().toString().equals("2025-10-09T18:19:52.000"), "scene observation time");
        Quat worldToObserver = Quat.createXY(Math.toRadians(-12), Math.toRadians(-37));
        Matrix4fc transform = scene.root().transform();
        checkColumn(transform, 0, worldToObserver.rotateInverseVector(new Vec3(0, 0, 1 / Sun.RadiusMeter)), "metre SOLZ axis");
        checkColumn(transform, 1, worldToObserver.rotateInverseVector(new Vec3(1_000 / Sun.RadiusMeter, 0, 0)), "kilometre SOLX axis");
        checkColumn(transform, 2, worldToObserver.rotateInverseVector(new Vec3(0, 1_000_000 / Sun.RadiusMeter, 0)), "megametre SOLY axis");
    }

    private static void checkColumn(Matrix4fc matrix, int column, Vec3 expected, String label) {
        checkClose(matrix.get(column, 0), (float) expected.x, label + " X");
        checkClose(matrix.get(column, 1), (float) expected.y, label + " Y");
        checkClose(matrix.get(column, 2), (float) expected.z, label + " Z");
    }

    private static void checkLayer(ModelLayer layer, Path path) throws IOException {
        check(layer.getName().equals("loader-showcase"), "layer name");
        check(layer.isEnabled(), "layer enabled");
        check(layer.isDeletable(), "layer deletable");
        check(layer.isLocal(), "layer local");
        JSONObject state = new JSONObject();
        layer.serialize(state);
        check(Path.of(state.getString("path")).equals(path.toAbsolutePath().normalize()), "layer path");
        check(new ModelLayer(state).getName().equals(layer.getName()), "restored layer name");
    }

    private static void checkScene(ModelScene scene) {
        check(scene.name().equals("loader-showcase"), "scene name");
        check(scene.meshes().size() == 5, "mesh count");
        check(scene.materials().size() == 5, "material count");
        check(scene.textures().size() == 3, "texture count");

        ModelMesh surface = scene.meshes().get(0);
        check(surface.primitive() == ModelMesh.Primitive.TRIANGLES, "surface primitive");
        check(surface.vertexCount() == 34, "surface vertex count");
        checkIndices(surface.indices(), 96, surface.vertexCount(), "surface indices");
        check(surface.lineOffsets().remaining() == 0, "surface line offsets");
        checkWave(surface, 0);
        // Material 0 selects TEXCOORD_1, and Assimp normalizes glTF V coordinates to its lower-left convention.
        checkSurfaceTexCoords(surface);
        checkColor(surface, -1, -1, 255, 255, 255, 255);
        checkColor(surface, 1, -1, 255, 210, 170, 255);
        checkColor(surface, -1, 1, 255, 180, 255, 210);
        checkColor(surface, 1, 1, 170, 255, 255, 230);

        ModelMesh overlay = scene.meshes().get(1);
        check(overlay.primitive() == ModelMesh.Primitive.TRIANGLES, "overlay primitive");
        check(overlay.vertexCount() == 28, "overlay vertex count");
        checkIndices(overlay.indices(), 78, overlay.vertexCount(), "overlay indices");
        checkWave(overlay, 0.1f);
        check(overlay.texCoords().remaining() == 2 * overlay.vertexCount(), "overlay texture coordinates");
        check(overlay.colors().remaining() == 4 * overlay.vertexCount(), "overlay colors");

        ModelMesh mask = scene.meshes().get(2);
        check(mask.primitive() == ModelMesh.Primitive.TRIANGLES, "mask primitive");
        check(mask.vertexCount() == 34, "mask vertex count");
        checkIndices(mask.indices(), 96, mask.vertexCount(), "mask indices");
        checkWave(mask, 0.2f);
        check(mask.texCoords().remaining() == 2 * mask.vertexCount(), "mask texture coordinates");
        checkWhite(mask.colors(), 4 * mask.vertexCount(), "mask colors");

        ModelMesh ring = scene.meshes().get(3);
        check(ring.primitive() == ModelMesh.Primitive.LINES, "ring primitive");
        check(ring.vertexCount() == 32, "ring vertex count");
        checkIndices(ring.indices(), 33, ring.vertexCount(), "ring indices");
        checkBuffer(ring.lineOffsets(), 0, 33);
        checkWave(ring, 0.3f);

        ModelMesh markers = scene.meshes().get(4);
        check(markers.primitive() == ModelMesh.Primitive.POINTS, "marker primitive");
        check(markers.vertexCount() == 5, "marker vertex count");
        checkBuffer(markers.indices(), 0, 1, 2, 3, 4);
        checkWave(markers, 0.4f);

        ModelMaterial surfaceMaterial = scene.materials().get(surface.materialIndex());
        check(surfaceMaterial.alphaMode() == ModelMaterial.AlphaMode.OPAQUE, "surface alpha mode");
        check(surfaceMaterial.doubleSided(), "surface double-sided mode");
        check(surfaceMaterial.baseColorTexture() == 0, "surface texture index");

        ModelMaterial overlayMaterial = scene.materials().get(overlay.materialIndex());
        checkClose(overlayMaterial.alpha(), 0.6f, "overlay alpha");
        check(overlayMaterial.alphaMode() == ModelMaterial.AlphaMode.BLEND, "overlay alpha mode");
        check(overlayMaterial.doubleSided(), "overlay double-sided mode");
        check(overlayMaterial.baseColorTexture() == 1, "overlay texture index");

        ModelMaterial maskMaterial = scene.materials().get(mask.materialIndex());
        check(maskMaterial.alphaMode() == ModelMaterial.AlphaMode.MASK, "mask alpha mode");
        checkClose(maskMaterial.alphaCutoff(), 0.5f, "mask alpha cutoff");
        check(maskMaterial.doubleSided(), "mask double-sided mode");
        check(maskMaterial.baseColorTexture() == 2, "mask texture index");

        ModelMaterial ringMaterial = scene.materials().get(ring.materialIndex());
        checkClose(ringMaterial.red(), 0.1f, "ring red");
        checkClose(ringMaterial.green(), 0.85f, "ring green");
        checkClose(ringMaterial.blue(), 1, "ring blue");
        check(ringMaterial.baseColorTexture() == ModelMaterial.NO_TEXTURE, "ring texture");

        ModelMaterial markerMaterial = scene.materials().get(markers.materialIndex());
        checkClose(markerMaterial.alpha(), 1, "marker alpha");
        check(markerMaterial.alphaMode() == ModelMaterial.AlphaMode.OPAQUE, "marker alpha mode");

        ModelTexture smoothColor = scene.textures().get(0);
        check(smoothColor.name().equals("color-grid"), "smooth texture name");
        check(smoothColor.width() == 4 && smoothColor.height() == 4, "smooth texture dimensions");
        check(smoothColor.sampler().equals(ModelSampler.DEFAULT), "smooth texture sampler");
        checkPixel(smoothColor.rgba(), 0, 50, 20, 100, 255);
        checkPixel(smoothColor.rgba(), 15, 80, 235, 110, 255);

        ModelTexture nearestColor = scene.textures().get(1);
        check(nearestColor.name().equals("color-grid"), "nearest texture name");
        check(nearestColor.sampler().equals(new ModelSampler(ModelSampler.MinFilter.NEAREST, ModelSampler.MagFilter.NEAREST,
                ModelSampler.Wrap.CLAMP_TO_EDGE, ModelSampler.Wrap.MIRRORED_REPEAT)), "nearest texture sampler");
        checkByteBuffer(nearestColor.rgba(), smoothColor.rgba());

        ModelTexture checker = scene.textures().get(2);
        check(checker.name().equals("alpha-checker"), "checker texture name");
        check(checker.sampler().equals(new ModelSampler(ModelSampler.MinFilter.NEAREST_MIPMAP_NEAREST, ModelSampler.MagFilter.NEAREST,
                ModelSampler.Wrap.REPEAT, ModelSampler.Wrap.CLAMP_TO_EDGE)), "checker texture sampler");
        checkPixel(checker.rgba(), 0, 255, 255, 255, 0);
        checkPixel(checker.rgba(), 1, 255, 255, 255, 255);

        ModelNode root = scene.root();
        check(scene.time() == null, "unpositioned scene time");
        check(root.name().equals("root"), "root node name");
        check(root.transform().equals(new Matrix4f(), 0), "unpositioned root transform");
        check(root.children().size() == 2, "root child count");
        ModelNode primary = root.children().get(0);
        check(primary.name().equals("primary"), "primary node name");
        checkBuffer(primary.meshIndices(), 0, 1, 2, 3, 4);
        checkClose(primary.transform().m00(), 0.9f, "primary scale");
        checkClose(primary.transform().m30(), -1.35f, "primary translation");

        ModelNode secondary = root.children().get(1);
        check(secondary.name().equals("secondary-group"), "secondary node name");
        check(secondary.meshIndices().remaining() == 0, "secondary mesh indices");
        check(secondary.children().size() == 1, "secondary child count");
        checkClose(secondary.transform().m00(), 0.9659258f, "secondary cosine");
        checkClose(secondary.transform().m01(), 0.25881904f, "secondary sine");
        checkClose(secondary.transform().m30(), 1.35f, "secondary translation");

        ModelNode mirrored = secondary.children().getFirst();
        check(mirrored.name().equals("mirrored"), "mirrored node name");
        checkBuffer(mirrored.meshIndices(), 0, 1, 2, 3, 4);
        checkClose(mirrored.transform().m00(), -0.75f, "mirrored x scale");
        checkClose(mirrored.transform().m11(), 0.75f, "mirrored y scale");
        check(mirrored.transform().determinant3x3() < 0, "mirrored winding");
    }

    private static Path createGlb(Path gltf) throws IOException {
        String jsonText = Files.readString(gltf);
        JSONObject document = new JSONObject(jsonText);
        JSONObject buffer = document.getJSONArray("buffers").getJSONObject(0);
        String uri = buffer.getString("uri");
        byte[] binary = decodeDataUri(uri);
        // Preserve the source document and replace only the buffer URI when packaging the same fixture as GLB.
        String uriProperty = "\"uri\": " + JSONObject.quote(uri);
        int uriStart = jsonText.indexOf(uriProperty);
        check(uriStart >= 0, "buffer URI not found in source JSON");
        int precedingComma = jsonText.lastIndexOf(',', uriStart);
        check(precedingComma >= 0, "buffer URI has no preceding comma");
        jsonText = jsonText.substring(0, precedingComma) + jsonText.substring(uriStart + uriProperty.length());

        byte[] json = jsonText.getBytes(StandardCharsets.UTF_8);
        int jsonLength = Math.addExact(json.length, 3) & ~3;
        int binaryLength = Math.addExact(binary.length, 3) & ~3;
        int totalLength = Math.addExact(28, Math.addExact(jsonLength, binaryLength));
        ByteBuffer glb = ByteBuffer.allocate(totalLength).order(ByteOrder.LITTLE_ENDIAN);
        glb.putInt(0x46546c67).putInt(2).putInt(totalLength);
        glb.putInt(jsonLength).putInt(0x4e4f534a).put(json);
        while ((glb.position() & 3) != 0)
            glb.put((byte) ' ');
        glb.putInt(binaryLength).putInt(0x004e4942).put(binary);

        Path output = Files.createTempFile("model-loader-", ".glb");
        Files.write(output, glb.array());
        return output;
    }

    private static byte[] decodeDataUri(String uri) {
        int separator = uri.indexOf(',');
        check(separator >= 5 && uri.startsWith("data:") && uri.substring(0, separator).endsWith(";base64"), "unsupported data URI");
        return Base64.getDecoder().decode(uri.substring(separator + 1));
    }

    private static void render(ModelLayer layer, ModelScene scene, Path output) throws Exception {
        System.setProperty("user.timezone", TimeZone.getDefault().getID());
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Locale.setDefault(Locale.US);
        Platform.init();
        Directories.createPersistentDirs();
        Log.init();
        Directories.createCacheDirs();
        AppInit.loadSpice();

        AngleRenderer renderer = AngleRenderer.pbuffer(RENDER_SIZE, RENDER_SIZE);
        try {
            GLRenderer.reshape(RENDER_SIZE, RENDER_SIZE);
            layer.init();

            Viewport vp = Display.getViewport(0);
            MapView mv = GLRenderer.getMapView();
            GL.glViewport(vp.x, vp.yGL, vp.width, vp.height);
            GL.glClearColor(0, 0, 0, 1);
            GL.glClear(GL.COLOR_BUFFER_BIT | GL.DEPTH_BUFFER_BIT);
            Transform.ortho(vp.aspect, 5, 0, 0, Quat.createXY(Math.toRadians(18), Math.toRadians(-28)));
            layer.render(mv, vp);
            GLException.checkErrors("AssimpModelLoaderTest.render");

            ByteBuffer pixels = BufferUtils.newByteBuffer(RENDER_SIZE * RENDER_SIZE * 4);
            GL.glReadPixels(0, 0, RENDER_SIZE, RENDER_SIZE, GL.RGBA, GL.UNSIGNED_BYTE, pixels);
            BufferedImage image = new BufferedImage(RENDER_SIZE, RENDER_SIZE, BufferedImage.TYPE_INT_RGB);
            int visiblePixels = 0;
            int leftPixels = 0;
            int rightPixels = 0;
            int centerPixels = 0;
            for (int y = 0; y < RENDER_SIZE; y++) {
                for (int x = 0; x < RENDER_SIZE; x++) {
                    int offset = 4 * (y * RENDER_SIZE + x);
                    int red = pixels.get(offset) & 0xff;
                    int green = pixels.get(offset + 1) & 0xff;
                    int blue = pixels.get(offset + 2) & 0xff;
                    if ((red | green | blue) != 0) {
                        visiblePixels++;
                        if (x < RENDER_SIZE / 2)
                            leftPixels++;
                        else
                            rightPixels++;
                        if (x >= 235 && x <= 276)
                            centerPixels++;
                    }
                    image.setRGB(x, RENDER_SIZE - 1 - y, red << 16 | green << 8 | blue);
                }
            }
            check(visiblePixels > 40_000, "too little model geometry was rendered");
            check(leftPixels > 20_000, "primary model instance is missing or misplaced");
            check(rightPixels > 14_000, "mirrored model instance is missing or misplaced");
            check(centerPixels == 0, "model instances overlap the expected center gap");

            layer.dispose();
            layer.init();
            GL.glClear(GL.COLOR_BUFFER_BIT | GL.DEPTH_BUFFER_BIT);
            layer.render(mv, vp);
            GLException.checkErrors("AssimpModelLoaderTest.reinitialized");
            ByteBuffer reinitializedPixels = BufferUtils.newByteBuffer(RENDER_SIZE * RENDER_SIZE * 4);
            GL.glReadPixels(0, 0, RENDER_SIZE, RENDER_SIZE, GL.RGBA, GL.UNSIGNED_BYTE, reinitializedPixels);
            checkByteBuffer(reinitializedPixels, pixels);

            checkMeshRendering(scene, mv, vp, 0, 20_000, "single-sided surface");
            checkMeshRendering(scene, mv, vp, 1, 10_000, "blended overlay");
            checkMeshRendering(scene, mv, vp, 2, 2_000, "masked surface");
            checkMeshRendering(scene, mv, vp, 3, 500, "line mesh");
            checkMeshRendering(scene, mv, vp, 4, 20, "point mesh");
            checkMeshRendering(withoutBaseColorTexture(scene, 0), mv, vp, 0, 20_000, "untextured surface");
            checkMeshRendering(singleSidedSurface(scene, 0), mv, vp, 0, 20_000, "single-sided surface");

            Path absoluteOutput = output.toAbsolutePath().normalize();
            Path parent = absoluteOutput.getParent();
            if (parent != null)
                Files.createDirectories(parent);
            check(ImageIO.write(image, "png", absoluteOutput.toFile()), "PNG writer unavailable");
            System.out.println("Rendered image: " + absoluteOutput);
        } finally {
            layer.dispose();
            renderer.destroy();
        }
    }

    private static ModelScene withoutBaseColorTexture(ModelScene scene, int meshIndex) {
        int materialIndex = scene.meshes().get(meshIndex).materialIndex();
        ModelMaterial material = scene.materials().get(materialIndex);
        return withMaterial(scene, materialIndex, new ModelMaterial(material.red(), material.green(), material.blue(), material.alpha(),
                ModelMaterial.NO_TEXTURE, material.alphaMode(), material.alphaCutoff(), material.doubleSided()));
    }

    private static ModelScene singleSidedSurface(ModelScene scene, int meshIndex) {
        int materialIndex = scene.meshes().get(meshIndex).materialIndex();
        ModelMaterial material = scene.materials().get(materialIndex);
        return withMaterial(scene, materialIndex, new ModelMaterial(material.red(), material.green(), material.blue(), material.alpha(),
                material.baseColorTexture(), material.alphaMode(), material.alphaCutoff(), false));
    }

    private static ModelScene withMaterial(ModelScene scene, int materialIndex, ModelMaterial material) {
        ArrayList<ModelMaterial> materials = new ArrayList<>(scene.materials());
        materials.set(materialIndex, material);
        return new ModelScene(scene.name(), scene.time(), scene.root(), scene.meshes(), materials, scene.textures());
    }

    private static void checkMeshRendering(ModelScene scene, MapView mv, Viewport vp, int meshIndex, int minimumPixels, String label) {
        GLSLModel model = new GLSLModel(new ModelScene(scene.name(), scene.time(), selectMesh(scene.root(), meshIndex), scene.meshes(), scene.materials(),
                scene.textures()));
        try {
            model.init();
            GL.glClear(GL.COLOR_BUFFER_BIT | GL.DEPTH_BUFFER_BIT);
            model.render(mv, vp);
            GLException.checkErrors("AssimpModelLoaderTest." + label);

            ByteBuffer pixels = BufferUtils.newByteBuffer(RENDER_SIZE * RENDER_SIZE * 4);
            GL.glReadPixels(0, 0, RENDER_SIZE, RENDER_SIZE, GL.RGBA, GL.UNSIGNED_BYTE, pixels);
            int visiblePixels = 0;
            int leftPixels = 0;
            int rightPixels = 0;
            for (int i = 0; i < RENDER_SIZE * RENDER_SIZE; i++) {
                int offset = 4 * i;
                if ((pixels.get(offset) | pixels.get(offset + 1) | pixels.get(offset + 2)) != 0) {
                    visiblePixels++;
                    if (i % RENDER_SIZE < RENDER_SIZE / 2)
                        leftPixels++;
                    else
                        rightPixels++;
                }
            }
            check(visiblePixels > minimumPixels, label + " rendered only " + visiblePixels + " pixels");
            if (meshIndex == 0) {
                check(leftPixels > 10_000, "single-sided primary surface is missing");
                check(rightPixels > 7_000, "single-sided mirrored surface is missing");
            }
        } finally {
            model.dispose();
        }
    }

    private static ModelNode selectMesh(ModelNode node, int meshIndex) {
        IntBuffer sourceIndices = node.meshIndices();
        IntBuffer selectedIndices = BufferUtils.newIntBuffer(sourceIndices.remaining());
        while (sourceIndices.hasRemaining()) {
            int index = sourceIndices.get();
            if (index == meshIndex)
                selectedIndices.put(index);
        }
        return new ModelNode(node.name(), node.transform(), selectedIndices.flip(), node.children().stream()
                .map(child -> selectMesh(child, meshIndex)).toList());
    }

    private static void checkBuffer(IntBuffer actual, int... expected) {
        check(actual.remaining() == expected.length, "integer buffer length");
        for (int i = 0; i < expected.length; i++)
            check(actual.get(i) == expected[i], "integer buffer element " + i);
    }

    private static void checkIndices(IntBuffer indices, int count, int vertexCount, String label) {
        check(indices.remaining() == count, label + " length");
        for (int i = 0; i < count; i++)
            check(indices.get(i) >= 0 && indices.get(i) < vertexCount, label + " element " + i);
    }

    private static void checkWave(ModelMesh mesh, float offset) {
        FloatBuffer positions = mesh.positions();
        check(positions.remaining() == 3 * mesh.vertexCount(), "position count");
        for (int i = 0; i < mesh.vertexCount(); i++) {
            float x = positions.get(3 * i);
            float expectedZ = (float) (0.25 * Math.sin(Math.PI * (x + 1))) + offset;
            checkClose(positions.get(3 * i + 2), expectedZ, "wave Z at vertex " + i);
        }
    }

    private static void checkSurfaceTexCoords(ModelMesh surface) {
        FloatBuffer positions = surface.positions();
        FloatBuffer texCoords = surface.texCoords();
        check(texCoords.remaining() == 2 * surface.vertexCount(), "surface texture coordinates");
        for (int i = 0; i < surface.vertexCount(); i++) {
            float x = positions.get(3 * i);
            float y = positions.get(3 * i + 1);
            checkClose(texCoords.get(2 * i), (y + 1) / 2, "surface texture U at vertex " + i);
            checkClose(texCoords.get(2 * i + 1), (x + 1) / 2, "surface texture V at vertex " + i);
        }
    }

    private static void checkColor(ModelMesh mesh, float x, float y, int red, int green, int blue, int alpha) {
        FloatBuffer positions = mesh.positions();
        ByteBuffer colors = mesh.colors();
        for (int i = 0; i < mesh.vertexCount(); i++) {
            if (positions.get(3 * i) != x || positions.get(3 * i + 1) != y)
                continue;
            int offset = 4 * i;
            check((colors.get(offset) & 0xff) == red, "vertex color red");
            check((colors.get(offset + 1) & 0xff) == green, "vertex color green");
            check((colors.get(offset + 2) & 0xff) == blue, "vertex color blue");
            check((colors.get(offset + 3) & 0xff) == alpha, "vertex color alpha");
            return;
        }
        throw new AssertionError("vertex not found at " + x + ", " + y);
    }

    private static void checkWhite(ByteBuffer colors, int count, String label) {
        check(colors.remaining() == count, label + " length");
        for (int i = 0; i < count; i++)
            check((colors.get(i) & 0xff) == 255, label + " element " + i);
    }

    private static void checkByteBuffer(ByteBuffer actual, ByteBuffer expected) {
        check(actual.equals(expected), "byte buffers differ");
    }

    private static void checkPixel(ByteBuffer rgba, int pixel, int red, int green, int blue, int alpha) {
        int offset = 4 * pixel;
        check((rgba.get(offset) & 0xff) == red, "pixel " + pixel + " red");
        check((rgba.get(offset + 1) & 0xff) == green, "pixel " + pixel + " green");
        check((rgba.get(offset + 2) & 0xff) == blue, "pixel " + pixel + " blue");
        check((rgba.get(offset + 3) & 0xff) == alpha, "pixel " + pixel + " alpha");
    }

    private static void checkClose(float actual, float expected, String label) {
        check(Math.abs(actual - expected) <= 1e-6f, label + ": " + actual + " != " + expected);
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }

}
