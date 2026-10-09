package org.helioviewer.jhv.layers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.image.ClipSet;
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.image.ImageProcessingSettings;
import org.helioviewer.jhv.image.lut.LUT;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.source.ResolutionSet;
import org.helioviewer.jhv.source.Source;
import org.helioviewer.jhv.time.JHVTime;

// Timeline selection and synthesis, without sources that decode.
public final class FramesTest {

    // A source whose frames become displayable as the test says.
    private static final class Stub implements Source {
        final boolean[] ready;

        Stub(int count) {
            ready = new boolean[count];
        }

        @Override public int frames() { return ready.length; }
        @Override public String xml(int frame) { return null; }
        @Override public ResolutionSet levels(int frame) { throw new UnsupportedOperationException(); }
        @Override public LUT lut() { return null; }
        @Override public ClipSet clipSet() { return null; }
        @Override public boolean usesFITSParameters() { return false; }
        @Override public ImageBuffer decode(int frame, int level, ImageFilter filter, ImageProcessingSettings.FITSParameters fits, ClipSet.Range clip) {
            throw new UnsupportedOperationException();
        }
        @Override public boolean displayable(int frame) { return ready[frame]; }
        @Override public void close() {}
    }

    public static void main(String[] arguments) throws Exception {
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        AppInit.loadSpice(); // synthetic frames carry an Earth viewpoint
        selection();
        synthesis();
        System.out.println("PASS: nearest selectable frame by time, equal synthetic endpoints give one frame");
    }

    private static void selection() {
        long base = 1_800_000_000L; // 2027, inside the bundled ephemerides
        long[] seconds = {base, base + 100, base + 101, base + 102};
        Stub source = new Stub(seconds.length);
        List<Frames.Frame> list = new ArrayList<>();
        for (int i = 0; i < seconds.length; i++) {
            JHVTime time = new JHVTime(seconds[i] * 1000);
            list.add(new Frames.Frame(time, null, Frames.EMPTY_METAXML, source, i)); // metadata unused
        }
        Frames frames = new Frames(list);
        try {
            source.ready[0] = true;
            source.ready[3] = true;
            if (!frames.select(new JHVTime((base + 100) * 1000)) || frames.current() != 3)
                throw new AssertionError("Requested 100 s with 0 s and 102 s available selected frame " + frames.current());
            if (frames.select(new JHVTime((base + 100) * 1000 + 500)) || frames.current() != 3)
                throw new AssertionError("Unchanged nearest selectable frame reported a move");
            source.ready[3] = false;
            if (!frames.select(new JHVTime((base + 101) * 1000)) || frames.current() != 0)
                throw new AssertionError("Only frame 0 available but selected " + frames.current());
            source.ready[0] = false;
            if (frames.select(new JHVTime((base + 50) * 1000)) || frames.current() != 0)
                throw new AssertionError("Nothing available must keep the selection");
            source.ready[1] = true;
            if (!frames.select(frames.requested()) || frames.current() != 1)
                throw new AssertionError("A frame becoming available was not selected on retry");
            long t = base * 1000;
            if (frames.nearest(new JHVTime(t + 101_400)).milli != t + 101_000 || frames.lower(new JHVTime(t + 101_000)).milli != t + 100_000
                    || frames.higher(new JHVTime(t + 101_000)).milli != t + 102_000 || frames.higher(new JHVTime(t + 102_000)).milli != t + 102_000)
                throw new AssertionError("Time navigation over all frames is wrong");
        } finally {
            frames.close();
        }
    }

    private static void synthesis() {
        long t = 1_800_000_000_000L;
        Frames same = Frames.synthetic(t, t, 60);
        if (same.size() != 1 || same.first().milli != t)
            throw new AssertionError("Equal endpoints did not give one frame");
        same.close();
        Frames span = Frames.synthetic(t, t + 150_000, 60);
        if (span.size() != 4 || span.last().milli != t + 150_000 || span.time(1).milli != t + 60_000)
            throw new AssertionError("Synthetic span has the wrong frames: " + span.size());
        span.close();
    }

    private FramesTest() {}
}
