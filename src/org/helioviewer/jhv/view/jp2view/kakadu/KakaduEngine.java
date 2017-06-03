package org.helioviewer.jhv.view.jp2view.kakadu;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import kdu_jni.Jp2_threadsafe_family_src;
import kdu_jni.Jpx_source;
import kdu_jni.KduException;
import kdu_jni.Kdu_cache;
import kdu_jni.Kdu_ilayer_ref;
import kdu_jni.Kdu_region_compositor;

public class KakaduEngine {

    private final Jp2_threadsafe_family_src familySrc = new Jp2_threadsafe_family_src();
    private final Jpx_source jpxSrc;
    private final Kdu_region_compositor compositor;

    public KakaduEngine(Kdu_cache cache, URI uri) throws KduException, IOException {
        if (cache == null) { // local
            File file = new File(uri);
            familySrc.Open(file.getCanonicalPath(), true);
        } else {
            familySrc.Open(cache);
        }

        jpxSrc = new Jpx_source();
        jpxSrc.Open(familySrc, false);
        compositor = createCompositor(jpxSrc);
    }

    public Jp2_threadsafe_family_src getFamilySrc() {
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
        } catch (KduException e) {
            e.printStackTrace();
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
