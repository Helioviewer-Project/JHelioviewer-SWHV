package org.helioviewer.jhv.plugin.renderable;

import java.util.ArrayList;

public class RenderableTypeContainer {

    private final ArrayList<RenderableType> renderableTypes = new ArrayList<RenderableType>();

    public RenderableTypeContainer() {
    }

    public void addType(RenderableType type) {
        renderableTypes.add(type);
    }

    public void removeType(RenderableType type) {
        renderableTypes.remove(type);
    }

}
