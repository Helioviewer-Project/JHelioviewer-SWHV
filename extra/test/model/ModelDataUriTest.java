package org.helioviewer.jhv.io;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ModelDataUriTest {

    public static void main(String[] args) throws Exception {
        Path local = Files.createTempFile("jhv-model-", ".GLB");
        Path cached = Files.createTempFile("jhv-model-cache-", null);
        try {
            DataUri localData = NetFileCache.get(local.toUri());
            check(localData.format() == DataUri.Format.GLTF, "local GLB format");
            check(localData.sourceUri().equals(local.toUri()), "local source URI");
            check(localData.file().toPath().equals(local), "local file path");

            checkRemoteFormat("https://example.test/model/scene.gltf?revision=1", cached, DataUri.Format.GLTF);
            checkRemoteFormat("https://example.test/model/scene.GLB#view", cached, DataUri.Format.GLTF);
            checkRemoteFormat("https://example.test/model/scene.gltf.gz", cached, DataUri.Format.GLTF);
            checkRemoteFormat("https://example.test/model/scene.GLB.GZ?revision=1", cached, DataUri.Format.GLTF);
            checkRemoteFormat("https://example.test/image/data.FITS.GZ?revision=1", cached, DataUri.Format.FITS);
        } finally {
            Files.deleteIfExists(cached);
            Files.deleteIfExists(local);
        }
    }

    private static void checkRemoteFormat(String source, Path cached, DataUri.Format expected) throws Exception {
        URI sourceUri = new URI(source);
        DataUri data = new DataUri(sourceUri, cached.toUri(), cached.toFile());
        check(data.format() == expected, "remote " + expected + " format");
        check(data.sourceUri().equals(sourceUri), "remote source URI");
        check(data.file().toPath().equals(cached), "remote cached file");
    }

    private static void check(boolean condition, String description) {
        if (!condition)
            throw new AssertionError(description);
    }

    private ModelDataUriTest() {}
}
