package org.helioviewer.jhv.opengl.angle;

import java.awt.EventQueue;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.Platform;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.threads.Tasks;

public final class AngleWarmup {

    private static boolean started;

    private AngleWarmup() {
    }

    public static void start() {
        if (started)
            return;
        started = true;

        Tasks.submit("angle-warmup", () -> {
            if (Platform.isMacOS())
                MacAngleBridge.prewarm();
            AngleRenderer.prewarm();
            return null;
        }, ignored -> EventQueue.invokeLater(AngleWarmup::attachAndRender), (context, error) -> {
            Log.warn("ANGLE warmup failed", error);
            EventQueue.invokeLater(AngleWarmup::attachAndRender);
        });
    }

    private static void attachAndRender() {
        JHVFrame.attachRenderCanvas();
        JHVFrame.requestRender();
    }

}
