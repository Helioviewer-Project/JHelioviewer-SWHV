package org.helioviewer.jhv.io;

import java.net.URI;
import java.util.List;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.connect.LoadSunJSON;

public interface Load {

    static void getAllCDF(@Nonnull List<URI> uris) {
        if (!uris.isEmpty()) {
            LoadRequest.submitCDF(uris);
        }
    }

    static void getCDF(@Nonnull URI uri) {
        getAllCDF(List.of(uri));
    }

    static void getAllImage(@Nonnull List<URI> uris) {
        if (!uris.isEmpty()) {
            LoadLayer.submit(ImageLayer.create(null), uris);
        }
    }

    static void getImage(@Nonnull URI uri) {
        getAllImage(List.of(uri));
    }

    static void getAllSunJSON(@Nonnull List<URI> uris) {
        if (!uris.isEmpty()) {
            LoadSunJSON.submit(uris);
        }
    }

    void get(@Nonnull URI uri);

    void get(@Nonnull String string);

    Load request = new Request();
    Load state = new State();
    Load sunJSON = new SunJSON();

    class Request implements Load {
        @Override
        public void get(@Nonnull URI uri) {
            LoadRequest.submit(uri);
        }

        @Override
        public void get(@Nonnull String json) {
            LoadRequest.submit(json);
        }
    }

    class State implements Load {
        @Override
        public void get(@Nonnull URI uri) {
            LoadState.submit(uri);
        }

        @Override
        public void get(@Nonnull String json) {
            LoadState.submit(json);
        }
    }

    class SunJSON implements Load {
        @Override
        public void get(@Nonnull URI uri) {
            getAllSunJSON(List.of(uri));
        }


        @Override
        public void get(@Nonnull String json) {
            LoadSunJSON.submit(json);
        }
    }

}
