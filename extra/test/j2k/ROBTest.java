package org.helioviewer.jhv.view.j2k;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;

import org.helioviewer.jhv.image.DecodedImage;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.Region;
import org.helioviewer.jhv.view.j2k.jpip.JPIPCache;
import org.helioviewer.jhv.view.j2k.jpip.JPIPCacheManager;
import org.helioviewer.jhv.view.j2k.jpip.JPIPResponse;
import org.helioviewer.jhv.view.j2k.jpip.JPIPSocket;

public final class ROBTest {

    private enum Mode { NORMAL, LIMITED, CACHED }

    public static void main(String[] arguments) throws Exception {
        System.load(arguments[0]);
        KakaduMessageSystem.startKduMessageSystem();
        URI uri = URI.create(arguments[1]);
        // Reopen the production cache without exposing its shutdown method to application callers.
        Method close = JPIPCacheManager.class.getDeclaredMethod("close");
        close.setAccessible(true);
        JPIPCacheManager.init();
        try {
            String[] normal = retrieve(uri, Mode.NORMAL);
            String[] limited = retrieve(uri, Mode.LIMITED);
            if (!Arrays.equals(normal, limited))
                throw new AssertionError("Metadata or decoded pixels differ with response byte limit");
            close.invoke(null);
            JPIPCacheManager.init();
            String[] cached = retrieve(uri, Mode.CACHED);
            if (!Arrays.equals(normal, cached))
                throw new AssertionError("Metadata or decoded pixels differ after disk-cache restoration");
            JPIPCacheManager.clear();
            if (JPIPCacheManager.get("level0", 0) != null)
                throw new AssertionError("Cache clear retained the image");
            System.out.println("PASS: normal, 16 KB and reopened disk-cache sessions produce identical metadata and pixels");
        } finally {
            close.invoke(null);
        }
    }

    private static String[] retrieve(URI uri, Mode mode) throws Exception {
        J2KSource.Remote source = new J2KSource.Remote();
        JPIPSocket socket = null;
        try {
            socket = new JPIPSocket(uri, source.cache());
            socket.init(source.cache());
            if (mode == Mode.CACHED)
                socket.close(); // Only metadata and the initial 64x64 image come from the server.
            source.open();
            if (source.maxFrame() != 0)
                throw new AssertionError("Expected the single-frame ROB fixture");
            String[] xml = new String[1];
            source.extractMetaData(xml);
            if (xml[0] == null || xml[0].isBlank())
                throw new AssertionError("Missing image metadata");
            String[] result = new String[3];
            result[0] = xml[0];
            int resultIndex = 1;
            // Decode needs only this coordinate conversion when no image filter is applied.
            MetaData metadata = (MetaData) Proxy.newProxyInstance(ROBTest.class.getClassLoader(),
                    new Class<?>[]{MetaData.class}, (proxy, method, values) -> {
                        if (method.getName().equals("roiToRegion"))
                            return Region.DEFAULT;
                        throw new AssertionError("Unexpected metadata call: " + method.getName());
                    });
            for (int level : new int[]{2, 0}) {
                ResolutionSet resolution = source.resolutionSet(0);
                ResolutionSet.Level size = resolution.getLevel(level);
                int requests = 0;
                if (mode == Mode.CACHED) {
                    JPIPCacheManager.Entry entry = JPIPCacheManager.get("level" + level, level);
                    if (entry == null || entry.level() != level)
                        throw new AssertionError("Missing persisted level " + level);
                    source.cache().put(0, entry.stream());
                } else {
                    String dimensions = size.width() + "," + size.height();
                    boolean complete;
                    do {
                        if (++requests > 256)
                            throw new AssertionError("No completion after 256 requests at level " + level);
                        if (mode == Mode.LIMITED) {
                            // Exercise small response limits without exposing raw queries in the public API.
                            Method request = JPIPSocket.class.getDeclaredMethod("requestInitialization", String.class, JPIPCache.class);
                            request.setAccessible(true);
                            String query = "stream=0&fsiz=" + dimensions + ",closest&rsiz=" + dimensions + "&roff=0,0&len=16384";
                            complete = ((JPIPResponse) request.invoke(socket, query, source.cache())).isResponseComplete();
                        } else {
                            socket.sendFrame(0, dimensions);
                            complete = socket.receiveFrame(source.cache()).complete();
                        }
                    } while (!complete);
                    if (mode == Mode.LIMITED && requests < 2)
                        throw new AssertionError("Small response limit did not exercise continuation");
                    if (mode == Mode.NORMAL) {
                        JPIPCacheManager.store("level" + level, level, source.cache(), 0);
                        // One key exercises replacement as the image reaches a finer resolution.
                        JPIPCacheManager.store("upgrade", level, source.cache(), 0);
                        JPIPCacheManager.Entry upgraded = JPIPCacheManager.get("upgrade", 2);
                        if (upgraded == null || upgraded.level() != level)
                            throw new AssertionError("Cache resolution upgrade failed");
                        if (level == 2 && JPIPCacheManager.get("level2", 0) != null)
                            throw new AssertionError("Coarse data satisfies a full-resolution request");
                    }
                }
                source.setFrameComplete(0, level);
                DecodedImage image = new J2KDecoder(source,
                        new J2KParams.Decode(0, size.subImage(), level, 1), resolution.numComps,
                        ImageFilter.Type.None, metadata, size.factorX(), size.factorY()).call();
                if (image == null || image.imageBuffer().width != size.width()
                        || image.imageBuffer().height != size.height())
                    throw new AssertionError("Unexpected decoded image dimensions");
                ByteBuffer pixels = (ByteBuffer) image.imageBuffer().buffer;
                MessageDigest hash = MessageDigest.getInstance("SHA-256");
                hash.update(pixels.duplicate());
                result[resultIndex++] = HexFormat.of().formatHex(hash.digest());
                System.out.println(mode + " level=" + level + " requests=" + requests
                        + " pixels=" + size.width() + "x" + size.height() + " sha256=" + result[resultIndex - 1]);
            }
            return result;
        } finally {
            try {
                if (socket != null)
                    socket.close();
            } finally {
                source.destroy();
            }
        }
    }
}
