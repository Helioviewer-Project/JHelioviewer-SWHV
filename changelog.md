
# Revision history for JHelioviewer

## JHelioviewer 2.16 (pending)

- Non-modal annotations (shift+click)
- Draw spiral in heliosphere
- Read Helioviewer metadata from JPG and PNG files
- Switch from NewtCanvasAWT to GLCanvas (full screen is lost)
- Support pixel scale in Windows 10
- Support fractional pixel scale
- Switch to Java 11
- Switch to OpenGL 3.3
- Move plugins options to preferences
- Support KCor dataset (#114)
- Simplify datetime selection and use NLP for input time parsing
- Request SOLO trajectory backdated 5 years
- Use install4j for packaging installation
- Support setting playback speed in time period per second
- Play sequence of files as movie
- Rework handling of threads throughout the program
- Incorporate SPICE and use it for input [time parsing](<https://naif.jpl.nasa.gov/pub/naif/toolkit_docs/C/cspice/str2et_c.html>) and position calculations for planets
- Support IRIS SJI
- Change video export to use FFmpeg and disk buffering
- Allow several video export qualities of H.264 and H.265, as well as series of PNGs
- Use SPICE for the calculations of internal reference frames
- Allow customization of grid in latitudinal projection (#99)
