package org.helioviewer.jhv.export;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.image.MappedImageFactory;
import org.helioviewer.jhv.time.TimeUtils;

class MovieExporter {

    private static final List<String> ffmpeg = List.of(new File(JHVGlobals.libCacheDir, "ffmpeg").getAbsolutePath());

    private final String prefix;
    private final String path;
    private final VideoFormat format;
    private final int w;
    private final int h;
    private final int fps;

    private File tempFile;

    MovieExporter(VideoFormat _format, int _w, int _h, int _fps) {
        prefix = JHVDirectory.EXPORTS.getPath() + "JHV_" + TimeUtils.formatFilename(System.currentTimeMillis());
        format = _format;
        path = prefix + format.extension;
        w = _w;
        h = _h;
        fps = _fps;
    }

    void encode(BufferedImage image) throws Exception {
        if (tempFile == null) {
            tempFile = File.createTempFile("dump", null, JHVGlobals.exportCacheDir);
            tempFile.deleteOnExit();
        }

        try {
            ByteBuffer data = MappedImageFactory.getByteBuffer(image).flip().limit(w * h * 3);
            try (FileChannel channel = new FileOutputStream(tempFile, true).getChannel()) {
                channel.write(data);
            }
        } catch (Exception e) {
            tempFile.delete();
            tempFile = null;
            throw e;
        }
    }

    void close() throws Exception {
        List<String> input = List.of(
                "-f", "rawvideo",
                "-pix_fmt", "bgr24",
                "-r", format == VideoFormat.PNG ? "1" : String.valueOf(fps),
                "-s", w + "x" + h,
                "-i", tempFile.getPath()
        );
        List<String> output = List.of(
                "-pix_fmt", "yuv420p",
                "-tune", "animation",
                "-movflags", "+faststart",
                "-y", path
        );
        ArrayList<String> command = new ArrayList<>(ffmpeg);
        command.addAll(input);
        command.addAll(format.settings);
        command.addAll(output);

        try {
            ProcessBuilder builder = new ProcessBuilder()
                    .directory(JHVGlobals.exportCacheDir)
                    .redirectError(File.createTempFile("fferr", null, JHVGlobals.exportCacheDir))
                    .redirectOutput(File.createTempFile("ffout", null, JHVGlobals.exportCacheDir))
                    .command(command);

            int exitCode = builder.start().waitFor();
            if (exitCode != 0)
                throw new Exception("FFmpeg exit code " + exitCode);
        } catch (Exception e) {
            FileFilter filter = p -> p.getName().startsWith(prefix);
            File[] toDelete = JHVDirectory.EXPORTS.getFile().listFiles(filter);
            if (toDelete != null) {
                for (File f : toDelete)
                    f.delete();
            }
            throw e;
        } finally {
            tempFile.delete();
            tempFile = null;
        }
    }

    String getPath() {
        return path;
    }

    int getHeight() {
        return h;
    }

}
