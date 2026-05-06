package org.helioviewer.jhv.gui;

import java.awt.EventQueue;
import java.io.File;

import javax.annotation.Nullable;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.gui.dialogs.TextDialog;

public final class CompletionNotifications {

    private static boolean initialized;

    private static final Commands.CompletionListener completionListener = new Commands.CompletionListener() {
        @Override
        public void loadStateFinished(@Nullable Commands.OperationContext context, boolean success, String message) {}

        @Override
        public void recordingFinished(@Nullable Commands.OperationContext context, boolean success, String message, @Nullable String output) {
            if (success)
                showRecordingFinished(output);
        }
    };

    private CompletionNotifications() {}

    static void init() {
        if (initialized)
            return;

        initialized = true;
        Commands.addCompletionListener(completionListener);
    }

    public static void fileReady(String path) {
        EventQueue.invokeLater(() -> show("File " + urify(path) + " is ready."));
    }

    private static String urify(String uri) {
        String openURI = new File(uri).toURI().toString();
        return "<a href=\"" + openURI + "\">" + uri + "</a>";
    }

    private static void show(String text) {
        new TextDialog("Ready", text, false).showDialog();
    }

    private static void showRecordingFinished(@Nullable String output) {
        String ready = " is ready in " + urify(JHVDirectory.EXPORTS.getPath()) + '.';
        String recording = output == null || output.contains("%") ? "Recording" : "Recording " + urify(output);
        show(recording + ready);
    }

}
