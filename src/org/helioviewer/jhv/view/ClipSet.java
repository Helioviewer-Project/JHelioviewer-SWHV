package org.helioviewer.jhv.view;

import javax.annotation.Nullable;

public record ClipSet(@Nullable Range percentile001, @Nullable Range percentile05) {

    public record Range(float lower, float upper) {}

}
