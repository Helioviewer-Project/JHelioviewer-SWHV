package org.helioviewer.jhv.io;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.connect.LoadSunJSON;

public interface Load {

    void get(@Nonnull URI uri);

    interface LoadString extends Load {
        void get(@Nonnull String string);
    }

    Load cdf = new CDF();
    Load fits = new FITS();
    Load image = new Image();
    LoadString request = new Request();
    LoadString state = new State();
    LoadString sunJSON = new SunJSON();

    class Image implements Load {
        @Override
        public void get(@Nonnull URI uri) {
            try {
                getAll(FileUtils.listDir(Path.of(uri)));
            } catch (Exception e) { // remote
                getAll(List.of(uri));
            }
        }

        public static void getAll(@Nonnull List<URI> uris) {
            if (!uris.isEmpty()) {
                LoadLayer.submit(ImageLayer.create(null), uris, false);
            }
        }
    }

    class FITS implements Load {
        @Override
        public void get(@Nonnull URI uri) {
            getAll(List.of(uri));
        }

        public static void getAll(@Nonnull List<URI> uris) {
            if (!uris.isEmpty()) {
                LoadLayer.submit(ImageLayer.create(null), uris, true);
            }
        }
    }

    class CDF implements Load {
        @Override
        public void get(@Nonnull URI uri) {
            getAll(List.of(uri));
        }

        public static void getAll(@Nonnull List<URI> uris) {
            if (!uris.isEmpty()) {
                LoadRequest.submitCDF(uris);
            }
        }
    }

    class Request implements LoadString {
        @Override
        public void get(@Nonnull URI uri) {
            LoadRequest.submit(uri);
        }

        @Override
        public void get(@Nonnull String json) {
            LoadRequest.submit(json);
        }
    }

    class State implements LoadString {
        @Override
        public void get(@Nonnull URI uri) {
            LoadState.submit(uri);
        }

        @Override
        public void get(@Nonnull String json) {
            LoadState.submit(json);
        }
    }

    class SunJSON implements LoadString {
        @Override
        public void get(@Nonnull URI uri) {
            getAll(List.of(uri));
        }

        public static void getAll(@Nonnull List<URI> uris) {
            if (!uris.isEmpty()) {
                LoadSunJSON.submit(uris);
            }
        }

        @Override
        public void get(@Nonnull String json) {
            LoadSunJSON.submit(json);
        }
    }

}
