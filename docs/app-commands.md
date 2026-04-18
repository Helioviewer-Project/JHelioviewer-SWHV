# App Commands

This note documents the app-command registry implemented in
[Commands.java](../src/org/helioviewer/jhv/app/Commands.java).

The command layer is app-level infrastructure:

- commands live in `org.helioviewer.jhv.app.Commands`
- commands are registered in `Commands.Registry`
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
- `zoom-in`
- `zoom-out`
- `zoom-fit`
- `zoom-one-to-one`
- `reset-view`
- `reset-view-axis`
- `rotate-view-90`

## Command Inputs

### `set-view-state`

Input type: `Commands.SetViewStateArgs`

Fields:

- `ProjectionMode projection`
- `Interaction.AnnotationMode annotationMode`
- `Boolean multiview`
- `Boolean tracking`
- `Boolean refresh`
- `Boolean showCorona`
- `Boolean differentialRotation`

Semantics:

This is a partial-update command. Callers may provide only the fields they want
to change and leave the rest as `null`. Omitted fields are merged with the
current mode state in [ViewState.java](../src/org/helioviewer/jhv/app/state/ViewState.java)
before the update is applied.

SAMP currently does not construct `Commands.SetViewStateArgs` directly. The
SAMP path goes through `Commands.setViewStateRaw(...)`, and
[ViewState.java](../src/org/helioviewer/jhv/app/state/ViewState.java) resolves
and validates those raw strings there.

Current enum/value domains used by this command:

- `ProjectionMode`: `Orthographic`, `HPC`, `Latitudinal`, `LogPolar`, `Polar`
- `Interaction.AnnotationMode`: `Rectangle`, `Circle`, `Cross`, `FOV`, `Line`, `Loop`

### `set-playback`

Input type: `Commands.SetPlaybackArgs`

Fields:

- `String advanceMode`
- `String speed`
- `String speedUnit`
- `String firstFrame`
- `String lastFrame`

Semantics:

This is a partial-update command. Callers may provide only the playback fields
they want to change and leave the rest as `null`. Omitted fields are merged
with the current playback state in
[ViewState.java](../src/org/helioviewer/jhv/app/state/ViewState.java) before the
update is applied. In particular, `speed` and `speedUnit` are resolved as one
pair, and `firstFrame` and `lastFrame` are resolved as one pair.

Those five fields are carried as raw strings and resolved in
[ViewState.java](../src/org/helioviewer/jhv/app/state/ViewState.java). Invalid
strings are warned about and ignored there; out-of-range numeric values are
warned about and clamped. They do not fail the command.

Current enum/value domains:

- `Movie.AdvanceMode`: `Loop`, `Stop`, `Swing`, `SwingDown`
- `ViewState.PlaybackSpeedUnit`:
  - `FRAMES_PER_SECOND`
  - `MINUTES_PER_SECOND`
  - `HOURS_PER_SECOND`
  - `DAYS_PER_SECOND`

Playback speed constraints:

- minimum: `1`
- maximum: `120`

Those constraints are defined in
[ViewState.java](../src/org/helioviewer/jhv/app/state/ViewState.java) as `PLAYBACK_SPEED_MIN` and `PLAYBACK_SPEED_MAX`.

### `play`

Input type: none

Effect:

- calls [Movie.play()](../src/org/helioviewer/jhv/layers/Movie.java)

### `pause`

Input type: none

Effect:

- calls [Movie.pause()](../src/org/helioviewer/jhv/layers/Movie.java)

### `toggle-playback`

Input type: none

Effect:

- calls [Movie.toggle()](../src/org/helioviewer/jhv/layers/Movie.java)

### `seek-frame`

Input type: `int`

Effect:

- calls [Movie.setFrame(...)](../src/org/helioviewer/jhv/layers/Movie.java)

### `seek-time`

Input type: `JHVTime`

Effect:

- calls [Movie.setTime(...)](../src/org/helioviewer/jhv/layers/Movie.java)

### `next-frame`

Input type: none

Effect:

- calls [Movie.nextFrame()](../src/org/helioviewer/jhv/layers/Movie.java)

### `previous-frame`

Input type: none

Effect:

- calls [Movie.previousFrame()](../src/org/helioviewer/jhv/layers/Movie.java)

### `set-recording`

Input type: `Commands.SetRecordingArgs`

Fields:

- `ViewState.RecordingMode mode`
- `ViewState.RecordingSize size`

Semantics:

This is a partial-update command. Callers may provide only the recording
fields they want to change and leave the rest as `null`. Omitted fields are
merged with the current recording state in
[ViewState.java](../src/org/helioviewer/jhv/app/state/ViewState.java) before the
update is applied.

SAMP currently does not construct `Commands.SetRecordingArgs` directly. The
SAMP path goes through `Commands.setRecordingRaw(...)`, and
[ViewState.java](../src/org/helioviewer/jhv/app/state/ViewState.java) resolves
and validates those raw strings there.

Current enum/value domains:

- `ViewState.RecordingMode`:
  - `LOOP`
  - `SHOT`
  - `FREE`
- `ViewState.RecordingSize`:
  - `ORIGINAL`
  - `H1024`
  - `H1080`
  - `H2048`
  - `H2160`
  - `H4096`

### `record-start`

Input type: `Commands.RecordStartArgs`

Fields:

- `String mode`
- `String size`
- `String advanceMode`
- `String speed`
- `String speedUnit`

Semantics:

- if recording is already active, the command returns immediately
- if input is non-null, it first updates recording and playback configuration
- those five fields are carried as raw strings and resolved in
  [ViewState.java](../src/org/helioviewer/jhv/app/state/ViewState.java)
- invalid strings are warned about and ignored in `ViewState`; out-of-range
  numeric values are warned about and clamped. They do not fail the command
- export then starts from the resulting [ViewState.recordingData()](../src/org/helioviewer/jhv/app/state/ViewState.java) and [ViewState.playbackData()](../src/org/helioviewer/jhv/app/state/ViewState.java)

### `record-stop`

Input type: none

Effect:

- if recording is active, calls [ExportMovie.shallStop()](../src/org/helioviewer/jhv/export/ExportMovie.java)

### `load-state`

Input shape:

- `URI`, or
- inline `String` JSON

Effect:

- URI case calls [Load.state(URI)](../src/org/helioviewer/jhv/io/Load.java)
- JSON case calls `Load.state(String)`

### `load-request`

Input shape:

- `URI`, or
- inline `String` JSON

Effect:

- URI case calls [Load.request(URI)](../src/org/helioviewer/jhv/io/Load.java)
- JSON case calls `Load.request(String)`

### `load-sunjson`

Input shape:

- `URI`, or
- inline `String` JSON

Effect:

- URI case calls [Load.sunJSON(URI)](../src/org/helioviewer/jhv/io/Load.java)
- JSON case calls `Load.sunJSON(String)`

### `load-image`

Input shape:

- `URI`, or
- non-empty `List<URI>`

Effect:

- calls [Load.image(...)](../src/org/helioviewer/jhv/io/Load.java)

### `load-cdf`

Input shape:

- `URI`, or
- non-empty `List<URI>`

Effect:

- calls [Load.cdf(...)](../src/org/helioviewer/jhv/io/Load.java)

### `load-votable`

Input type: `URI`

Effect:

- calls [Load.votable(...)](../src/org/helioviewer/jhv/io/Load.java)

### `load-hapi`

Input shape:

- `URI`, or
- non-empty `List<URI>`

Effect:

- loads each URI with [BandReaderHapi.loadUri(...)](../src/org/helioviewer/jhv/timelines/band/BandReaderHapi.java)

### `zoom-in`

Input type: none

Effect:

- calls [ViewActions.zoomIn()](../src/org/helioviewer/jhv/camera/ViewActions.java)

### `zoom-out`

Input type: none

Effect:

- calls [ViewActions.zoomOut()](../src/org/helioviewer/jhv/camera/ViewActions.java)

### `zoom-fit`

Input type: none

Effect:

- calls [ViewActions.zoomFit()](../src/org/helioviewer/jhv/camera/ViewActions.java)

### `zoom-one-to-one`

Input type: none

Effect:

- calls [ViewActions.zoomOneToOne()](../src/org/helioviewer/jhv/camera/ViewActions.java)

### `reset-view`

Input type: none

Effect:

- calls [ViewActions.resetView()](../src/org/helioviewer/jhv/camera/ViewActions.java)

### `reset-view-axis`

Input type: none

Effect:

- calls [ViewActions.resetViewAxis()](../src/org/helioviewer/jhv/camera/ViewActions.java)

### `rotate-view-90`

Input type: `Quat`

Effect:

- calls [ViewActions.rotateView90(...)](../src/org/helioviewer/jhv/camera/ViewActions.java)

## State Semantics

The current command layer updates real app state. It is not a temporary override layer.

In particular:

- `set-view-state`
- `set-playback`
- `set-recording`
- `record-start`

leave the resulting state visible in the UI after execution.

## Current SAMP Mapping

The following SAMP operations are currently wired from
[SampClient.java](../src/org/helioviewer/jhv/io/samp/SampClient.java):

- `image.load.fits` -> `load-image`
- `table.load.fits` -> `load-image`
- `table.load.votable` -> `load-votable`
- `table.load.cdf` -> `load-cdf`
- `jhv.load.image` -> `load-image`
- `jhv.load.hapi` -> `load-hapi`
- `jhv.load.request` -> `load-request`
- `jhv.load.state` -> `load-state`
- `jhv.load.sunjson` -> `load-sunjson`
- `jhv.set.view.state` -> `set-view-state`
- `jhv.set.playback` -> `set-playback`
- `jhv.play` -> `play`
- `jhv.pause` -> `pause`
- `jhv.toggle.playback` -> `toggle-playback`
- `jhv.seek.frame` -> `seek-frame`
- `jhv.seek.time` -> `seek-time`
- `jhv.next.frame` -> `next-frame`
- `jhv.previous.frame` -> `previous-frame`
- `jhv.set.recording` -> `set-recording`
- `jhv.record.start` -> `record-start`
- `jhv.record.stop` -> `record-stop`
- `jhv.zoom.in` -> `zoom-in`
- `jhv.zoom.out` -> `zoom-out`
- `jhv.zoom.fit` -> `zoom-fit`
- `jhv.zoom.one.to.one` -> `zoom-one-to-one`
- `jhv.reset.view` -> `reset-view`
- `jhv.reset.view.axis` -> `reset-view-axis`
- `jhv.rotate.view.90` -> `rotate-view-90`

Notes:

- all of the above currently go through `Commands.Registry`, except:
  - `jhv.load.state`
  - `jhv.set.view.state`
  - `jhv.set.playback`
  - `jhv.play`
  - `jhv.pause`
  - `jhv.toggle.playback`
  - `jhv.seek.frame`
  - `jhv.seek.time`
  - `jhv.next.frame`
  - `jhv.previous.frame`
  - `jhv.set.recording`
  - `jhv.record.start`
  - `jhv.record.stop`
  - `jhv.zoom.in`
  - `jhv.zoom.out`
  - `jhv.zoom.fit`
  - `jhv.zoom.one.to.one`
  - `jhv.reset.view`
  - `jhv.reset.view.axis`
  - `jhv.rotate.view.90`
- `jhv.load.state` uses the context-aware `Commands.loadState(...)` path directly so JHV can send a correlated completion notification later
- the view/playback/recording/camera SAMP commands above use direct `Commands` helpers rather than `Commands.Registry`
- `table.load.votable` is currently accepted only from sender `SolarOrbiterARchive`
- `table.load.fits` is currently accepted only from senders `SolarOrbiterARchive` and `SSA`

## Current SAMP Payloads

This section describes the payload shape an external SAMP client can send today.

These are the message params that [SampClient.java](../src/org/helioviewer/jhv/io/samp/SampClient.java) actually reads now.

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

For `jhv.load.state`, clients may also send an optional `requestId` parameter.
This is not a SAMP-standard field; it is part of JHV's application-level
message contract and is used only so the client can correlate the eventual
completion notification.

Accepted payload shapes:

URL form:

```json
{
  "url": "https://example.invalid/state.json",
  "requestId": "abc-123"
}
```

Inline-value form:

```json
{
  "value": "{\"org.helioviewer.jhv.state\":{...}}",
  "requestId": "abc-123"
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
- `requestId` is currently meaningful for `jhv.load.state` and `jhv.record.start`

### Recording payloads

`jhv.set.recording` currently accepts these optional params:

- `mode`
- `size`

Each param is read as a plain string and passed to
`Commands.setRecordingRaw(...)`. Omitted params leave the existing recording
configuration unchanged. `ViewState` later resolves and validates the values
when the command is applied.

Expected string domains:

- `mode`: `LOOP`, `SHOT`, `FREE`
- `size`: `ORIGINAL`, `H1024`, `H1080`, `H2048`, `H2160`, `H4096`

Example:

```json
{
  "mode": "LOOP",
  "size": "H1080"
}
```

`jhv.record.start` currently accepts these optional params:

- `requestId`
- `mode`
- `size`
- `advanceMode`
- `speed`
- `speedUnit`

Each param is read as a plain string and stored in the corresponding
`Commands.RecordStartArgs` field. Omitted params leave the existing recording or
playback configuration unchanged. `ViewState` later resolves and validates the
values when the command is applied.

Expected string domains:

- `mode`: `LOOP`, `SHOT`, `FREE`
- `size`: `ORIGINAL`, `H1024`, `H1080`, `H2048`, `H2160`, `H4096`
- `advanceMode`: `Loop`, `Stop`, `Swing`, `SwingDown`
- `speedUnit`: `FRAMES_PER_SECOND`, `MINUTES_PER_SECOND`, `HOURS_PER_SECOND`, `DAYS_PER_SECOND`

These values are currently matched with Java `valueOf(...)`, so spelling and
case must match the names above exactly.

Example:

```json
{
  "requestId": "rec-1",
  "mode": "LOOP",
  "size": "H1080",
  "advanceMode": "Loop",
  "speed": "24",
  "speedUnit": "FRAMES_PER_SECOND"
}
```

`jhv.record.stop` currently reads no params.

### Playback payloads

`jhv.set.view.state` currently accepts these optional params:

- `projection`
- `annotationMode`
- `multiview`
- `tracking`
- `refresh`
- `showCorona`
- `differentialRotation`

Each param is read as a plain string and passed to
`Commands.setViewStateRaw(...)`. Omitted params leave the existing mode
configuration unchanged. `ViewState` later resolves and validates the values
when the command is applied.

Expected string domains:

- `projection`: `Orthographic`, `HPC`, `Latitudinal`, `LogPolar`, `Polar`
- `annotationMode`: `Rectangle`, `Circle`, `Cross`, `FOV`, `Line`, `Loop`
- `multiview`, `tracking`, `refresh`, `showCorona`, `differentialRotation`:
  `true` or `false` case-insensitively

Example:

```json
{
  "projection": "HPC",
  "annotationMode": "Cross",
  "multiview": "false",
  "tracking": "true"
}
```

`jhv.set.playback` currently accepts these optional params:

- `advanceMode`
- `speed`
- `speedUnit`
- `firstFrame`
- `lastFrame`

Each param is read as a plain string and stored in the corresponding
`Commands.SetPlaybackArgs` field. Omitted params leave the existing playback
configuration unchanged. `ViewState` later resolves and validates the values
when the command is applied.

Expected string domains:

- `advanceMode`: `Loop`, `Stop`, `Swing`, `SwingDown`
- `speedUnit`: `FRAMES_PER_SECOND`, `MINUTES_PER_SECOND`, `HOURS_PER_SECOND`, `DAYS_PER_SECOND`

`speed`, `firstFrame`, and `lastFrame` are decimal integer strings.

Example:

```json
{
  "advanceMode": "Loop",
  "speed": "24",
  "speedUnit": "FRAMES_PER_SECOND",
  "firstFrame": "0",
  "lastFrame": "120"
}
```

`jhv.seek.frame` currently accepts:

- `frame`

`frame` is parsed in [SampPlaybackHandlers.java](../src/org/helioviewer/jhv/io/samp/SampPlaybackHandlers.java)
as a decimal integer string before calling `Commands.seekFrame(...)`. Invalid
values are warned about and ignored there.

Example:

```json
{
  "frame": "12"
}
```

`jhv.seek.time` currently accepts:

- `time`

`time` is parsed in [SampPlaybackHandlers.java](../src/org/helioviewer/jhv/io/samp/SampPlaybackHandlers.java)
with `new JHVTime(time)` before calling `Commands.seekTime(...)`. Invalid
values are warned about and ignored there.

Example:

```json
{
  "time": "2024-01-01T12:00:00"
}
```

These simple playback message types currently read no params:

- `jhv.play`
- `jhv.pause`
- `jhv.toggle.playback`
- `jhv.next.frame`
- `jhv.previous.frame`

### Camera payloads

These camera message types currently read no params:

- `jhv.zoom.in`
- `jhv.zoom.out`
- `jhv.zoom.fit`
- `jhv.zoom.one.to.one`
- `jhv.reset.view`
- `jhv.reset.view.axis`

`jhv.rotate.view.90` currently accepts:

- `axis`

`axis` is parsed in [SampCameraHandlers.java](../src/org/helioviewer/jhv/io/samp/SampCameraHandlers.java)
and accepts `X`, `Y`, or `Z` case-insensitively, mapping them to
`Quat.X90`, `Quat.Y90`, or `Quat.Z90`. Invalid values are warned about and
ignored there.

Example:

```json
{
  "axis": "Z"
}
```

## `jhv.load.state` Client Contract

This is the current end-to-end contract implemented in
[SampClient.java](../src/org/helioviewer/jhv/io/samp/SampClient.java).

Request:

- `samp.mtype`: `jhv.load.state`
- `samp.params`:
  - either `url` or `value`
  - optional `requestId`

Response:

- completion is sent as a targeted SAMP notify back to the same client that
  sent the request
- `samp.mtype`: `jhv.load.state.completed`
- `samp.params` always includes:
  - `mtype`: `jhv.load.state`
  - `status`: `success` or `failure`
  - `message`
- `samp.params` additionally includes `requestId` if and only if the original
  request supplied one

Success example:

```json
{
  "samp.mtype": "jhv.load.state.completed",
  "samp.params": {
    "requestId": "abc-123",
    "mtype": "jhv.load.state",
    "status": "success",
    "message": "State loaded."
  }
}
```

Failure example:

```json
{
  "samp.mtype": "jhv.load.state.completed",
  "samp.params": {
    "requestId": "abc-123",
    "mtype": "jhv.load.state",
    "status": "failure",
    "message": "An error occurred opening the remote file."
  }
}
```

Semantics:

- `success` means the state restore actually finished, not just that the JSON
  was fetched or parsed
- `failure` may come from:
  - fetch/parse failure in [LoadState.java](../src/org/helioviewer/jhv/io/LoadState.java)
  - restore-time failure in [State.java](../src/org/helioviewer/jhv/app/state/State.java)
- without `requestId`, multiple outstanding `jhv.load.state` requests from the
  same client are ambiguous to correlate
- clients that want reliable correlation should always send `requestId`

## `jhv.record.start` Client Contract

This is the current end-to-end contract implemented in
[SampClient.java](../src/org/helioviewer/jhv/io/samp/SampClient.java) and
[SampRecordingHandlers.java](../src/org/helioviewer/jhv/io/samp/SampRecordingHandlers.java).

Request:

- `samp.mtype`: `jhv.record.start`
- `samp.params`:
  - optional `requestId`
  - optional `mode`
  - optional `size`
  - optional `advanceMode`
  - optional `speed`
  - optional `speedUnit`

Response:

- completion is sent as a targeted SAMP notify back to the same client that
  sent the start request
- `samp.mtype`: `jhv.record.start.completed`
- `samp.params` always includes:
  - `mtype`: `jhv.record.start`
  - `status`: `success` or `failure`
  - `message`
- `samp.params` additionally includes:
  - `requestId` if and only if the original request supplied one
  - `output` when recording finishes successfully and JHV has a result path or
    output pattern to report

Success example:

```json
{
  "samp.mtype": "jhv.record.start.completed",
  "samp.params": {
    "requestId": "rec-1",
    "mtype": "jhv.record.start",
    "status": "success",
    "message": "Recording finished.",
    "output": "/path/to/file.mp4"
  }
}
```

Failure example:

```json
{
  "samp.mtype": "jhv.record.start.completed",
  "samp.params": {
    "requestId": "rec-1",
    "mtype": "jhv.record.start",
    "status": "failure",
    "message": "Recording failed."
  }
}
```

Semantics:

- completion is tied to the original `jhv.record.start` request, even if the
  recording is later stopped via `jhv.record.stop`
- `jhv.record.stop` currently just requests stop; it does not carry correlation
  and does not produce a separate completion message
- `failure` may come from:
  - export failure while closing the recording
- invalid `jhv.record.start` string values do not currently fail the request;
  they are warned about and ignored in
  [ViewState.java](../src/org/helioviewer/jhv/app/state/ViewState.java)
- out-of-range numeric values such as playback speed likewise do not fail the
  request; they are warned about and clamped in
  [ViewState.java](../src/org/helioviewer/jhv/app/state/ViewState.java)
- without `requestId`, multiple outstanding `jhv.record.start` requests from
  the same client are ambiguous to correlate
- clients that want reliable correlation should always send `requestId`

## How SAMP Should Pass Arguments

The intended pattern is:

1. parse SAMP message params
2. build the corresponding `Commands` input object
3. either call `Commands.Registry.run(commandId, input)` or, for the small
   number of context-aware commands, call the dedicated `Commands` helper

This is already how the load commands are wired, except for `jhv.load.state`,
which uses the dedicated context-aware `Commands.loadState(...)` helper so
it can carry `requestId` and client identity through to completion.

`jhv.set.view.state`, `jhv.set.playback`, `jhv.play`, `jhv.pause`,
`jhv.toggle.playback`, `jhv.seek.frame`, `jhv.seek.time`, `jhv.next.frame`,
`jhv.previous.frame`, `jhv.set.recording`, `jhv.record.start`,
`jhv.record.stop`, `jhv.zoom.in`, `jhv.zoom.out`, `jhv.zoom.fit`,
`jhv.zoom.one.to.one`, `jhv.reset.view`, `jhv.reset.view.axis`, and
`jhv.rotate.view.90` likewise use direct `Commands` helpers rather than
`Commands.Registry`.

## Completion Feedback

Current implementation:

- `jhv.load.state.completed` is implemented as described above
- `jhv.record.start.completed` is implemented as described above
