# JHV execution paths

This map describes the implemented paths, with source links for further tracing.
Use the [production-area test mapping](../extra/test/README.md#production-areas-to-tests)
to select checks. Update this map in the same patch when entry points, ownership
or thread boundaries change. Keep detailed algorithms in their source or focused
documents rather than expanding this into a class inventory.

## Other subsystem entry points

Use these entry points for areas outside the detailed paths below. Suite names
refer to the [test guide](../extra/test/README.md#production-areas-to-tests).

| Subsystem | Start tracing here | Responsibility and boundaries | Documentation and checks |
| --- | --- | --- | --- |
| Events and SWEK | [`SWEKPlugin.install`](../src/org/helioviewer/jhv/plugins/swek/SWEKPlugin.java), [`SWEKSession.requestForInterval`](../src/org/helioviewer/jhv/event/SWEKSession.java) | The plugin creates the session, canvas `SWEKLayer` and `EventTimelineLayer`. The session coordinates filters, downloads and `EventDatabase` storage, publishing downloaded events on `UiThread`. `installGUI` separately binds the Swing interaction callbacks. | [`SWEK.json`](../resources/settings/SWEK.json) defines the catalog. The `event` suite covers data/session/layer behavior; verify interactive selection and overlay appearance in the GUI. |
| 3D models (glTF) | [`Load.model`](../src/org/helioviewer/jhv/io/Load.java), [`ModelLayer`](../src/org/helioviewer/jhv/layers/ModelLayer.java), [`AssimpModelLoader.load`](../src/org/helioviewer/jhv/opengl/model/AssimpModelLoader.java) | `Load.model` constructs the layer in background work and registers it on `UiThread`. The loader converts the imported Assimp scene into `ModelScene` and releases the Assimp import. `ModelLayer` delegates GL initialization, drawing and disposal to [`GLSLModel`](../src/org/helioviewer/jhv/opengl/GLSLModel.java). | [3D data interface](jhv-heliocentric-3d-data-interface.md); `model` for loading/metadata and `opengl` for rendering. |
| SPICE and viewpoints | [`AppInit.loadSpice`](../src/org/helioviewer/jhv/app/AppInit.java), [`PositionLoad.submit`](../src/org/helioviewer/jhv/astronomy/PositionLoad.java), [`DisplayController.setViewpointUpdate`](../src/org/helioviewer/jhv/display/DisplayController.java) | Startup loads bundled and user kernels through `Spice`. Position loading tries local SPICE for internal objects, then uses the remote position service when local data is unavailable or objects are external. [`UpdateViewpoint`](../src/org/helioviewer/jhv/astronomy/UpdateViewpoint.java) selects observer/fixed-distance or interpolated position behavior; `DisplayController` applies it to the camera. | See [startup](#startup-and-thread-ownership) and [playback](#playback-decoding-and-presentation). The current test runner has no dedicated ephemeris/viewpoint suite; check the affected object, time range and viewpoint mode explicitly. |
| Canvas input and annotations | [`AwtInputAdapter`](../src/org/helioviewer/jhv/gui/AwtInputAdapter.java), [`InputController`](../src/org/helioviewer/jhv/input/InputController.java), [`Interaction`](../src/org/helioviewer/jhv/display/interaction/Interaction.java), [`Annotations`](../src/org/helioviewer/jhv/annotation/Annotations.java) | The adapter converts canvas coordinates to device pixels. The controller routes input to camera/annotation interactions and mouse listeners. `Annotations` owns pending/completed shapes, selection, JSON state and GL drawing. Timeline charts handle their own mouse events in logical component coordinates. | See [chart drawing](#timeline-data-and-chart-drawing) and [saved state](#ui-commands-and-saved-state). No dedicated input suite is registered; verify gestures, focus, display scaling and annotation save/restore in the GUI. |

## Startup and thread ownership

[`JHelioviewer.main`](../src/org/helioviewer/jhv/JHelioviewer.java) establishes US
locale and UTC, initializes directories, logging, sources and settings, and loads
SPICE. It then selects the Swing or headless host:

- Swing startup runs on the EDT. [`MainFrame.prepare`](../src/org/helioviewer/jhv/gui/MainFrame.java)
  starts background ANGLE warmup, then `attachAndRender` attaches the canvas and
  connects `DisplayController` to `AngleCanvas.requestRender` on the EDT.
- Headless startup installs `QueueUiThread` on the main thread and connects
  `DisplayController` to `AnglePbuffer.requestRender`. Headless rendering still
  requires ANGLE and a working graphics backend.
- Both hosts load plugins and submit [`AppInit.init`](../src/org/helioviewer/jhv/app/AppInit.java)
  as background work. Its success callback loads source catalogs and processes
  command-line input.

[`UiThread`](../src/org/helioviewer/jhv/thread/UiThread.java) is the application-state
thread boundary: the default implementation uses the EDT, while headless startup
installs the queue implementation. `post` always enqueues. `postAndWait` runs
inline when already on that thread.
[`Task`](../src/org/helioviewer/jhv/thread/Task.java) runs work on an executor and
posts success/failure callbacks to `UiThread`.
[`UiTimer`](../src/org/helioviewer/jhv/thread/UiTimer.java) schedules ticks off-thread
but executes its task on `UiThread`, coalescing pending ticks.

## Opening images and replacing a layer's frames

1. [`Load.image`](../src/org/helioviewer/jhv/io/Load.java) posts layer creation and
   loading to `UiThread`. Remote API requests also enter through
   [`ImageLayer.load(APIRequest)`](../src/org/helioviewer/jhv/layers/ImageLayer.java).
2. [`ImageLayerLoader`](../src/org/helioviewer/jhv/layers/ImageLayerLoader.java)
   resolves requests and expands input directories on its load worker.
   `NetFileCache.get` supplies a `DataUri`; `openUri` expands ZIPs and selects
   `J2KSource`, `FITSSource` or `RasterSource` from the detected format.
3. The loader reads metadata, sorts frames by time and removes duplicate times.
   Sources transfer to [`Frames`](../src/org/helioviewer/jhv/layers/Frames.java);
   sources with no retained frames and failed opens are closed. Completion posts
   back to `UiThread`. A superseded load closes its result instead of installing it.
4. `ImageLayer.loaded` installs the result through `setFrames`. That resets the
   decoder to the new frame-set serial, releases retained images, switches the
   listener, and closes the previous `Frames`.

`Load.image` completes through `ImageLayer.whenLoadFinished`, reporting whether
the layer remains registered. This is load readiness, not a rendered-frame or
movie-export completion signal. Layer removal releases CPU image ownership in
`ImageLayer.detach`; [`Layers`](../src/org/helioviewer/jhv/layers/Layers.java) queues
GL removal for `prerender`, where a context is available.

## Playback, decoding and presentation

[`Commands`](../src/org/helioviewer/jhv/app/Commands.java) forwards playback actions
to [`Player`](../src/org/helioviewer/jhv/movie/Player.java). Timer advancement and
explicit seeks converge on `Player.syncTime`:

1. Ask `ExportMovie.beginPlaybackFrame` whether recording permits advancement.
2. Update the viewpoint through `DisplayController.timeChanged`, select each
   image layer's frame through `Layers.setImageLayersNearestFrame`, and call
   [`DisplayController.render`](../src/org/helioviewer/jhv/display/DisplayController.java).
3. [`ImageLayers.decode`](../src/org/helioviewer/jhv/layers/ImageLayers.java)
   requests decoding for visible image layers. Recording requests the finest
   level; ordinary display uses the viewport's pixel factor.
4. [`FrameDecoder`](../src/org/helioviewer/jhv/layers/FrameDecoder.java) chooses an
   available level, requests source data, and checks `ImageBufferCache`. On a
   miss it decodes and filters on a `LatestWorker`, returning a `DecodedImage`.
5. The callback caches results still valid for the frame-set serial and processing
   settings, but delivers only fresh work. `ImageLayer.handleData` retains the
   current/comparison images and calls `ImageLayers.displaySynced`. That gate
   compares viewpoint identity across visible layers that already have image data.

[`LatestWorker`](../src/org/helioviewer/jhv/thread/LatestWorker.java) retains only the
latest pending request and can reuse a matching outstanding key. `invalidate`
marks work stale without interrupting a running task. Completed stale work still
reaches its callback, so consumers must handle ownership and freshness explicitly.
Source arrivals return through `Frames` to `ImageLayer.frameReady`, which can move
the selection or refine the displayed frame.

[`DecodedImage`](../src/org/helioviewer/jhv/image/DecodedImage.java) owns native pixels
through explicit references. Cache lookup borrows an image, queued delivery owns
a temporary reference, and retained layer slots own their references. See
[image ownership and cache](image-buffer-cache.md) for cleanup details and
[image coordinates](image-coordinates.md) for metadata, decoder-grid and texture
conventions.

`DisplayController.display` requests presentation through the installed host.
[`AngleCanvas`](../src/org/helioviewer/jhv/opengl/AngleCanvas.java) coalesces rendering
on the EDT; [`AnglePbuffer`](../src/org/helioviewer/jhv/opengl/AnglePbuffer.java)
coalesces it through `UiThread`.
[`AngleRenderer.render`](../src/org/helioviewer/jhv/opengl/angle/AngleRenderer.java)
makes its EGL context current and calls
[`GLRenderer.display`](../src/org/helioviewer/jhv/opengl/GLRenderer.java).
`Layers.prerender` processes pending GL removal/initialization and texture uploads,
then the renderer draws the scene and calls `ExportMovie.renderedFrame`.

## Timeline data and chart drawing

[`TimelineLayers.addBands`](../src/org/helioviewer/jhv/timelines/TimelineLayers.java)
reuses existing bands by `BandType` and fetches data for newly added bands.
[`BandImporter`](../src/org/helioviewer/jhv/timelines/band/BandImporter.java) brings
HAPI URI, CDF and saved band data into that layer collection.

Changing the selected interval in
[`DrawController`](../src/org/helioviewer/jhv/timelines/draw/DrawController.java)
calls `TimelineLayers.fetchData`. For a
[`Band`](../src/org/helioviewer/jhv/timelines/band/Band.java), request planning rounds
outward to UTC days, excludes cached/in-flight coverage through `RequestCache`,
and splits missing intervals.
[`BandDownloads`](../src/org/helioviewer/jhv/timelines/band/BandDownloads.java) groups
requests by dataset and interval, runs `BandReaderHapi` work on the download pool,
and publishes results through `Task` callbacks on `UiThread`.

Each band stores samples in `BandCacheFull` or `BandCacheMinute`. Graph preparation
uses a per-band `LatestWorker` backed by the shared graph pool. Only fresh graph
results are installed, followed by `DrawController.drawRequest`.

`DrawController` coalesces chart notifications with a 10 Hz `UiTimer`.
[`ChartDrawGraphPane`](../src/org/helioviewer/jhv/timelines/chart/ChartDrawGraphPane.java)
paints a cached Java2D image and draws the movie line and hover labels separately.
Its `addNotify`/`removeNotify` methods manage draw-listener registration, while
showing changes start/stop chart notifications and register/unregister the export
frame source. Keep these lifetimes distinct when changing the chart host.

## UI commands and saved state

[`MoviePanel`](../src/org/helioviewer/jhv/gui/component/MoviePanel.java) is one Swing
consumer of [`ViewState`](../src/org/helioviewer/jhv/app/state/ViewState.java), which
applies view, playback and recording configuration and notifies listeners.
[`SAMP handlers`](../src/org/helioviewer/jhv/io/samp/) post mutations to `UiThread`
and use `Commands`/`Load`. `Commands` itself does not dispatch onto that thread.
See the [SAMP contract](jhv-samp-commands.md) for payload and completion semantics.

[`State.save`](../src/org/helioviewer/jhv/app/state/State.java) captures JSON before
writing it on a background thread. `Load.state` uses
[`LoadState`](../src/org/helioviewer/jhv/io/LoadState.java) to read/parse JSON in the
background, then calls `State.load` on `UiThread`. Restoration rebuilds layers,
restores timelines when EVE is active, and restores plugin state. Image entries
are recreated from their saved `APIRequest`; entries without one are skipped.
It waits for the new image layers' load futures before applying the final view
mode, active image layer and time. A serial rejects completion from an older
state load after a newer one has started.

## Recording and export

`Commands.recordStart` enters
[`ExportMovie`](../src/org/helioviewer/jhv/movie/ExportMovie.java). The recording
session coordinates playback and render completion: while a canvas frame is
pending, `beginPlaybackFrame` prevents another playback advance. With the canvas
visible, `GLRenderer.display` signals the capture point through `renderedFrame`.
Timeline-only recording uses the registered chart frame source.

Capture uses [`GLGrab`](../src/org/helioviewer/jhv/opengl/GLGrab.java) for the scene
and copies the chart's base `BufferedImage` before asynchronous encoding. It passes
the movie-line position separately. The single encode executor runs
[`ExportWriter`](../src/org/helioviewer/jhv/movie/ExportWriter.java), and the frame
consumer frees its captured buffers in `finally`. Writer closure is queued after
frame encoding and reports operation completion. Preserve the distinction between
load readiness, presentation, capture, and finished output when changing this path.
