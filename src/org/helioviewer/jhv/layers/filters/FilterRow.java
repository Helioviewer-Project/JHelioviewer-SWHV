package org.helioviewer.jhv.layers.filters;

import java.awt.Component;

record FilterRow(Component first, Component second, Component third) implements FilterDetails {

    @Override
    public Component getFirst() {
        return first;
    }

    @Override
    public Component getSecond() {
        return second;
    }

    @Override
    public Component getThird() {
        return third;
    }

}
