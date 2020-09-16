
# Revision history for JHelioviewer

## JHelioviewer 2.16 (pending)

- Non-modal annotations (shift+click)
- Read Helioviewer metadata from JPG and PNG files
- Switch from NewtCanvasAWT to GLCanvas (full screen is lost)
- Support pixel scale in Windows 10 ([#75](<https://github.com/Helioviewer-Project/JHelioviewer-SWHV/issues/75>),[#76](<https://github.com/Helioviewer-Project/JHelioviewer-SWHV/issues/76>))
- Support fractional pixel scale
- Switch to Java 11
- Switch to OpenGL 3.3
- Move plugins options to preferences
- Support KCor dataset ([#114](<https://github.com/Helioviewer-Project/JHelioviewer-SWHV/issues/114>))
- Simplify datetime selection and use NLP for input time parsing
- Request SOLO trajectory backdated 5 years
- Use install4j for packaging installation
- Support setting playback speed in time period per second
- Play sequence of files as movie ([#119](<https://github.com/Helioviewer-Project/JHelioviewer-SWHV/issues/119>))
- Rework handling of threads throughout the program
- Incorporate SPICE and use it for input [time parsing](<https://naif.jpl.nasa.gov/pub/naif/toolkit_docs/C/cspice/str2et_c.html>) and position calculations for planets
- Support IRIS SJI
- Separate native libraries bundling per operating system
- Change video export to use FFmpeg and disk buffering
- Allow several video export qualities of H.264 and H.265, as well as series of PNGs ([#26](<https://github.com/Helioviewer-Project/JHelioviewer-SWHV/issues/26>),[#44](<https://github.com/Helioviewer-Project/JHelioviewer-SWHV/issues/44>),[#45](<https://github.com/Helioviewer-Project/JHelioviewer-SWHV/issues/45>))
- Use SPICE for the calculations of internal reference frames
- Allow customization of grid type in latitudinal projection ([#99](<https://github.com/Helioviewer-Project/JHelioviewer-SWHV/issues/99>))
- Draw spiral in heliosphere through the trajectory of the highlighted object
- Draw field-of-views of some SOLO, SDO and PROBA-2 remote sensing instruments; ability to "off-point" and to zoom-fit FOV with right-click
- Optionally distort displayed images according to solar differential rotation (Snodgrass, magnetic features)
- Load sequences of files as movie from the "jhv.load.image" SAMP message, example at <https://github.com/Helioviewer-Project/samp4jhv/blob/master/examples/python/samp_multi.py>
- Support Solar Orbiter remote sensing datasets (EUI, PHI, Metis, SoloHI)
- Support Hi-C 1 and 2.1
