package org.helioviewer.jhv.app;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.state.ViewState;
import org.helioviewer.jhv.layers.ImageLayers;

import org.json.JSONObject;

public final class Commands {

    @FunctionalInterface
    public interface Completion {
        Completion NONE = (success, message, output) -> {};

        void finished(boolean success, String message, @Nullable String output);
    }

    public static void setViewState(JSONObject input) {
        boolean wasRefresh = ViewState.isRefresh();
        ViewState.applyMode(ViewState.readModeJson(input));
        if (!wasRefresh && ViewState.isRefresh())
            ImageLayers.refreshLayersSpan();
    }

    public static void setRefresh(boolean refresh) {
        boolean wasRefresh = ViewState.isRefresh();
        ViewState.setRefresh(refresh);
        if (!wasRefresh && refresh)
            ImageLayers.refreshLayersSpan();
    }

    private Commands() {}
}
