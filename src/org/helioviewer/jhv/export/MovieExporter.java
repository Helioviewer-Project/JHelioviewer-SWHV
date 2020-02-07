package org.helioviewer.jhv.export;

import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.image.MappedImageFactory;
import org.helioviewer.jhv.base.image.NIOImageFactory;
import org.helioviewer.jhv.time.TimeUtils;

class MovieExporter {

    private static final List<String> ffmpeg = List.of(new File(JHVGlobals.libCacheDir, "ffmpeg").getAbsolutePath());

    private final String prefix;
    private final VideoFormat format;
    private final int w;
    private final int h;
    private final int fps;

    private File tempFile;

    MovieExporter(VideoFormat _format, int _w, int _h, int _fps) {
        prefix = JHVDirectory.EXPORTS.getPath() + "JHV_" + TimeUtils.formatFilename(System.currentTimeMillis());
        format = _format;
        w = _w;
        h = _h;
        fps = _fps;
    }

    void encode(BufferedImage mainImage, BufferedImage eveImage, int movieLinePosition) throws Exception {
        if (tempFile == null) {
            tempFile = File.createTempFile("dump", null, JHVGlobals.exportCacheDir);
            tempFile.deleteOnExit();
        }

        int mainH = mainImage.getHeight();
        BufferedImage scaled = null;
        ByteBuffer eveData = null;
        if (eveImage != null) {
            scaled = ExportUtils.scaleImage(eveImage, w, h - mainH, movieLinePosition);
            eveData = NIOImageFactory.getByteBuffer(scaled).flip().limit(3 * w * scaled.getHeight());
        }

        ByteBuffer mainData = MappedImageFactory.getByteBuffer(mainImage);
        try (FileChannel channel = FileChannel.open(tempFile.toPath(), StandardOpenOption.APPEND)) {
            for (int j = mainH - 1; j >= 0; j--) { // write image flipped
                int pos = 3 * w * j;
                mainData.position(pos);
                mainData.limit(pos + 3 * w);
                channel.write(mainData);
            }
            if (eveData != null)
                channel.write(eveData);
        } catch (Exception e) {
            tempFile.delete();
            tempFile = null;
            throw e;
        } finally {
            NIOImageFactory.free(scaled);
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
        String outPath = prefix + format.extension;
        List<String> output = List.of(
                "-pix_fmt", "yuv420p",
                "-tune", "animation",
                "-movflags", "+faststart",
                "-y", outPath
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

            if (format == VideoFormat.PNG) // don't know name and how many
                EventQueue.invokeLater(() -> JHVGlobals.displayNotificationEx("Recording is ready in ", JHVDirectory.EXPORTS.getPath(), "."));
            else
                EventQueue.invokeLater(() -> JHVGlobals.displayNotification(outPath));
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

}
