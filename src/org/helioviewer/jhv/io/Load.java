package org.helioviewer.jhv.io;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.connect.LoadSunJSON;

public interface Load {

    void get(@Nonnull URI uri);

    Load image = new Image();
    Load fits = new FITS();
    Load.Request request = new Request();
    Load state = new State();

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
            LoadLayer.submit(ImageLayer.create(null), uris);
        }
    }

    class FITS implements Load {
        @Override
        public void get(@Nonnull URI uri) {
            LoadLayer.submitFITS(ImageLayer.create(null), uri);
        }
    }

    class Request implements Load {
        @Override
        public void get(@Nonnull URI uri) {
            LoadRequest.submit(uri);
        }

        public void get(@Nonnull String json) {
            LoadRequest.submit(json);
        }
    }

    class State implements Load {
        @Override
        public void get(@Nonnull URI uri) {
            LoadState.submit(uri);
        }
    }

    class SunJSON implements Load {
        @Override
        public void get(@Nonnull URI uri) {
            LoadSunJSON.submit(uri);
        }
    }

}
