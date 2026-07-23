package org.helioviewer.jhv.layers.selector;

import java.awt.Container;
import java.awt.GridBagConstraints;

import org.helioviewer.jhv.layers.filters.FilterDetails;

// Shared 3-column GridBagLayout row layout for a FilterDetails widget, used by both
// ImageLayerRenderingPanel and ImageLayerGeometryPanel so the layout logic lives in one place.
final class FilterRowLayout {

    static void addFilterRow(Container container, GridBagConstraints c, FilterDetails details) {
        c.gridwidth = 1;

        c.gridx = 0;
        c.weightx = 0;
        c.weighty = 1;
        c.anchor = GridBagConstraints.LINE_END;
        c.fill = GridBagConstraints.NONE;
        container.add(details.getFirst(), c);

        c.gridx = 1;
        c.weightx = 1;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        container.add(details.getSecond(), c);

        c.gridx = 2;
        c.weightx = 0;
        c.anchor = GridBagConstraints.LINE_END;
        c.fill = GridBagConstraints.NONE;
        container.add(details.getThird(), c);
    }

    private FilterRowLayout() {}
}
