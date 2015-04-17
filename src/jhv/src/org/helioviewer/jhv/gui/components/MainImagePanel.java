package org.helioviewer.jhv.gui.components;

import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

import org.helioviewer.viewmodel.view.ComponentView;

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
public class MainImagePanel extends BasicImagePanel {

    private static final long serialVersionUID = 1L;

    private final ArrayList<MouseMotionListener> mouseMotionListeners = new ArrayList<MouseMotionListener>();

    /**
     * The public constructor
     * */
    public MainImagePanel() {
        // call constructor of super class
        super();

        // the one GLCanvas
        add(renderedImageComponent);

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

}
