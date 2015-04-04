package org.helioviewer.jhv.gui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.renderer.screen.ScreenRenderGraphics;
import org.helioviewer.viewmodel.renderer.screen.ScreenRenderer;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.opengl.GLSharedDrawable;

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

    private final NoImagePostRenderer noImagePostRenderer = new NoImagePostRenderer();
    private boolean noImagePostRendererSet = false;

    private final LoadingPostRendererSwitch loadingPostRenderer = new LoadingPostRendererSwitch();
    private int loadingTasks = 0;

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
        renderedImageComponent = GLSharedDrawable.getSingletonInstance().getCanvas();
        add(renderedImageComponent);

        // add post render that no image is loaded
        noImagePostRenderer.setContainerSize(getWidth(), getHeight());
        addPostRenderer(noImagePostRenderer);
        noImagePostRendererSet = true;

        loadingPostRenderer.setContainerSize(getWidth(), getHeight());
        Displayer.getLayersModel().addLayersListener(this);
    }

    /**
     * Shows the image loading animation.
     *
     * Manages a counter, so that the animation appears on the first loading
     * process and disappears on the last.
     *
     * @param isLoading
     *            true to start animation, false to stop
     */
    public void setLoading(boolean isLoading) {
        if (isLoading) {
            if (loadingTasks <= 0) {
                if (noImagePostRendererSet) {
                    removePostRenderer(noImagePostRenderer);
                    noImagePostRendererSet = false;
                }
                addPostRenderer(loadingPostRenderer);
                loadingPostRenderer.startAnimation();
            }
            loadingTasks++;
        } else if (loadingTasks > 0) {
            loadingTasks--;
            if (loadingTasks == 0) {
                removePostRenderer(loadingPostRenderer);
                loadingPostRenderer.stopAnimation();

                if (Displayer.getRenderablecontainer().countImageLayers() == 0) {
                    addPostRenderer(noImagePostRenderer);
                    noImagePostRendererSet = true;
                }
            }
        }
        repaint();
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

            LayersModel layersModel = Displayer.getLayersModel();
            if (layersModel != null) {
                if (layersModel.getNumLayers() > 0 || loadingTasks > 0) {
                    // remove
                    if (noImagePostRendererSet) {
                        removePostRenderer(noImagePostRenderer);
                        noImagePostRendererSet = false;
                    }
                } else {
                    // add
                    if (!noImagePostRendererSet) {
                        addPostRenderer(noImagePostRenderer);
                        noImagePostRendererSet = true;
                    }
                }
            }
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
        if (!noImagePostRendererSet) {
            addPostRenderer(noImagePostRenderer);
            noImagePostRendererSet = true;
        }
        loadingPostRenderer.useCenterRenderer(Displayer.getLayersModel().getNumLayers() == 0);
    }

    @Override
    public void layerRemoved(int oldIdx) {
        if (noImagePostRendererSet) {
            removePostRenderer(noImagePostRenderer);
            noImagePostRendererSet = false;
        }
        loadingPostRenderer.useCenterRenderer(Displayer.getLayersModel().getNumLayers() == 0);
    }

    @Override
    public void activeLayerChanged(JHVJP2View view) {
    }

    /**
     * A post renderer which displays an image which shows that no image (layer)
     * is loaded.
     *
     * @author Stephan Pagel
     * */
    private class NoImagePostRenderer implements ScreenRenderer {

        private BufferedImage image = null;
        private Dimension size = new Dimension(0, 0);

        /**
         * Default constructor.
         */
        public NoImagePostRenderer() {
            try {
                image = IconBank.getImage(JHVIcon.NOIMAGE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Sets the size of the available image area (viewport).
         *
         * @param width
         *            width of the available space of the image.
         * @param height
         *            height of the available space of the image.
         */
        public void setContainerSize(int width, int height) {
            size = new Dimension(width, height);
        }

        /**
         * {@inheritDoc}
         *
         * Draws the no image loaded image.
         */
        @Override
        public void render(ScreenRenderGraphics g) {
            if (image != null) {
                g.drawImage(image, (size.width - image.getWidth()) / 2, (size.height - image.getHeight()) / 2);
            }
        }
    }

    /**
     * A post renderer which indicates that something is being loaded right now.
     *
     * @author Markus Langenberg
     */
    private interface LoadingPostRenderer extends ScreenRenderer {

        /**
         * Sets the size of the available image area (viewport).
         *
         * @param width
         *            width of the available space of the image.
         * @param height
         *            height of the available space of the image.
         */
        public void setContainerSize(int width, int height);

        /**
         * Starts animating the pearls.
         */
        public void startAnimation();

        /**
         * Stops animating the pearls.
         */
        public void stopAnimation();

        /**
         * Returns, whether the animation is running.
         *
         * @return True, if the animation is running, false otherwise
         */
        public boolean isAnimating();
    }

    /**
     * Base implementation of LoadingPostRenderer.
     *
     * This implementation is abstract and does not specify some specific
     * appearance parameters.
     *
     * @author Markus Langenberg
     */
    private abstract class BaseLoadingPostRenderer implements LoadingPostRenderer {

        // track
        private final int offsetX;
        private final int offsetY;
        private final int radiusTrack;
        private final float[] sinPositions;

        // pearls
        private final int numPearlPositions;
        private final int numPearls;
        private final int radiusPearl;
        private final Color[] pearlColors;
        private int currentPearlPos = 0;

        // image
        protected BufferedImage image;

        // coordinate of the upper left corner of the image
        protected Point position = new Point(0, 0);

        // timer
        Timer timer;

        /**
         * Default constructor.
         */
        public BaseLoadingPostRenderer(JHVIcon icon, int offsetX, int offsetY, int radiusTrack, int radiusPearl, int numPearlPositions, int numPearls) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.radiusTrack = radiusTrack;
            this.radiusPearl = radiusPearl;
            this.numPearlPositions = numPearlPositions;
            this.numPearls = numPearls;

            sinPositions = new float[numPearlPositions];
            for (int i = 0; i < numPearlPositions; i++) {
                sinPositions[i] = (float) Math.sin(Math.PI * 2 * i / numPearlPositions);
            }

            pearlColors = new Color[numPearls];
            for (int i = 0; i < numPearls; i++) {
                int alpha = 192 - (int) (192 * ((float) i / numPearls));
                pearlColors[i] = new Color(192, 192, 192, alpha);
            }

            try {
                image = IconBank.getImage(icon);
            } catch (Exception e) {
                image = new BufferedImage(0, 0, BufferedImage.TYPE_BYTE_GRAY);
                e.printStackTrace();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void startAnimation() {
            currentPearlPos = numPearlPositions / 2;

            if (timer == null) {
                timer = new Timer("Loading Animation", true);
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        currentPearlPos++;
                        repaint();
                        Displayer.display();
                    }
                }, 0, 200);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void stopAnimation() {
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isAnimating() {
            return timer != null;
        }

        /**
         * {@inheritDoc}
         *
         * Draws the loading image and its animation.
         */
        @Override
        public void render(ScreenRenderGraphics g) {
            int wf = 1;
            int hf = 1;

            if (image != null) {
                g.drawImage(image, wf * position.x - (radiusPearl - offsetX) * (wf - 1), hf * position.y - (radiusPearl - offsetY) * (hf - 1));
            }

            int centerX = wf * (position.x - radiusPearl + offsetX);
            int centerY = hf * (position.y - radiusPearl + offsetY);

            for (int i = 0; i < numPearls; i++) {
                g.setColor(pearlColors[i]);
                g.fillOval(centerX + (int) (radiusTrack * sinPositions[(i - currentPearlPos) & (numPearlPositions - 1)]), centerY + (int) (radiusTrack * sinPositions[(i - currentPearlPos + (numPearlPositions >> 2)) & (numPearlPositions - 1)]), radiusPearl * 2, radiusPearl * 2);
            }
        }
    }

    /**
     * Extension of BaseLoadingPostRenderer for drawing a big animation in the
     * middle of the screen.
     *
     * @author Markus Langenberg
     */
    private class CenterLoadingPostRenderer extends BaseLoadingPostRenderer {

        /**
         * Default constructor.
         */
        public CenterLoadingPostRenderer() {
            super(IconBank.JHVIcon.LOADING_BIG, 124, 101, 97, 6, 32, 12);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setContainerSize(int width, int height) {
            position = new Point((width - image.getWidth()) / 2, (height - image.getHeight()) / 2);
        }
    }

    /**
     * Extension of BaseLoadingPostRenderer for drawing a small animation in the
     * top right corner of the screen.
     *
     * @author Markus Langenberg
     */
    private class CornerLoadingPostRenderer extends BaseLoadingPostRenderer {

        /**
         * Default constructor.
         */
        public CornerLoadingPostRenderer() {
            super(IconBank.JHVIcon.LOADING_SMALL, 193, 25, 30, 4, 16, 16);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setContainerSize(int width, int height) {
            // position = new Point(width - 185, 12);
            position = new Point(width - 231, 12);
        }
    }

    /**
     * Implementation of LoadingPostRenderer for switching between multiple
     * other implementations.
     *
     * This class owns a CenterLoadingPostRenderer and a
     * CornerLoadingPostRenderer. All calls to this class a redirected to one of
     * them. When no image is loaded, the center version is used, otherwise the
     * corner version.
     *
     * @author Markus Langenberg
     */
    private class LoadingPostRendererSwitch implements LoadingPostRenderer {

        private final LoadingPostRenderer centerRenderer = new CenterLoadingPostRenderer();
        private final LoadingPostRenderer cornerRenderer = new CornerLoadingPostRenderer();
        private LoadingPostRenderer currentRenderer = centerRenderer;

        /**
         * {@inheritDoc}
         */
        @Override
        public void render(ScreenRenderGraphics g) {
            currentRenderer.render(g);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setContainerSize(int width, int height) {
            centerRenderer.setContainerSize(width, height);
            cornerRenderer.setContainerSize(width, height);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void startAnimation() {
            currentRenderer.startAnimation();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void stopAnimation() {
            currentRenderer.stopAnimation();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isAnimating() {
            return currentRenderer.isAnimating();
        }

        /**
         * Switches between the use of the center renderer or the corner
         * renderer.
         *
         * @param use
         *            If true, the center renderer ist used, otherwise, the
         *            corner renderer is used
         */
        public void useCenterRenderer(boolean use) {
            if (use) {
                if (cornerRenderer.isAnimating()) {
                    cornerRenderer.stopAnimation();
                    centerRenderer.startAnimation();
                }
                currentRenderer = centerRenderer;
            } else {
                if (centerRenderer.isAnimating()) {
                    centerRenderer.stopAnimation();
                    cornerRenderer.startAnimation();
                }
                currentRenderer = cornerRenderer;
            }
        }
    }

}
