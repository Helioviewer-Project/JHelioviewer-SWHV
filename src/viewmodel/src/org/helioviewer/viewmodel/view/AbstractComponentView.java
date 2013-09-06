package org.helioviewer.viewmodel.view;

import java.util.AbstractList;
import java.util.LinkedList;

import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.viewmodel.renderer.screen.ScreenRenderer;

public abstract class AbstractComponentView extends AbstractBasicView implements ComponentView {

    protected volatile Vector2dInt mainImagePanelSize;

    protected AbstractList<ScreenRenderer> postRenderers = new LinkedList<ScreenRenderer>();

    public void updateMainImagePanelSize(Vector2dInt size) {
        mainImagePanelSize = size;
    }

    /**
     * {@inheritDoc}
     */
    public void addPostRenderer(ScreenRenderer postRenderer) {
        if (postRenderer != null) {
            synchronized (postRenderers) {
                if (!containsPostRenderer(postRenderer)) {
                    postRenderers.add(postRenderer);
                    if (postRenderer instanceof ViewListener) {
                        addViewListener((ViewListener) postRenderer);
                    }
                }
            }
        }
    }

    private boolean containsPostRenderer(ScreenRenderer postrenderer) {
        for (ScreenRenderer r : this.postRenderers) {
            if (r.getClass().equals(postrenderer.getClass())) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void removePostRenderer(ScreenRenderer postRenderer) {
        if (postRenderer != null) {
            synchronized (postRenderers) {
                do {
                    postRenderers.remove(postRenderer);
                    if (postRenderer instanceof ViewListener) {
                        removeViewListener((ViewListener) postRenderer);
                    }
                } while (postRenderers.contains(postRenderer));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public AbstractList<ScreenRenderer> getAllPostRenderer() {
        return postRenderers;
    }
}
