package org.helioviewer.jhv.layers;

import java.awt.EventQueue;
import java.io.InterruptedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.io.Load;
import org.helioviewer.jhv.thread.EDTQueue;

// The layer's load-finished future, headless, through the real registry.
// Arguments: a FITS file, a file that is not an image.
public final class LayerLoadTest {

    public static void main(String[] arguments) throws Exception {
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        AppInit.loadSpice();
        URI fits = Path.of(arguments[0]).toUri(), bogus = Path.of(arguments[1]).toUri();

        // A load that succeeds completes true, and a wait asked afterward completes at once.
        ImageLayer layer = EDTQueue.invokeAndWait(ImageLayer::create);
        CompletableFuture<Boolean> finished = EDTQueue.invokeAndWait(() -> {
            layer.load(List.of(fits));
            return layer.whenLoadFinished();
        });
        if (!finished.get(60, TimeUnit.SECONDS))
            throw new AssertionError("Successful load did not complete true");
        if (!EDTQueue.invokeAndWait(layer::whenLoadFinished).get(5, TimeUnit.SECONDS))
            throw new AssertionError("Finished layer did not complete at once");
        System.out.println("PASS: a successful load completes true, a finished layer at once");

        // A failed first load removes the layer and completes false.
        ImageLayer failing = EDTQueue.invokeAndWait(ImageLayer::create);
        finished = EDTQueue.invokeAndWait(() -> {
            failing.load(List.of(bogus));
            return failing.whenLoadFinished();
        });
        if (finished.get(60, TimeUnit.SECONDS))
            throw new AssertionError("Failed first load completed true");
        if (EDTQueue.invokeAndWait(() -> Layers.getImageLayers().contains(failing)))
            throw new AssertionError("Failed first load did not remove the layer");
        System.out.println("PASS: a failed first load completes false after removal");

        // Removing a layer with a pending wait completes it false.
        ImageLayer removed = EDTQueue.invokeAndWait(ImageLayer::create);
        finished = EDTQueue.invokeAndWait(() -> {
            removed.load(List.of(fits));
            return removed.whenLoadFinished();
        });
        EventQueue.invokeAndWait(() -> Layers.remove(removed));
        if (finished.get(60, TimeUnit.SECONDS))
            throw new AssertionError("Removal did not complete the pending wait false");
        System.out.println("PASS: removal completes a pending wait false");

        // Load.image completes at the same readiness: true, false for a removed layer, failing with no files.
        if (!Load.image(fits).get(60, TimeUnit.SECONDS))
            throw new AssertionError("Load.image of a FITS file did not complete true");
        if (Load.image(bogus).get(60, TimeUnit.SECONDS))
            throw new AssertionError("Load.image of an unreadable file did not complete false");
        if (Load.image(List.of()).get(60, TimeUnit.SECONDS))
            throw new AssertionError("Load.image of nothing completed true");
        Path empty = Files.createTempDirectory("jhv-empty");
        try {
            if (Load.image(empty.toUri()).get(60, TimeUnit.SECONDS))
                throw new AssertionError("Load.image of an empty directory completed true");
            if (!Load.image(Path.of(arguments[0]).getParent().toUri()).get(120, TimeUnit.SECONDS))
                throw new AssertionError("Load.image of the fixture directory did not complete true");
        } finally {
            Files.delete(empty);
        }
        System.out.println("PASS: Load.image completes at readiness, false after removal, nothing or an empty directory, true for a directory");

        // An interrupted load thread stops enumerating a directory.
        Thread.currentThread().interrupt();
        try {
            FileUtils.resolveURIList(List.of(Path.of(arguments[0]).getParent().toUri()));
            throw new AssertionError("Interrupted enumeration completed");
        } catch (InterruptedIOException expected) {
        } finally {
            Thread.interrupted();
        }
        System.out.println("PASS: directory enumeration stops when interrupted");

        EventQueue.invokeAndWait(() -> { // the layers Load.image added are not needed any more
            for (ImageLayer l : List.copyOf(Layers.getImageLayers()))
                Layers.remove(l);
        });
        System.exit(0);
    }

    private LayerLoadTest() {}
}
