package org.helioviewer.jhv.io;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.ModelLayer;
import org.helioviewer.jhv.layers.connect.LoadSunJSON;
import org.helioviewer.jhv.thread.Task;
import org.helioviewer.jhv.timelines.band.BandImporter;

import org.json.JSONObject;

// All data load entry points.
public final class Load {

    public static CompletableFuture<ImageLayer> image(@Nonnull URI uri) {
        return image(List.of(uri));
    }

    public static CompletableFuture<ImageLayer> image(@Nonnull List<URI> uris) {
        return image(uris, null);
    }

    public static CompletableFuture<ImageLayer> image(@Nonnull List<URI> uris, @Nullable JSONObject imageParams) {
        CompletableFuture<ImageLayer> future = new CompletableFuture<>();
        if (uris.isEmpty()) {
            future.complete(null);
            return future;
        }

        Task.submitBackground(() -> FileUtils.resolveURIList(uris), resolved -> {
            if (resolved.isEmpty()) {
                future.complete(null);
                return;
            }

            try {
                ImageLayer layer = ImageLayer.create(null);
                layer.applyImageParams(imageParams);
                layer.load(resolved);
                future.complete(layer);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }, future::completeExceptionally);
        return future;
    }

    public static void state(@Nonnull URI uri) {
        LoadState.submit(null, uri);
    }

    public static void state(@Nullable Commands.OperationContext context, @Nonnull URI uri) {
        LoadState.submit(context, uri);
    }

    public static void state(@Nullable Commands.OperationContext context, @Nonnull String json) {
        LoadState.submit(context, json);
    }

    public static void request(@Nonnull URI uri) {
        LoadRequest.submit(uri);
    }

    public static void request(@Nonnull String json) {
        LoadRequest.submit(json);
    }

    public static void cdf(@Nonnull URI uri) {
        LoadRequest.submitCDF(List.of(uri));
    }

    public static void cdf(@Nonnull List<URI> uris) {
        LoadRequest.submitCDF(uris);
    }

    public static void sunJSON(@Nonnull URI uri) {
        LoadSunJSON.submit(List.of(uri));
    }

    public static void sunJSON(@Nonnull List<URI> uris) {
        LoadSunJSON.submit(uris);
    }

    public static void sunJSON(@Nonnull String json) {
        LoadSunJSON.submit(json);
    }

    public static void hapi(@Nonnull URI uri) {
        BandImporter.loadHapi(uri);
    }

    public static void hapi(@Nonnull List<URI> uris) {
        uris.forEach(BandImporter::loadHapi);
    }

    public static void votable(@Nonnull URI uri) {
        SoarClient.submitTable(uri);
    }

    public static void model(@Nonnull URI uri) {
        Task.submitBackground(uri.toString(), () -> new ModelLayer(uri), Layers::add, "Error loading model");
    }

    private Load() {}
}
