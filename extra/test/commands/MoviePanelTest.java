package org.helioviewer.jhv.ui;

import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.app.state.ViewState;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.movie.ExportFormat;
import org.helioviewer.jhv.movie.ExportMovie;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.thread.UiThread;

// The transport screen and its domain state, with no toolkit and no window.
public final class MoviePanelTest {

    private static final int SLIDER = 1;
    private static final int PREVIOUS = 4;
    private static final int RECORD = 10;
    private static final int OPTIONS = 15;
    private static final int FORMAT = 17;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.io.tmpdir", System.getProperty("user.home"));
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        AppInit.loadSpice();
        DisplayController.setRenderRequestHandler(_ -> {});
        List<String> recordings = new ArrayList<>();
        MoviePanel movie = UiThread.postAndWait(() -> new MoviePanel((success, message, output) -> recordings.add(success + " " + message + " " + output)));
        try {
            UiThread.postAndWait(() -> {
                checkRange(movie);
                checkOptions(movie);
                checkCommands(movie, recordings);
                checkNotifications(movie);
            });
        } finally {
            UiThread.postAndWait(() -> {
                Player.pause();
                movie.dispose();
            });
        }
        System.out.println("MoviePanelTest passed");
    }

    private static void checkRange(MoviePanel movie) {
        Player.setPlaybackRange(10, 20);
        int[] notifications = {0};
        Player.addPlaybackRangeListener(() -> notifications[0]++);
        notifications[0] = 0; // Registration immediately reports the existing range.
        movie.setRange(0, SLIDER, 30, 40);
        check(Player.getPlaybackFirstFrame() == 30 && Player.getPlaybackLastFrame() == 40, "Range move right was clipped");
        check(notifications[0] == 1, "Range observer saw intermediate endpoints");
        movie.setRange(0, SLIDER, 0, 10);
        check(Player.getPlaybackFirstFrame() == 0 && Player.getPlaybackLastFrame() == 10, "Range move left was clipped");
        check(notifications[0] == 2, "Range move left was not atomic");
        Player.setPlaybackRange(0, Player.getMaximumFrameNumber());
    }

    private static void checkOptions(MoviePanel movie) {
        movie.command(0, OPTIONS, Panel.Command.CLICK);
        ViewState.setRecordingMode(ViewState.RecordingMode.LOOP);
        check(enabled(movie.build(), Panel.Kind.COMBO, FORMAT), "Format must be enabled for movies");
        movie.choice(0, FORMAT, ExportFormat.H265.ordinal());
        check(ExportFormat.fromSettings() == ExportFormat.H265, "Format choice was not saved");
        ViewState.setRecordingMode(ViewState.RecordingMode.SHOT);
        check(!enabled(movie.build(), Panel.Kind.COMBO, FORMAT), "Screenshots are saved as PNG");
        ViewState.setRecordingMode(ViewState.RecordingMode.LOOP);
        movie.command(0, OPTIONS, Panel.Command.CLICK);
        check(!has(movie.build(), Panel.Kind.COMBO, FORMAT), "Options must collapse");
    }

    // The wheel steps through playback, keys pause it, and a recording reports to the frontend's own completion.
    private static void checkCommands(MoviePanel movie, List<String> recordings) {
        Commands.seekFrame(5);
        int frame = Layers.getActiveImageLayer().frames().current();
        Commands.play();
        movie.command(0, SLIDER, Panel.Command.INCREASE);
        check(Player.isPlaying() && Layers.getActiveImageLayer().frames().current() == frame + 1, "Wheel must step without pausing");
        movie.command(0, SLIDER, Panel.Command.DECREASE);
        check(Player.isPlaying() && Layers.getActiveImageLayer().frames().current() == frame, "Wheel must step back without pausing");
        movie.command(0, SLIDER, Panel.Command.SCRUB_START);
        check(!Player.isPlaying(), "A scrub must pause");
        movie.command(0, SLIDER, Panel.Command.SCRUB_END);
        check(Player.isPlaying(), "A scrub must resume what it paused");
        movie.command(0, SLIDER, Panel.Command.STEP_FORWARD);
        check(!Player.isPlaying() && Layers.getActiveImageLayer().frames().current() == frame + 1, "Keys must pause and step");
        movie.command(0, SLIDER, Panel.Command.SCRUB_START);
        movie.command(0, SLIDER, Panel.Command.SCRUB_END);
        check(!Player.isPlaying(), "A scrub must not start paused playback");
        movie.command(0, PREVIOUS, Panel.Command.CLICK);
        check(Layers.getActiveImageLayer().frames().current() == frame, "Previous must step back");
        movie.setAdvanced(true);
        check(has(movie.build(), Panel.Kind.COMBO, FORMAT), "setAdvanced must show the options");
        movie.setAdvanced(false);
        // Without a canvas or a timeline the recording fails before it starts, into the completion the frontend gave.
        ExportMovie.setMainCanvasVisible(false);
        movie.command(0, RECORD, Panel.Command.CLICK);
        ExportMovie.setMainCanvasVisible(true);
        check(recordings.equals(List.of("false The timeline is not available. null")), "A failed recording must report to the injected completion: " + recordings);
    }

    // Every source of what the screen shows reaches its one view, until the owner disposes it.
    private static void checkNotifications(MoviePanel movie) {
        int[] notifications = {0};
        movie.listen(() -> notifications[0]++);
        List<Runnable> sources = List.of(
                () -> movie.setAdvanced(false),
                () -> Player.setPlaybackRange(1, Player.getMaximumFrameNumber()),
                () -> Player.setPlaybackRange(0, Player.getMaximumFrameNumber()),
                () -> Player.setFrame(1),
                () -> Player.setFrame(2),
                Commands::play,
                Commands::pause,
                () -> ViewState.setPlaybackAdvanceMode(Player.AdvanceMode.Stop),
                () -> ViewState.setPlaybackAdvanceMode(Player.AdvanceMode.Loop),
                () -> ViewState.setRecordingMode(ViewState.RecordingMode.FREE),
                () -> ViewState.setRecordingMode(ViewState.RecordingMode.LOOP),
                () -> Layers.fireLayerUpdated(Layers.getViewpointLayer()));
        for (int i = 0; i < sources.size(); i++) {
            int before = notifications[0];
            sources.get(i).run();
            check(notifications[0] > before, "Source " + i + " did not reach the view");
        }
        movie.dispose();
        int disposed = notifications[0];
        for (Runnable source : sources)
            source.run();
        check(notifications[0] == disposed, "A disposed screen kept reporting");
    }

    private static int find(Panel panel, Panel.Kind kind, int id) {
        for (int i = 0; i < panel.size(); i++) {
            if (panel.kind(i) == kind && panel.value(i, Panel.IntField.ID) == id)
                return i;
        }
        return -1;
    }

    private static boolean has(Panel panel, Panel.Kind kind, int id) {
        return find(panel, kind, id) >= 0;
    }

    private static boolean enabled(Panel panel, Panel.Kind kind, int id) {
        return panel.has(find(panel, kind, id), Panel.ENABLED);
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }

}
