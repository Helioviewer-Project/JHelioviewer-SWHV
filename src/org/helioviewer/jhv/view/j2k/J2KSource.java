package org.helioviewer.jhv.view.j2k;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nullable;

import kdu_jni.Jp2_input_box;
import kdu_jni.Jp2_palette;
import kdu_jni.Jp2_threadsafe_family_src;
import kdu_jni.Jpx_codestream_source;
import kdu_jni.Jpx_input_box;
import kdu_jni.Jpx_meta_manager;
import kdu_jni.Jpx_metanode;
import kdu_jni.Jpx_source;
import kdu_jni.KduException;
import kdu_jni.Kdu_cache;
import kdu_jni.Kdu_channel_mapping;
import kdu_jni.Kdu_codestream;
import kdu_jni.Kdu_coords;
import kdu_jni.Kdu_dims;
import kdu_jni.Kdu_global;

import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.view.View;

abstract class J2KSource {

    private final Jp2_threadsafe_family_src jp2Src = new Jp2_threadsafe_family_src();
    private final Jpx_source jpxSrc = new Jpx_source();
    private boolean isJP2;

    static class Local extends J2KSource {

        private final String path;

        Local(String _path, boolean isJP2) {
            path = _path;
            super.isJP2 = isJP2;
        }

        @Override
        void open() throws KduException {
            super.jp2Src.Open(path, true);
            super.jpxSrc.Open(super.jp2Src, false);
        }

    }

    static class Remote extends J2KSource {

        private final Kdu_cache cache;

        Remote(Kdu_cache _cache) {
            cache = _cache;
        }

        @Override
        void open() throws KduException {
            super.jp2Src.Open(cache);
            super.jpxSrc.Open(super.jp2Src, false);
        }

    }

    abstract void open() throws KduException;

    void close() throws KduException {
        jpxSrc.Close();
        jp2Src.Close();
    }

    Jpx_source jpxSource() {
        return jpxSrc;
    }

    boolean isJP2() {
        return isJP2;
    }

    int getNumberLayers() throws KduException {
        int[] temp = new int[1];
        jpxSrc.Count_compositing_layers(temp);
        return temp[0];
    }

    ResolutionSet getResolutionSet(int frame) throws KduException {
        Jpx_codestream_source xstream = jpxSrc.Access_codestream(frame);
        Jpx_input_box inputBox = xstream.Open_stream();
        Kdu_codestream stream = new Kdu_codestream();
        stream.Create(inputBox);
        if (!stream.Exists()) {
            throw new KduException(">> stream does not exist " + frame);
        }

        // Since it gets tricky here I am just grabbing a bunch of values
        // and taking the max of them. It is acceptable to think that an
        // image is color when it's not monochromatic, but not the other way
        // around... so this is just playing it safe.
        Kdu_channel_mapping cmap = new Kdu_channel_mapping();
        cmap.Configure(stream);

        int maxComponents = MathUtils.max(cmap.Get_num_channels(), cmap.Get_num_colour_channels(), stream.Get_num_components(true), stream.Get_num_components(false));
        // numComponents = maxComponents == 1 ? 1 : 3;
        // With new file formats we may have 2 components
        cmap.Clear();
        cmap.Native_destroy();

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

        stream.Destroy();
        inputBox.Close();
        inputBox.Native_destroy();

        return res;
    }

    @Nullable
    int[] getLUT() throws KduException {
        Jpx_codestream_source xstream = jpxSrc.Access_codestream(0);
        if (!xstream.Exists()) {
            throw new KduException(">> stream does not exist");
        }
        Jp2_palette palette = xstream.Access_palette();

        int numLUTs = palette.Get_num_luts();
        if (numLUTs == 0)
            return null;

        int len = palette.Get_num_entries();
        int[] lut = new int[len];
        float[] red = new float[len];
        float[] green = new float[len];
        float[] blue = new float[len];

        palette.Get_lut(0, red, Kdu_global.JP2_CHANNEL_FORMAT_DEFAULT);
        palette.Get_lut(1, green, Kdu_global.JP2_CHANNEL_FORMAT_DEFAULT);
        palette.Get_lut(2, blue, Kdu_global.JP2_CHANNEL_FORMAT_DEFAULT);

        for (int i = 0; i < len; i++) {
            lut[i] = 0xFF000000 | ((int) ((red[i] + 0.5f) * 0xFF) << 16) | ((int) ((green[i] + 0.5f) * 0xFF) << 8) | ((int) ((blue[i] + 0.5f) * 0xFF));
        }
        return lut;
    }

    private static final long[] xmlFilter = {Kdu_global.jp2_xml_4cc};

    void extractMetaData(String[] xmlMetaData) throws KduException {
        Jpx_meta_manager metaManager = jpxSrc.Access_meta_manager();
        Jpx_metanode node = new Jpx_metanode();
        int i = 0;

        Jp2_input_box xmlBox = new Jp2_input_box();
        while ((node = metaManager.Peek_and_clear_touched_nodes(1, xmlFilter, node)).Exists()) {
            if (i == xmlMetaData.length)
                break;
            if (node.Open_existing(xmlBox)) {
                xmlMetaData[i] = xmlBox2String(xmlBox);
                xmlBox.Close();
            }
            i++;
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
