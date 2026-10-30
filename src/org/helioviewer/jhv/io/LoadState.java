package org.helioviewer.jhv.io;

import java.net.URI;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.app.Message;
import org.helioviewer.jhv.app.state.State;
import org.helioviewer.jhv.thread.Task;

import org.json.JSONObject;

class LoadState {

    static void submit(Commands.Completion completion, @Nonnull URI uri) {
        Task.submitBackground(uri.toString(), () -> JSONUtils.get(uri).getJSONObject("org.helioviewer.jhv.state"),
                result -> State.load(completion, result), (logContext, t) -> onFailure(completion, logContext, t));
    }

    static void submit(Commands.Completion completion, @Nonnull String json) {
        Task.submitBackground("state", () -> new JSONObject(json).getJSONObject("org.helioviewer.jhv.state"),
                result -> State.load(completion, result), (logContext, t) -> onFailure(completion, logContext, t));
    }

    private static void onFailure(Commands.Completion completion, String logContext, Throwable error) {
        String errorMessage = "Error getting the data";
        Log.error(logContext, error);
        Message.err(errorMessage, error.getMessage());
        String message = error.getMessage() == null || error.getMessage().isBlank() ? errorMessage : error.getMessage();
        completion.finished(false, message, null);
    }

}
