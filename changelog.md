
# Revision history for JHelioviewer

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

- Add multi-scale Gaussian normalization (<https://arxiv.org/abs/1403.6613>) image enhancement

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
