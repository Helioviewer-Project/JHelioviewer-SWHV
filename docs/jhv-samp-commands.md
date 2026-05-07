# JHV SAMP Commands

This note describes the SAMP interface exposed by JHelioviewer to external
clients. It documents the accepted `samp.mtype` values, the expected message
parameters, the handling of invalid input, and the completion notifications
that JHV may send back.

Only implemented SAMP commands are documented here.

## Accepted Message Types

JHV accepts the following SAMP message types:

### Load messages

- `image.load.fits`
- `table.load.fits`
- `table.load.votable`
- `table.load.cdf`
- `jhv.load.image`
- `jhv.load.cdf`
- `jhv.load.hapi`
- `jhv.load.request`
- `jhv.load.state`
- `jhv.load.sunjson`

### Playback messages

- `jhv.playback.set`
- `jhv.playback.play`
- `jhv.playback.pause`
- `jhv.playback.toggle`
- `jhv.playback.seek-frame`
- `jhv.playback.seek-time`
- `jhv.playback.next-frame`
- `jhv.playback.previous-frame`

### View messages

- `jhv.view.set`

### Recording messages

- `jhv.record.set`
- `jhv.record.start`
- `jhv.record.stop`

### Camera messages

- `jhv.view.zoom-in`
- `jhv.view.zoom-out`
- `jhv.view.zoom-fit`
- `jhv.view.zoom-1-to-1`
- `jhv.view.reset`
- `jhv.view.reset-axis`
- `jhv.view.rotate90`

Restrictions:

- `table.load.votable` is accepted only from sender `SolarOrbiterARchive`
- `table.load.fits` is accepted only from senders `SolarOrbiterARchive` and `SSA`

## Client Rules

### Partial-update messages

These messages update only the fields they receive:

- `jhv.view.set`
- `jhv.playback.set`
- `jhv.record.set`
- `jhv.record.start`

For these messages:

- omitted parameters leave the existing JHV state unchanged
- invalid string values are warned about and ignored
- out-of-range numeric values are warned about and clamped

JHV applies those rules at the state boundary. See
[ViewState.java](../src/org/helioviewer/jhv/app/state/ViewState.java) for the
exact implementation.

### Immediate-action messages

These messages act immediately instead of performing a partial state merge:

- `image.load.fits`
- `table.load.fits`
- `table.load.votable`
- `table.load.cdf`
- `jhv.load.image`
- `jhv.load.cdf`
- `jhv.load.hapi`
- `jhv.load.request`
- `jhv.load.state`
- `jhv.load.sunjson`
- `jhv.playback.play`
- `jhv.playback.pause`
- `jhv.playback.toggle`
- `jhv.playback.seek-frame`
- `jhv.playback.seek-time`
- `jhv.playback.next-frame`
- `jhv.playback.previous-frame`
- `jhv.record.stop`
- `jhv.view.zoom-in`
- `jhv.view.zoom-out`
- `jhv.view.zoom-fit`
- `jhv.view.zoom-1-to-1`
- `jhv.view.reset`
- `jhv.view.reset-axis`
- `jhv.view.rotate90`

### State-changing messages

These messages update real JHV state and leave the resulting state visible in
the UI after execution:

- `jhv.view.set`
- `jhv.playback.set`
- `jhv.record.set`
- `jhv.record.start`

## Payloads

This section describes the payloads an external SAMP client can send.

### Load Messages

### Single-URL load payloads

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

### Multi-URL or single-URL load payloads

These message types read the `url` parameter in one of two forms:

- `jhv.load.image`
- `jhv.load.cdf`
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

### URL-or-inline-value load payloads

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

The `url` or inline `value` for `jhv.load.state` must contain a JHV state
document, not arbitrary JSON.

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

Behavior:

- if `url` is present, JHV uses it
- otherwise, if `value` is present, JHV uses the inline string
- these handlers do not combine `url` and `value`

### Load notes

- SAMP parameters are read as plain values and converted with `toString()`
- for `jhv.load.image`, `jhv.load.cdf`, and `jhv.load.hapi`, the `url` array
  elements are each converted with `toString()`
- `table.load.votable` is additionally sender-restricted to `SolarOrbiterARchive`
- `table.load.fits` is additionally sender-restricted to `SolarOrbiterARchive` and `SSA`

### Playback Messages

### `jhv.playback.set`

Accepted parameters:

- `advanceMode`
- `speed`
- `speedUnit`
- `firstFrame`
- `lastFrame`

This is a partial-update message. Omitted parameters leave the existing playback
configuration unchanged. Invalid strings are warned about and ignored.

Expected string domains:

- `advanceMode`: `Loop`, `Stop`, `Swing`
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

### `jhv.playback.seek-frame`

Accepted parameters:

- `frame`

`frame` is parsed as a decimal integer string before dispatch. Invalid values
are warned about and ignored.

Example:

```json
{
  "frame": "12"
}
```

### `jhv.playback.seek-time`

Accepted parameters:

- `time`

`time` is parsed with `new JHVTime(time)` before dispatch. Invalid values are
warned about and ignored.

Example:

```json
{
  "time": "2024-01-01T12:00:00"
}
```

### Parameterless playback messages

These messages read no parameters:

- `jhv.playback.play`
- `jhv.playback.pause`
- `jhv.playback.toggle`
- `jhv.playback.next-frame`
- `jhv.playback.previous-frame`

### View Messages

### `jhv.view.set`

Accepted parameters:

- `projection`
- `annotationMode`
- `multiview`
- `tracking`
- `refresh`
- `showCorona`
- `differentialRotation`

This is a partial-update message. Omitted parameters leave the existing mode
configuration unchanged. Invalid strings are warned about and ignored.

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

### Recording Messages

### `jhv.record.set`

Accepted parameters:

- `mode`
- `size`

This is a partial-update message. Omitted parameters leave the existing recording
configuration unchanged. Invalid strings are warned about and ignored.

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

### `jhv.record.start`

Accepted parameters:

- `requestId`
- `mode`
- `size`
- `advanceMode`
- `speed`
- `speedUnit`

This message starts recording using the resulting recording and playback state.
If recording is already active, JHV sends a failure completion.

Omitted parameters leave the existing recording or playback configuration
unchanged. Invalid strings are warned about and ignored. Out-of-range numeric
values are warned about and clamped.

Expected string domains:

- `mode`: `LOOP`, `SHOT`, `FREE`
- `size`: `ORIGINAL`, `H1024`, `H1080`, `H2048`, `H2160`, `H4096`
- `advanceMode`: `Loop`, `Stop`, `Swing`
- `speedUnit`: `FRAMES_PER_SECOND`, `MINUTES_PER_SECOND`, `HOURS_PER_SECOND`, `DAYS_PER_SECOND`

These values are matched with Java `valueOf(...)`, so spelling and case must
match the names above exactly.

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

### `jhv.record.stop`

Accepted parameters: none

This requests stop if recording is active. It does not have its own completion
message.

### Camera Messages

### `jhv.view.rotate90`

Accepted parameters:

- `axis`

`axis` accepts `X`, `Y`, or `Z` case-insensitively. Invalid values are warned
about and ignored.

Example:

```json
{
  "axis": "Z"
}
```

### Parameterless camera messages

These messages read no parameters:

- `jhv.view.zoom-in`
- `jhv.view.zoom-out`
- `jhv.view.zoom-fit`
- `jhv.view.zoom-1-to-1`
- `jhv.view.reset`
- `jhv.view.reset-axis`

## `jhv.load.state` Client Contract

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
  - `clientId`: public SAMP id of the client that sent the request
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
    "clientId": "cli#1",
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
    "clientId": "cli#1",
    "requestId": "abc-123",
    "mtype": "jhv.load.state",
    "status": "failure",
    "message": "An error occurred opening the remote file."
  }
}
```

Notes:

- `success` means the state restore actually finished, not just that the JSON
  was fetched or parsed
- `failure` may come from:
  - fetch/parse failure in [LoadState.java](../src/org/helioviewer/jhv/io/LoadState.java)
  - restore-time failure in [State.java](../src/org/helioviewer/jhv/app/state/State.java)
- without `requestId`, multiple outstanding `jhv.load.state` requests from the
  same client are ambiguous to correlate
- clients that want reliable correlation should always send `requestId`

## `jhv.record.start` Client Contract

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
  - `clientId`: public SAMP id of the client that sent the start request
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
    "clientId": "cli#1",
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
    "clientId": "cli#1",
    "requestId": "rec-1",
    "mtype": "jhv.record.start",
    "status": "failure",
    "message": "Recording failed."
  }
}
```

Notes:

- completion is tied to the original `jhv.record.start` request, even if the
  recording is later stopped via `jhv.record.stop`
- `jhv.record.stop` just requests stop; it does not carry correlation
  and does not produce a separate completion message
- `failure` may come from:
  - export failure while closing the recording
- invalid `jhv.record.start` string values do not fail the request;
  they are warned about and ignored in
  [ViewState.java](../src/org/helioviewer/jhv/app/state/ViewState.java)
- out-of-range numeric values such as playback speed likewise do not fail the
  request; they are warned about and clamped in
  [ViewState.java](../src/org/helioviewer/jhv/app/state/ViewState.java)
- without `requestId`, multiple outstanding `jhv.record.start` requests from
  the same client are ambiguous to correlate
- clients that want reliable correlation should always send `requestId`

## Completion Feedback

Implemented completion messages:

- `jhv.load.state.completed` is implemented as described above
- `jhv.record.start.completed` is implemented as described above

## JHV State Document Format

`jhv.load.state` accepts a JHV state document, not arbitrary JSON.

The role of the state document is to rebuild the same scene:

- reload all datasets described by the `data` keys
- restore their per-dataset view options
- restore the global view mode
- reapply the equivalent of `seek-time`

The document must be a JSON object with this top-level wrapper:

```json
{
  "org.helioviewer.jhv.state": {
    "...": "..."
  }
}
```

Inside `org.helioviewer.jhv.state`, the top-level keys can be understood in
three roles:

- `data`
  - `annotations`
  - `layers`
  - `imageLayers`
  - `timelines`
  - `plugins`
- `view mode`
  - `projection`
  - `annotationMode`
  - `multiview`
  - `tracking`
  - `refresh`
  - `showCorona`
  - `differentialRotation`
- `commands`
  - `time`

Meaning:

- `data` tells JHV what data to reload and which per-entry view options to
  restore for annotations, layers, image layers, timelines, and plugin-backed
  data such as PFSS and SWEK
- `view mode` restores the global viewing mode
- `commands` reapply the equivalent of `seek-time`, because the selected time
  is intimately linked to data presentation

`layers` and `timelines` are arrays of objects with this general shape:

```json
{
  "className": "org.helioviewer.jhv.layers.GridLayer",
  "name": "Display name",
  "data": { "...": "layer or timeline view options..." },
  "enabled": true
}
```

Real `className` examples from the source:

- layers:
  - `org.helioviewer.jhv.layers.GridLayer`
  - `org.helioviewer.jhv.layers.TimestampLayer`
- timelines:
  - `org.helioviewer.jhv.timelines.radio.RadioData`
  - `org.helioviewer.jhv.timelines.band.Band`

`imageLayers` uses the same shape, plus optional `master`:

```json
{
  "className": "org.helioviewer.jhv.layers.ImageLayer",
  "name": "Layer name",
  "data": { "...": "image-layer view options..." },
  "enabled": true,
  "master": true
}
```

Notes:

- the `view mode` keys above are the full serialized view mode
- `time` is the stored movie time value and is the same kind of value that
  `jhv.playback.seek-time` applies
- for image layers, `master: true` marks the layer that should become the
  active image layer after restore

Implementation references:

- [SampClient.java](../src/org/helioviewer/jhv/io/samp/SampClient.java)
- [LoadHandlers.java](../src/org/helioviewer/jhv/io/samp/LoadHandlers.java)
- [PlaybackHandlers.java](../src/org/helioviewer/jhv/io/samp/PlaybackHandlers.java)
- [RecordHandlers.java](../src/org/helioviewer/jhv/io/samp/RecordHandlers.java)
- [ViewHandlers.java](../src/org/helioviewer/jhv/io/samp/ViewHandlers.java)
- [CameraHandlers.java](../src/org/helioviewer/jhv/io/samp/CameraHandlers.java)
