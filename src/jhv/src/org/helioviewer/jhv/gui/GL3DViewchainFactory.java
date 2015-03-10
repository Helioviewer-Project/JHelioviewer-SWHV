package org.helioviewer.jhv.gui;

import java.util.AbstractList;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.gui.components.QualitySpinner;
import org.helioviewer.jhv.internal_plugins.selectedLayer.SelectedLayerPanel;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.factory.GL3DViewFactory;
import org.helioviewer.viewmodel.factory.ViewFactory;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.FilterView;
import org.helioviewer.viewmodel.view.ImageInfoView;
import org.helioviewer.viewmodel.view.LayeredView;
import org.helioviewer.viewmodel.view.ModifiableInnerViewView;
import org.helioviewer.viewmodel.view.MovieView;
import org.helioviewer.viewmodel.view.OverlayView;
import org.helioviewer.viewmodel.view.SubimageDataView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.jp2view.JP2View;
import org.helioviewer.viewmodel.view.opengl.GL3DCameraView;
import org.helioviewer.viewmodel.view.opengl.GL3DSceneGraphView;
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

//import org.helioviewer.viewmodel.view.opengl.GL3DViewportView;

public class GL3DViewchainFactory {

    private final ViewFactory viewFactory;
    public static GL3DSceneGraphView currentSceneGraph;

    public GL3DViewchainFactory() {
        this.viewFactory = new GL3DViewFactory();
    }

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
        return createNewViewchainMain();
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

    /**
     * Creates a new main view chain with the minimal needed views.
     *
     * @return a instance of a ComponentView which is the topmost view of the
     *         new chain.
     */

    protected ComponentView createNewViewchainMain() {
        ViewFactory viewFactory = getUsedViewFactory();

        LayeredView layeredView = viewFactory.createNewView(LayeredView.class);

        OverlayView overlayView = viewFactory.createNewView(OverlayView.class);
        overlayView.setView(layeredView);

        GL3DCameraView cameraView = viewFactory.createNewView(GL3DCameraView.class);
        cameraView.setView(overlayView);

        //GL3DViewportView viewportView = viewFactory.createNewView(GL3DViewportView.class);
        //viewportView.setView(cameraView);

        GL3DSceneGraphView sceneGraph = new GL3DSceneGraphView();
        currentSceneGraph = sceneGraph;
        sceneGraph.setView(/* viewportView */cameraView);
        sceneGraph.setGLOverlayView((GLOverlayView) overlayView);

        ComponentView componentView = viewFactory.createNewView(ComponentView.class);
        componentView.setView(sceneGraph);

        // add Overlays (OverlayView added before LayeredView and after GL3DCameraView)
        updateOverlayViewsInViewchainMain((GLOverlayView) overlayView);

        return componentView;
    }

    protected View getViewNextToOverlayView(ComponentView componentView) {
        return componentView.getAdapter(LayeredView.class);
    }

    protected ModifiableInnerViewView getViewBeforeToOverlayView(ComponentView componentView) {
        return componentView.getAdapter(GL3DCameraView.class);
        // return componentView;
    }

}