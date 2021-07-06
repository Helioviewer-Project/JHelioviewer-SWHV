
# Revision history for JHelioviewer

## JHelioviewer 4.0.3 (pending)

- Indicate diameter of circle annotation
- Indicate pixel coordinates of the decoded image under the mouse pointer
- Add ESAC as source server
- Add option to playback at high resolution

## JHelioviewer 4.0.2 (2021-06-11)

- Add multi-scale Gaussian normalization (<https://arxiv.org/abs/1403.6613>) image enhancement

## JHelioviewer 4.0.1 (2021-05-31)

- Load SOLO/EUI, GOES/SUVI images from ROB server
- Allow to sync the current image layer time interval to the other layers
- Allow drag'n'drop of image files
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
- Non-modal annotations (shift+click)
- Simplify datetime selection and use NLP for input time parsing
- Support setting playback speed in time period per second
- Allow customization of grid type in latitudinal projection ([#99](<https://github.com/Helioviewer-Project/JHelioviewer-SWHV/issues/99>))
- Allow several video export qualities of H.264 and H.265, as well as series of PNGs ([#26](<https://github.com/Helioviewer-Project/JHelioviewer-SWHV/issues/26>),[#44](<https://github.com/Helioviewer-Project/JHelioviewer-SWHV/issues/44>),[#45](<https://github.com/Helioviewer-Project/JHelioviewer-SWHV/issues/45>))
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
- Support GOES SUVI datasets
