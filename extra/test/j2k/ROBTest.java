package org.helioviewer.jhv.view.j2k;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;

import org.helioviewer.jhv.view.j2k.jpip.JPIPCacheManager;
import org.helioviewer.jhv.view.j2k.jpip.JPIPSocket;

// A single-frame JPIP image at levels 2 and 0, from the server and from the reopened disk cache.
// Arguments: Kakadu library, bridge library, JPIP URI.
public final class ROBTest {

    public static void main(String[] arguments) throws Exception {
        System.load(arguments[0]);
        System.load(arguments[1]);
        URI uri = URI.create(arguments[2]);
        // Reopen the production cache without exposing its shutdown method to application callers.
        Method close = JPIPCacheManager.class.getDeclaredMethod("close");
        close.setAccessible(true);
        JPIPCacheManager.init();
        try {
            String[] normal = retrieve(uri, false);
            close.invoke(null);
            JPIPCacheManager.init();
            String[] cached = retrieve(uri, true);
            if (!Arrays.equals(normal, cached))
                throw new AssertionError("Metadata or decoded pixels differ after disk-cache restoration");
            JPIPCacheManager.clear();
            if (JPIPCacheManager.get("image", 5) != null)
                throw new AssertionError("Cache clear retained the image");
            System.out.println("PASS: server and reopened disk-cache sessions produce identical metadata and pixels");
        } finally {
            close.invoke(null);
        }
    }

    private static String[] retrieve(URI uri, boolean cached) throws Exception {
        J2KSource source = new J2KSource(null);
        J2KReader reader = null;
        try {
            reader = new J2KReader(uri, source);
            reader.setCacheKey(new String[]{"image"});
            if (source.frames() != 1)
                throw new AssertionError("Expected the single-frame ROB fixture");
            Field socketField = J2KReader.class.getDeclaredField("socket");
            socketField.setAccessible(true);
            if (cached)
                ((JPIPSocket) socketField.get(reader)).abort(); // Only metadata and the first coarse level come from the server.
            String xml = source.xml(0);
            if (xml == null || xml.isBlank())
                throw new AssertionError("Missing image metadata");

            Method readFrames = J2KReader.class.getDeclaredMethod("readFrames", J2KParams.Read.class, ResolutionSet.Level.class, boolean.class);
            readFrames.setAccessible(true);
            String[] result = {xml, null, null};
            int index = 1;
            for (int level : new int[]{2, 0}) {
                ResolutionSet.Level size = source.resolutionSet(0).getLevel(level);
                // The reopened cache holds level 0, which also serves level 2.
                if (!cached && (source.getFrameStatus(0, level) || JPIPCacheManager.get("image", level) != null))
                    throw new AssertionError("Level " + level + " complete or cached before it was fetched");
                J2KParams.Decode decode = new J2KParams.Decode(0, level);
                if (!(boolean) readFrames.invoke(reader, new J2KParams.Read(null, decode, false), size, false))
                    throw new AssertionError("Fetch interrupted unexpectedly");
                if (!source.getFrameStatus(0, level) || (level > 0 && !cached && source.getFrameStatus(0, level - 1)))
                    throw new AssertionError("Wrong completion after fetching level " + level);
                JPIPCacheManager.Entry entry = JPIPCacheManager.get("image", level);
                if (entry == null || entry.level() != (cached ? 0 : level))
                    throw new AssertionError("Missing persisted level " + level);

                ByteBuffer pixels;
                try (J2KNative.Decode job = source.beginDecode(0, level)) {
                    pixels = ByteBuffer.allocateDirect(size.width() * size.height());
                    J2KSource.decode(job, 0, 0, size.width(), size.height(), pixels);
                }
                MessageDigest hash = MessageDigest.getInstance("SHA-256");
                hash.update(pixels);
                result[index++] = HexFormat.of().formatHex(hash.digest());
            }
            return result;
        } finally {
            if (reader != null)
                reader.stop();
            source.close();
        }
    }

    private ROBTest() {}
}
