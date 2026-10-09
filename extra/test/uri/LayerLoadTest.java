package org.helioviewer.jhv.layers;

import java.awt.EventQueue;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.io.Directories;
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
        if (finished.get(60, TimeUnit.SECONDS) || EDTQueue.invokeAndWait(() -> Layers.getImageLayers().contains(failing)))
            throw new AssertionError("Failed first load did not remove the layer and complete false");
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

        EventQueue.invokeAndWait(() -> Layers.remove(layer));
        System.exit(0);
    }

    private LayerLoadTest() {}
}
