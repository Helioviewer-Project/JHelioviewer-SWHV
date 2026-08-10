package org.helioviewer.jhv.layers.filters;

import java.awt.Component;

public interface FilterDetails {

    Component getFirst();

    Component getSecond();

    Component getThird();

    default void setVisible(boolean visible) {
        getFirst().setVisible(visible);
        getSecond().setVisible(visible);
        getThird().setVisible(visible);
    }

}
