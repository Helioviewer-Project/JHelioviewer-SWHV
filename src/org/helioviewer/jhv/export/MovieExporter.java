package org.helioviewer.jhv.export;

import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.imagedata.nio.MappedImageFactory;
import org.helioviewer.jhv.imagedata.nio.NIOImageFactory;
import org.helioviewer.jhv.io.FileUtils;
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
                writeFully(channel, mainData);
            }
            if (eveData != null)
                writeFully(channel, eveData);
        } catch (Exception e) {
            tempFile.delete();
            tempFile = null;
            throw e;
        } finally {
            NIOImageFactory.free(scaled);
        }
    }

    private static void writeFully(FileChannel channel, ByteBuffer data) throws Exception {
        while (data.hasRemaining()) {
            channel.write(data);
        }
    }

    private static final List<String> formatVideo = List.of(
            "-pix_fmt", "yuv420p",
            "-vf", "colorspace=iall=bt709:itrc=iec61966-2-1:irange=pc:all=bt709:trc=bt709:range=pc:fast=0",
            "-color_primaries", "bt709",
            "-color_trc", "bt709",
            "-colorspace", "bt709",
            "-color_range", "2",
            "-tune", "animation",
            "-movflags", "+faststart",
            "-movflags", "+write_colr" // may be useless
    );
    private static final List<String> formatImage = List.of(
            "-vf", "scale=in_range=pc:out_range=pc"
    );

    void close() throws Exception {
        if (tempFile == null) // unlikely reach here on encode error
            return;

        List<String> input = List.of(
                "-hide_banner",
                "-f", "rawvideo",
                "-pix_fmt", "bgr24",
                "-r", format == VideoFormat.PNG ? "1" : String.valueOf(fps),
                "-s", w + "x" + h,
                "-i", tempFile.getPath()
        );
        String outPath = prefix + format.extension;

        List<String> command = new ArrayList<>(ffmpeg);
        command.addAll(input);
        command.addAll(format.settings);
        command.addAll(format == VideoFormat.PNG ? formatImage : formatVideo);
        command.add("-y");
        command.add(outPath);

        try {
            ProcessBuilder builder = new ProcessBuilder()
                    .directory(JHVGlobals.exportCacheDir)
                    .redirectError(File.createTempFile("fferr", null, JHVGlobals.exportCacheDir))
                    .redirectOutput(File.createTempFile("ffout", null, JHVGlobals.exportCacheDir))
                    .command(command);

            int exitCode = builder.start().waitFor();
            if (exitCode != 0)
                throw new Exception("FFmpeg exit code " + exitCode);

            String ready = " is ready in " + JHVGlobals.urify(JHVDirectory.EXPORTS.getPath()) + '.';
            if (format == VideoFormat.PNG) // don't know name and how many
                EventQueue.invokeLater(() -> JHVGlobals.displayNotificationEx("Recording" + ready));
            else
                EventQueue.invokeLater(() -> JHVGlobals.displayNotificationEx("Recording " + JHVGlobals.urify(outPath) + ready));
        } catch (Exception e) {
            DirectoryStream.Filter<Path> filter = p -> p.toString().startsWith(prefix);
            FileUtils.deleteFromDir(Path.of(JHVDirectory.EXPORTS.getPath()), filter);
            throw e;
        } finally {
            tempFile.delete();
            tempFile = null;
        }
    }

}
