# App Commands

This note documents the app-command registry implemented in
[AppCommands.java](/Users/bogdan/jhv/JHelioviewer-SWHV/src/org/helioviewer/jhv/AppCommands.java).

The command layer is app-level infrastructure:

- commands live in `org.helioviewer.jhv.AppCommands`
- commands are registered in `AppCommands.Registry`
- transport layers such as SAMP can parse incoming messages, build command inputs, and call the registry

This document reflects the code as it exists now. It does not describe planned commands that are not implemented yet.

## Registry

The registry currently contains these command ids:

- `set-view-state`
- `set-playback`
- `play`
- `pause`
- `toggle-playback`
- `seek-frame`
- `seek-time`
- `next-frame`
- `previous-frame`
- `set-recording`
- `record-start`
- `record-stop`
- `load-state`
- `load-request`
- `load-sunjson`
- `load-image`
- `load-cdf`
- `load-votable`
- `load-hapi`

## Command Inputs

### `set-view-state`

Input type: `AppCommands.SetViewStateArgs`

Fields:

- `ProjectionMode projection`
- `Interaction.AnnotationMode annotationMode`
- `Boolean multiview`
- `Boolean tracking`
- `Boolean refresh`
- `Boolean showCorona`
- `Boolean differentialRotation`

Semantics:

- this is a partial-update command
- `null` fields mean "leave current state unchanged"
- the command reads current [ViewerState.modeData()](/Users/bogdan/jhv/JHelioviewer-SWHV/src/org/helioviewer/jhv/gui/ViewerState.java) and applies a merged `ModeData`

Current enum/value domains used by this command:

- `ProjectionMode`: `Orthographic`, `HPC`, `Latitudinal`, `LogPolar`, `Polar`
- `Interaction.AnnotationMode`: `Rectangle`, `Circle`, `Cross`, `FOV`, `Line`, `Loop`

### `set-playback`

Input type: `AppCommands.SetPlaybackArgs`

Fields:

- `Movie.AdvanceMode advanceMode`
- `Integer speed`
- `ViewerState.PlaybackSpeedUnit speedUnit`

Semantics:

- this is a partial-update command
- `advanceMode == null` leaves playback mode unchanged
- `speed` and `speedUnit` are applied together with the current missing half filled from [ViewerState.playbackData()](/Users/bogdan/jhv/JHelioviewer-SWHV/src/org/helioviewer/jhv/gui/ViewerState.java)

Current enum/value domains:

- `Movie.AdvanceMode`: `Loop`, `Stop`, `Swing`, `SwingDown`
- `ViewerState.PlaybackSpeedUnit`:
  - `FRAMES_PER_SECOND`
  - `MINUTES_PER_SECOND`
  - `HOURS_PER_SECOND`
  - `DAYS_PER_SECOND`

Playback speed constraints:

- minimum: `1`
- maximum: `120`

Those constraints are defined in
[ViewerState.java](/Users/bogdan/jhv/JHelioviewer-SWHV/src/org/helioviewer/jhv/gui/ViewerState.java) as `PLAYBACK_SPEED_MIN` and `PLAYBACK_SPEED_MAX`.

### `play`

Input type: none

Effect:

- calls [Movie.play()](/Users/bogdan/jhv/JHelioviewer-SWHV/src/org/helioviewer/jhv/layers/Movie.java)

### `pause`

Input type: none

Effect:

- calls [Movie.pause()](/Users/bogdan/jhv/JHelioviewer-SWHV/src/org/helioviewer/jhv/layers/Movie.java)

### `toggle-playback`

Input type: none

Effect:

- calls [Movie.toggle()](/Users/bogdan/jhv/JHelioviewer-SWHV/src/org/helioviewer/jhv/layers/Movie.java)

### `seek-frame`

Input type: `AppCommands.SeekFrameArgs`

Fields:

- `int frame`

Effect:

- calls [Movie.setFrame(...)](/Users/bogdan/jhv/JHelioviewer-SWHV/src/org/helioviewer/jhv/layers/Movie.java)

### `seek-time`

Input type: `AppCommands.SeekTimeArgs`

Fields:

- `JHVTime time`

Effect:

- calls [Movie.setTime(...)](/Users/bogdan/jhv/JHelioviewer-SWHV/src/org/helioviewer/jhv/layers/Movie.java)

### `next-frame`

Input type: none

Effect:

- calls [Movie.nextFrame()](/Users/bogdan/jhv/JHelioviewer-SWHV/src/org/helioviewer/jhv/layers/Movie.java)

### `previous-frame`

Input type: none

Effect:

- calls [Movie.previousFrame()](/Users/bogdan/jhv/JHelioviewer-SWHV/src/org/helioviewer/jhv/layers/Movie.java)

### `set-recording`

Input type: `AppCommands.SetRecordingArgs`

Fields:

- `ViewerState.RecordingMode mode`
- `ViewerState.RecordingSize size`

Semantics:

- this is a partial-update command
- `null` fields mean "leave current recording configuration unchanged"

Current enum/value domains:

- `ViewerState.RecordingMode`:
  - `LOOP`
  - `SHOT`
  - `FREE`
- `ViewerState.RecordingSize`:
  - `ORIGINAL`
  - `H1024`
  - `H1080`
  - `H2048`
  - `H2160`
  - `H4096`

### `record-start`

Input type: `AppCommands.RecordStartArgs`

Fields:

- `ViewerState.RecordingMode mode`
- `ViewerState.RecordingSize size`
- `Movie.AdvanceMode advanceMode`
- `Integer speed`
- `ViewerState.PlaybackSpeedUnit speedUnit`

Semantics:

- if recording is already active, the command returns immediately
- if input is non-null, it first updates recording and playback configuration
- export then starts from the resulting [ViewerState.recordingData()](/Users/bogdan/jhv/JHelioviewer-SWHV/src/org/helioviewer/jhv/gui/ViewerState.java) and [ViewerState.playbackData()](/Users/bogdan/jhv/JHelioviewer-SWHV/src/org/helioviewer/jhv/gui/ViewerState.java)

### `record-stop`

Input type: none

Effect:

- if recording is active, calls [ExportMovie.shallStop()](/Users/bogdan/jhv/JHelioviewer-SWHV/src/org/helioviewer/jhv/export/ExportMovie.java)

### `load-state`

Input shape:

- `URI`, or
- inline `String` JSON

Effect:

- URI case calls [Load.state(URI)](/Users/bogdan/jhv/JHelioviewer-SWHV/src/org/helioviewer/jhv/io/Load.java)
- JSON case calls `Load.state(String)`

### `load-request`

Input shape:

- `URI`, or
- inline `String` JSON

Effect:

- URI case calls [Load.request(URI)](/Users/bogdan/jhv/JHelioviewer-SWHV/src/org/helioviewer/jhv/io/Load.java)
- JSON case calls `Load.request(String)`

### `load-sunjson`

Input shape:

- `URI`, or
- inline `String` JSON

Effect:

- URI case calls [Load.getAllSunJSON(...)](/Users/bogdan/jhv/JHelioviewer-SWHV/src/org/helioviewer/jhv/io/Load.java)
- JSON case calls `Load.sunJSON(String)`

### `load-image`

Input type: `AppCommands.LoadURIsArgs`

Fields:

- `List<URI> uris`

Effect:

- calls [Load.getAllImage(...)](/Users/bogdan/jhv/JHelioviewer-SWHV/src/org/helioviewer/jhv/io/Load.java)

### `load-cdf`

Input type: `AppCommands.LoadURIsArgs`

Fields:

- `List<URI> uris`

Effect:

- calls [Load.getAllCDF(...)](/Users/bogdan/jhv/JHelioviewer-SWHV/src/org/helioviewer/jhv/io/Load.java)

### `load-votable`

Input type: `URI`

Effect:

- calls [SoarClient.submitTable(...)](/Users/bogdan/jhv/JHelioviewer-SWHV/src/org/helioviewer/jhv/io/SoarClient.java)

### `load-hapi`

Input type: `AppCommands.LoadURIsArgs`

Fields:

- `List<URI> uris`

Effect:

- loads each URI with [BandReaderHapi.loadUri(...)](/Users/bogdan/jhv/JHelioviewer-SWHV/src/org/helioviewer/jhv/timelines/band/BandReaderHapi.java)

## State Semantics

The current command layer updates real app state. It is not a temporary override layer.

In particular:

- `set-view-state`
- `set-playback`
- `set-recording`
- `record-start`

leave the resulting state visible in the UI after execution.

This is important for transport clients such as SAMP. External control should leave the application in the state it requested, unless a future command is explicitly documented as temporary.

## Current SAMP Mapping

The following SAMP load operations currently dispatch through `AppCommands.Registry` in
[SampClient.java](/Users/bogdan/jhv/JHelioviewer-SWHV/src/org/helioviewer/jhv/io/SampClient.java):

- `image.load.fits` -> `load-image`
- `table.load.fits` -> `load-image`
- `table.load.votable` -> `load-votable`
- `table.load.cdf` -> `load-cdf`
- `jhv.load.image` -> `load-image`
- `jhv.load.hapi` -> `load-hapi`
- `jhv.load.request` -> `load-request`
- `jhv.load.state` -> `load-state`
- `jhv.load.sunjson` -> `load-sunjson`

Notes:

- `table.load.votable` is currently accepted only from sender `SolarOrbiterARchive`
- `table.load.fits` is currently accepted only from senders `SolarOrbiterARchive` and `SSA`

## Current SAMP Payloads

This section describes the payload shape an external SAMP client can send today.

These are the message params that [SampClient.java](/Users/bogdan/jhv/JHelioviewer-SWHV/src/org/helioviewer/jhv/io/SampClient.java) actually reads now.

### Single-URL payloads

These message types expect one `url` parameter:

- `image.load.fits`
- `table.load.fits`
- `table.load.votable`
- `table.load.cdf`

Payload shape:

```json
{
  "url": "https://example.invalid/file.fits"
}
```

The `url` value may also be a local path string. JHV will treat a value with no URI scheme as a local file path.

### Multi-URL or single-URL payloads

These message types read the `url` param in one of two forms:

- `jhv.load.image`
- `jhv.load.hapi`

Accepted payload shapes:

Single URL:

```json
{
  "url": "https://example.invalid/file1.fits"
}
```

Multiple URLs:

```json
{
  "url": [
    "https://example.invalid/file1.fits",
    "https://example.invalid/file2.fits"
  ]
}
```

### URL-or-inline-value payloads

These message types accept either:

- a `url` parameter, or
- a `value` parameter containing inline text payload

Message types:

- `jhv.load.request`
- `jhv.load.state`
- `jhv.load.sunjson`

Accepted payload shapes:

URL form:

```json
{
  "url": "https://example.invalid/state.json"
}
```

Inline-value form:

```json
{
  "value": "{\"org.helioviewer.jhv.state\":{...}}"
}
```

Current behavior:

- if `url` is present, JHV uses it
- otherwise, if `value` is present, JHV uses the inline string
- these handlers do not currently combine `url` and `value`

### Notes for External Clients

- SAMP params are read as plain values and converted with `toString()`
- for `jhv.load.image` and `jhv.load.hapi`, the `url` array elements are each converted with `toString()`
- `table.load.votable` is additionally sender-restricted to `SolarOrbiterARchive`
- `table.load.fits` is additionally sender-restricted to `SolarOrbiterARchive` and `SSA`

## How SAMP Should Pass Arguments

The intended pattern is:

1. parse SAMP message params
2. build the corresponding `AppCommands` input object
3. call `AppCommands.Registry.run(commandId, input)`

This is already how the load commands are wired.

The same pattern can be used for future SAMP verbs such as:

- `set-view-state`
- `set-playback`
- `seek-frame`
- `seek-time`
- `set-recording`
- `record-start`
- `record-stop`

## Completion Feedback

Two places are natural completion hooks for future transport feedback:

- recording completion:
  [MovieExporter.java](/Users/bogdan/jhv/JHelioviewer-SWHV/src/org/helioviewer/jhv/export/MovieExporter.java)
- state loading completion:
  [LoadState.java](/Users/bogdan/jhv/JHelioviewer-SWHV/src/org/helioviewer/jhv/io/LoadState.java)

These are not fully documented protocol callbacks yet; this note only records the current code-backed hook points.
