package org.helioviewer.jhv.gui;

import java.util.AbstractList;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.gui.components.QualitySpinner;
import org.helioviewer.jhv.internal_plugins.selectedLayer.SelectedLayerPanel;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.opengl.GLInfo;
import org.helioviewer.viewmodel.factory.BufferedImageViewFactory;
import org.helioviewer.viewmodel.factory.GLViewFactory;
import org.helioviewer.viewmodel.factory.ViewFactory;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.FilterView;
import org.helioviewer.viewmodel.view.ImageInfoView;
import org.helioviewer.viewmodel.view.LayeredView;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.ModifiableInnerViewView;
import org.helioviewer.viewmodel.view.MovieView;
import org.helioviewer.viewmodel.view.StandardSolarRotationTrackingView;
import org.helioviewer.viewmodel.view.SubimageDataView;
import org.helioviewer.viewmodel.view.SynchronizeView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.jp2view.JP2View;
import org.helioviewer.viewmodel.view.opengl.GLLayeredView;
import org.helioviewer.viewmodel.view.opengl.GLOverlayView;
import org.helioviewer.viewmodelplugin.controller.PluginManager;
import org.helioviewer.viewmodelplugin.filter.FilterContainer;
import org.helioviewer.viewmodelplugin.filter.FilterTab;
import org.helioviewer.viewmodelplugin.filter.FilterTabDescriptor;
import org.helioviewer.viewmodelplugin.filter.FilterTabDescriptor.Type;
import org.helioviewer.viewmodelplugin.filter.FilterTabList;
import org.helioviewer.viewmodelplugin.filter.FilterTabPanelManager;
import org.helioviewer.viewmodelplugin.overlay.OverlayContainer;
import org.helioviewer.viewmodelplugin.overlay.OverlayControlComponent;
import org.helioviewer.viewmodelplugin.overlay.OverlayControlComponentManager;
import org.helioviewer.viewmodelplugin.overlay.OverlayPanel;

/**
 * This class handles the buildup of a view chain.
 *
 * The class follows the factory pattern. It is responsible to build up a view
 * chain in the right structure. It adds the corresponding views depending on
 * the chosen mode (software mode or OpenGL mode).
 *
 * @author Markus Langenberg
 * @author Stephan Pagel
 *
 */
public class ViewchainFactory {

    private ViewFactory viewFactory;

    /**
     * Default constructor.
     *
     * This constructor calls the constructor {@link #ViewchainFactory(boolean)}
     * with the argument false. This results in that a BufferedImage view chain
     * (Software Mode) will be created.
     */
    public ViewchainFactory() {
        this(false);
    }

    /**
     * Constructor which creates the view chain factory depending on the chosen
     * and available mode.
     *
     * Depending on the passed parameter value the constructor creates the used
     * view factory as a OpenGL view factory (OpenGL mode) or a BufferedImage
     * view factory (Software mode). If OpenGL mode is not available the
     * parameter will be ignored and the software mode will be used.
     *
     * @param useBufferedImageViewChain
     *            indicates if the software mode has to be used.
     */
    public ViewchainFactory(boolean useBufferedImageViewChain) {
        if (!useBufferedImageViewChain && GLInfo.glIsEnabled()) {
            viewFactory = new GLViewFactory();
        } else {
            viewFactory = new BufferedImageViewFactory();
        }

    }

    public ViewchainFactory(ViewFactory viewFactory) {
        this.viewFactory = viewFactory;
    }

    /**
     * This method returns the internal used view factory.
     *
     * @return used view factory.
     */
    public ViewFactory getUsedViewFactory() {
        return viewFactory;
    }

    /**
     * This method creates the main view chain depending on the selected mode.
     *
     * The mode was defined when the instance of the class was created.
     * <p>
     * If the passed parameter value is null a new main view chain will be
     * created. If the passed parameter value represents a
     * {@link org.helioviewer.viewmodel.view.ComponentView} instance (the
     * topmost view of the view chain) the existing view chain will be
     * transfered with all its settings to a new view chain.
     *
     * @param currentMainImagePanelView
     *            instance of the ComponentView which is the topmost view of the
     *            view chain which has to be transfered to the new one.
     * @param keepSource
     *            If true, the source view chain stays untouched, otherwise it
     *            will be unusable afterwards
     * @return instance of the ComponentView of the new created view chain.
     */
    public ComponentView createViewchainMain(ComponentView currentMainImagePanelView, boolean keepSource) {
        if (currentMainImagePanelView == null) {
            return createNewViewchainMain();
        } else {
            return createViewchainFromExistingViewchain(currentMainImagePanelView, null, keepSource);
        }
    }

    /**
     * This method creates the overview view chain always in software mode and
     * if a main view chain already exists.
     *
     * If there is no main view chain (represented by there topmost view, the
     * ComponentView) than the method did nothing and returns a null value. The
     * instance is needed to make synchronization available.
     * <p>
     * If a ComponentView of an existing view chain was passed to the method the
     * whole view chain will be transfered with all its settings to a new view
     * chain. If no ComponentView instance was passed the method creates a new
     * overview view chain.
     *
     * @param mainImagePanelView
     *            the ComponentView instance of the main view chain which acts
     *            as the observed view chain.
     * @param currentOverviewImagePanelView
     *            the ComponentView instance (or null) of an existing overview
     *            view chain.
     * @param keepSource
     *            If true, the source view chain stays untouched, otherwise it
     *            will be unusable afterwards
     * @return the ComponentView of the new overview view chain or null if it
     *         could not be created.
     */
    public ComponentView createViewchainOverview(ComponentView mainImagePanelView, ComponentView currentOverviewImagePanelView, boolean keepSource) {
        if (mainImagePanelView == null)
            return null;

        if (currentOverviewImagePanelView == null) {
            return createNewViewchainOverview(mainImagePanelView);
        } else {
            return createViewchainFromExistingViewchain(currentOverviewImagePanelView, mainImagePanelView, keepSource);
        }
    }

    /**
     * Adds a new ImageInfoView to the main view chain and creates the
     * corresponding user interface components.
     *
     * The ImageInfoViews are always the undermost views in the view chain so
     * they will be added as a new layer to the LayeredView. For this reason
     * this method creates also the complete sub view chain (including the
     * needed filter views) and add it to the LayeredView.
     * <p>
     * If one of the passed parameter values is null nothing will happen.
     *
     * @param newLayer
     *            new ImageInfoView for which to create the sub view chain as a
     *            new layer.
     * @param attachToViewchain
     *            a view of the main view chain which is equal or over the
     *            LayeredView.
     */
    public void addLayerToViewchainMain(ImageInfoView newLayer, View attachToViewchain) {
        if (newLayer == null || attachToViewchain == null)
            return;

        // Fetch LayeredView
        LayeredView layeredView = attachToViewchain.getAdapter(LayeredView.class);

        synchronized (layeredView) {
            // wait until image is loaded
            while (newLayer.getAdapter(SubimageDataView.class).getSubimageData() == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

            // Create list which manages all filter tabs
            FilterTabList tabList = new FilterTabList();

            // Adjust Panel for basic functions
            JPanel adjustPanel = new JPanel();
            adjustPanel.setLayout(new BoxLayout(adjustPanel, BoxLayout.PAGE_AXIS));

            FilterTabPanelManager compactPanelManager = new FilterTabPanelManager();
            tabList.add(new FilterTab(FilterTabDescriptor.Type.COMPACT_FILTER, "Color", compactPanelManager));

            // If JP2View, add QualitySlider
            if (newLayer instanceof JP2View) {
                compactPanelManager.add(new QualitySpinner((JP2View) newLayer));
            }

            compactPanelManager.add(new SelectedLayerPanel(newLayer));

            // Add filter to view chain
            AbstractList<FilterContainer> filterContainerList = PluginManager.getSingletonInstance().getFilterContainers(true);
            View nextView = newLayer;

            for (int i = filterContainerList.size() - 1; i >= 0; i--) {
                FilterContainer container = filterContainerList.get(i);

                FilterView filterView = viewFactory.createNewView(FilterView.class);
                filterView.setView(nextView);

                container.installFilter(filterView, tabList);

                if (filterView.getFilter() != null) {
                    nextView = filterView;
                } else {
                    filterView.setView(null);
                }
            }

            // Add layer
            layeredView.addLayer(nextView);

            // Add JTabbedPane
            JTabbedPane tabbedPane = new JTabbedPane() {

                private static final long serialVersionUID = 1L;

                /**
                 * Override the setEnabled method in order to keep the
                 * containing components' enabledState synced with the
                 * enabledState of this component.
                 */
                @Override
                public void setEnabled(boolean enabled) {
                    for (int i = 0; i < this.getTabCount(); i++) {
                        this.getComponentAt(i).setEnabled(enabled);
                    }
                }
            };

            // tabbedPane.setTabPlacement(JTabbedPane.BOTTOM);

            for (int i = 0; i < tabList.size(); i++) {
                FilterTab filterTab = tabList.get(i);
                if (filterTab.getType() == Type.COMPACT_FILTER) {
                    tabbedPane.add(filterTab.getTitle(), filterTab.getPaneManager().createCompactPanel());
                }
            }
            for (int i = 0; i < tabList.size(); i++) {
                FilterTab filterTab = tabList.get(i);
                if (filterTab.getType() != Type.COMPACT_FILTER) {
                    tabbedPane.add(filterTab.getTitle(), filterTab.getPaneManager().createPanel());
                }
            }

            ImageInfoView imageInfoView = nextView.getAdapter(ImageInfoView.class);

            // If MoviewView, add MoviePanel
            if (newLayer instanceof MovieView) {
                MoviePanel moviePanel = new MoviePanel((MovieView) newLayer);
                if (LayersModel.getSingletonInstance().isTimed(newLayer)) {
                    LayersModel.getSingletonInstance().setLink(newLayer, true);
                }

                ImageViewerGui.getSingletonInstance().getMoviePanelContainer().addLayer(imageInfoView, moviePanel);
            } else {
                MoviePanel moviePanel = new MoviePanel(null);
                ImageViewerGui.getSingletonInstance().getMoviePanelContainer().addLayer(imageInfoView, moviePanel);
            }

            ImageViewerGui.getSingletonInstance().getFilterPanelContainer().addLayer(imageInfoView, tabbedPane);
            ImageViewerGui.getSingletonInstance().getLeftContentPane().expand(ImageViewerGui.getSingletonInstance().getFilterPanelContainer());
            LayersModel.getSingletonInstance().setActiveLayer(imageInfoView);
        }
    }

    /**
     * Updates all enabled overlay views in a given view chain. The method
     * removes all existing from the view chain and after this it adds all
     * enabled overlays.
     *
     * @param componentView
     *            the ComponentView instance of the view chain where to update
     *            the overlay views.
     */
    public void updateOverlayViewsInViewchainMain(GLOverlayView overlayView) {
        // /////////////
        // Remove all existing overlays
        // /////////////

        // remove overlay control components from GUI
        ImageViewerGui.getSingletonInstance().getLeftContentPane().remove(OverlayPanel.class);

        // /////////////
        // Add all enabled overlays to view chain
        // /////////////

        // add overlay view to view chain
        // S. Meier, must be fixed, use just one overlayView with more then one
        // overlaysPlugin
        AbstractList<OverlayContainer> overlayContainerList = PluginManager.getSingletonInstance().getOverlayContainers(true);
        OverlayControlComponentManager manager = new OverlayControlComponentManager();

        // View nextView = componentView.getView();

        for (int i = overlayContainerList.size() - 1; i >= 0; i--) {
            OverlayContainer container = overlayContainerList.get(i);
            container.installOverlay(overlayView, manager);

        }

        // add overlay control components to view chain
        for (OverlayControlComponent comp : manager.getAllControlComponents()) {
            ImageViewerGui.getSingletonInstance().getLeftContentPane().add(comp.getTitle(), comp.getOverlayPanel(), false);
        }
    }

    protected View getViewNextToOverlayView(ComponentView componentView) {
        return componentView.getView();
    }

    protected ModifiableInnerViewView getViewBeforeToOverlayView(ComponentView componentView) {
        return componentView;
    }

    /**
     * Creates a new main view chain with the minimal needed views.
     *
     * @return a instance of a ComponentView which is the topmost view of the
     *         new chain.
     */
    protected ComponentView createNewViewchainMain() {
        // Layered View
        LayeredView layeredView = viewFactory.createNewView(LayeredView.class);

        // Solar Rotation Tracking View
        StandardSolarRotationTrackingView trackingView = viewFactory.createNewView(StandardSolarRotationTrackingView.class);
        trackingView.setView(layeredView);

        // Component View
        ComponentView componentView = viewFactory.createNewView(ComponentView.class);
        componentView.setView(trackingView);

        // add Overlays
        // updateOverlayViewsInViewchainMain(componentView);

        return componentView;
    }

    /**
     * Creates a new overview view chain with the minimum needed views.
     *
     * @param mainImagePanelView
     *            the topmost view of the view chain which is the observed view
     *            chain.
     * @return a instance of a ComponentView which is the topmost view of the
     *         new chain.
     */
    protected ComponentView createNewViewchainOverview(ComponentView mainImagePanelView) {
        // Always use BufferedImageViewFactory
        ViewFactory viewFactory = new BufferedImageViewFactory();

        // Layered View
        LayeredView layeredView = viewFactory.createNewView(LayeredView.class);

        // Synchronize View
        SynchronizeView synchronizeView = viewFactory.createNewView(SynchronizeView.class);
        synchronizeView.setView(layeredView);
        synchronizeView.setObservedView(mainImagePanelView);

        // Component View
        ComponentView componentView = viewFactory.createNewView(ComponentView.class);
        componentView.setView(synchronizeView);

        return componentView;
    }

    /**
     * Builds up a new view chain on the basis of an existing one depending on
     * the selected mode.
     *
     * The new view chain will have the the same structure as the original one.
     *
     * @param sourceImagePanelView
     *            The topmost view of the original view chain.
     * @param mainImagePanelView
     *            The topmost view of the main view chain.
     * @return The topmost view of the new created view chain.
     */
    public ComponentView createViewchainFromExistingViewchain(ComponentView sourceImagePanelView, ComponentView mainImagePanelView, boolean keepSource) {
        reinsertSolarRotationTrackingView(sourceImagePanelView);

        ComponentView newView = viewFactory.createViewFromSource(sourceImagePanelView, keepSource);
        createViewchainFromExistingViewchain(sourceImagePanelView.getView(), newView, mainImagePanelView, keepSource);

        LayeredView layeredView = sourceImagePanelView.getAdapter(LayeredView.class);
        GLLayeredView newLayeredView = (GLLayeredView) newView.getAdapter(LayeredView.class);
        for (int i = 0; i < layeredView.getNumLayers(); i++) {
            if (!layeredView.isVisible(layeredView.getLayer(i))) {
                newLayeredView.toggleVisibility(newLayeredView.getLayer(i));
            }
        }

        return newView;
    }

    private void reinsertSolarRotationTrackingView(ComponentView sourceImagePanelView) {
        ViewFactory viewFactory = getUsedViewFactory();
        // find view before Layered View
        ModifiableInnerViewView layeredViewPredecessor = sourceImagePanelView;

        while (!(layeredViewPredecessor.getView() instanceof LayeredView)) {
            if (layeredViewPredecessor.getView() instanceof ModifiableInnerViewView) {
                layeredViewPredecessor = (ModifiableInnerViewView) layeredViewPredecessor.getView();
            } else {
                return;
            }
        }
        StandardSolarRotationTrackingView solarRotationView = viewFactory.createNewView(StandardSolarRotationTrackingView.class);
        solarRotationView.setView(layeredViewPredecessor.getView());
        layeredViewPredecessor.setView(solarRotationView);
    }

    /**
     * Method goes recursively through a view chain and creates a new one with
     * the same structure.
     *
     * @param sourceView
     *            current view of the view chain which has to be transfered to
     *            the new view chain.
     * @param targetView
     *            equivalent view in the new view chain to the source view.
     * @param mainImagePanelView
     *            topmost view of the main view chain.
     */
    protected void createViewchainFromExistingViewchain(View sourceView, View targetView, ComponentView mainImagePanelView, boolean keepSource) {
        View newView;
        // if sourceView is an ImageInfoView (such as JHVJP2View), remove all
        // ViewListeners from the
        // old view chain and use the sourceView as input for the new view chain
        if (sourceView instanceof ImageInfoView && !keepSource) {
            AbstractList<ViewListener> listeners = sourceView.getAllViewListeners();
            for (int i = listeners.size() - 1; i >= 0; i--) {
                if (listeners.get(i) instanceof View) {
                    sourceView.removeViewListener(listeners.get(i));
                }
            }
            newView = sourceView;
            // otherwise create new view - if null, skip it
        } else if (sourceView instanceof GLOverlayView) {
            newView = new GLOverlayView();
            ((GLOverlayView) newView).setOverlays(((GLOverlayView) sourceView).getOverlays());
        } else {
            newView = viewFactory.createViewFromSource(sourceView, keepSource);

            if (newView == null) {
                if (sourceView instanceof ModifiableInnerViewView) {
                    createViewchainFromExistingViewchain(((ModifiableInnerViewView) sourceView).getView(), targetView, mainImagePanelView, keepSource);
                }
                return;
            }
        }

        // if newView is a SynchronizeView, connect it to the main view chain
        // and copy the mapping
        if (newView instanceof SynchronizeView) {
            ((SynchronizeView) newView).setObservedView(mainImagePanelView);
            ((SynchronizeView) newView).setViewMapping(((SynchronizeView) sourceView).getViewMapping());
        }

        // insert newView in new view chain
        if (targetView instanceof ModifiableInnerViewView) {
            ((ModifiableInnerViewView) targetView).setView(newView);
        } else if (targetView instanceof LayeredView) {

            if (sourceView instanceof ModifiableInnerViewView) {
                createViewchainFromExistingViewchain(((ModifiableInnerViewView) sourceView).getView(), newView, mainImagePanelView, keepSource);
            }

            ((LayeredView) targetView).addLayer(newView);
            return;
        }

        // go on with the next view
        if (sourceView instanceof ModifiableInnerViewView) {
            createViewchainFromExistingViewchain(((ModifiableInnerViewView) sourceView).getView(), newView, mainImagePanelView, keepSource);
        } else if (sourceView instanceof LayeredView) {
            LayeredView layeredView = (LayeredView) sourceView;
            for (int i = 0; i < layeredView.getNumLayers(); i++) {
                if (layeredView.getLayer(i) != null) {
                    createViewchainFromExistingViewchain(layeredView.getLayer(i), newView, mainImagePanelView, keepSource);
                }
            }
        }
    }

}