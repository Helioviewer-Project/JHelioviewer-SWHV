package org.helioviewer.jhv.io;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.layers.ImageLayer;

public interface Load {

    void get(URI uri);

    Load.Image image = new Image();
    Load request = new Request();
    Load fits = new FITS();
    Load state = new State();

    class Image implements Load {
        @Override
        public void get(URI uri) {
            try {
                getAll(FileUtils.listDir(Path.of(uri)));
            } catch (Exception e) {
                JHVGlobals.getExecutorService().execute(new DownloadRemoteTask(ImageLayer.create(null), null, uri));
            }
        }

        public void getAll(List<URI> uris) {
            JHVGlobals.getExecutorService().execute(new LoadViewTask(ImageLayer.create(null), uris.toArray(URI[]::new)));
        }
    }

    class Request implements Load {
        @Override
        public void get(URI uri) {
            JHVGlobals.getExecutorService().execute(new LoadRequestTask(uri));
        }
    }

    class FITS implements Load {
        @Override
        public void get(URI uri) {
            JHVGlobals.getExecutorService().execute(new LoadFITSTask(ImageLayer.create(null), uri));
        }
    }

    class State implements Load {
        @Override
        public void get(URI uri) {
            String name = uri.getPath().toLowerCase();
            if (name.endsWith("jhvz"))
                JHVGlobals.getExecutorService().execute(new LoadZipTask(uri));
            else
                JHVGlobals.getExecutorService().execute(new LoadStateTask(uri));
        }
    }

}
