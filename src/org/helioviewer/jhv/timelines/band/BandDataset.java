package org.helioviewer.jhv.timelines.band;

import java.util.List;

public record BandDataset(String title, List<BandType> bandTypes) {

    public BandDataset {
        bandTypes = List.copyOf(bandTypes);
    }

    @Override
    public String toString() {
        return title;
    }
}
