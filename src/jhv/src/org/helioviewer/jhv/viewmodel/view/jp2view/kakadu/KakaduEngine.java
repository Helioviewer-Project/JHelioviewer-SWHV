package org.helioviewer.jhv.viewmodel.view.jp2view.kakadu;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import kdu_jni.Jp2_family_src;
import kdu_jni.Jpx_source;
import kdu_jni.KduException;
import kdu_jni.Kdu_cache;
import kdu_jni.Kdu_ilayer_ref;
import kdu_jni.Kdu_region_compositor;

public class KakaduEngine {

    private final Jp2_family_src familySrc;
    private final Jpx_source jpxSrc;
    private final Kdu_region_compositor compositor;
    private final Kdu_cache slaveCache;

    public KakaduEngine(Kdu_cache cache, URI uri) throws KduException, IOException {
        familySrc = new Jp2_family_src();
        if (cache == null) { // local
            slaveCache = null;
            familySrc.Open(new File(uri).getCanonicalPath(), true);
        } else {
            slaveCache = new Kdu_cache();
            slaveCache.Attach_to(cache);
            familySrc.Open(slaveCache);
        }

        jpxSrc = new Jpx_source();
        jpxSrc.Open(familySrc, false);
        compositor = createCompositor(jpxSrc);
    }

    public Jp2_family_src getFamilySrc() {
        return familySrc;
    }

    public Jpx_source getJpxSource() {
        return jpxSrc;
    }

    public Kdu_region_compositor getCompositor() {
        return compositor;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            destroyCompositor(compositor);
            if (jpxSrc != null) {
                if (jpxSrc.Exists())
                    jpxSrc.Close();
                jpxSrc.Native_destroy();
            }
            if (familySrc != null) {
                if (familySrc.Exists())
                    familySrc.Close();
                familySrc.Native_destroy();
            }
            if (slaveCache != null) {
                slaveCache.Close();
                slaveCache.Native_destroy();
            }
        } finally {
            super.finalize();
        }
    }

    private static Kdu_region_compositor createCompositor(Jpx_source jpx) throws KduException {
        Kdu_region_compositor compositor = new Kdu_region_compositor();
        // System.out.println(">>>> compositor create " + compositor + " " + Thread.currentThread().getName());
        compositor.Create(jpx, KakaduConstants.CODESTREAM_CACHE_THRESHOLD);
        compositor.Set_surface_initialization_mode(false);
        return compositor;
    }

    private static void destroyCompositor(Kdu_region_compositor compositor) throws KduException {
        // System.out.println(">>>> compositor destroy " + compositor + " " + Thread.currentThread().getName());
        compositor.Halt_processing();
        compositor.Remove_ilayer(new Kdu_ilayer_ref(), true);
        compositor.Set_thread_env(null, null);
        compositor.Native_destroy();
    }

}
