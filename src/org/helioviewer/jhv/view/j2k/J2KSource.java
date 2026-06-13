package org.helioviewer.jhv.view.j2k;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.image.lut.LUT;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.view.View;
import org.helioviewer.jhv.view.j2k.jpip.JPIPCache;

import kdu_jni.Jp2_input_box;
import kdu_jni.Jp2_palette;
import kdu_jni.Jp2_threadsafe_family_src;
import kdu_jni.Jpx_codestream_source;
import kdu_jni.Jpx_input_box;
import kdu_jni.Jpx_meta_manager;
import kdu_jni.Jpx_metanode;
import kdu_jni.Jpx_source;
import kdu_jni.KduException;
import kdu_jni.Kdu_channel_mapping;
import kdu_jni.Kdu_codestream;
import kdu_jni.Kdu_coords;
import kdu_jni.Kdu_dims;
import kdu_jni.Kdu_global;

abstract class J2KSource {

    private final Jp2_threadsafe_family_src jp2Src = new Jp2_threadsafe_family_src();
    private final Jpx_source jpxSrc = new Jpx_source();
    private final boolean isJP2;
    private boolean isClosed = true;
    private boolean closing;
    private int users;
    private int maxFrame;
    private boolean resolutionStateInitialized;

    private J2KSource(boolean _isJP2) {
        isJP2 = _isJP2;
    }

    // Source lifecycle

    final void open() throws KduException {
        if (!isClosed)
            return;
        doOpenFamilySource();
        jpxSrc.Open(jp2Src, false);
        isClosed = false;
        initResolutionStateOnce();
    }

    void destroy() throws KduException {}

    void close() throws KduException {
        if (isClosed)
            return;
        jpxSrc.Close();
        jp2Src.Close();
        isClosed = true;
    }

    void closeWhenUnused() throws KduException {
        synchronized (this) {
            closing = true;
            while (users > 0) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // The background abolisher thread is not expected to be interrupted.
                    // If this wait ever hangs in normal flow, some decode path leaked
                    // beginUse() without a matching endUse() and that path should be fixed.
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        close();
    }

    // Native access guards

    Use use() {
        if (!beginUse())
            throw new CancellationException("J2KSource access cancelled after close");
        return new Use(this);
    }

    synchronized boolean beginUse() {
        if (closing)
            return false;
        users++;
        return true;
    }

    synchronized void endUse() {
        users--;
        if (users == 0)
            notifyAll();
    }

    record Use(J2KSource source) implements AutoCloseable {
        @Override
        public void close() {
            source.endUse();
        }
    }

    // Source information and native handles

    int maxFrame() {
        return maxFrame;
    }

    ResolutionSet readResolutionSet(int frame) throws KduException {
        Jpx_input_box inputBox = null;
        Kdu_codestream stream = null;
        try {
            Jpx_codestream_source xstream = jpxSrc.Access_codestream(frame);
            inputBox = xstream.Open_stream();
            stream = new Kdu_codestream();
            stream.Create(inputBox);
            if (!stream.Exists()) {
                throw new KduException(">> stream does not exist " + frame);
            }

            // Since it gets tricky here I am just grabbing a bunch of values
            // and taking the max of them. It is acceptable to think that an
            // image is color when it's not monochromatic, but not the other way
            // around... so this is just playing it safe.
            int maxComponents;
            Kdu_channel_mapping cmap = new Kdu_channel_mapping();
            try {
                cmap.Configure(stream);

                maxComponents = MathUtils.max(
                        cmap.Get_num_channels(), cmap.Get_num_colour_channels(),
                        stream.Get_num_components(true), stream.Get_num_components(false));
                // numComponents = maxComponents == 1 ? 1 : 3;
                // With new file formats we may have 2 components
            } finally {
                cmap.Clear();
                cmap.Native_destroy();
            }

            int maxDWT = stream.Get_min_dwt_levels();
            ResolutionSet res = new ResolutionSet(maxDWT + 1, maxComponents);

            Kdu_dims dims = new Kdu_dims();
            stream.Get_dims(0, dims);
            Kdu_coords siz = dims.Access_size();
            int width0 = siz.Get_x(), height0 = siz.Get_y();
            res.addLevel(0, width0, height0, 1, 1);

            for (int i = 1; i <= maxDWT; i++) {
                stream.Apply_input_restrictions(0, 0, i, 0, null, Kdu_global.KDU_WANT_CODESTREAM_COMPONENTS);
                stream.Get_dims(0, dims);
                siz = dims.Access_size();
                int width = siz.Get_x(), height = siz.Get_y();
                res.addLevel(i, width, height, width0 / (double) width, height0 / (double) height);
            }

            return res;
        } finally {
            if (stream != null) {
                try {
                    stream.Destroy();
                } catch (KduException ignore) {}
            }
            if (inputBox != null) {
                inputBox.Close();
                inputBox.Native_destroy();
            }
        }
    }

    @Nullable
    LUT getLUT() throws KduException {
        Jpx_codestream_source xstream = jpxSrc.Access_codestream(0);
        if (!xstream.Exists()) {
            throw new KduException(">> stream does not exist");
        }
        Jp2_palette palette = xstream.Access_palette();

        int numLUTs = palette.Get_num_luts();
        if (numLUTs == 0)
            return null;

        int len = palette.Get_num_entries();
        float[] red = new float[len];
        float[] green = new float[len];
        float[] blue = new float[len];

        palette.Get_lut(0, red, Kdu_global.JP2_CHANNEL_FORMAT_DEFAULT);
        palette.Get_lut(1, green, Kdu_global.JP2_CHANNEL_FORMAT_DEFAULT);
        palette.Get_lut(2, blue, Kdu_global.JP2_CHANNEL_FORMAT_DEFAULT);

        return LUT.fromOpaqueRgb("built-in", red, green, blue);
    }

    private static final long[] xmlFilter = {Kdu_global.jp2_xml_4cc};

    void extractMetaData(String[] xmlMetaData) throws KduException {
        Jpx_meta_manager metaManager = jpxSrc.Access_meta_manager();
        Jpx_metanode node = new Jpx_metanode();
        int i = 0;

        Jp2_input_box xmlBox = new Jp2_input_box();
        try {
            while ((node = metaManager.Peek_and_clear_touched_nodes(1, xmlFilter, node)).Exists()) {
                if (i == xmlMetaData.length)
                    break;
                if (node.Open_existing(xmlBox)) {
                    try {
                        xmlMetaData[i] = xmlBox2String(xmlBox);
                    } finally {
                        xmlBox.Close();
                    }
                }
                i++;
            }
        } finally {
            xmlBox.Close(); // harmless if already closed
            xmlBox.Native_destroy();
        }
    }

    Jpx_source jpxSource() {
        return jpxSrc;
    }

    boolean isJP2() {
        return isJP2;
    }

    // Progressive completion state

    abstract int getPartialUntil();

    abstract ResolutionSet resolutionSet(int frame);

    abstract boolean isComplete(int level);

    @Nullable
    abstract AtomicBoolean getFrameStatus(int frame, int level);

    // Initialization internals

    private void initResolutionStateOnce() throws KduException {
        if (resolutionStateInitialized)
            return;
        maxFrame = getNumberLayers() - 1;
        doInitResolutionState();
        resolutionStateInitialized = true;
    }

    private int getNumberLayers() throws KduException {
        int[] temp = new int[1];
        jpxSrc.Count_compositing_layers(temp);
        return temp[0];
    }

    // Internal subclass hooks

    abstract void doOpenFamilySource() throws KduException;

    abstract void doInitResolutionState() throws KduException;

    private static final AtomicBoolean full = new AtomicBoolean(true);

    static class Local extends J2KSource {

        private final String path;
        private ResolutionSet[] resolutionSet;

        Local(String _path, boolean isJP2) {
            super(isJP2);
            path = _path;
        }

        @Override
        void doOpenFamilySource() throws KduException {
            super.jp2Src.Open(path, true);
        }

        @Override
        void doInitResolutionState() throws KduException {
            resolutionSet = new ResolutionSet[maxFrame() + 1];
            for (int i = 0; i <= maxFrame(); ++i) {
                resolutionSet[i] = readResolutionSet(i);
                resolutionSet[i].setComplete(0);
            }
        }

        @Override
        int getPartialUntil() {
            return maxFrame();
        }

        @Override
        ResolutionSet resolutionSet(int frame) {
            return resolutionSet[frame];
        }

        @Override
        boolean isComplete(int level) {
            return true;
        }

        @Nullable
        @Override
        AtomicBoolean getFrameStatus(int frame, int level) {
            return full;
        }

    }

    static class Remote extends J2KSource {

        private final JPIPCache cache = new JPIPCache();
        private ResolutionSet[] resolutionSet;
        private int partialUntil = 0;
        private boolean fullyComplete;

        Remote() {
            super(false);
        }

        JPIPCache cache() {
            return cache;
        }

        @Override
        void doOpenFamilySource() throws KduException {
            super.jp2Src.Open(cache);
        }

        @Override
        void destroy() throws KduException {
            cache.Close();
            cache.Native_destroy();
        }

        @Override
        void doInitResolutionState() throws KduException {
            resolutionSet = new ResolutionSet[maxFrame() + 1];
            resolutionSet[0] = readResolutionSet(0);
        }

        @Override
        int getPartialUntil() {
            int i;
            for (i = partialUntil; i <= maxFrame(); i++) {
                if (resolutionSet[i] == null)
                    break;
            }
            partialUntil = Math.max(0, i - 1);
            return partialUntil;
        }

        @Override
        ResolutionSet resolutionSet(int frame) {
            if (resolutionSet[frame] == null) {
                Log.error("resolutionSet[" + frame + "] is null"); // never happened?
                return resolutionSet[0];
            }
            return resolutionSet[frame];
        }

        @Override
        boolean isComplete(int level) {
            if (fullyComplete)
                return true;

            for (int i = 0; i <= maxFrame(); i++) {
                if (resolutionSet[i] == null)
                    return false;
                AtomicBoolean status = resolutionSet[i].getComplete(level);
                if (status == null || !status.get())
                    return false;
            }
            if (level == 0)
                fullyComplete = true;
            return true;
        }

        @Nullable
        @Override
        AtomicBoolean getFrameStatus(int frame, int level) {
            if (fullyComplete)
                return full;
            if (resolutionSet[frame] == null)
                return null;
            return resolutionSet[frame].getComplete(level);
        }

        @SuppressWarnings("try")
        void setFramePartial(int frame) throws KduException {
            if (resolutionSet[frame] == null) {
                try (Use ignored = use()) {
                    resolutionSet[frame] = readResolutionSet(frame);
                }
            }
        }

        void setFrameComplete(int frame, int level) throws KduException {
            setFramePartial(frame);
            if (fullyComplete)
                return;

            if (resolutionSet[frame] != null)
                resolutionSet[frame].setComplete(level);
        }

    }

    private static String xmlBox2String(Jp2_input_box xmlBox) throws KduException {
        int len = (int) xmlBox.Get_remaining_bytes();
        if (len <= 0)
            return View.EMPTY_METAXML;
        byte[] buf = new byte[len];
        xmlBox.Read(buf, len);
        return new String(buf, StandardCharsets.UTF_8).trim().replace("&", "&amp;");
    }

}
