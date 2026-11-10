# Revision history for JHelioviewer

## JHelioviewer 5.11.0 (pending)

### Display and rendering
- Add annotation color and line-thickness controls, and draw the active annotation thicker instead of red (fixes #156)
- Improve thick-line joins, rectangle corners, annotation loops, and field-of-view outlines
- Improve grid stability and longitude labels, and add grid color, opacity, line-width, and label-size controls
- Make trajectory colors readable against a white background (fixes #260)
- Add the RHEF radial histogram equalizing filter with an Upsilon midtone control (by @GillySpace27)
- Add RadialWarp and RectWarp wide-field views with adjustable outer-corona compression while keeping the solar disk linear, replacing Polar and LogPolar (by @GillySpace27)
- Add image-layer controls for outer-radius masking and sector direction and opening
- Improve image-canvas positioning and resizing on macOS
- Add GOES-R SUVI to the Earth-orbit field-of-view catalog
- Add glTF/GLB 3D model layers with surfaces, lines, points, textures, transparency, and lighting
- Show planets and stars at their apparent positions in the Location viewpoint, with names on hover, in Orthographic and HPC views
- Include Gaia DR3 stars and bright stars from Hipparcos, and update STEREO-A trajectory data
- Improve FITS image positioning and coordinate interpretation with WCSLIB, including angular units, rotated or skewed images, and CAR/CEA surface maps
- Fix surface-map sampling in HPC and warp views, image rotation, and alignment of reduced-resolution JPEG 2000 images
- Fix annotation visibility in projected views and restore zoom limits when zooming multiview panels separately
- Fix numerical artifacts in the MGN filter and refresh images when changing filters, including difference images

### Image loading and data
- Add a New PUNCH Layer source for FITS images from the PUNCH archive (by @GillySpace27)
- Allow selecting an image extension (HDU) and a cube plane in FITS layer options
- Read FITS images with CFITSIO, replacing the custom compressed-image reader
- Move FITS clipping and scaling controls into each image layer's options
- Use a shared percentile clipping range across FITS sequences to reduce brightness flicker, and remove ZScale clipping
- Use OpenJPEG as the default JPEG 2000 decoder, with Kakadu available as an alternative
- Improve JPIP movie loading, reconnection, cancellation, and disk-cache restoration, and reduce image-cache memory use
- Use the same proxy settings for HTTP and JPIP connections
- Add Proba-3 datasets to image-server browsing
- Preserve image-layer settings and the movie interval while replacement data loads
- Improve Callisto image loading, refresh, and saved-state restoration

### Interaction and UI
- Add frame-count sampling alongside time-step sampling when adding, replacing, and synchronizing image layers
- Add controls to collapse the sidebar and hide the status bar, toolbar, or toolbar text
- Show the original image URI in the Image Information dialog
- Show dataset time coverage in the New Image Layer dialog and distinguish short EUI exposures in layer names
- Improve layer-list updates when names or metadata change, and keep moved layers selected after reordering
- Improve download progress reporting and show Carrington rotation ranges in calendar tooltips
- Reorganize the sidebar and keep playback and navigation controls visible while scrolling through layer options
- Allow multiple dataset selections in the New Image Layer and New Timeline Layer dialogs
- Keep image request settings consistent when selecting datasets, replacing layers, and changing the movie interval
- Add a Show Annotations menu item to hide drawings without deleting them
- Change the background-color menu item to alternate between Use White Background and Use Black Background
- Allow holding X, Y, or Z to choose the rotation axis in Rotate Axis mode
- Keep mouse gestures tied to their starting view and finish them when the application loses focus
- Add general and timeline interaction guides to the Help menu
- Fix canvas positioning after display-scale changes on Windows and improve recovery after a graphics-context loss

### Timelines and events
- Add stacked timelines with independent vertical scales, scrolling, and a maximize/restore control
- Browse HAPI datasets from multiple catalogs and groups, and load predefined plots supplied by the server
- Support HAPI bar plots, value-level colors, warning thresholds, and a choice of multicolor or single-color rendering
- Improve timeline panning, zooming, and plotting of irregular data and gaps
- Improve HEK event handling for Flare Trigger events (fixes #105) and CACTus events (fixes #190)
- Disable the COMESEP event source
- Allow HAPI timelines to load at full time resolution, and preserve that choice in saved states and exports
- Allow additional HAPI servers to be configured in `sources.json`
- Improve timeline loading and drawing, including clearer lines on high-resolution displays and more legible overview dates
- Improve SWEK event loading, supplier filtering, cancellation, highlighting, related-event details, and updates after changing filters
- Preserve events with missing optional information and gaps between related observations
- Fix event-icon positioning across projections and clean up event controls when disabling the plugin

### Playback, export, and integration
- Add a Movie menu command and Cmd-R shortcut to start or stop recording
- Fix timeline export when the timeline panel is maximized
- Keep playback and loop recording within the selected time range, including solar-time playback
- Improve saved-state loading, including refresh-enabled states and restored plugin layers
- Fix recording-speed rounding and improve reporting of frame-capture and video-encoding failures
- Improve H.265 export quality and support for video sizes, and correct video color-transfer metadata
- Supply FITS settings through per-layer SAMP `imageParams` instead of the global FITS command
- Document the heliocentric 3D data interface and add a COCONUT conversion example

### Performance and maintenance
- Prepare spacecraft orbit trails and PFSS field lines in the background to reduce interruptions while viewing
- Reduce memory use and unnecessary redraws during image playback and timeline interaction
- Update bundled libraries, FFmpeg, native FITS and WCS libraries, SPICE kernels, and supporting build tools
- Expand regression coverage for image loading, rendering, coordinates, timelines, events, playback, and recording
- Various bug fixes, cleanups, and internal refactoring

## JHelioviewer 5.5.0 (2026-06-01)

### Display and rendering
- Add `ARC` WCS projection support
- Add option to zoom multiview panels separately
- Improve multiview layouts, flat-grid density, and grid label sizing
- Improve map projection, annotation, SWEK, and connection rendering performance

### Image loading and data
- Load user color lookup tables from `$HOME/JHelioviewer-SWHV/Settings/user-luts.txt`
- Harden FITS metadata handling, WCS parsing, LUT restoration, BLANK/BZERO/BSCALE handling, and clipping range behavior
- Speed up FITS loading with faster Rice decompression and normalized pixel-conversion lookup tables
- Improve JPIP/J2K robustness, including socket buffering, premature EOF handling, dropped decode callbacks, and resource cleanup
- Improve image-layer loading ownership and asynchronous decode handling

### Application control and integration
- Set IAS as default image data server, remove the automatic setting, and restore the preference in Settings
- Allow server-like headless operation, including full software rendering (see `extra/samp`)
- Expand SAMP support for image load completion and FITS view settings
- Improve startup, crash reporting, uncaught exception routing, and ANGLE diagnostics

### Timeline, events, and UI
- Improve timeline rendering performance with off-EDT polyline building, cached SWEK events, and reduced repaint churn
- Improve SWEK event insertion and association, related-event lookup, timeline cache, and popup ownership
- Reduce Swing coupling in layer options and layer selector updates

### Technical
- Expand GLSL/WCS validation coverage, including SwiftShader and Electron-backed shader checks
- Rework and tighten display/viewpoint/camera internals
- Various profile-driven optimizations, bug fixes, cleanups, and major internal refactoring

## JHelioviewer 5.0 (2026-05-08)

### Display and rendering
- Render through ANGLE using the native backend renderer of each platform (DirectX11, Metal, OpenGL)
- Replace the JOGL-based rendering path with LWJGL/OpenGL ES infrastructure
- Improve line rendering with shader antialiasing during live display (see `docs/line-rendering-sketch.png`)
- Improve text rendering with a fixed SDF glyph atlas
- Improve grid, FOV, SWEK, PFSS, point, and overlay rendering details
- Use MSAA specifically for frame/movie export quality
- Preserve layers across GL/ANGLE context recreation

### Image loading and playback
- Support arbitrary detector-frame masks, including the bundled EUI occulting mask
- Centralize caching and lifetime management of image-buffers. Move decoded image storage to direct/native buffers for GL upload
  and free them promptly on cache eviction (see `docs/image-buffer-cache.md`)
- Avoid extra heap copies for the common unfiltered image decode paths
- Replace serialized JPIP stream caching with compact segment log files
- Disable JPIP disk caching for the rest of the run after persistent-cache write/commit failures

### Application control and integration
- Restructure viewer, playback, recording, and state changes around explicit application commands
- Expose playback, recording, load-state, view, seek, and camera commands through SAMP (see `docs/jhv-samp-commands.md`)
- Add completion notifications for command-driven state loads and recordings

### Interaction and UI
- Decouple AWT input events from core application input handling
- Make transient cache paths ASCII-safe on Windows

### Technical
- Update bundled libraries, native rendering libraries, and supporting tools
- Various bug fixes, cleanups, and major internal refactoring

## JHelioviewer 4.8.1 (2026-04-19)

- Make possible to restrict playback range: drag with Option/Alt in frame slider to modify range, drag with Command/Ctrl to move range
- Various bug fixes, cleanups, and significant internal refactoring

## JHelioviewer 4.8.0 (2026-04-01)

### Display and interaction
- Add helioprojective Cartesian (`HPC`) projection mode
- Add "Observer at 1au" viewpoint option
- Add "New Synoptic Layer..." dialog for loading HMI and AIA synchronous synoptic FITS maps from <https://idoc-ssa-prod.ias.u-psud.fr/synopticmaps/s050a/portal/>
- Improve flat-grid rendering, labels, and position readout in non-orthographic modes
- Improve zoom, mouse interaction, and several toolbar/macOS interaction details, including `1:1` sizing in flat modes

### Image and datasets
- Add `AZP` and `ZPN` WCS projections support
- Add `CRL*-CAR` and `CRL*-CEA` WCS surface-map support
- Add more bundled colormaps

### Technical
- Add Astropy-based WCS validation tooling, test data, and documentation (see `docs/wcs-validation`)
- Various bug fixes, cleanups, and internal refactoring

### Caveats
- Display of heliospheric imaging data in `Orthographic` mode may not be ideal
- `HPC` mode is fundamentally linked to the observer viewpoint, the general Viewpoint functionality of JHV is not supported
- `CAR` and `CEA` datasets are supported only for `Latitudinal` and `Orthographic` modes

## JHelioviewer 4.7.7 (2026-03-03)

- Various bug fixes and performance improvements

## JHelioviewer 4.7.6 (2026-02-20)

- Improve display of events
- Improve startup time
- Various bug fixes and stability improvements

## JHelioviewer 4.7.5 (2026-02-10)

- Various bug fixes and stability improvements

## JHelioviewer 4.7.4 (2026-02-05)

- Make strength of radial enhancement configurable
- Add ability to manually set the FITS data clipping range and to adjust the ZScale contrast
- Load user SPICE kernels from `$HOME/JHelioviewer-SWHV/kernels`
- Load user server settings from `$HOME/JHelioviewer-SWHV/Settings/sources.json`
- Draw light timeline panel for light UI theme

## JHelioviewer 4.7.3 (2025-07-01)

- Improve performance on ARM Macs
- Add a light color UI theme
- Allow adjustment of image pointing
- Apply PV2_1 distortion for AZP and ZPN projections
- CCOR-1 dataset available from GSFC server

## JHelioviewer 4.7.2 (2025-06-11)

- Add option of ZScale clipping algorithm for FITS data
- Add logarithmic scaling for FITS data
- Add a subtle dither to reduce color banding of smooth gradient image data

## JHelioviewer 4.7.1 (2025-03-12)

- Add wavelet-optimized whitening (WOW, <https://doi.org/10.1051/0004-6361/202245345>) image enhancement
- Adjust the parameters of MGN image enhancement to bring it closer to WOW results

## JHelioviewer 4.7 (2024-07-17)

- Load timelines from new HAPI ROB server
- Support RHESSI datasets
- Add menu item to reload the list of datasets
- Allow to copy timestamp from image layer
- Switch to Java 21

## JHelioviewer 4.6.4 (2024-05-06)

- Attempt to detect image file formats and reduce dependency on file names extensions
- Allow loading of images by pasting files or their locations
- Use the possible colormap information from PNG files
- Switch to JetBrains Runtime; correct pixel factor for Linux HiDPI

## JHelioviewer 4.6.3 (2024-04-22)

- Make possible to adjust the FITS pixel conversion gamma parameter. Offer an alternative conversion controlled by beta. Use menu View -> FITS Settings
- Improve the metadata of the exported movies

## JHelioviewer 4.6.2 (2024-03-21)

- Fix Windows image canvas size bug
- Load zip files of images
- No need to restart after changing global image display settings

## JHelioviewer 4.6.1 (2024-02-08)

- Fix colormap bug

## JHelioviewer 4.6 (2024-02-07)

- New ROB server

## JHelioviewer 4.5.5 (2023-11-20)

- Avert a potential crash related to external screens

## JHelioviewer 4.5.4 (2023-11-20)

- Load PHI, Metis and SoloHI datasets from SOAR

## JHelioviewer 4.5.3 (2023-11-14)

- Add image inner masking
- Allow loading GONG images from all servers
- Set default image server by computer IP location (IAS for Europe, GSFC for rest of the world)

## JHelioviewer 4.5.2 (2023-08-14)

- Add line annotation
- Make Connection layer visible in released versions

## JHelioviewer 4.5.1 (2023-06-19)

- Toolbar button for automatic image layers refresh to current data every 15 minutes
- Allow loading IRIS SJI images from servers
- Ability to query SOAR by SOOP

## JHelioviewer 4.5 (2023-05-10)

- Capability for SSL connection to JPIP movie streaming server, required for GSFC server
- Up to six layers in multiview

## JHelioviewer 4.4.2 (2023-04-13)

- Allow loading of SOLO/EUI and GOES-R/SUVI images from all servers

## JHelioviewer 4.4.1 (2023-03-10)

- Updates and bugfixes

## JHelioviewer 4.4 (2022-12-20)

- Switch to Java 19
- Add macOS ARM64 (Apple Silicon) as supported computer architecture

## JHelioviewer 4.3.3 (2022-10-19)

- Improve metadata display to include comments and history
- Add toolbar menu to rotate view 90˚ around the axes
- Delay timelines in terms of time and not propagation speed

## JHelioviewer 4.3.2 (2022-05-11)

- Linux: switch to OpenJRE, GNOME HiDPI users will need to pass the pixel factor as argument at program start, e.g., `jhelioviewer -J-Dsun.java2d.uiScale=2.0`
- Add more export resolutions
- Load VOTable from SOAR
- Add menu option to show the current log
- Always playback at high resolution

## JHelioviewer 4.3.1 (2022-01-18)

- Windows: split program directory:
    - Cache and Downloads to `C:\Users\$USER\AppData\Local\Temp\JHelioviewer-SWHV\`
    - Exports and rest to `C:\Users\$USER\JHelioviewer-SWHV\`

## JHelioviewer 4.3 (2022-01-14)

- Overhaul logging system and remove log4j
- Switch to a dark theme
- Windows: move program directory to `C:\Users\$USER\AppData\Local\Temp\JHelioviewer-SWHV\` for better compatibility with non-ASCII user names

## JHelioviewer 4.2 (2021-12-10)

- Load EUI, MAG and SWA datasets from SOAR
- Add CDF file format support
- Allow drag'n'drop of directories
- Add preference option to adjust the image timestamp to be:
    - observed time minus light time from Sun center
    - observed time minus light time from Sun center plus light time to Earth

## JHelioviewer 4.1.1 (2021-10-30)

- Use SOLO heliospheric reference frames

## JHelioviewer 4.1 (2021-10-29)

- Indicate diameter of circle annotation
- Indicate height above sphere of loop annotation top
- Indicate pixel coordinates of the decoded image under the mouse pointer
- Add ESAC as source server
- Add preference option to playback at high resolution
- Double-click to reset sliders to default
- Add toolbar button to reset camera axis
- Allow playback without image layers loaded (within selected time interval)
- Switch to Java 17

## JHelioviewer 4.0.2 (2021-06-11)

- Add multi-scale Gaussian normalization (MGN, <https://arxiv.org/abs/1403.6613>) image enhancement

## JHelioviewer 4.0.1 (2021-05-31)

- Load SOLO/EUI, GOES-R/SUVI images from ROB server
- Allow to sync the current image layer time interval to the other layers
- Allow drag'n'drop of image files
- Use FlatLaf look'n'feel
- Bug fixes

## JHelioviewer 4.0 (2021-03-29)

### Technical
- Switch from NewtCanvasAWT to GLCanvas (full screen is lost)
- Switch to OpenGL 3.3
- Switch to Java 11
- Rework handling of threads throughout the program
- Use install4j for packaging installation
- Separate native libraries bundling per operating system
- Change video export to use FFmpeg and disk buffering
- Support pixel scale (HiDPI) in Windows 10 ([#75](<https://github.com/Helioviewer-Project/JHelioviewer-SWHV/issues/75>),[#76](<https://github.com/Helioviewer-Project/JHelioviewer-SWHV/issues/76>))
- Support pixel scale (HiDPI) in Linux
- Support fractional pixel scale

### User interface
- Move plugins options to preferences
- Non-modal annotations (use shift+click), add loop annotation
- Simplify datetime selection and use NLP for input time parsing
- Support setting playback speed in time period per second
- Allow customization of grid type in latitudinal projection ([#99](<https://github.com/Helioviewer-Project/JHelioviewer-SWHV/issues/99>))
- Add preference setting for several video export qualities of H.264 and H.265, as well as series of PNGs ([#26](<https://github.com/Helioviewer-Project/JHelioviewer-SWHV/issues/26>),[#44](<https://github.com/Helioviewer-Project/JHelioviewer-SWHV/issues/44>),[#45](<https://github.com/Helioviewer-Project/JHelioviewer-SWHV/issues/45>))
- Add white background view option
- Allow setting a state file to load at start-up

### Datasets
- Play sequence of files as movie ([#119](<https://github.com/Helioviewer-Project/JHelioviewer-SWHV/issues/119>))
- Incorporate SPICE and use it for input [time parsing](<https://naif.jpl.nasa.gov/pub/naif/toolkit_docs/C/cspice/str2et_c.html>) and position calculations for planets
- Use SPICE for the calculations of internal reference frames
- Draw spiral in heliosphere through the trajectory of the highlighted object
- Draw field-of-views of some SOLO, STEREO-A, SDO, and PROBA-2 remote sensing instruments with ability to "off-point"; draw borders of visible hemisphere, center meridian, and the great circle perpendicular on the central meridian
- Optionally distort displayed images according to solar differential rotation (Snodgrass, magnetic features)
- Read Helioviewer metadata from JPG and PNG files
- Load sequences of files as movie from the "jhv.load.image" SAMP message, example at <https://github.com/Helioviewer-Project/samp4jhv/blob/master/examples/python/samp_multi.py>
- Update AIA degradation correction
- Support IRIS SJI
- Support KCor dataset ([#114](<https://github.com/Helioviewer-Project/JHelioviewer-SWHV/issues/114>))
- Support Solar Orbiter remote sensing datasets (EUI, PHI, Metis, SoloHI)
- Support Hi-C 1 and 2.1 datasets
- Support GOES-R/SUVI datasets
