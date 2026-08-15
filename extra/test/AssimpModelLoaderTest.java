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
import java.util.Base64;
import java.util.Locale;
import java.util.TimeZone;

import javax.imageio.ImageIO;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.opengl.angle.AngleRenderer;
import org.helioviewer.jhv.opengl.model.AssimpModelLoader;
import org.helioviewer.jhv.opengl.model.ModelMaterial;
import org.helioviewer.jhv.opengl.model.ModelMesh;
import org.helioviewer.jhv.opengl.model.ModelNode;
import org.helioviewer.jhv.opengl.model.ModelSampler;
import org.helioviewer.jhv.opengl.model.ModelScene;
import org.helioviewer.jhv.opengl.model.ModelTexture;

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

        if (defaultFixture) {
            Path glb = createGlb(path);
            try {
                checkScene(AssimpModelLoader.load(glb));
            } finally {
                Files.deleteIfExists(glb);
            }
        }

        if (renderOutput != null)
            render(scene, renderOutput);
        System.out.println("AssimpModelLoaderTest passed");
    }

    private static void checkScene(ModelScene scene) {
        check(scene.name().equals("loader-showcase"), "scene name");
        check(scene.meshes().size() == 5, "mesh count");
        check(scene.textures().size() == 3, "texture count");

        ModelMesh surface = scene.meshes().get(0);
        check(surface.primitive() == ModelMesh.Primitive.TRIANGLES, "surface primitive");
        check(surface.vertexCount() == 4, "surface vertex count");
        checkBuffer(surface.indices(), 0, 1, 2, 0, 2, 3);
        check(surface.lineOffsets().remaining() == 0, "surface line offsets");
        checkFloatBuffer(surface.positions(), -1, -1, 0, 1, -1, 0, 1, 1, 0, -1, 1, 0);
        // Material 0 selects TEXCOORD_1, and Assimp normalizes glTF V coordinates to its lower-left convention.
        checkFloatBuffer(surface.texCoords(), 0, 0, 0, 1, 1, 1, 1, 0);
        checkByteBuffer(surface.colors(), 255, 255, 255, 255, 255, 210, 170, 255, 170, 255, 255, 230, 255, 180, 255, 210);

        ModelMesh overlay = scene.meshes().get(1);
        check(overlay.primitive() == ModelMesh.Primitive.TRIANGLES, "overlay primitive");
        check(overlay.vertexCount() == 3, "overlay vertex count");
        checkBuffer(overlay.indices(), 0, 1, 2);
        checkFloatBuffer(overlay.texCoords(), 0, 1, 1, 1, 0.5f, 0);

        ModelMesh mask = scene.meshes().get(2);
        check(mask.primitive() == ModelMesh.Primitive.TRIANGLES, "mask primitive");
        check(mask.vertexCount() == 4, "mask vertex count");
        checkBuffer(mask.indices(), 0, 1, 2, 0, 2, 3);
        checkFloatBuffer(mask.texCoords(), 0, 1, 2, 1, 2, 0, 0, 0);
        checkByteBuffer(mask.colors(), 255, 255, 255, 255, 255, 255, 255, 255,
                255, 255, 255, 255, 255, 255, 255, 255);

        ModelMesh ring = scene.meshes().get(3);
        check(ring.primitive() == ModelMesh.Primitive.LINES, "ring primitive");
        check(ring.vertexCount() == 8, "ring vertex count");
        checkBuffer(ring.indices(), 0, 1, 2, 3, 4, 5, 6, 7, 0);
        checkBuffer(ring.lineOffsets(), 0, 9);

        ModelMesh markers = scene.meshes().get(4);
        check(markers.primitive() == ModelMesh.Primitive.POINTS, "marker primitive");
        check(markers.vertexCount() == 5, "marker vertex count");
        checkBuffer(markers.indices(), 0, 1, 2, 3, 4);

        ModelMaterial surfaceMaterial = scene.materials().get(surface.materialIndex());
        check(surfaceMaterial.alphaMode() == ModelMaterial.AlphaMode.OPAQUE, "surface alpha mode");
        check(!surfaceMaterial.doubleSided(), "surface culling");
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
        checkClose(markerMaterial.alpha(), 0.85f, "marker alpha");
        check(markerMaterial.alphaMode() == ModelMaterial.AlphaMode.BLEND, "marker alpha mode");

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
        check(root.name().equals("root"), "root node name");
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

    private static void render(ModelScene scene, Path output) throws Exception {
        System.setProperty("user.timezone", TimeZone.getDefault().getID());
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Locale.setDefault(Locale.US);
        Platform.init();
        Directories.createPersistentDirs();
        Log.init();
        Directories.createCacheDirs();
        AppInit.loadSpice();

        AngleRenderer renderer = AngleRenderer.pbuffer(RENDER_SIZE, RENDER_SIZE);
        GLSLModel model = null;
        try {
            GLRenderer.reshape(RENDER_SIZE, RENDER_SIZE);
            model = new GLSLModel(scene);
            model.init();

            Viewport vp = Display.getViewport(0);
            MapView mv = GLRenderer.getMapView();
            GL.glViewport(vp.x, vp.yGL, vp.width, vp.height);
            GL.glClearColor(0, 0, 0, 1);
            GL.glClear(GL.COLOR_BUFFER_BIT | GL.DEPTH_BUFFER_BIT);
            Transform.ortho(vp.aspect, 5, 0, 0, Quat.ZERO);
            model.render(mv, vp);
            GLException.checkErrors("AssimpModelLoaderTest.render");

            ByteBuffer pixels = BufferUtils.newByteBuffer(RENDER_SIZE * RENDER_SIZE * 4);
            GL.glReadPixels(0, 0, RENDER_SIZE, RENDER_SIZE, GL.RGBA, GL.UNSIGNED_BYTE, pixels);
            BufferedImage image = new BufferedImage(RENDER_SIZE, RENDER_SIZE, BufferedImage.TYPE_INT_RGB);
            int visiblePixels = 0;
            for (int y = 0; y < RENDER_SIZE; y++) {
                for (int x = 0; x < RENDER_SIZE; x++) {
                    int offset = 4 * (y * RENDER_SIZE + x);
                    int red = pixels.get(offset) & 0xff;
                    int green = pixels.get(offset + 1) & 0xff;
                    int blue = pixels.get(offset + 2) & 0xff;
                    if ((red | green | blue) != 0)
                        visiblePixels++;
                    image.setRGB(x, RENDER_SIZE - 1 - y, red << 16 | green << 8 | blue);
                }
            }
            check(visiblePixels != 0, "rendered image is empty");

            Path absoluteOutput = output.toAbsolutePath().normalize();
            Path parent = absoluteOutput.getParent();
            if (parent != null)
                Files.createDirectories(parent);
            check(ImageIO.write(image, "png", absoluteOutput.toFile()), "PNG writer unavailable");
            System.out.println("Rendered image: " + absoluteOutput);
        } finally {
            if (model != null)
                model.dispose();
            renderer.destroy();
        }
    }

    private static void checkBuffer(IntBuffer actual, int... expected) {
        check(actual.remaining() == expected.length, "integer buffer length");
        for (int i = 0; i < expected.length; i++)
            check(actual.get(i) == expected[i], "integer buffer element " + i);
    }

    private static void checkFloatBuffer(FloatBuffer actual, float... expected) {
        check(actual.remaining() == expected.length, "float buffer length");
        for (int i = 0; i < expected.length; i++)
            checkClose(actual.get(i), expected[i], "float buffer element " + i);
    }

    private static void checkByteBuffer(ByteBuffer actual, int... expected) {
        check(actual.remaining() == expected.length, "byte buffer length");
        for (int i = 0; i < expected.length; i++)
            check((actual.get(i) & 0xff) == expected[i], "byte buffer element " + i);
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
