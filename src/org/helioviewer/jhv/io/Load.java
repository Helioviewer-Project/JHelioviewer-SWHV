package org.helioviewer.jhv.io;

import java.net.URI;
import java.util.List;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.connect.LoadSunJSON;

public final class Load {

    private Load() {
    }

    public static void getAllCDF(@Nonnull List<URI> uris) {
        if (!uris.isEmpty()) {
            LoadRequest.submitCDF(uris);
        }
    }

    public static void getAllImage(@Nonnull List<URI> uris) {
        if (!uris.isEmpty()) {
            LoadLayer.submit(ImageLayer.create(null), uris);
        }
    }

    public static void getAllSunJSON(@Nonnull List<URI> uris) {
        if (!uris.isEmpty()) {
            LoadSunJSON.submit(uris);
        }
    }

    public static void request(@Nonnull URI uri) {
        LoadRequest.submit(uri);
    }

    public static void request(@Nonnull String json) {
        LoadRequest.submit(json);
    }

    public static void state(@Nonnull URI uri) {
        LoadState.submit(uri);
    }

    public static void state(@Nonnull String json) {
        LoadState.submit(json);
    }

    public static void sunJSON(@Nonnull String json) {
        LoadSunJSON.submit(json);
    }
}
