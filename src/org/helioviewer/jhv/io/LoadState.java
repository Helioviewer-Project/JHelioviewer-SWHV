package org.helioviewer.jhv.io;

import java.net.URI;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.Message;
import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.app.state.State;
import org.helioviewer.jhv.thread.Task;

import org.json.JSONObject;

class LoadState {

    static void submit(@Nullable Commands.OperationContext context, @Nonnull URI uri) {
        Task.submit(uri.toString(), new LoadStateURI(uri), result -> onSuccess(context, result), (logContext, t) -> onFailure(context, logContext, t));
    }

    static void submit(@Nullable Commands.OperationContext context, @Nonnull String json) {
        Task.submit("state", new LoadStateString(json), result -> onSuccess(context, result), (logContext, t) -> onFailure(context, logContext, t));
    }

    private static void onSuccess(@Nullable Commands.OperationContext context, JSONObject state) {
        State.load(context, state);
    }

    private static void onFailure(@Nullable Commands.OperationContext context, String logContext, Throwable error) {
        String errorMessage = "Error getting the data";
        Log.error(logContext, error);
        Message.err(errorMessage, error.getMessage());
        String message = error.getMessage() == null || error.getMessage().isBlank() ? errorMessage : error.getMessage();
        Commands.notifyLoadStateFinished(context, false, message);
    }

    private record LoadStateURI(URI uri) implements Callable<JSONObject> {
        @Override
        public JSONObject call() throws Exception {
            return JSONUtils.get(uri).getJSONObject("org.helioviewer.jhv.state");
        }
    }

    private record LoadStateString(String json) implements Callable<JSONObject> {
        @Override
        public JSONObject call() {
            return new JSONObject(json).getJSONObject("org.helioviewer.jhv.state");
        }
    }

}
