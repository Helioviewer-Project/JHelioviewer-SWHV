JHelioviewer
============

[![Coverity Scan Build Status](https://scan.coverity.com/projects/9940/badge.svg)](https://scan.coverity.com/projects/9940)
[![DOI](https://zenodo.org/badge/50179170.svg)](https://zenodo.org/badge/latestdoi/50179170)

Website: https://www.jhelioviewer.org

User manual: https://swhv.oma.be/user_manual/

About
-----

JHelioviewer is a visualization tool for solar physics data based on the JPEG
2000 image compression standard, and part of the open source ESA/NASA Helioviewer
Project (https://github.com/Helioviewer-Project/).

JPEG 2000 offers many useful features and has the potential to revolutionize the
way high-resolution image data are disseminated and analyzed. Using JPEG 2000, data
can be served to a client in highly compressed, progressive in quality, and
region-of-interest based form. These features make it possible to minimize the data
transmitted while maximizing the use of the data that is transmitted.

Solar observatories are providing the world-wide community with a wealth of data,
covering wide time ranges (e.g. Solar and Heliospheric Observatory, SOHO), multiple
viewpoints (Solar TErrestrial RElations Observatory, STEREO), and returning large
amounts of data (Solar Dynamics Observatory, SDO). In particular, the large volume
of SDO data presents challenges; the data are available only from a few repositories,
and full-disk, full-cadence data for reasonable durations of scientific interest are
difficult to download, due to their size and the download rates available to most
users. From a scientist's perspective this poses three problems: accessing, browsing,
and finding interesting data as efficiently as possible.

With JHelioviewer users can visualise the Sun for any time period between September
1991 and today; they can perform basic image processing in real time, track features
on the Sun, and interactively overlay magnetic field extrapolations. The software
integrates solar event data and a timeline display. Once an interesting event has
been identified, science quality data can be accessed for in-depth analysis.

As support for the science planning of the Solar Orbiter mission, JHelioviewer
offers a virtual camera model that enables users to set the vantage point to the
location of a spacecraft or celestial body at any given time.

References:

- JHelioviewer paper by Mueller et al., Astronomy & Astrophysics, 2017:
  https://doi.org/10.1051/0004-6361/201730893

- JHelioviewer paper by Mueller et al., Computing Science and Engineering, 2009:
  http://jhelioviewer.org/pub/Mueller+al_CiSE2009.pdf

- Information about JPEG 2000:
  http://wiki.helioviewer.org/wiki/JPEG_2000

Report a Problem or Idea
------------------------

If you have a problem or have ideas for improvements please report them at:

https://github.com/Helioviewer-Project/JHelioviewer-SWHV/issues

We will try to solve your problem as fast as possible. The more details you can
provide, the easier it is for us. For example your system specifications
(hardware and software with version numbers) as well as a detailed description
what you did (so we can reproduce the problem) are very helpful. If possible,
please also be ready to provide the log files from the session where the problem
occured. The log files can be found in the Logs/ directory of your JHelioviewer
home folder. You can find the correct log file by searching for the correct
start date. The files have the form:

JHV_'year'-'month'-'day'_'hours'.'minutes'.'seconds'.log
