# Swing UI reference captures

These native-window captures show the production Swing UI with the bundled
`sample.171.fits` image. They are visual review artifacts, not pixel-test goldens.

Generate a new set with:

```sh
python3 extra/test/ui/capture.py /path/to/output --theme Dark
python3 extra/test/ui/capture.py /path/to/output --theme Light
python3 extra/test/ui/capture.py /path/to/compact --width 1280 --height 850
```

The main-window references cover both themes. The compact playback reference
shows the toolbar and expanded playback controls at 1280 pixels. The dialog
references cover Settings, image metadata, dataset selection, and About. The
populated timeline reference uses a clearly labeled synthetic signal.

The capture runner uses disposable preferences and local image data. The additional timeline view uses an explicitly labeled synthetic signal to
check plot composition. The runner rejects horizontally clipped sidebar controls
and captures missing the rendered solar image, coordinate grid, or fixture curve.
Catalog contents can vary with live service responses. Screenshots do not establish OS
focus, pointer routing, or appearance on other platforms.

| View | Capture |
| --- | --- |
| Main window, Dark | [Dark main window](dark-main.png) |
| Main window, Light | [Light main window](light-main.png) |
| Compact playback | [Expanded playback at 1280 pixels](compact-playback.png) |
| Settings | [Settings dialog](settings.png) |
| Image metadata | [Image information dialog](metadata.png) |
| Dataset selection | [New image layer dialog](new-layer.png) |
| About | [About dialog](about.png) |
| Populated timeline | [Timeline with synthetic fixture](timeline.png) |
