package org.helioviewer.jhv.gui.components;

import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;

/**
 * This class represents an image component that is used to display the image of
 * all images.
 *
 * @author caplins
 * @author Alen Agheksanterian
 * @author Benjamin Wamsler
 * @author Stephan Pagel
 * @author Markus Langenberg
 */
public class MainImagePanel extends BasicImagePanel implements LayersListener {

    // ///////////////////////////////////////////////////////////////////////////
    // Definitions
    // ///////////////////////////////////////////////////////////////////////////

    // default serialVersionUID
    private static final long serialVersionUID = 1L;
    private final int loadingTasks = 0;

    private final ArrayList<MouseMotionListener> mouseMotionListeners = new ArrayList<MouseMotionListener>();

    // ///////////////////////////////////////////////////////////////////////////
    // Methods
    // ///////////////////////////////////////////////////////////////////////////

    /**
     * The public constructor
     * */
    public MainImagePanel() {
        // call constructor of super class
        super();

        // the one GLCanvas
        add(renderedImageComponent);

        Displayer.getLayersModel().addLayersListener(this);
    }

    /**
     * {@inheritDoc}
     */

    @Override
    public void setView(ComponentView newView) {
        if (renderedImageComponent != null)
            for (MouseMotionListener l : mouseMotionListeners)
                renderedImageComponent.removeMouseMotionListener(l);

        super.setView(newView);

        if (newView != null) {
            if (renderedImageComponent != null)
                for (MouseMotionListener l : mouseMotionListeners)
                    renderedImageComponent.addMouseMotionListener(l);
        }
    }

    /**
     * {@inheritDoc}
     */

    @Override
    public void addMouseMotionListener(MouseMotionListener l) {
        if (l != null)
            mouseMotionListeners.add(l);
    }

    /**
     * {@inheritDoc}
     */

    @Override
    public void removeMouseMotionListener(MouseMotionListener l) {
        if (l != null)
            mouseMotionListeners.remove(l);
    }

    @Override
    public void layerAdded(int idx) {

    }

    @Override
    public void layerRemoved(int oldIdx) {

    }

    @Override
    public void activeLayerChanged(JHVJP2View view) {
    }

}
