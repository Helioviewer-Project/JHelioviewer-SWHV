package org.helioviewer.jhv.gui;

import java.awt.EventQueue;
import java.io.File;

import javax.annotation.Nullable;

import org.helioviewer.jhv.gui.dialog.TextDialog;
import org.helioviewer.jhv.io.Directories;

public final class CompletionNotifications {

    public static void recordingFinished(boolean success, String message, @Nullable String output) {
        if (success)
            EventQueue.invokeLater(() -> showRecordingFinished(output));
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
        String ready = " is ready in " + urify(Directories.EXPORTS.getPath()) + '.';
        String recording = output == null || output.contains("%") ? "Recording" : "Recording " + urify(output);
        show(recording + ready);
    }

    private CompletionNotifications() {}
}
