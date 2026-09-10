package org.helioviewer.jhv.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

public record ClipSet(@Nullable Range percentile001, @Nullable Range percentile05) {

    public record Range(float lower, float upper) {}

    @Nullable
    public static ClipSet median(List<ClipSet> clipSets) {
        List<Range> ranges001 = new ArrayList<>();
        List<Range> ranges05 = new ArrayList<>();
        for (ClipSet clipSet : clipSets) {
            if (clipSet != null) {
                ranges001.add(clipSet.percentile001());
                ranges05.add(clipSet.percentile05());
            }
        }
        return ranges001.isEmpty() ? null : new ClipSet(medianRange(ranges001), medianRange(ranges05));
    }

    @Nullable
    private static Range medianRange(List<Range> ranges) {
        float[] lower = new float[ranges.size()];
        float[] upper = new float[ranges.size()];
        int count = 0;
        for (Range range : ranges) {
            if (range != null && Float.isFinite(range.lower()) && Float.isFinite(range.upper())) {
                lower[count] = range.lower();
                upper[count] = range.upper();
                count++;
            }
        }
        if (count == 0)
            return null;
        Arrays.sort(lower, 0, count);
        Arrays.sort(upper, 0, count);
        return new Range((float) (((double) lower[(count - 1) / 2] + lower[count / 2]) / 2),
                (float) (((double) upper[(count - 1) / 2] + upper[count / 2]) / 2));
    }

}
