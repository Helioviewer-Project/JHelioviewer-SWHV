package org.helioviewer.jhv.io;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.layers.ImageLayer;

public interface Load {

    void get(@Nonnull URI uri);

    Load.Image image = new Image();
    Load fits = new FITS();
    Load request = new Request();
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

        public static void getAll(List<URI> uris) {
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
    }

    class State implements Load {
        @Override
        public void get(@Nonnull URI uri) {
            LoadState.submit(uri);
        }
    }

}
