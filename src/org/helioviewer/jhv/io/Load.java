package org.helioviewer.jhv.io;

import java.net.URI;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.connect.LoadSunJSON;
import org.helioviewer.jhv.timelines.band.BandReaderHapi;

public final class Load {

    private Load() {}

    public static void cdf(@Nonnull List<URI> uris) {
        if (!uris.isEmpty())
            LoadRequest.submitCDF(uris);
    }

    public static void cdf(@Nonnull URI uri) {
        cdf(List.of(uri));
    }

    public static void cdf(@Nullable Object input) {
        dispatchURIOrURIList("cdf", input, Load::cdf, Load::cdf);
    }

    public static void image(@Nonnull List<URI> uris) {
        if (!uris.isEmpty()) {
            FileUtils.resolveURIListOffEDT(uris, "JHV-LoadDirectory", resolved -> {
                if (!resolved.isEmpty()) {
                    ImageLayer layer = ImageLayer.create(null);
                    layer.load(resolved);
                }
            });
        }
    }

    public static void image(@Nonnull URI uri) {
        image(List.of(uri));
    }

    public static void image(@Nullable Object input) {
        dispatchURIOrURIList("image", input, Load::image, Load::image);
    }

    public static void sunJSON(@Nonnull List<URI> uris) {
        if (!uris.isEmpty())
            LoadSunJSON.submit(uris);
    }

    public static void sunJSON(@Nonnull URI uri) {
        sunJSON(List.of(uri));
    }

    public static void sunJSON(@Nonnull String json) {
        LoadSunJSON.submit(json);
    }

    public static void sunJSON(@Nullable Object input) {
        switch (input) {
            case null -> {}
            case URI uri -> sunJSON(uri);
            case String json -> sunJSON(json);
            default -> sunJSON(requireURIList("sunJSON", input));
        }
    }

    public static void request(@Nonnull URI uri) {
        LoadRequest.submit(uri);
    }

    public static void request(@Nonnull String json) {
        LoadRequest.submit(json);
    }

    public static void request(@Nullable Object input) {
        dispatchURIOrString("request", input, Load::request, Load::request);
    }

    public static void state(@Nonnull URI uri) {
        state(null, uri);
    }

    public static void state(@Nullable Commands.OperationContext context, @Nonnull URI uri) {
        LoadState.submit(context, uri);
    }

    public static void state(@Nonnull String json) {
        state(null, json);
    }

    public static void state(@Nullable Commands.OperationContext context, @Nonnull String json) {
        LoadState.submit(context, json);
    }

    public static void state(@Nullable Object input) {
        dispatchURIOrString("state", input, Load::state, Load::state);
    }

    public static void hapi(@Nonnull URI uri) {
        BandReaderHapi.loadUri(uri);
    }

    public static void hapi(@Nonnull List<URI> uris) {
        if (!uris.isEmpty())
            for (URI uri : uris)
                hapi(uri);
    }

    public static void hapi(@Nullable Object input) {
        dispatchURIOrURIList("hapi", input, Load::hapi, Load::hapi);
    }

    public static void votable(@Nonnull URI uri) {
        SoarClient.submitTable(uri);
    }

    @SuppressWarnings("unchecked")
    private static List<URI> requireURIList(String operation, Object input) {
        if (!(input instanceof List<?> uris))
            throw new IllegalArgumentException(operation + " accepts URI or List<URI>");
        if (uris.isEmpty())
            throw new IllegalArgumentException(operation + " accepts non-empty List<URI>");
        for (Object uri : uris) {
            if (!(uri instanceof URI))
                throw new IllegalArgumentException(operation + " accepts URI or List<URI>");
        }
        return (List<URI>) uris;
    }

    private static void dispatchURIOrURIList(
            String operation,
            @Nullable Object input,
            Consumer<URI> uriLoader,
            Consumer<List<URI>> listLoader) {
        switch (input) {
            case null -> {}
            case URI uri -> uriLoader.accept(uri);
            default -> listLoader.accept(requireURIList(operation, input));
        }
    }

    private static void dispatchURIOrString(
            String operation,
            @Nullable Object input,
            Consumer<URI> uriLoader,
            Consumer<String> jsonLoader) {
        switch (input) {
            case null -> {}
            case URI uri -> uriLoader.accept(uri);
            case String json -> jsonLoader.accept(json);
            default -> throw new IllegalArgumentException(operation + " accepts URI or String");
        }
    }
}
