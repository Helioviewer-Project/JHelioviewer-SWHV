---
title: SWHV CCN2 Design Document
subtitle: ROB-SWHV(7186)-DDF2 v1.4
author: SWHV Team
subject: Space Weather HelioViewer
date: 2018-11-21
geometry: margin=1in
papersize: A4
book: true
toc: true
colorlinks: true
mainfont: "Source Serif Pro"
romanfont: "Source Serif Pro"
sansfont: "Source Sans Pro"
monofont: "Source Code Pro"
titlepage: true
titlepage-color: "0088cc"
titlepage-text-color: "ffffff"
titlepage-rule-color: "ffffff"
titlepage-rule-height: 5
listings-disable-line-numbers: true
logo: hvLogo.png
logo-width: 40
...
\frenchspacing

`id: \exec{git hash-object \file}`

[architecture]: jhv_architecture.pdf
[traceability]: wp_traceability.pdf

[^jpylyzer]: <https://github.com/openpreserve/jpylyzer>
[^schematron]: <http://en.wikipedia.org/wiki/Schematron>
[^openjpeg]: <http://www.openjpeg.org>
[^glymur]: <https://github.com/quintusdias/glymur>
[^pfssPaper]: <http://wso.stanford.edu/words/pfss.pdf>

# Introduction

+---------------------+------------------------------------------------------------------------------------------+
|Contributing authors | Roman Bolzern (FHNW), Bram Bourgoignie (ROB), Silvan Laube (FHNW), Bogdan Nicula (ROB), Freek Verstringe (ROB)|
+---------------------+------------------------------------------------------------------------------------------+
|Approved by          | Bogdan Nicula (ROB)                                                                      |
+---------------------+------------------------------------------------------------------------------------------+

+------------+--------------------------------------------------------------------+
| Date       | Notes                                                              |
+:===========+:===================================================================+
| 2018-01-15 | Version 1.00 (Initial release)                                     |
+------------+--------------------------------------------------------------------+
| 2018-03-12 | Version 1.01 (Update following CDR2)                               |
+------------+--------------------------------------------------------------------+
| 2018-04-01 | Version 1.1 (Add design notes)                                     |
+------------+--------------------------------------------------------------------+
| 2018-04-04 | Version 1.2 (Clarify document structure, more server design notes) |
+------------+--------------------------------------------------------------------+
| 2018-10-17 | Version 1.3 (Complete the sections about CCN2 work)                |
+------------+--------------------------------------------------------------------+
| 2018-11-21 | Version 1.4 (Complete JHV design notes for FAR))                   |
+------------+--------------------------------------------------------------------+

## Purpose & Scope

This document (SWHV-DDF2) is the design study report of the work performed during the CCN2 phase of "Space Weather Helioviewer" project (Contract No. 4000107325/12/NL/AK, "High Performance Distributed Solar Imaging and Processing System" ESTEC/ITT AO/1-7186/12/NL/GLC). It focuses on the detailed explanation of the changes for several software components.

## Applicable Documents

\[1\] Contract Change Notice No. 2: SWHV-CCN2-Proposal3-BN2.pdf

## Reference Documents

\[2\] TR Architectural outline of JHV3 by Simon Felix (FHNW)

# Work Logic

In the following JHelioviewer and SWHV are used interchangeably. They refer to the Java client of the Helioviewer system available at <https://github.com/Helioviewer-Project/JHelioviewer-SWHV> and subject of this project. The term JHV3D refers to a similar software, outcome of SRE-SM/JHV3 "Time-Dependent 3-D Visualisation of Solar Data" project, available at <https://github.com/Helioviewer-Project/JHelioviewer>. A big part of the work performed during the CCN2 phase was merging of ideas from JHV3D into SWHV.

The current system architecture is presented in Chapter 3, the interfaces of the JHelioviewer client are presented in Chapter 4, the current design of JHelioviewer is presented in Chapter 5, while the Chapter 6 presents the identified tasks for the CCN2 phase.

Chapter 7 presents a traceability matrix for the tasks, as well as the assigned priority and the milestone for delivery. Features already delivered will be subjected to refinement and refactoring as new functionality becomes available in the client-server system.

# System Architecture #

The following figure depicts the architecture of the Helioviewer system as installed on the ROB server. For the purpose of this project, the focus is on the interaction between the JHelioviewer client and the Helioviewer server.

![Helioviewer system architecture][architecture]

## Server Infrastructure ##

The following servers are included:

- HTTP server (e.g., Apache or `nginx`) to serve static files, to proxy HTTP requests, and to run various services:
  - Image services API (<https://github.com/Helioviewer-Project/api>): It lists the available image datasets and commands the creation of JPX movies on demand. It includes a facility to ingest new images files. Metadata about the image files is stored in a MySQL database.
  - Timeline services API: This is an adapter brokering between the JHelioviewer client and the backend timeline storage services (ODI -- <http://lund.irf.se/odi/> and STAFF backend -- <http://www.staff.oma.be>). It lists the available timeline datasets and serves the data in a JSON format.
  - A PFSS dataset, static FITS files produced regularly out of GONG magnetograms. The JHelioviewer client retrieves them on demand, based on monthly listings (e.g., <http://swhv.oma.be/magtest/pfss/2018/01/list.txt>).
  - COMESEP service which subscribes to the COMESEP alert system (not part of this project), stores the alerts and makes them available to the JHelioviewer server in a JSON format.
  - The Helioviewer web client (<https://github.com/Helioviewer-Project/helioviewer.org>), not relevant for this project.
- The `esajpip` server (<https://github.com/Helioviewer-Project/esajpip-SWHV>), which delivers the JPEG2000 data streams to the JHelioviewer client using the JPIP protocol, built on top of the HTTP network protocol.
- The `GeometryService` server (<https://github.com/Helioviewer-Project/GeometryService>) implements a set of high precision celestial computation services based on NASA's Navigation and Ancillary Information Facility (NAIF) SPICE Toolkit and communicates with the JHelioviewer client using a JSON format.
- The `PropagationService` server (<https://github.com/Helioviewer-Project/PropagationService>) is currently a mock-up server with the aim in aiding the correlation of in-situ data with remote sensing data.
- The HEK server (maintained by LMSAL <https://www.lmsal.com/hek/>, not part of this project) which serves JSON formatted heliophysics events out of HER. JHelioviewer retrieves a curated list of space weather focused events.

To ensure encapsulation, reproducibility, and full configuration control, the services which are part of this project are currently being containerized at <https://gitlab.com/SWHV/SWHV-COMBINED>.

## JPEG2000 Infrastructure ##

### JPIP Server ###

The `esajpip` server serves the JPEG2000 encoded data to the JHelioviewer client. This software was forked from the code at <https://launchpad.net/esajpip>. It was ported to a CMake build system and to C++11 standard features. Several bugs, vulnerabilities, and resource leaks (memory, file descriptors) were solved; sharing and locks between threads were eliminated; C library read functions were replaced by memory-mapping of input files (for up to 10× higher network throughput). The JPX metadata is now sent compressed and several ranges of images can be requested in one JPIP request.

The code is periodically verified with IDEA CLion and Synopsys Coverity static code analyzers and with `valgrind` dynamic analyzer, as well as various sanitize options of several C++ compilers.

### FITS to JPEG2000 ###

The JPEG2000 files can be created with the IDL JPEG2000 implementation. Those files have to be transcoded for the use in the Helioviewer system.

The open-source alternative for the creation of JPEG2000 files is the `fits2img` package (<https://github.com/Helioviewer-Project/fits2img>), which uses a patched version of the open source OpenJPEG[^openjpeg] library. The files created by this tool do not need to be transcoded.

While ingesting new datasets during the SWHV project, it became apparent that the metadata in the FITS headers of some datasets is lacking or is defective. A FITS-to-FITS conversion stage is needed for those datasets to adjust the metadata to the needs of the Helioviewer system. At <https://github.com/bogdanni/hv-HEP/blob/master/HEP-0010.md> there is a summary of those needs.

The following new datasets were added in the course of the project: Kanzelhoehe H-alpha,  NSO-GONG far side, NSO-GONG H-alpha, NSO-GONG magnetogram, NSO-SOLIS Azimuth, NSO-SOLIS CoreFluxDens, NSO-SOLIS CoreWingInt, NSO-SOLIS FillFactor, NSO-SOLIS Inclination, NSO-SOLIS Intensity 1083Å, NSO-SOLIS Intensity 6302Å, NSO-SOLIS Strength, ROB-USET H-alpha.

In addition, daily radio spectrograms are created from the Callisto network observations. The data files are downloaded from the e-Callisto network website and merged into a composite dataset in order to ensure good 24-hour coverage. The data values are calibrated to correct for instrument sensitivity in frequency and time. During this operation the values are also normalized and transformed to fit into fixed time and frequency bins, covering fixed time and frequency ranges. When multiple values contribute to one bin of the overall image, an average is taken as the final value. An averaging procedure was implemented in order to reduce the noise. It only involves approximately the highest 10% of the signal. This allows reducing the noise sufficiently, while still being able to have enough contrast. The data is written to a temporary FITS file with keywords indicating the frequency and time ranges, frequency and time bin sizes. The composite image is then transformed into a JPEG2000 image file (size 86400×380), one per day.

### JPEG2000 Files Handling ###

The image data is encoded using the JPEG2000 coding standards. The JPEG2000 data consists of compressed data codestreams organized using *markers* according to a specific syntax, and several file formats, such as JP2 and JPX, which are organized using *boxes* encapsulating the codestreams of compressed image data organized in *packets* and the associated information.

In order to ensure the communication between the server and the client, the Helioviewer system imposes a set of constraints on the codestreams and file formats. This includes requirements for codestream organization such as specific packetization (PLT markers), coding precincts, and order of progression (RPCL), for file format organization, such as the presence of specific boxes aggregating the codestreams and the associated information like metadata, and for file naming conventions.

The JPEG2000 standards have a high degree of sophistication and versatility. In order to encourage the proliferation of Helioviewer image datasets, it should be possible to generate those files with standard conforming software other than the proprietary Kakadu software currently used. It becomes therefore necessary to validate the full structure of Helioviewer image files formally and automatically. A verification system based on Schematron[^schematron] XML schemas was developed. This procedure is able to verify the structure of JPEG2000 file and codestream, including the associated information such as the Helioviewer specific XML metadata, ensuring the end-to-end compatibility with the Helioviewer system.

Before the SWHV project, both the server and the client-side software were derived from the Kakadu Software toolkit (<http://kakadusoftware.com>). Much of the server-side usage of the Kakadu software can be now replaced with the `fits2img` and `hvJP2K` (<https://github.com/Helioviewer-Project/hvJP2K>) packages. If the server does not handle files produced by IDL, no server-side use of Kakadu software is necessary.

`hvJP2K` consists in the following tools:

- `hv_jp2_decode`: replacement for `kdu_expand`, sufficient for the web client image tile decoding;
- `hv_jp2_encode`: proto-replacement for `fits2img`, not yet capable of emitting conforming JP2 files;
- `hv_jp2_transcode`: wrapper for `kdu_transcode`, it can output JP2 format and can re-parse the XML metadata to ensure conformity;
- `hv_jp2_verify`: verify the conformity of JP2 file format to ensure end-to-end compatibility;
- `hv_jpx_merge`: standalone replacement for `kdu_merge`, it can create JPX movies out of JP2 files;
- `hv_jpx_mergec`: client for `hv_jpx_merged`, written in C;
- `hv_jpx_merged`: Unix domain sockets threaded server for JPX merging functionality, it avoids the startup overhead of `hv_jpx_merge`;
- `hv_jpx_split`: split JPX movies into standalone JP2 files.

This software is mainly written in Python and is based on the `glymur`[^glymur] and `jpylyzer`[^jpylyzer] open source libraries.

The Helioviewer server needs to interpret JPEG2000 data at several stages:

1. During the generation of the JP2 files -- until now typically from software code written in IDL. `fits2img` can replace the Kakadu implementation embedded in IDL. It outputs conforming JP2 files with PLT markers, XML boxes and it has the capability of embedding colormaps.

1. During the ingestion into the Helioviewer server -- since the IDL code cannot be configured for the required features, i.e., precincts and PLT markers, the JP2 files produced by IDL have to be transcoded. The vanilla version of Kakadu's `kdu_transcode` program is just a demo application for the Kakadu core system and is not able to produce JP2 files. `hv_jp2_transcode` can wrap the transcoding process and allows to use an unmodified `kdu_transcode`. It is not yet possible to replace the core functionality of `kdu_transcode`. This stage is not necessary for files generated by `fits2img`.

1. The web client itself has to decode the JP2 files in order to serve the image data to its browser clients, since those do not typically include JPEG2000 support. This can be replaced by `hv_jp2_decode`, at the cost of lower performance, but with little impact for the user experience.

1. Before the image data is streamed using the JPIP server, it has to be aggregated into JPX files. `kdu_merge` can be replaced with `hv_jpx_merge` (up to 10× faster than `kdu_merge`). This task involves only manipulations at the byte level structure of the file formats and not the decoding of the codestreams. From the point of view of the JHelioviewer user, the client -- server interaction latency and bandwidth dominate the waiting time for the display of the image data. The server latency has two main components: the database query for the list of files that will make up the JPX and the parsing of those files to extract the information for the assembly of the JPX.

Additionally, the JHelioviewer client has to decode the codestreams for display. The Kakadu software offers superior performance in this area and, for the foreseeable future, will not be replaced on the client side.

## PFSS Dataset ##

A PFSS algorithm[^pfssPaper] was implemented in C for fast computation. GONG magnetograms from <http://gong.nso.edu/data/magmap/index.html> are used as input. Those FITS files contain a full map of the latest available solar magnetic data at a resolution of 256×180 in a sine-latitude grid.

The PFSS algorithm consists of several steps:

1. For algorithm input, interpolate the magnetogram data onto an appropriate grid;
1. Run the algorithm by solving a Poisson-type equation;
1. Select the field lines and save them to a FITS file.

For the starting points on the photosphere, an equally spaced `theta`--`phi` grid of points that lie above the photosphere is used. The set of starting points is augmented with starting points for which the magnetic field is strong.

The algorithm to compute the field lines uses an Adams–Bashforth explicit method (third order precision) that requires less evaluations of the vector field than the more commonly used fourth order precision Runge-Kutta methods. This is mainly done because the evaluation of the vector field at a given point is relatively slow.

The resulting FITS files consist of `BINARY TABLE`s with four columns `FIELDLINEx`, `FIELDLINEy`, `FIELDLINEz`, `FIELDLINEs`. The first three are mapped to unsigned shorts and can be converted to Cartesian coordinates using the formula 3 × (value × 2 / 65535 - 1) (on the client side one needs to add 32768). The `FIELDLINEs` value encodes the strength and, by the sign, the radial direction of the field. This encoding was chosen for a compact representation.

The field strength is mapped in the default JHelioviewer display as blue (negative radial) or red (positive radial); the lesser the color saturation, the weaker the field. In order to better see the direction of the field, points of the lines beyond 2.4 solar radii have red or blue colors without blending with white.

# JHelioviewer Interfaces #

JHelioviewer communicates with the Helioviewer services using the HTTP network protocol. The JPEG2000 data service is implemented using a subset of the JPIP protocol on top of the HTTP network protocol. In addition, JHelioviewer supports the SAMP protocol (<http://www.ivoa.net/documents/SAMP/>) and includes a SAMP hub.

## Image Services API ##

The image services API is documented together with some examples at <https://api.helioviewer.org/docs/v2/>.

The API endpoints used by JHelioviewer are:

- `getDataSources`: retrieve the list of available datasets which is then used by JHelioviewer to populate the "New Layer" UI elements for each server;
- `getJPX`: request a time series of JP2 images as one JPX file;
- `getJP2Image`: to request one JP2 image.

The API server reacts to the `getJPX` call by querying its database of image metadata, constructing a list of filenames to be used, passing the list to the program that will create the JPX file (`kdu_merge` or `hv_jpx_merge`), and making the resulting JPX file available to the `esajpip` server. Finally, the server sends back to the client a JSON response which contains the JPIP URI to use. The client connects to that URI and starts interacting with the JPIP server.

JPX files can be of two types:

- aggregates of metadata with pointers to the JPEG2000 codestream data inside the original JP2 files;
- aggregates of data with embedded JPEG2000 codestreams.

The first form (`linked=true` and `jpip=true`) is used for the regular JHelioviewer interaction over JPIP, while the second form (`linked=false` and `jpip=false`) is used by the "Download layer" functionality to request the assembly of a self-contained JPX file which is then retrieved over HTTP and can be played back on the user computer without a network connection.

The JPEG2000 data services are provided by the `esajpip ` server which implements a restricted subset of the JPIP protocol over HTTP (to be described).

## Timeline Services API ##

The timeline API is a REST service and consists of three parts.

1. **Dataset Query API**: The client requests the description of the available datasets. The description includes group and label names to be used client-side, server-side name and units. It is available at <http://swhv.oma.be/datasets/index.php> and will list all datasets available and the corresponding groups. The response will be a JSON file with 2 keys:
    - `groups`: the list of groups visible in the client, it has 2 keys:
        - `key`: a unique identifier;
        - `groupLabel`: the name of the group.
    - `objects`: the list of datasets with keys:
        - `baseUrl`: the base URL of where to request the dataset;
        - `group`: the group identifier to which the dataset belongs;
        - `label`: the label of the dataset;
        - `name`: the unique identifier of the dataset.

1. **Data Availability API**: The server returns `coverage` as an array of disjoint time intervals, in increasing order. The coverage intervals are defined as containing data samples no further apart than five times the regular cadence. A similarly defined parameter demarcates the data gaps in the responses to the Data Request API.

1. **Data Request API**: Each individual dataset can be accessed with a URL like:

```
http://swhv.oma.be/datasets/odi_read_data.php?
        start_date=2016-09-01&
        end_date=2016-09-02&
        timeline=GOES_XRSA_ODI&
        data_format=json
```

The first part of the URL is the `baseUrl` from the Dataset Query API. The parameters in the URL are:

- `start_date`: the start date of the wanted timeline in the format YYYY-MM-DD;
- `end_date`: the end date of the wanted timeline in the format YYYY-MM-DD (full day included);
- `timeline`: the name of the timeline as defined by the Dataset Query API;
- `data_format`: only JSON available currently.

The response is a JSON file with the keys:

- `timeline`: the name of the timeline as defined in the Data Request API;
- `multiplier`: the multiplier that needs to be applied to the values;
- `data`: a list of `[timestamp,value]` pairs. The values have to be multiplied by the `multiplier`.

The `multiplier` parameter allows for sending scaled data to the client when necessary. The values of many datasets are rather small numbers when expressed in standard units like W/m², thus scaling them allows for more floating-point precision in the response to the client.

The timestamps are with respect to Unix epoch. There is a guarantee that the data is sent ordered by timestamp.

The timeline API is implemented on the server side as several PHP scripts that forward the data requests to the relevant backend timeline storage service and format the JSON response for the JHelioviewer client. The current backend services are based on ODI and STAFF.

## HEK Services API ##

This API is described at <http://solar.stanford.edu/hekwiki/ApplicationProgrammingInterface>.

A similar API was implemented for the COMESEP alert caching server.

## GeometryService API ##

The `GeometryService` is a REST network service which can return JSON and MessagePack (<https://msgpack.org>) encoded responses. For example, given the following request:

```
http://swhv.oma.be/position?
            utc=2014-04-12T20:23:35&
            utc_end=2014-04-13T19:44:11&
            deltat=21600&
            observer=SUN&
            target=STEREO%20Ahead&
            ref=HEEQ&
            kind=latitudinal
```

the server returns the following JSON response:

```json
{
  "result": [
    { "2014-04-12T20:23:35.000":
      [143356392.01232576,2.712634949777619,0.12486990461569629]},
    { "2014-04-13T02:23:35.000":
      [143359318.57914788,2.7129759257313513,0.12473463991365513]},
    { "2014-04-13T08:23:35.000":
      [143362256.29411626,2.7133174795109087,0.12459673837570125]},
    { "2014-04-13T14:23:35.000":
      [143365205.0945752,2.713659603829239,0.12445620339056596]}
  ]
}
```

This is a list of UTC timestamps and coordinates indicating the geometric position of the STEREO Ahead spacecraft in this example. The first coordinate is the distance to Sun, the second and third coordinates are the Stonyhurst heliographic longitude and latitude of the given object.

The maximum number of points accepted for computation is 1e6 and the Python code distributes the computation to the available number of processors in the computer using Python multi-processing because the SPICE library is not re-entrant.

At the moment, the following locations are available: all JPL DE430 ephemeris locations (solar system planets, Pluto, the Moon), comet 67P/Churyumov-Gerasimenko. Also available are the following spacecraft trajectories (existing or planned): SOHO, STEREO, SDO, PROBA-2, PROBA-3, Solar Orbiter, Parker Solar Probe. Several reference frames often used in the heliophysics domain are known.

The following functions are implemented:

- `position` and `state` (in km, km/s, and/or radian) of `target` relative to `observer` in `ref` reference frame, optionally corrected for `abcorr` (`NONE` - geometric, default, `LT`, `LT%2BS`, `CN`, `CN%2BS`, `XLT`, `XLT%2BS`, `XCN`, `XCN%2BS`, see SPICE documentation); representations (`kind`): `rectangular` (default), `latitudinal`, `radec`, `spherical`, `cylindrical`;

- `transform` between `from_ref` reference frame and `to_ref` reference frame; representations (`kind`): `matrix` (default), `angle` (Euler, radian), `quaternion` (SPICE format);

- `utc2scs` and `scs2utc`: transform between UTC and spacecraft OBET (Solar Orbiter supported).

Other arguments:

- `utc`: start of time range;
- `utc_end` (optional): end of time range;
- `deltat` (optional): time step in seconds.

There is a guarantee that the data is sent ordered by UTC timestamps.

This service is used to support the Viewpoint functionality of JHelioviewer.

## PropagationService API ##

The `PropagationService` is a REST network service which returns JSON encoded responses. It is currently just a mock-up and handles only the case of radial propagation with a fixed speed. It uses the `GeometryService`.

The following function is implemented:

- `propagate`
    - Arguments:
        - `name` (ignored): quantity;
        - `utc`: start of time range;
        - `utc_end` (optional): end of time range;
        - `deltat` (optional): time step.
    - Returns: rectangular coordinates position of SOHO in HEEQ reference frame in km and fixed propagation speed of 1000km/s.

## SAMP ##

The incoming SAMP messages supported by JHelioviewer are:

- `image.load.fits`: specific FITS image;
- `table.load.fits`: only for ESA SOHO Science Archive tool, as JHelioviewer does not support FITS tables yet;
- `jhv.load.image`: any image type supported by JHelioviewer, type determined by filename extension;
- `jhv.load.request`: image request file;
- `jhv.load.timeline`: timeline request file;
- `jhv.load.state`: state file.

Those messages have as parameter an URI to load. Both local and remote URIs are supported. An example of SAMP Web Profile usage is at <http://swhv.oma.be/test/samp/>.

In addition, JHelioviewer can broadcast information about the loaded image layers. Two clients of this functionality use the information to load the corresponding science data from virtual solar observatories into the SolarSoft and SunPy environments. They are available in the source tree in the directories `extra/samp/idl` and `extra/samp/python`, respectively.

The broadcasted SAMP message has the following form:

- Message: `jhv.vso.load`
- Arguments:
    - timestamp (string): date of the currently viewed frame coded in ISO8601 format (e.g., 2017-08-28T14:33:28);
    - start (string): start date of the currently viewed sequence coded in ISO8601 format (e.g., 2017-08-28T14:33:28);
    - end (string): end date of the currently viewed sequence coded in ISO8601 format (e.g., 2017-08-28T14:33:28);
    - cadence (SAMP long): number of milliseconds between each frame;
    - cutout.set (SAMP boolean): whether or not only a part of the sun is visible:
        - 0: the full Sun is visible;
        - 1: only a cutout of the Sun is visible.
    - cutout.x0 (SAMP float, arcsec, optional): x-position of the currently viewed part of the Sun;
    - cutout.y0 (SAMP float, arcsec, optional): y-position of the currently viewed part of the Sun;
    - cutout.w (SAMP float, arcsec, optional): width of the currently viewed part of the Sun;
    - cutout.h (SAMP float, arcsec, optional): height of the currently viewed part of the Sun;
    - layers (list of map): the different layers currently displayed. The parameters of each layer are stored as a key-value pair with the following keys:
        - observatory (string, required);
        - instrument (string, required);
        - detector (string, optional);
        - measurement (string, optional);
        - timestamp (string, ISO8601 date, required).

        The keys which are set depend on the selected instrument.
- Return Values: None.
- Description: Broadcasts information about all the currently visible layers in JHelioviewer including the current timestamp. Receiving applications can use this information to load the raw data from VSO, for example.

Example:

```
{
  samp.mtype=jhv.vso.load,
  samp.params={
    timestamp=2017-09-24T19:52:28, start=2017-09-24T00:00:00, end=2017-09-26T00:00:00,
    cutout.set=1, cutout.h=2460.524544, cutout.w=2460.524544, cutout.x0=3.3579912600000625, cutout.y0=1.1773994400000447,
    cadence=1800000,
    layers=[
      {observatory=SDO, instrument=AIA, detector=, measurement=304, timestamp=2017-09-24T19:53:05},
      {observatory=SDO, instrument=AIA, detector=, measurement=171, timestamp=2017-09-24T19:52:45},
      {observatory=SDO, instrument=AIA, detector=, measurement=193, timestamp=2017-09-24T19:52:28}
    ]
  }
}
```

## File Formats ##

Many of the file formats supported by JHelioviewer are based on the JSON format. All files can be either local on the user's computer or can be loaded over HTTP. JPX data can additionally be loaded over the JPIP protocol. Files can be loaded at start-up via the command line interface.

### State File ###

JSON document specifying with high accuracy the program state. Most of the fields have direct correspondence to the user interface, thus they are self-documenting. Natural language specification for time is supported. Most of the fields are optional with sensible defaults.

```json
{
  "org.helioviewer.jhv.state": {
    "play": false,
    "multiview": false,
    "showCorona": true,
    "imageLayers": [
      {
        "data": {
          "APIRequest": {
            "sourceId": 13,
            "server": "IAS",
            "startTime": "2017-09-24T10:52:57",
            "endTime": "2017-09-26T10:52:57",
            "cadence": 1800
          },
          "imageParams": {
            "slitLeft": 0,
            "enhanced": true,
            "sharpen": 0.5,
            "differenceMode": "None",
            "color": {
              "red": true,
              "green": false,
              "blue": false
            },
            "invert": false,
            "brightScale": 1,
            "blend": 0,
            "brightOffset": 0,
            "slitRight": 1,
            "opacity": 1
          }
        },
        "name": "AIA 304",
        "className": "org.helioviewer.jhv.layers.ImageLayer",
        "enabled": true
      },
      {
        "data": {
          "APIRequest": {
            "sourceId": 10,
            "server": "IAS",
            "startTime": "2017-09-24T10:52:57",
            "endTime": "2017-09-26T10:52:57",
            "cadence": 1800
          },
          "imageParams": {
            "slitLeft": 0,
            "enhanced": true,
            "sharpen": 0.5,
            "differenceMode": "None",
            "color": {
              "red": false,
              "green": true,
              "blue": false
            },
            "invert": false,
            "brightScale": 1,
            "blend": 0,
            "brightOffset": 0,
            "slitRight": 1,
            "opacity": 1
          }
        },
        "name": "AIA 171",
        "className": "org.helioviewer.jhv.layers.ImageLayer",
        "enabled": true
      },
      {
        "data": {
          "APIRequest": {
            "sourceId": 11,
            "server": "IAS",
            "startTime": "2017-09-24T10:52:57",
            "endTime": "2017-09-26T10:52:57",
            "cadence": 1800
          },
          "imageParams": {
            "slitLeft": 0,
            "enhanced": true,
            "sharpen": 0.5,
            "differenceMode": "None",
            "color": {
              "red": false,
              "green": false,
              "blue": true
            },
            "invert": false,
            "brightScale": 1,
            "blend": 0,
            "brightOffset": 0,
            "slitRight": 1,
            "opacity": 1
          }
        },
        "name": "AIA 193",
        "className": "org.helioviewer.jhv.layers.ImageLayer",
        "enabled": true,
        "master": true
      }
    ],
    "timelines": [
      {
        "data": {
          "colormap": "Spectral"
        },
        "name": "Callisto Radiogram",
        "className": "org.helioviewer.jhv.timelines.radio.RadioData",
        "enabled": false
      },
      {
        "data": {},
        "name": "SWEK Events",
        "className": "org.helioviewer.jhv.plugins.swek.EventTimelineLayer",
        "enabled": true
      },
      {
        "data": {
          "bandType": {
            "baseUrl": "http://swhv.oma.be/datasets/odi_read_data.php?",
            "unitLabel": "W/m^2",
            "name": "GOES_XRSB_ODI",
            "range": [
              1e-07,
              0.001
            ],
            "scale": "logarithmic",
            "label": "GOES XRS-B (longwave)",
            "warnLevels": [
              {
                "warnLabel": "B",
                "warnValue": 1e-07
              },
              {
                "warnLabel": "C",
                "warnValue": 1e-06
              },
              {
                "warnLabel": "M",
                "warnValue": 1e-05
              },
              {
                "warnLabel": "X",
                "warnValue": 0.0001
              }
            ],
            "group": "GROUP_SWHV"
          },
          "color": {
            "r": 80,
            "b": 80,
            "g": 80
          }
        },
        "name": "GOES XRS-B (longwave)",
        "className": "org.helioviewer.jhv.timelines.band.Band",
        "enabled": true
      }
    ],
    "plugins": {
      "org.helioviewer.jhv.plugins.pfss.PfssPlugin": {},
      "org.helioviewer.jhv.plugins.swek.SWEKPlugin": {
        "Active Region": {
          "NOAA SWPC": false,
          "SPoCA": false
        },
        "Flare": {
          "NOAA SWPC": false
        },
        "Filament": {
          "AAFDCC": false
        },
        "Eruption": {
          "Eruption Patrol": false
        },
        "Emerging Flux": {
          "EFRM": false
        },
        "Coronal Hole": {
          "SPoCA": false
        },
        "Coronal Wave": {
          "Halo CME": false
        },
        "Flare Trigger": {
          "Flare Detective": false
        },
        "COMESEP": {
          "Solar Demon": false,
          "Drag Based Model": false,
          "Geomag24": false,
          "CACTus": false,
          "SEP Forecast": false,
          "Flaremail": false,
          "cgft": false,
          "GLE Alert": false
        },
        "Coronal Dimming": {
          "Coronal Dimming Module": false,
          "Halo CME": false
        },
        "Coronal Mass Ejection": {
          "CACTus": false
        },
        "Sunspot": {
          "EGSO SFC": false
        },
        "Filament Eruption": {
          "Halo CME": false
        }
      },
      "org.helioviewer.jhv.plugins.eve.EVEPlugin": {
        "selectedAxis": {
          "startTime": "2017-09-24T10:52:57",
          "endTime": "2017-09-26T10:52:57"
        },
        "locked": false
      }
    },
    "layers": [
      {
        "data": {
          "showLabels": true,
          "lonStep": 15,
          "latStep": 20,
          "showRadial": false,
          "showAxis": true,
          "type": "Viewpoint"
        },
        "name": "Grid",
        "className": "org.helioviewer.jhv.layers.GridLayer",
        "enabled": true
      },
      {
        "data": {
          "mode": "Observer",
          "expert": {
            "syncInterval": true,
            "objects": [
              "Earth"
            ],
            "frame": "HEEQ"
          },
          "fovAngle": 1,
          "camera": {
            "translationX": 0.9308824978512158,
            "translationY": -0.0579670419907351,
            "fov": 0.006023977858680044,
            "dragRotation": [
              1,
              0,
              0,
              0
            ]
          },
          "equatorial": {
            "syncInterval": true,
            "objects": [
              "Earth"
            ],
            "frame": "HCI"
          }
        },
        "name": "Viewpoint",
        "className": "org.helioviewer.jhv.layers.ViewpointLayer",
        "enabled": false
      },
      {
        "data": {
          "scale": 100
        },
        "name": "Timestamp",
        "className": "org.helioviewer.jhv.layers.TimestampLayer",
        "enabled": false
      },
      {
        "data": {
          "scale": 10
        },
        "name": "Miniview",
        "className": "org.helioviewer.jhv.layers.MiniviewLayer",
        "enabled": true
      },
      {
        "data": {
          "icons": true
        },
        "name": "SWEK Events",
        "className": "org.helioviewer.jhv.plugins.swek.SWEKLayer",
        "enabled": true
      },
      {
        "data": {
          "fixedColor": false,
          "detail": 0,
          "radius": 2.5
        },
        "name": "PFSS Model",
        "className": "org.helioviewer.jhv.plugins.pfss.PfssLayer",
        "enabled": false
      }
    ],
    "annotations": {
      "annotateables": [
        {
          "endPoint": [
            -0.6444571138969956,
            -0.5251132039160703,
            0.5558157531237797
          ],
          "startPoint": [
            -1.2173078818054361,
            0.6410472878975405,
            -0
          ],
          "type": "FOV"
        }
      ],
      "activeIndex": 0
    },
    "time": "2017-09-24T19:52:28",
    "projection": "Orthographic",
    "tracking": false
  }
}
```

### Image Request File ###

JSON document specifying image requests to the default server in a simple manner as in the example below. Natural language specification for time is supported and, besides the `dataset` field, all fields are optional with sensible defaults.

Example:

```json
{
  "org.helioviewer.jhv.request.image": {
    "observatory":"SDO",
    "startTime":"yesterday",
    "endTime":"today",
    "cadence":1800,
    "dataset":"AIA 304"
  }
}
```

### Timeline Request File ###

```json
{
  "org.helioviewer.jhv.request.timeline": {
    "bandType": {
      "baseUrl": "http://swhv.oma.be/datasets/odi_read_data.php?",
        "unitLabel": "W/m^2",
        "name": "GOES_XRSA_ODI",
        "range": [
          1e-09,
          0.001
        ],
        "scale": "logarithmic",
        "label": "GOES XRS-A (shortwave)",
        "group": "GROUP_SWHV"
    },
    "data": [],
    "multiplier": 1.3000000187446403e-08,
    "timeline": "GOES_XRSA_ODI"
  }
}
```

### Image Formats ###

WCS metadata is used to place image data at the correct viewpoint (time and position). Without metadata, image data is placed at a default viewpoint. JHelioviewer can extract and interpret metadata from JP2, JPX, and FITS formats. It also supports at a basic level, without metadata, the Java ImageIO formats (JPEG, PNG, BMP, GIF).

# JHelioviewer Design #

In contrast to the 32k lines of code to implement all its many features, the core JHelioviewer design is very simple and can probably be expressed in a couple of thousands of lines of code.

The program is structured in a manner that is amenable to performance. The principle of separation of concerns is applied throughout. Objects are asked to update themselves, they proceed to do so independently, and they report back when done. To remain responsive while performing long lasting network and computation operations, the program uses threads, caches, and high performance algorithms and data structures. There are essentially no locks and few data structures are accessed from concurrent threads.

The program is driven via two timers:

- `Movie` beats at a configurable frequency (default 20 Hz) and commands the setting of the program time (i.e., frame advance);
- `UITimer` beats at constant 10 Hz and commands the refresh of the Swing UI components that need to change together with the movie frame; additionally, it commands the refresh of the timeline canvas.

Various parts of the program can request to refresh the image canvas and a balance has to be found between avoiding excessive redraws and avoiding CPU wake-ups when idle. `Interaction` and `MovieDisplay` contain supplemental timers which are started on-demand and are used to limit the rate of user-induced decoding and redraw requests.

## Concepts

### Coordinates

Distances are expressed in units of solar radii (photometric, Allen). This is for numerical stability and to ease the expression of some computations. Additionally, this unit helps with the limited precision of the OpenGL depth. On ingestion, the program can optionally normalize the apparent solar radius observed in various EUV wavelengths to the photometric reference. This is done to match the synthetic elements drawn independently, such as the grid, to the data. The program is able to display elements at distances up to about 10755$R_\odot$ from the Sun, which is beyond the aphelion of Pluto.

Orientation is expressed in latitudinal form (latitude, longitude in radian) with respect to a Carrington reference frame for ease of expression of rotations as quaternions. Computations of rotations are performed using quaternions for performance and numerical stability reasons. The interaction with OpenGL is done using matrices since, besides rotation, it involves projection and translation.

Together with a timestamp, the distance to Sun and the orientation constitute the fundamental concept of `Viewpoint`. One important viewpoint is Earth's. This is computed using an algorithm translated from the SolarSoft `get_sun` function (derived from Meuus, "Astronomy with a PC", ed. 2, tbc).

Viewpoints can be computed at various timestamps using the `UpdateViewpoint`. Several forms are provided:

- `Observer` takes the closest in time from the metadata of the master layer, see the Metadata section.
- `Earth` computes the viewpoint with the algorithm mentioned above.
- `EarthFixedDistance` is as above but with the distance fixed at 1au (it is used for the latitudinal and polar projections).
- `Equatorial` is a viewpoint looking from above the solar North pole at a distance (~229au) such that, for a field-of-view angle of 1˚, objects up to 2au far from the Sun are visible, the longitude is derived from the closest in time metadata of the master layer such that the Earth appears on the right-hand side.
- `Other` (in UI, `Expert` in code) are viewpoints which are computed from the responses to requests to the `GeometryService`. A maximum of 10000 points are requested to the server and interpolation is used as needed for the intermediate timestamps.

### Camera

The computed `Viewpoint` is used in setting up of the `Camera`, which intermediates to the rest of the program and to OpenGL.

One important task of `Camera` is to set up the projection matrix. The projection is always a variant of an orthographic projection with the Sun at depth 0. In the orthographic display mode, there are two types of projection matrices, one with deep clipping planes (range [-10755$R_\odot$,+10755$R_\odot$]), appropriate for far viewing distances such of the `Equatorial` viewpoint, and one with more shallow clipping planes (range [-32$R_\odot$,+32$R_\odot$], a bit more than LASCO C3 FOV), appropriate for the normal solar observations. This duality is necessary for the preservation of precision in the OpenGL depth buffer.

Another important task of `Camera` is to set up the model-view matrix. This is based on the orientation of the `Viewpoint` and on the rotation and the translation due to the user interaction with the image canvas. When image data draw commands are issued, a difference rotation with respect to the image metadata is computed, configured and used in the shader programs. For other drawn elements, the camera orientation may be saved, the camera may be rotated as desired, the draw command issued, and then the camera orientation may be restored.

Functionality to translate from the two-dimensional coordinates of the image canvas to the three-dimensional internal coordinates is available in `CameraHelper`.

### Metadata

Metadata about the observations is extracted from the incoming image data format. It is currently possible to extract it either from the JPEG2000 formatted streams or from the FITS formatted streams. In the JPEG2000 streams it was derived from the original FITS header and inserted as XML at the moment of creation of the JP2 and JPX files while, for FITS streams, it is part of the format and it is transformed by the program to XML in order to use the same parsing code.

A discussion about the necessary metadata components is at <https://github.com/bogdanni/hv-HEP/blob/master/HEP-0010.md>.

If the necessary metadata can be derived, it is made available to the rest of the program in `HelioviewerMetaData` structures. For image formats without metadata, or when the metadata is not present, or its parsing fails, a default metadata structure is built corresponding to the Earth viewpoint at 2000-01-01T00:00:00.

### View

A `View` is a representation of the incoming image data stream which knows how to read the stream and to decode it into pixel data which can be drawn. There are several specializations:

- `SimpleImageView` reads image streams via Java `ImageIO` API; metadata is not available.
- `FITSView` deals with FITS streams and uses the `nom-tam-fits` library (<https://github.com/nom-tam-fits/nom-tam-fits>).
- `J2KView` deals with JPEG2000 streams and it is the most complex, as it has to implement both the image decoding via the Kakadu library and to implement the JPIP streaming protocol.

`SimpleImageView` uses the `ImageIO` API capabilities for reading remote streams, while `FITSView` uses the `NetClient` interface of JHelioviewer. `J2KViewCallisto` is a specialization of `J2KView` which uses the `NetClient` interface to read JP2 files over HTTP, then cache and decode them as local files.

The two major components of `J2KView` are:

- `J2KReader` implements a minimal HTTP client and a minimal JPIP-over-HTTP streaming client. It fills the memory cache of `JPIPCache` (an extension of Kakadu `KduCache`) from which the image decoding takes place. Once the entirety of the data for a resolution level for a frame is available, it is extracted from the memory cache and it is sent via `JPIPCacheManager` into the disk persistence layer provided by the `Ehcache` library. The `sourceId` of the dataset and the timestamp of the frame is combined to form universal identifiers. `J2KReader` is also used to fill the cache indicator of the UI time slider via the `CacheStatus` interface. `J2KReader` is implemented as a thread that receives from `J2KView` commands to read. It tries to read requested data first via the `JPIPCacheManager` before constructing and issuing a request to the JPIP server if not available in the persistent disk cache. Once all data is read, the HTTP connection is closed and the `J2KReader` stops listening for commands.

- `J2KDecoder` is in charge for forwarding commands to `Kdu_region_compositor` to decode image data out of `KduCache` (wrapped by `JPIPCache`). The commands are issued by `J2KView` over a queue of size one to a `DecodeExecutor` which handles just one thread at a time. If a command is already queued, it is removed from the queue to make place for the most recent one. The `J2KDecoder` thread manages instances of `Kdu_thread_env` and `Kdu_region_compositor` in thread local storage. On demand, those are created, re-created (as a result of exceptions raised by the Kakadu native code during decoding), and destroyed. The `Kdu_thread_env` object is used to distribute the decoding work of the native code over all CPUs. The resulting bytes of `Kdu_compositor_buf` in native memory are copied one-by-one using `LWJGL` `MemoryUtil.memGetByte`, which was found to have the highest performance and to reduce the size of necessary memory buffers. `J2KDecoder` also manages a `Guava Cache` of soft references to the already decoded image data and, if the received command to decode corresponds to an item in the cache, it immediately returns that item instead of entering the Kakadu processing.

The decoded pixel data together with the associated information such as the metadata is constructed into `ImageData` structures which are handed over to the `ImageDataHandler`, i.e., `ImageLayer`.

### Layer

A `Layer` is an interface to an object that knows how to fetch its data and to draw itself on the image canvas. The program orders them in a `Layers` list and represents them in the user interface via a `LayersPanel` list selector where the user can interact with them (add, remove, select, made invisible). The layers in the list are drawn in order from the top to the bottom of the list selector. Each layer has a panel of options visible under the list selector when the layer is selected. Those options are used to configure the draw commands.

A special group of layers is made of `ImageLayer`. Those can be re-ordered in the list by the user via drag-and-drop. Additional layers include `ViewpointLayer`, `GridLayer`, `TimestampLayer`, `MiniviewLayer`, `SWEKLayer`, and `PfssLayer`. The names correspond to user visible functionalities.

The most sophisticated type of layer is the `ImageLayer`. This is because it implements the core functionality for image display and because it allows for the replacement of the underlying `View`.

The same concept is used for timelines, where several of `TimelineLayer` are organized in a `TimelineLayers` list and are represented in the user interface via the `TimelinePanel` list selector. Specializations are `Band` for plots, `RadioData` for Callisto spectrograms, and `EventTimelineLayer` for events.

## Drawing

The image canvas is implemented using a JOGL NEWT `GLWindow` encapsulated within a `NewtCanvasAWT` which integrates into the rest of the Swing interface. To handle the situation in which the OpenGL context (`GLContext`) has to be recreated due to events in the underlying windowing system, the `GLWindow` uses a slave context derived from a master context of a dummy drawable. `GLListener` implements the event based mechanism for performing OpenGL rendering (`init()/dispose()/reshape()/display()`). All rendering ensues from `display()` in the AWT Event Dispatch Thread and uses the primary rendering context associated with the `GLWindow` for its lifetime. Similarly for initialization and disposal.

The drawing on the image canvas is done entirely using GLSL programs. The following interfaces are available:

- `GLSLSolar` handles `solarOrtho`, `solarLati`, `solarPolar`, and `solarLogPolar` shaders to draw image data, and it is used exclusively by `ImageLayer`.
- `GLSLLine` handles the `line` shaders to use instanced rendering for drawing triangles out of line segments.
- `GLSLShape` handles the `shape` and `point` shaders to draw polygons and points.
- `GLSLTexture` handles `texture` shaders to superimpose texture data such as event icons, and it is used by `JhvTextRenderer` to draw text rendered into cached textures.

Besides the drawing done by layers, annotations can be drawn by `InteractionAnnotate`.

Matrices compatible with the OpenGL representation are maintained by the `Transform` class which implements projection and model-view matrix stacks and a simple cache of the result of the multiplication of the top of the matrix stacks. The stacks can be pushed and popped similarly to the traditional OpenGL fixed-function matrix stacks and are queried at configuration time by the draw commands.

## User Input

The user input (mouse movements and keyboard strokes) to the image canvas is received by the `GLWindow` from the JOGL NEWT input system. The events arrive on the NEWT Event Dispatch Thread and are re-issued on the AWT Event Dispatch Thread such that all computations in response to user interaction are performed on the Swing thread. The event dispatch to the interested subscribers is mediated by the `InputController`. The available interactions are

- `InteractionAnnotate`: draw annotations, possibly interactively, on the image canvas;
- `InteractionAxis`: rotate around the current `Viewpoint` axis;
- `InteractionPan`: translate camera origin;
- `InteractionRotate`: free rotation around camera origin.

## Network I/O

Besides the implementation specific for the JPIP network client, all the rest of network I/O is done via `NetClient`, which provides an interface implemented on top of `Okio` and `OkHTTP`, transparent to the actual location -- remote or local -- of the requested resource. All the remote APIs are REST and the implemented functionality is that of the HTTP GET request. The caching functionality of `OkHTTP` can be used or can be bypassed. An additional local cache where direct access to the cached files is possible is provided by `NetFileCache`.

## Timelines

An important insight for high performance plotting is that plots can be requested for short and long time ranges, and, for the latter case, an excess of data points make for an illegible plot. Therefore, a multi-resolution approach for the time dimension is beneficial.

For the Callisto spectrograms which are 2D, this approach is supported by using the JPEG2000 format for the data, with daily images having on the *x*-axis the second of the day, and on the *y*-axis the frequency bin.

The 1D timelines are cached in data structures which are pyramids of time resolutions. The highest resolution has one-minute bins and the lower resolutions are obtained by decimation. Depending on the nature of the dataset, several types of data are supported, namely linear, strictly positive linear, and logarithmic. For the latter two, the time decimation is performed by taking the maximum in the time bin to be decimated, as the user is likely to be interested in data peaks, e.g., flares.

## Events

Supported event sources are HEK and COMESEP. The interface to the COMESEP caching server is similar to the interface to the HEK and the format was designed to be similar, namely JSON structures with parameters. Since the user can view a large variety of events over long time ranges, significant effort was put into reducing the memory usage as Java objects have a high memory overhead. This was done by pruning some of the parameters and extensive use of interned strings.

The event data is cached into an SQLite database, with the JSON structures inserted as gzip-compressed blobs. The last two weeks' worth of data can be updated to allow for server-side corrections. Some of the parameters are also available for query, allowing to implement filtering of some event types on the value of those parameters.

# CCN2 Tasks #

## WP20100 -- Study SWHV and JHV3D and WP20150 -- Merge JHV3D Ideas

The following topics were identified as areas of work for merging ideas from the JHV3D branch into SWHV.

**T1. Unified network/download (SWHV-CCN2-20100-01)**

The aim of this is to reduce the number of thread pools allocated for network protocols and therefore the memory usage, as well as to provide a central place for network connection management.

SWHV inherited `DownloadStream`, a thin wrapper over the Java `URLConnection`. This is a synchronous API; therefore, it has to be managed in separate threads in order to not block the program while interacting with remote hosts. The different network services are handled within SWHV by separate thread pools due to the historical evolution of plugins -- add-on components to be loaded by the main program in order to augment the functionality. The JPIP network functionality is implemented at the lower level of network sockets and the JPIP protocol over HTTP is handled in a separate thread per JPIP connection.

JHV3D implements a download manager on top of the Apache HTTP Client library. This download manager handles also the JPIP protocol.

SWHV implements all HTTP protocol functionality, with the exception of JPIP protocol, on top of the `OkHTTP3` network library. Possible future work may consist in replacing the various download thread pools with the `OkHTTP3` asynchronous API and therefore the use of the thread pool provided by the library.

Re-implementing the JPIP protocol on top of the high-level `OkHTTP3` library appears more difficult. It is not clear if a re-implementation is beneficial in the context of how the `esajpip` server handles the connections with the clients. The current low-level access into the network software stack is useful for achieving high data throughput between the JPIP client and server.

**T2. Helioviewer v2 API (SWHV-CCN2-20100-02)**

Both SWHV and JHV3D implement the version 2 of the Helioviewer API. The `getClosestToMidpoint` API call was added for the benefit of JHV3D request of data, and this call is linked to the JHV3D approach for the acquisition of data. It shall be studied if the approach is beneficial for SWHV and thus if this API is useful for SWHV. This has ramifications with regard to playback and JPEG2000 caching. The `esajpip` server was modified to allow multiple ranges of frame requests in a single JPIP request, which may be a possible substitute.

SWHV supports both versions of API and has implemented the validation of server responses against a JSON schema. This functionality is also available as a separate standalone program (<https://github.com/Helioviewer-Project/DataSourcesChecker>).

**T3. Memory/disk cache: Save/read to/from cache on disk (SWHV-CCN2-20100-03)**

This deals with memory and disk caching of JPEG2000 data and is intimately linked with the playback and how the JPIP protocol is handled.

SWHV abandoned the concept of downloading regions of interest from the JPIP server, but still tries to minimize the resolution levels requested, such that, if the highest resolutions are never requested, they are never downloaded. Note, however, that recording movies forces the decoding of the full resolution of frames and therefore the download of the entire codestream.

Remote streams are cached by SWHV in Kakadu `KduCache` objects in memory. This is a concern for memory usage, therefore saving the codestream data to disk and limiting the `KduCache` size seems beneficial. However, SWHV was demonstrated to be capable of playing back streams of 10000 frames. This is the limit accepted by the ROB server, GSFC and IAS servers are limited to 1000 frames per stream.

Another concern is that the data of already downloaded frames cannot be reused between streams, for example when extending or shifting the temporal navigation of datasets. Having the capability of reusing already downloaded data is of high importance.

SWHV implements JPEG2000 disk persistence by extracting the codestream data from the Kakadu `KduCache` and then stores it using Ehcache3 (<http://www.ehcache.org>). The process is performed as needed after the data for a resolution level of a frame is completely received. Higher resolution levels overwrite lower ones. The caching is cross-server under the assumption of immutability and idempotence (Callisto spectrogram data is treated differently, it is requested over HTTP and it is cached at the level of JP2 files). The `sourceId` and timestamp of observation are combined to generate unique identifiers. The memory overhead is minimal because only the disk cache tier of Ehcache is used.

The memory codestream cache is handled by Kakadu's `KduCache` as before. Given the cache tiers (memory, off-heap, disk, and even clustered) capabilities of the Ehcache 3 library used, it is probably possible now to restrict the size of `KduCache` objects, although, in practice until now, it never seemed to pose a problem.

**T4. Plugin interface (SWHV-CCN2-20100-04)**

Historically, plugins were implemented as standalone libraries to be loaded by the program to augment functionality. They necessitated a relatively limited support from the main program. As the functionality of the program becomes more complex and the expectations of integration between the parts higher, it is unclear if this approach is sustainable and if keeping programming interfaces too stable impedes progress. The current plugins are so integrated with the main program that they represent merely GUI elements which can be optionally disabled for a more streamlined appearance.

As a consequence of implementing the SAMP functionality, SWHV has simple programming interfaces for adding data layers and for loading state.

**T5. Playback engine: Extract portable ideas from JHV3D (SWHV-CCN2-20100-05)**

SWHV operates in a fully asynchronous manner and can achieve high frame rates with low CPU utilization. This topic deals on how the time and data flows are distributed within the program and is closely linked to how Kakadu is used for decoding the JPEG2000 data. It is unclear whether there are advantages to the JHV3D approach. On a superficial level, SWHV appears to reach about half the CPU utilization of JHV3D with un-cached decoded data and less than 5% with cached decoded data.

During the CCN2 period, significant progress was made by moving the cache of the decoded image data in Java memory and out of the `Kdu_region_compositor`. The cache uses soft references, thus the decoded image data fills the JVM heap which has a fixed size established at startup. When the maximum heap size is reached, the garbage collector eagerly collects the buffers not referenced elsewhere in the program. The effect is an automatic trade-off between memory and CPU usage for already decoded image data. This also leads to a potentially dramatic drop of the total memory used by the application since the JHV-side caching uses one byte per pixel (for color-mapped layers) compared to the four bytes per pixel of `Kdu_region_compositor`. Other benefits include a performance increase due to reduced need for copies between Kakadu native buffers and JVM memory, and the cache adaptation to the JVM heap size. The benefits scale with the available JVM heap and with the number of loaded image layers. Some test cases achieved significant reductions both in CPU and memory usage.

Additionally, the decoded image data is now copied to Java memory directly from the Kakadu native buffer for up to 40% CPU reduction for this task and further reduction of the Java buffers used.

Some experiments with texture data upload into OpenGL directly from the decoding thread were conducted. Such an approach would shift the decoded image data cache from the JVM heap to OpenGL, i.e., process memory or texture buffers inside GPU memory, as decided by OpenGL. The disadvantages are: duplicated code paths for radio data which needs the image data into Java memory, difficult OpenGL buffers handling, and more sophisticated OpenGL context handling. The latter can be approached by migration of the OpenGL context between threads or, alternatively, multiple contexts. The first approach leads to thread contention for the context, which blocks the rendering pipeline and thus may introduce stuttering at high frame rates, while the second approach leads to OpenGL context switch overhead. The experiments concluded that this may be useful only for many loaded layers.

**T6. Handling of transparency (SWHV-CCN2-20100-06)**

SWHV uses alpha-premultiplied color blending. The default compositing of the layers is at the middle between the ADD and OVER operators and an extra setting named "Blend" was added to control the variable level of additivity and opacity.

**T7. Installation (SWHV-CCN2-20100-07)**

The installation procedures of SWHV are derived from the previous versions. The install4j procedure of JHV3D is currently ported, but lightly tested and not deployed. As of the Java 11 release, the approach to the release of Java changed in several ways. For example, there is no runtime environment anymore and the expectation is that a tailored modular distribution is carved out from the Java Development Kit. Preserving the capability to automatically generate the distribution packages for all three supported platforms from the macOS environment is a concern in the new context. If the current monolithic install4j procedure was adopted, it would lead to a 10× increase of distribution packages.

**T8. Telemetry (SWHV-CCN2-20100-08)**

SWHV communicates back only crash reports to a Sentry server deployed at ROB. This server is connected to private email, Slack and GitLab services. Since the 2.12 release, several crashes unreported by users were intercepted. Some which were not uncovered by testing were fixed, others need more information.

The best manner to report issues is via <https://github.com/Helioviewer-Project/JHelioviewer-SWHV/issues>.

**T9. Guidelines for contributing (SWHV-CCN2-20100-09)**

SWHV preliminary version at:

<https://github.com/Helioviewer-Project/JHelioviewer-SWHV/blob/master/CONTRIBUTING.md>

## WP20200 -- Improve Core Functionality

The following tasks were identified:

1.  **(SWHV-CCN2-20200-01)** Study how to improve the temporal navigation jointly for image and timeline data at both short and long timescales.

Time selection for the image layers was brought to the forefront and it became possible to use the interaction with time range of the timelines panel to temporally navigate jointly with the image layers. This is achieved by reloading as necessary the image layers for the time range of the timelines panel. The capability to cache the JPEG2000 codestream data improved the user experience.

2.  **(SWHV-CCN2-20200-02)** Save and restore program state.

This is implemented as a JSON document preserving with high fidelity the state of the program.

3.  **(SWHV-CCN2-20200-03)** Improve command line interface.

The following command-line options are now available:

```
  -load    file location
         Load or request a supported file at program start. The option can be used multiple times.
  -request request file location
         Load a request file and issue a request at program start. The option can be used multiple times.
  -state   state file
         Load state file.
```

4.  **(SWHV-CCN2-20200-04)** On disk caching of JP2 code-stream data.

This was tackled in WP20150.

5.  **(SWHV-CCN2-20200-05)** Specific event filtering, now that the filtering is not done anymore server side.

This is implemented on HEK derived events for NOAA SWPC flares on the GOES intensity values and for CACTus CME detections on the radial linear velocity. The implementation relies on event filtering in the SQL database that is used as event cache.

6.  **(SWHV-CCN2-20200-06)** Proxy support.

This is implemented using the Java network proxy facilities.

## WP20300 -- Support Solar Orbiter Operations

The following tasks were identified:

1.  **(SWHV-CCN2-20300-01)** Consolidate user requirements, taking inputs from instrument teams such as EUI and STIX and the Solar Orbiter Science Operations Working Group (SOWG).

This is implemented by the draft Solar Orbiter User Requirements Document (ROB-SWHV-URD2) to be updated for the end of the project.

2.  **(SWHV-CCN2-20300-02)** Publish ROB ephemeris server on Helioviewer GitHub and collaborate with Solar Orbiter MADAWG for maintaining a coherent data (kernel) tree.

This is implemented by the `GeometryService`.

3.  **(SWHV-CCN2-20300-03)** Build propagation server incorporating several propagation models, enabling correlation of in-situ data with remote sensing data.

This is implemented by the `PropagationService`, a mock-up RPC server built on the Spyne toolkit (<http://spyne.io>, the same as the `GeometryService` service).

4.  **(SWHV-CCN2-20300-04)** Test with and integrate ACE, DSCOVR or other current in-situ datasets identified as relevant today for space weather forecasters.

The considered use-case is measurements of phenomena propagating radially slower than speed of light.

This is how one user imagines this feature would work:

> The user sees in the SWAP movie a flare and dimming on the Sun and wonders if this has arrived at the Earth/L1. They open a GOES X-ray timeline to see the flare timing and then open an ACE solar wind timeline to see the arrival. They will now put in the textbox reasonable differences from 20h to 5d and see if there is a match of the flare timeline and of the solar wind disturbance. A slightly more natural way to do this would be to put in propagation speeds. From the SWAP movie one can get at least a rough feeling if this is a slow eruption (300 km/s) or a fast one (2000 km/s). The user fills in a guess for a speed, the system calculates the delta-t in sec and we see the two timelines.

A textbox was added in the timeline options panel where one can set a propagation speed (0 meaning disabled). The location is the calculated Sun-Earth L1 point. When that speed is different from zero, another time-scale (in the timeline color) appears under the timelines panel time-scale (in black).

Therefore, the displayed time-scale has the following meaning:

* the colored time-scale is time of observation;
* when speed is 0, the time of the timelines panel (black time) is disconnected from image layers time → time is UTC at an undefined location (most likely Earth);
* when speed is not 0, timelines panel time (black time) is the time determined by the image layers viewpoint, there is an additional speed-of-light propagation from Sun to the viewpoint → time is UTC at the viewpoint (according to viewpoint settings, may be different from Earth).

Several DSCOVR in-situ timeline datasets were integrated. They were identified as highest priority for the space weather forecasters. The added datasets are the interplanetary magnetic field Bz, Bt, and Phi and the solar wind density, radial speed, and temperature.

5.  **(SWHV-CCN2-20300-05)** Plot the sub-spacecraft point on the solar surface (both radially and w.r.t. magnetic connectivity, i.e., Parker spiral).

This is implemented using existing functionality. The sub-spacecraft point is the center of the FOV indicated when the Viewpoint layer is activated. The magnetic connectivity aspect will need to be tackled together with the implementation of the Solar Orbiter requirements, therefore is out of scope.

6.  **(SWHV-CCN2-20300-06)** Build on annotations to indicate instruments fields of view. Add ability to drag and show their centers. Optionally distort according to differential rotation. Distortion of solar images is outside of the resources for this CCN.

Arbitrary FOV annotations can be drawn and their centers are indicated. There is functionality to automatically zoom to the size of the current FOV annotation. Dragging and distortion are not implemented.

## WP20400 -- Enable Server Interoperability

The following tasks were identified:

1.  **(SWHV-CCN2-20400-01)** Improve the functionality of JPX manipulation to add movie merging.

This will be implemented by combining the `hvJP2K` package functionality splitting of JPX files into JP2 files and merging of JP2 files into JPX files.

2.  **(SWHV-CCN2-20400-02)** Packaging of software tools developed for the production and manipulation of JPEG2000 data.

This is implemented by modifying the validation functionality of the `hvJP2K` package to use a current `jpylyzer` package release.

3.  **(SWHV-CCN2-20400-03)** Cache JP2 headers in database at insertion time.
4.  **(SWHV-CCN2-20400-04)** Build JPX from database headers.

The purpose of those two tasks is to reduce the server latency between the user selecting a dataset and the server making available the prepared JPX file to the `esajpip` server. While merge functionality of `hvJP2K` can be 10× faster than the Kakadu similar functionality, it is limited by I/O operations necessary to parse the JP2 files. Parsing the JP2 ahead of the time and storing the necessary information in the database together with the record of each file can possibly reduce this latency.

Other means to achieve a reduction in this latency is by changing the way SWHV requests the datasets to always request entire days.

This is implemented by two new programs part of the `hvJP2K` package (`hv_jpx_merge_to_db` and `hv_jpx_merge_from_db`) which store the extracted JP2 headers into an SQLite3 database and can generate JPX files from those stored headers.

5.  **(SWHV-CCN2-20400-05)** Support GSFC for the adoption of the `hvJP2K` tools, especially in the areas of transcoding and verification of the JP2 files.

This is an ongoing activity.

6.  **(SWHV-CCN2-20400-06)** Maintenance for `esajpip` server (not including implementation of feature requests).

This is an ongoing activity.

## WP20500 -- Release, Testing, Documentation

**(SWHV-CCN2-20500-01)** The tasks of this WP are an ongoing activity.

The support for 32bit operating systems was removed in order to avoid the incidence of out-of-memory crashes and virtual address exhaustion, especially during movie creation. The software is supported under Windows, macOS and Linux for Oracle Java 8 64bit.

The software is also tested under later versions of OpenJDK. There are several illegal reflective accesses from JOGL into the now modularized Java frameworks, some of them OS-specific. Currently manifesting as warnings, it will be very difficult to fix those for future Java versions where they will become errors.

The software is under continuous refactoring and re-architecting as new features become available. Simplification and reduction of the number of lines of code are top priorities. Besides continuous testing, the software is regularly submitted to static code analysis using IDEA IntelliJ, Google ErrorProne, SpotBugs, PMD, and Synopsys Coverity.

## WP21200 -- Improve Client Interoperability

The following tasks were identified:

1.  **(SWHV-CCN2-21200-01)** Study and design a SAMP plugin; implement a prototype version based on a use case that will allow to estimate the time needed to produce an operational version.

SAMP capabilities were integrated into SWHV. It is possible to send information about the loaded image layers to SunPy or SolarSoft scripts in order to load the original FITS files via VSO onto the user's computer. It is also possible to receive compatible data from any external SAMP-aware program or web page (<http://swhv.oma.be/test/samp/>).

2.  **(SWHV-CCN2-21200-02)** Explore how to implement a new VSO plugin, including use case and time estimates.

In view of the above SAMP capabilities, direct VSO capabilities are better handled by a community wide effort such as SunPy which incorporates solar physics data web services such as VSO, JSOC and others.

Therefore, the VSO connection is achieved over the SAMP protocol via SunPy or SolarSoft (for which an additional IDL-Java bridge is necessary). Compared with direct VSO capabilities, this solution is better in the sense that the best use of the science data retrieved this way is within an environment made for data analysis such as SunPy or SolarSoft.

3.  **(SWHV-CCN2-21200-03)** Explore ways to improve and/or integrate FITS, NetCDF, VOTable formats.

SWHV has improved support FITS files and can support CDF (not NetCDF) format using the <https://github.com/mbtaylor/jcdf> library and VOTable format using the <https://github.com/aschaaff/savot> library.

The FITS support improvements include compressed and remote files, more supported instruments, more data-types, physical units, the BLANK keyword, better pixel scaling including the port of ZMax autoscaling algorithm of SAOImage DS9 (better results for EUV observations). SWHV can now display the value of the pixel under the mouse pointer in physical values.

This support is limited to known use cases exhibiting data calibration and known standard metadata.

## WP21300 -- Investigate JPIP Alternatives

**(SWHV-CCN2-21300-01)** For the task of this WP, a first web-based video streaming proof of concept was investigated. The focus lays on the identification and verification of required technical core components.

1. __Video support__\
     The most supported video format by current web browsers is MP4 H.264 (<https://caniuse.com/#feat=mpeg4>), including «fragmented» versions for streaming). All tested platforms support HTML5 video playback of grayscale 8bit interlaced mp4 videos.

1. __Storage__\
     For the proof of concept, a video tree was built from AIA images, covering one day, with 4 different pixel resolutions (full res 4k down to 512) and 6 different time resolutions, i.e., playback speeds (1x, 2x, 4x, 8x, 16x, 32x). Helioviewer.org provided 2379 JPEG2000 images, from which 6\'035 videos were built. With the current video format, we expect **50TB** of videos, compared to the **57TB** of JPEG2000 images they cover.

1. __Bandwidth__\
     In full resolution streaming mode, i.e., streaming the full 4k images, only 7MB/sec are required. Yet the tool is built to download with region of interests, with a realistic worst case of **2.5MB/sec** bandwidth required. For mobile devices, bandwidth usage can be throttled down to **0.15MB/sec**.

1. __WebGL__\
     The proof of concept projects 64 videos (8 512px videos in width and height) onto a 3D sphere. While the GPU has no issues, this worst-case performance test shows significant CPU consumption, with the naive implementation slowing down the playback to 10 FPS (frames per second). We are confident though that this can be addressed with improved GPU communication. Practically all devices support WebGL and 4k textures (<https://webglstats.com/webgl/parameter/MAX_TEXTURE_SIZE>)

1. __Decoding__\
     The decoding speed of MP4 videos proved to be no performance bottleneck at all. Even more, this work can be offloaded to a worker thread, and would therefore be non-blocking. All browsers support web workers (<https://caniuse.com/#feat=webworkers>)

The proof of concept showed that no criterion from this first evaluation set hinders further investigation into video streaming of AIA images.

Further, with the growing support for the Media Source Extensions API (<https://caniuse.com/#feat=mediasource>), close control can be exercised over video playback, chunking and streaming. For caching, IndexedDB seems suitable (<https://caniuse.com/#search=indexeddb>).

# Traceability Matrix

![][traceability]

\include{"acronyms.md"}
