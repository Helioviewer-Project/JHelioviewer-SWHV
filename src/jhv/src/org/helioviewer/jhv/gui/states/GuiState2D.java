package org.helioviewer.jhv.gui.states;

import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ViewListenerDistributor;
import org.helioviewer.jhv.gui.ViewchainFactory;
import org.helioviewer.jhv.gui.components.SideContentPane;
import org.helioviewer.jhv.gui.components.TopToolBar;
import org.helioviewer.jhv.gui.components.statusplugins.RenderModeStatusPanel;
import org.helioviewer.jhv.gui.controller.MainImagePanelMousePanController;
import org.helioviewer.jhv.gui.interfaces.ImagePanelInputController;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.SynchronizeView;

public class GuiState2D implements State {

    protected TopToolBar topToolBar;

    protected ComponentView mainComponentView;
    protected ComponentView overviewComponentView;

    protected RenderModeStatusPanel renderModeStatus;

    private final ViewchainFactory viewchainFactory;

    public GuiState2D() {
        this(new ViewchainFactory());
    }

    public GuiState2D(ViewchainFactory viewchainFactory) {
        this.viewchainFactory = viewchainFactory;
    }

    @Override
    public void addStateSpecificComponents(SideContentPane sideContentPane) {

    }

    @Override
    public void removeStateSpecificComponents(SideContentPane sideContentPane) {

    }

    @Override
    public void activate() {
    }

    @Override
    public void deactivate() {
        getMainComponentView().deactivate();
    }

    @Override
    public boolean createViewChains() {
        Log.info("Start creating view chains");

        boolean firstTime = (mainComponentView == null);

        // Create main view chain
        ViewchainFactory mainFactory = this.viewchainFactory;
        mainComponentView = mainFactory.createViewchainMain(mainComponentView, false);

        // create overview view chain
        if (firstTime) {
            ViewchainFactory overviewFactory = new ViewchainFactory(true);
            overviewComponentView = overviewFactory.createViewchainOverview(mainComponentView, overviewComponentView, false);
        } else {
            overviewComponentView.getAdapter(SynchronizeView.class).setObservedView(mainComponentView);
        }

        ViewListenerDistributor.getSingletonInstance().setView(mainComponentView);
        // imageSelectorPanel.setLayeredView(mainComponentView.getAdapter(LayeredView.class));

        return firstTime;
    }

    @Override
    public boolean recreateViewChains(State previousState) {
        // Inhibit Event distribution during recreation
        ViewListenerDistributor.getSingletonInstance().setView(null);

        Displayer.getSingletonInstance().removeListeners();

        if (previousState == null || previousState.getMainComponentView() == null) {
            return this.createViewChains();
        } else {
            mainComponentView = getViewchainFactory().createViewchainFromExistingViewchain(previousState.getMainComponentView(), this.mainComponentView, false);
            overviewComponentView = previousState.getOverviewComponentView();
            if (mainComponentView != null) {
                if (overviewComponentView != null) {
                    overviewComponentView.getAdapter(SynchronizeView.class).setObservedView(mainComponentView);
                }
                ViewListenerDistributor.getSingletonInstance().setView(mainComponentView);
            }
            return false;
        }
    }

    @Override
    public ViewStateEnum getType() {
        return ViewStateEnum.View2D;
    }

    @Override
    public TopToolBar getTopToolBar() {
        if (topToolBar == null)
            topToolBar = new TopToolBar();

        return topToolBar;
    }

    @Override
    public ComponentView getMainComponentView() {
        return mainComponentView;
    }

    @Override
    public ComponentView getOverviewComponentView() {
        return overviewComponentView;
    }

    public RenderModeStatusPanel getRenderModeStatus() {
        return renderModeStatus;
    }

    @Override
    public ViewchainFactory getViewchainFactory() {
        return this.viewchainFactory;
    }

    @Override
    public ImagePanelInputController getDefaultInputController() {
        return new MainImagePanelMousePanController();
    }

    @Override
    public boolean isOverviewPanelInteractionEnabled() {
        return true;
    }

}
