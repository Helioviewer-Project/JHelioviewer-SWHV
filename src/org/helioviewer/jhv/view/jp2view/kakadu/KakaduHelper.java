package org.helioviewer.jhv.view.jp2view.kakadu;

import kdu_jni.Jp2_palette;
import kdu_jni.Jpx_codestream_source;
import kdu_jni.Jpx_source;
import kdu_jni.KduException;
import kdu_jni.Kdu_channel_mapping;
import kdu_jni.Kdu_codestream;
import kdu_jni.Kdu_coords;
import kdu_jni.Kdu_dims;
import kdu_jni.Kdu_ilayer_ref;
import kdu_jni.Kdu_istream_ref;
import kdu_jni.Kdu_region_compositor;

import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.view.jp2view.image.ResolutionSet;

public class KakaduHelper {

    public static ResolutionSet getResolutionSet(Kdu_region_compositor compositor, int frame) throws KduException {
        compositor.Refresh();

        Kdu_dims ref1 = new Kdu_dims(), ref2 = new Kdu_dims(); // avoid gc
        Kdu_ilayer_ref ilayer = compositor.Add_ilayer(frame, ref1, ref2);
        Kdu_codestream stream = compositor.Access_codestream(compositor.Get_next_istream(new Kdu_istream_ref(), false, true, frame));
        if (!stream.Exists()) {
            throw new KduException(">> stream does not exist " + frame);
        }

        // Since it gets tricky here I am just grabbing a bunch of values
        // and taking the max of them. It is acceptable to think that an
        // image is color when its not monochromatic, but not the other way
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

        compositor.Set_scale(false, false, false, 1f);
        Kdu_dims dims = new Kdu_dims();
        if (!compositor.Get_total_composition_dims(dims)) {
            throw new KduException(">> cannot determine dimensions for stream " + frame);
        }

        Kdu_coords siz = dims.Access_size();
        int width0 = siz.Get_x(), height0 = siz.Get_y();
        res.addResolutionLevel(0, width0, height0, 1, 1);

        for (int i = 1; i <= maxDWT; i++) {
            compositor.Set_scale(false, false, false, 1f / (1 << i));
            dims = new Kdu_dims();
            if (!compositor.Get_total_composition_dims(dims))
                throw new KduException(">> cannot determine dimensions for stream " + frame);

            siz = dims.Access_size();
            int width = siz.Get_x(), height = siz.Get_y();
            res.addResolutionLevel(i, width, height, width0 / (double) width, height0 / (double) height);
        }

        compositor.Remove_ilayer(ilayer, true);

        return res;
    }

    public static int[] getLUT(Jpx_source jpxSrc) throws KduException {
        Jpx_codestream_source stream = jpxSrc.Access_codestream(0);
        if (!stream.Exists()) {
            throw new KduException(">> stream does not exist");
        }

        Jp2_palette palette = stream.Access_palette();

        int numLUTs = palette.Get_num_luts();
        if (numLUTs == 0)
            return null;

        int[] lut = new int[palette.Get_num_entries()];
        float[] red = new float[lut.length];
        float[] green = new float[lut.length];
        float[] blue = new float[lut.length];

        palette.Get_lut(0, red);
        palette.Get_lut(1, green);
        palette.Get_lut(2, blue);

        for (int i = 0; i < lut.length; i++) {
            lut[i] = 0xFF000000 | ((int) ((red[i] + 0.5f) * 0xFF) << 16) | ((int) ((green[i] + 0.5f) * 0xFF) << 8) | ((int) ((blue[i] + 0.5f) * 0xFF));
        }

        return lut;
    }

    /*
     * Deactivates the internal color lookup table for the given composition layer
     *
     * It is not allowed to call this function for a layer which is not loaded yet.
     *
     * @param numLayer
     *            composition layer to deactivate internal color lookup for
     */
    /*
        in preservation - not needed
        void deactivateColorLookupTable(int numLayer) throws KduException {
            for (int i = 0; i < numLUTs; i++) {
                jpxSrc.Access_layer(numLayer).Access_channels().Set_colour_mapping(i, 0, -1, numLayer);
            }
        }
    */

}
