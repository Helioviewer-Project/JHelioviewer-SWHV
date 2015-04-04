package org.helioviewer.viewmodel.view;

import java.util.AbstractList;
import java.util.LinkedList;

import org.helioviewer.jhv.gui.dialogs.ExportMovieDialog;
import org.helioviewer.viewmodel.renderer.screen.ScreenRenderer;

public abstract class AbstractComponentView extends AbstractView implements ComponentView {

    protected AbstractList<ScreenRenderer> postRenderers = new LinkedList<ScreenRenderer>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void addPostRenderer(ScreenRenderer postRenderer) {
        if (postRenderer != null) {
            if (!containsPostRenderer(postRenderer)) {
                postRenderers.add(postRenderer);
                if (postRenderer instanceof ViewListener) {
                    addViewListener((ViewListener) postRenderer);
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
    @Override
    public void removePostRenderer(ScreenRenderer postRenderer) {
        if (postRenderer != null) {
            do {
                postRenderers.remove(postRenderer);
                if (postRenderer instanceof ViewListener) {
                    removeViewListener((ViewListener) postRenderer);
                }
            } while (postRenderers.contains(postRenderer));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractList<ScreenRenderer> getAllPostRenderer() {
        return postRenderers;
    }

    abstract public void startExport(ExportMovieDialog exportMovieDialog);

}
