package org.helioviewer.jhv.layers;

import java.awt.EventQueue;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.image.ClipSet;
import org.helioviewer.jhv.image.DecodedImage;
import org.helioviewer.jhv.image.ImageBufferCache;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.image.ImageProcessingSettings;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.Region;
import org.helioviewer.jhv.movie.ExportMovie;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.source.ResolutionSet;
import org.helioviewer.jhv.source.Source;
import org.helioviewer.jhv.thread.LatestWorker;

// Decodes a layer's current frame on a worker and delivers it on the EDT. EDT only.
final class FrameDecoder {

    interface Target {
        // Borrowed for this call. Retain the decoded image if it will be kept afterward.
        void handleData(ImageData data);
    }

    // The timeline frame identifies its source; no Source object is in the key.
    private record Key(int installation, int frame, int level, ImageFilter.Type filter,
                       @Nullable ImageProcessingSettings.FITSParameters fits, @Nullable ClipSet.Range clip) implements ImageBufferCache.Key {
        @Override
        public Object owner() {
            return installation;
        }
    }

    private final LatestWorker<DecodedImage> executor = new LatestWorker<>("Frame-Decoder");
    private final ImageProcessingSettings settings;
    private final Target target;

    private int installation = -1;
    private Position viewpoint;
    private double pixFactor;

    FrameDecoder(ImageProcessingSettings _settings, Target _target) {
        settings = _settings;
        target = _target;
    }

    // Outstanding work for the previous timeline is released instead of cached or delivered.
    void reset(int _installation) {
        executor.invalidate();
        installation = _installation;
        viewpoint = null;
    }

    void dispose() {
        installation = -1;
        executor.dispose();
    }

    void decode(Frames frames, Position _viewpoint, double _pixFactor) {
        viewpoint = _viewpoint;
        pixFactor = _pixFactor;
        redecode(frames);
    }

    // The current frame at the last viewpoint and scale.
    void redecode(Frames frames) {
        if (viewpoint == null || frames.serial() != installation)
            return;
        int index = frames.current();
        Frames.Frame frame = frames.get(index);
        Source source = frame.source();
        if (source == null)
            return;

        int wanted = level(frame, pixFactor);
        // The whole finest complete level while a remote frame lacks the wanted one, read before requesting.
        int available = source.levels(frame.index()).getCompleteLevel(wanted).level();
        source.request(frame.index(), wanted, !Player.isPlaying());

        ImageProcessingSettings.FITSParameters fits = source.usesFITSParameters() ? settings.fitsParameters() : null;
        ClipSet.Range clip = fits == null ? null : fits.clipRange(frames.clipSet());
        Key key = new Key(installation, index, available, settings.getFilter(), fits, clip);
        Position vp = viewpoint;

        DecodedImage image = ImageBufferCache.get(key);
        if (image != null) {
            // Mark running decodes stale before publishing this cached result.
            executor.invalidate();
            deliver(key, frame, vp, image);
            return;
        }
        executor.submit(key, () -> decodeImage(source, frame, key), new LatestWorker.Callback<>() {
            @Override
            public void onSuccess(DecodedImage result, boolean fresh) {
                if (!isCurrent(key)) {
                    result.release();
                    return;
                }
                ImageBufferCache.put(key, result);
                // Keep valid superseded results in the cache, without delivering them.
                if (fresh)
                    deliver(key, frame, vp, result);
            }

            @Override
            public void onFailure(Throwable t, boolean fresh) {
                if (key.installation == installation)
                    Log.errorStack(t);
            }
        });
    }

    // Level 0 while recording; otherwise the coarsest level not smaller than the display needs.
    static int level(Frames.Frame frame, double pixFactor) {
        if (ExportMovie.isRecording())
            return 0;
        MetaData m = frame.metaData();
        int reqWidth = (int) (m.getPhysicalRegion().width * pixFactor + .5);
        int reqHeight = (int) (m.getPhysicalRegion().height * pixFactor + .5);
        return frame.source().levels(frame.index()).getNextLevel(reqWidth, reqHeight).level();
    }

    // Settings unchanged since the request, for the same timeline.
    private boolean isCurrent(Key key) {
        return key.installation == installation && key.filter == settings.getFilter()
                && (key.fits == null || key.fits.equals(settings.fitsParameters()));
    }

    private void deliver(Key key, Frames.Frame frame, Position vp, DecodedImage image) {
        ImageData data = new ImageData(image, frame.metaData(), vp);
        image.retain(); // The queued delivery owns a reference until its callback ends.
        EventQueue.invokeLater(() -> {
            try {
                if (isCurrent(key))
                    target.handleData(data);
            } finally {
                image.release();
            }
        });
    }

    // Runs on the decode worker; the decoder owns solar geometry and image filtering.
    private static DecodedImage decodeImage(Source source, Frames.Frame frame, Key key) throws Exception {
        MetaData m = frame.metaData();
        ResolutionSet.Level full = source.levels(frame.index()).getLevel(0);
        Region region = m.imageToRegion(full.width(), full.height());
        ImageFilter filter = ImageFilter.of(key.filter, region, m);
        return new DecodedImage(source.decode(frame.index(), key.level, filter, key.fits, key.clip), region);
    }

}
