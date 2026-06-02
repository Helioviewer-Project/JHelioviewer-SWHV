package org.helioviewer.jhv.layers.spaceobject;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public final class SpaceObjectContainer {

    private final boolean exclusive;
    private final List<SpaceObjectElement> elements;
    private Runnable changeListener = () -> {};

    private SpaceObjectElement highlighted;

    public SpaceObjectContainer(List<SpaceObjectElement> _elements, boolean _exclusive) {
        exclusive = _exclusive;
        elements = _elements;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public List<SpaceObjectElement> getElements() {
        return elements;
    }

    public void setHighlightedElement(SpaceObjectElement element) {
        highlighted = element;
    }

    @Nullable
    public SpaceObjectElement getHighlightedElement() {
        return highlighted;
    }

    public List<SpaceObjectElement> getSelectedElements() {
        ArrayList<SpaceObjectElement> selected = new ArrayList<>();
        for (SpaceObjectElement element : elements) {
            if (element.isSelected())
                selected.add(element);
        }
        return selected;
    }

    public void selectElement(SpaceObjectElement element) {
        highlighted = element;
        if (exclusive) {
            if (element.isSelected()) // avoid reload on re-clicking same
                return;
            for (SpaceObjectElement selected : elements) {
                if (selected.isSelected())
                    selected.deselect();
            }
            element.select();
        } else {
            if (element.isSelected())
                element.deselect();
            else
                element.select();
        }
        fireChanged();
    }

    public void setChangeListener(Runnable listener) {
        changeListener = listener;
    }

    public void setStatus(SpaceObjectElement element, String status) {
        element.setStatus(status);
        fireChanged();
    }

    private void fireChanged() {
        changeListener.run();
    }

}
