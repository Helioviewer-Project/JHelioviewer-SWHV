package org.helioviewer.jhv.export;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.image.MappedImageFactory;

class FFmpegExporter implements MovieExporter {

    private static final List<String> ffmpeg = List.of(new File(JHVGlobals.libCacheDir, "ffmpeg").getAbsolutePath());

    private VideoFormat format = VideoFormat.H264;
    private String path;
    private int w;
    private int h;
    private int fps;

    private File tempFile;

    @Override
    public void open(String prefix, int _w, int _h, int _fps) throws Exception {
        try {
            format = VideoFormat.valueOf(Settings.getProperty("video.format"));
        } catch (Exception ignore) {
        }
        path = prefix + format.extension;

        w = _w;
        h = _h;
        fps = _fps;
        tempFile = File.createTempFile("dump", null, JHVGlobals.exportCacheDir);
        tempFile.deleteOnExit();
    }

    @Override
    public void encode(BufferedImage image) throws Exception {
        ByteBuffer data = MappedImageFactory.getByteBuffer(image).flip().limit(w * h * 3);
        try (FileChannel channel = new FileOutputStream(tempFile, true).getChannel()) {
            channel.write(data);
        }
    }

    @Override
    public void close() throws Exception {
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
        } finally {
            tempFile.delete();
        }
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public int getHeight() {
        return h;
    }

}
