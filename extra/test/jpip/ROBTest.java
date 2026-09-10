package org.helioviewer.jhv.view.j2k;

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
import org.helioviewer.jhv.view.j2k.jpip.JPIPResponse;
import org.helioviewer.jhv.view.j2k.jpip.JPIPSocket;

public final class ROBTest {

    public static void main(String[] arguments) throws Exception {
        System.load(arguments[0]);
        KakaduMessageSystem.startKduMessageSystem();
        URI uri = URI.create(arguments[1]);
        String[] normal = retrieve(uri, false);
        String[] limited = retrieve(uri, true);
        if (!Arrays.equals(normal, limited))
            throw new AssertionError("Metadata or decoded pixels differ with response byte limit");
        System.out.println("PASS: independent normal and 16 KB sessions produce identical metadata and pixels");
    }

    private static String[] retrieve(URI uri, boolean limited) throws Exception {
        J2KSource.Remote source = new J2KSource.Remote();
        JPIPSocket socket = null;
        try {
            socket = new JPIPSocket(uri, source.cache());
            socket.init(source.cache());
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
                String query = JPIPSocket.createLayerQuery(0, size.width() + "," + size.height());
                if (limited)
                    query = query.replaceAll("len=[0-9]+", "len=16384");
                int requests = 0;
                JPIPResponse response;
                do {
                    if (++requests > 256)
                        throw new AssertionError("No completion after 256 requests at level " + level);
                    response = socket.request(query, source.cache(), 0);
                } while (!response.isResponseComplete());
                if (limited && requests < 2)
                    throw new AssertionError("Small response limit did not exercise continuation");
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
                System.out.println((limited ? "16 KB" : "Normal") + " level=" + level + " requests=" + requests
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
