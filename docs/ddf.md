---
title: SWHV CCN2 Design Document
subtitle: ROB-SWHV(7186)-DDF2 v1.2
author: SWHV Team
subject: Space Weather HelioViewer
date: 2018-04-04
geometry: margin=1in
papersize: A4
toc: true
colorlinks: true
mainfont: "Georgia"
monofont: "Source Code Pro"
sansfont: "Source Sans Pro"
fontsize: 11pt
titlepage: true
titlepage-color: "0088cc"
titlepage-text-color: "ffffff"
titlepage-rule-color: "ffffff"
titlepage-rule-height: 5
logo: hvLogo.png
logowidth: 0.1
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
|Contributing authors | Roman Bolzern (FHNW), Bram Bourgoignie (ROB), Bogdan Nicula (ROB), Freek Verstringe (ROB)|
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

## Purpose & Scope

This document (SWHV-DDF2) is the design study report of the work performed during the CCN2 phase of "Space Weather Helioviewer" project (Contract No. 4000107325/12/NL/AK, "High Performance Distributed Solar Imaging and Processing System" ESTEC/ITT AO/1-7186/12/NL/GLC). It focuses on the detailed explanation of the changes for several software components that need to be implemented or were already implemented by the time this document was made available for review.

## Applicable Documents

\[1\] Contract Change Notice No. 2: SWHV-CCN2-Proposal3-BN2.pdf

## Reference Documents

\[2\] TR Architectural outline of JHV3 by Simon Felix (FHNW)

# Work Logic

The current system architecture is presented in Chapter 3, the interfaces of the JHelioviewer client are presented in Chapter 4, the current design of JHelioviewer is presented in Chapter 5, while the Chapter 6 presents the identified tasks for the CCN2 phase, together with a proposed implementation. For several tasks, at the time of the current version of this document (MS6), the work was already performed, therefore the present tense is used. For the work to be performed for the MS7, the future tense is used.

Chapter 7 presents a traceability matrix for the tasks, as well as the assigned priority and the milestone for delivery. Features already delivered will be subjected to refinement and refactoring as new functionality becomes available in the client-server system.

# System Architecture #

The following figure depicts the architecture of the Helioviewer System as installed on the ROB server. For the purpose of this project, the focus is on the interaction between the JHelioviewer client and the Helioviewer server.

![Helioviewer system architecture][architecture]

## Server Infrastructure ##

The following servers are included:

- HTTP server (e.g., Apache or `nginx`) to serve static files, to proxy HTTP requests, and to run various services:
  - Image services API (<https://github.com/Helioviewer-Project/api>): For the JHelioviewer client, it lists the available image datasets and commands the creation of JPX movies on demand. It includes a facility to ingest new images files. Metadata about the image files is stored in a MySQL database.
  - Timeline services API: This is an adapter brokering between the JHelioviewer client and the backend timeline storage services (ODI and STAFF backend -- <http://www.staff.oma.be>). For the JHelioviewer client, it lists the available timeline datasets and serves the data in a JSON format.
  - A PFSS dataset, static FITS files produced regularly out of GONG magnetograms. The JHelioviewer client retrieves them on demand, based on monthly listings (e.g., <http://swhv.oma.be/magtest/pfss/2018/01/list.txt>).
  - COMESEP service which subscribes to the COMESEP alert system (not part of this project), stores the alerts and makes them available to the JHelioviewer server in a JSON format.
  - The Helioviewer web client (<https://github.com/Helioviewer-Project/helioviewer.org>), not relevant for this project.
- The `esajpip` server (<https://github.com/Helioviewer-Project/esajpip-SWHV>), which delivers the JPEG2000 data streams to the JHelioviewer client using the JPIP protocol, built on top of the HTTP network protocol.
- The `GeometryService` server (<https://github.com/Helioviewer-Project/GeometryService>) implements a set of high precision celestial computation services based on NASA's Navigation and Ancillary Information Facility (NAIF) SPICE Toolkit and communicates with the JHelioviewer client using a JSON format.
- The HEK server (maintained by LMSAL <https://www.lmsal.com/hek/>, not part of this project) which serves JSON formatted heliophysics events out of HER. The JHelioviewer client retrieves a curated list of space weather focused events.

To ensure encapsulation, reproducibility, and full configuration control, the services which are part of this project are currently being containerized at <https://gitlab.com/SWHV/SWHV-COMBINED>.

## JPEG2000 Infrastructure ##

### JPIP Server ###

The `esajpip` server serves the JPEG2000 encoded data to the JHelioviewer client. This software was forked from the code at <https://launchpad.net/esajpip>. It was ported to a CMake build system and to C++11 standard features. Several bugs, vulnerabilities, and resource leaks (memory, file descriptors) were solved; sharing and locks between threads were eliminated; C library read functions were replaced by memory-mapping of input files (for up to 10× higher network throughput). The JPX metadata is now sent compressed and several ranges of images can be requested in one JPIP request.

The code is periodically verified with IDEA CLion and Synopsys Coverity static code analyzers and with `valgrind` dynamic analyzer, as well as various sanitize options of several C++ compilers.

### FITS to JPEG2000 ###

The JPEG2000 files can be created with the IDL JPEG2000 implementation. Those files have to be transcoded for the use in the Helioviewer system.

The open-source alternative for the creation of JPEG2000 files is the `fits2img` package (<https://github.com/Helioviewer-Project/fits2img>), which uses a patched version of the open source OpenJPEG[^openjpeg] library. The files created by this tool do not need to be transcoded.

While ingesting new datasets during the SWHV project, it became apparent that the metadata in the FITS headers of some datasets is lacking or is defective. A FITS-to-FITS conversion stage is needed for those datasets to adjust the metadata to the needs of the Helioviewer system. At <https://github.com/bogdanni/hv-HEP/blob/master/HEP-0010.md> there is a summary of those needs.

### JPEG2000 Files Handling ###

The image data is encoded using the JPEG2000 coding standards. The JPEG2000 data consists of compressed data codestreams organized using *markers* according to a specific syntax, and several file formats, such as JP2 and JPX, which are organized using *boxes* encapsulating the codestreams of compressed image data organized in *packets* and the associated information.

In order to ensure the communication between the server and the client, the Helioviewer system imposes a set of constraints on the codestreams and file formats. This includes requirements for codestream organization such as specific packetization (PLT markers), coding precincts, and order of progression (RPCL), for file format organization, such as the presence of specific boxes aggregating the codestreams and the associated information like metadata, and for file naming conventions.

The JPEG2000 standards have a high degree of sophistication and versatility. In order to encourage the proliferation of Helioviewer image datasets, it should be possible to generate those files with standard conforming software other than the proprietary Kakadu software currently used. It becomes therefore necessary to validate the full structure of Helioviewer image files formally and automatically. A verification system based on Schematron[^schematron] XML schemas was developed. This procedure is able to verify the structure of JPEG2000 file and codestream, including the associated information such as the Helioviewer specific XML metadata, ensuring the end-to-end compatibility with the Helioviewer system.

Before the SWHV project, both the server and the client side software were derived from the Kakadu Software toolkit (<http://kakadusoftware.com>). Much of the server side usage of the Kakadu software can be now replaced with the `fits2img` and `hvJP2K` (<https://github.com/Helioviewer-Project/hvJP2K>) packages. If the server does not handle files produced by IDL, no server side use of Kakadu software is necessary.

`hvJP2K` consists in the following tools:

- `hv_jp2_decode` -- replacement for `kdu_expand`, sufficient for the web client image tile decoding;
- `hv_jp2_encode` -- proto-replacement for `fits2img`, not yet capable of emitting conforming JP2 files;
- `hv_jp2_transcode` -- wrapper for `kdu_transcode`, it can output JP2 format and can reparse the XML metadata to ensure conformity;
- `hv_jp2_verify` -- verify the conformity of JP2 file format to ensure end-to-end compatibility;
- `hv_jpx_merge` -- standalone replacement for `kdu_merge`, it can create JPX movies out of JP2 files;
- `hv_jpx_mergec` -- client for `hv_jpx_merged`, written in C;
- `hv_jpx_merged` -- Unix domain sockets threaded server for JPX merging functionality, it avoids the startup overhead of `hv_jpx_merge`;
- `hv_jpx_split` -- split JPX movies into standalone JP2 files.

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

The resulting FITS files consist of `BINARY TABLE`s with four columns `FIELDLINEx`, `FIELDLINEy`, `FIELDLINEz`, `FIELDLINEs`. The first three are mapped to unsigned shorts and can be converted to Cartesian coordinates using the formula 3 × (value × 2 / 65535 − 1) (on the client side one needs to add 32768). The `FIELDLINEs` value encodes the strength and, by the sign, the radial direction of the field. This encoding was chosen for a compact representation.

The field strength is mapped in the default JHelioviewer display as blue (negative radial) or red (positive radial); the lesser the color saturation, the weaker the field. In order to better see the direction of the field, points of the field lines beyond 2.4 solar radii have red or blue colors without blending with white.

# JHelioviewer Interfaces #

JHelioviewer communicates with the Helioviewer services using the HTTP network protocol. The JPEG2000 data service is implemented using a subset of the JPIP protocol on top of the HTTP network protocol. In addition, the JHelioviewer client supports the SAMP protocol (<http://www.ivoa.net/documents/SAMP/>) and includes a SAMP hub.

## Image Services API ##

The image services API is documented together with some examples at <https://api.helioviewer.org/docs/v2/>.

The API endpoints used by JHelioviewer are:

- `getDataSources` -- to retrieve the list of available datasets which is then used by JHelioviewer to populate the "New Layer" UI elements for each server;
- `getJPX` -- to request a time series of JP2 images as one JPX file;
- `getJP2Image` -- to request one JP2 image.

The API server reacts to the `getJPX` call by querying its database of image metadata, constructing a list of filenames to be used, passing the list to the program that will create the JPX file (`kdu_merge` or `hv_jpx_merge`), and making the resulting JPX file available to the `esajpip` server. Finally, the server sends back to the JHelioviewer client a JSON response which contains the JPIP URI to use. The client connects to that URI and starts interacting with the JPIP server.

JPX files can be of two types:

- aggregates of metadata with pointers to the JPEG2000 codestream data inside the original JP2 files;
- aggregates of data with embedded JPEG2000 codestreams.

The first form (`linked=true` and `jpip=true`) is used for the regular JHelioviewer interaction over JPIP, while the second form (`linked=false` and `jpip=false`) is used by the "Download layer" functionality to request the assembly of a self-contained JPX file which is then retrieved over HTTP and can be played back on the user computer without a network connection.

The JPEG2000 data services are provided by the `esajpip ` server which implements a restricted subset of the JPIP protocol over HTTP (to be described).

## Timeline Services API ##

The timeline API is a REST service and consists of three parts.

1. **Dataset Query API**: The client requests the description of the available datasets. The description includes group and label names to be used client-side, server-side name and units. It is available at <http://swhv.oma.be/datasets/index.php> and will list all datasets available and the corresponding groups. The response will be a JSON file with 2 keys:
    - `groups`: the list of groups visible in the client, it has 2 keys:
        - `key`: a unique identifier
        - `groupLabel`: the name of the group
    - `objects`: the list of datasets with keys:
        - `baseUrl`: the base URL of where to request the dataset
        - `group`: the group identifier to which the dataset belongs
        - `label`: the label of the dataset
        - `name`: the unique identifier of the dataset

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

The `multiplier` parameter allows for sending scaled data to the client when necessary. The values of many datasets are rather small numbers when expressed in standard units like W/m^2, thus scaling them allows for more floating-point precision in the response to the client.

The timestamps are with respect to Unix epoch. There is a guarantee that the data is sent ordered by timestamp.

The timeline API is implemented on the server side as several PHP scripts that forward the data requests to the relevant backend timeline storage service and format the JSON response for the JHelioviewer client.

## HEK Services API ##

This API is described at <http://solar.stanford.edu/hekwiki/ApplicationProgrammingInterface>.

## Geometry Services API ##

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
   { "2014-04-12T20:23:35.000": [143356392.01232576,2.712634949777619,0.12486990461569629]},
   { "2014-04-13T02:23:35.000": [143359318.57914788,2.7129759257313513,0.12473463991365513]},
   { "2014-04-13T08:23:35.000": [143362256.29411626,2.7133174795109087,0.12459673837570125]},
   { "2014-04-13T14:23:35.000": [143365205.0945752,2.713659603829239,0.12445620339056596]}
 ]
}
```

This is a list of UTC timestamps and coordinates indicating the geometric position of the camera (the STEREO Ahead spacecraft in this example). The first coordinate is the distance to Sun, the second and third coordinates are the Stonyhurst heliographic longitude and latitude of the given object. At the moment, the following locations are available: all JPL DE430 ephemeris locations (solar system planets, Pluto, the Moon), comet 67P/Churyumov-Gerasimenko. Also available are the following spacecraft trajectories (existing or planned): SOHO, STEREO, SDO, PROBA-2, PROBA-3, Solar Orbiter, Parker Solar Probe.

The following functions are implemented:

- `position` and `state` (in km and km/s); representations (`kind`): `rectangular`, `latitudinal`, `radec`, `spherical`, `cylindrical`;
- `transform` -- transform between several reference frames used in heliophysics; representations (`kind`): `matrix`, `angle` (Euler, rad), `quaternion`;
- `utc2scs` and `scs2utc` -- transform between UTC and spacecraft OBET (Solar Orbiter supported).

There is a guarantee that the data is sent ordered by UTC timestamps.

This service is used to support the Viewpoint functionality of the JHelioviewer client.

## SAMP ##

The SAMP messages supported by the JHelioviewer client are:

- `image.load.fits` -- specific FITS image;
- `table.load.fits` -- only for ESA SOHO Science Archive tool, as JHelioviewer does not support FITS tables yet;
- `jhv.load.image` -- any image type supported by JHelioviewer, type determined by filename extension;
- `jhv.load.request` -- image request file;
- `jhv.load.timeline` -- timeline request file;
- `jhv.load.state` -- state file.

Those messages have as parameter an URI to load. Both local and remote URIs are supported. An example of SAMP Web Profile usage is at <http://swhv.oma.be/test/samp/>.

## File Formats ##

Many of the file formats supported by the JHelioviewer client are based on the JSON format. All files can be either local on the user's computer or can be loaded over HTTP. JPX data can additionally be loaded over the JPIP protocol. Files can be loaded at start-up via the command line interface.

### State File ###

```json
{"org.helioviewer.jhv.state": {...}}
```

### Image Request File ###

JSON document specifying image requests to the default server in a simple manner as in the example below. Natural language specification of time is supported (as is the case for the state file) and, besides the `dataset` field, all fields are optional with sensible defaults.

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

In contrast to the 31k lines of code to implement all its many features, the core JHelioviewer design is very simple and can probably be expressed in a couple of thousands of lines of code. The principle of separation of concerns is applied throughout. Objects are asked to update themselves, they proceed to do so independently, and they report back when done. There are essentially no locks and no data structures synchronized between threads.

The program is driven via three timers:

- `Displayer` beats at constant 60 Hz and coalesces requests for decoding the image layers and refreshing the image canvas;
- `Movie` beats at a configurable frequency (default 20 Hz) and is responsible for setting the program time (i.e., frame advance);
- `UITimer` beats at constant 10 Hz and commands the refresh of the Swing UI components that need to change together with the movie frame; additionally, it commands the refresh of the timeline canvas.

The design description will be expanded in a future version of this document.

# CCN2 Tasks #

## WP20100 -- Study SWHV and JHV3D and WP20150 -- Merge JHV3D Ideas

The following topics were identified as areas of work for merging ideas from the JHV3D branch into SWHV.

**T1. Unified network/download (SWHV-CCN2-20100-01)**

The aim of this is to reduce the number of thread pools allocated for network protocols and therefore the memory usage, as well as to provide a central place for network connection management.

SWHV inherited `DownloadStream`, a thin wrapper over the Java `URLConnection`. This is a synchronous API; therefore, it has to be managed in separate threads in order to not block the program while interacting with remote hosts. The different network services are handled within SWHV by separate thread pools due to the historical evolution of plugins -- add-on components to be loaded by the main program in order to augment the functionality. The JPIP network functionality is implemented at the lower level of network sockets and the JPIP protocol over HTTP is handled in a separate thread per JPIP connection.

JHV3D implements a download manager on top of the Apache HTTP Client library. This download manager handles also the JPIP protocol.

SWHV currently re-implements all HTTP protocol functionality, with the exception of JPIP protocol, on top of the `OkHTTP3` network library. Possible future work may consist in replacing the various download thread pools with the `OkHTTP3` asynchronous API and therefore the use of the thread pool provided by the library.

Re-implementing the JPIP protocol on top of the high-level `OkHTTP3` library appears more difficult. It is not clear if a re-implementation is beneficial in the context of how the `esajpip` server handles the connections with the clients. The current low-level access into the network software stack is useful for achieving high data throughput between the JPIP client and server.

**T2. Helioviewer v2 API (SWHV-CCN2-20100-02)**

Both SWHV and JHV3D implement the version 2 of the Helioviewer API. The `getClosestToMidpoint` API call was added for the benefit of JHV3D request of data, and this call is linked to the JHV3D approach for the acquisition of data. It shall be studied if the approach is beneficial for SWHV and thus if this API is useful for SWHV. This has ramifications
with regard to playback and JPEG2000 caching. The `esajpip` server was modified to allow multiple ranges of frame requests in a single JPIP request, which may be a possible substitute.

SWHV supports both versions of API and has implemented the validation of server responses against a JSON schema. This functionality is also available as a separate standalone program (<https://github.com/Helioviewer-Project/DataSourcesChecker>).

**T3. Memory/disk cache: Save/read to/from cache on disk (SWHV-CCN2-20100-03)**

This deals with memory and disk caching of JPEG2000 data and is intimately linked with the playback and how the JPIP protocol is handled.

SWHV abandoned the concept of downloading regions out of the JPEG2000 frames, but still tries to minimize the resolution levels requested, such that, if the highest resolutions are never requested, they are never downloaded. Note, however, that recording movies forces the decoding of the full resolution of frames and therefore the download of the entire codestream.

Remote streams are cached by SWHV in Kakadu `KduCache` objects in memory. This is a concern for memory usage, therefore saving the codestream data to disk and limiting the `KduCache` size seems beneficial. However, SWHV was demonstrated to be capable of playing back streams of 10000 frames. This is the limit accepted by the ROB server, GSFC and IAS servers are limited to 1000 frames per stream.

Another concern is that the data of already downloaded frames cannot be reused between streams, for example when extending or shifting the temporal navigation of datasets. Having the capability of reusing already downloaded data is of high importance.

The approach taken by JHV3D is described in \[2\]. Note the limitations in the Kakadu API and documentation. It is possible that there are alternatives to the JHV3D approach.

**T4. Plugin interface (SWHV-CCN2-20100-04)**

Historically, plugins were implemented as standalone libraries to be loaded by the program to augment functionality. They necessitated a relatively limited support from the main program. As the functionality of the program becomes more complex and the expectations of integration between the parts higher, it is unclear if this approach is sustainable
and if keeping programming interfaces too stable impedes progress.

As part of implementing the SAMP plugin functionality, SWHV currently has simple interfaces for adding data layers and for loading state.

**T5. Playback engine: Extract portable ideas from JHV3D (SWHV-CCN2-20100-05)**

SWHV operates in a fully asynchronous manner and can achieve high frame rates with low CPU utilization. This topic deals on how the time and data flows are distributed within system and is closely linked to how Kakadu is used for decoding the JPEG2000 data. It is unclear whether there are advantages to the JHV3D approach. On a superficial level, SWHV appears to reach about 50% the CPU utilization of JHV3D with uncached decoded data and as little as 5% with cached decoded data.

**T6. Handling of transparency (SWHV-CCN2-20100-06)**

SWHV currently uses alpha-premultiplied color blending. The default compositing of the layers is at the middle between the ADD and OVER operators and an extra setting named "Blend" was added to control the variable level of additivity and opacity.

**T7. Installation (SWHV-CCN2-20100-07)**

The installation procedures of SWHV are derived from the previous versions. The install4j procedure of JHV3D is currently ported, but lightly tested and not deployed. In light of the announced Oracle approach to the release of future versions of Java, this is an essential feature.

**T8. Telemetry (SWHV-CCN2-20100-08)**

SWHV communicates back only crash reports to a Sentry server deployed at ROB. This server is connected to private email, Slack and GitLab services. Since the 2.12 release, several crashes unreported by users were intercepted. Some which were not uncovered by testing were fixed, others need more information.

The best manner to report issues is via <https://github.com/Helioviewer-Project/JHelioviewer-SWHV/issues>.

**T9. Guidelines for contributing (SWHV-CCN2-20100-09)**

SWHV preliminary version at:

<https://github.com/Helioviewer-Project/JHelioviewer-SWHV/blob/master/CONTRIBUTING.md>

From the list above, T1, T2, T3, T5 can be identified as possible candidates for further allocation of manpower in the next phase of merging of ideas. The current estimation is that T5 is too difficult and invasive to be tackled during the current project and the benefits are uncertain. From the remaining tasks, T3 has the highest priority and any remaining parts of T1 and T2 can be tackled as needed in an integrated manner.

## WP20200 -- Improve Core Functionality

The following tasks were identified:

1.  **(SWHV-CCN2-20200-01)** Study how to improve the temporal navigation jointly for image and timeline data at both short and long timescales.

This is highly dependent on the JPEG2000 data caching. However, the work proceeded independently as the caching is essentially an optimization. Time selection for the image layers was brought to the forefront and it became possible to use the timelines panel to temporally navigate jointly with the image layers.

2.  **(SWHV-CCN2-20200-02)** Save and restore program state.

This is implemented as a JSON document preserving with high fidelity the state of the program. The annotation save/restore will have to be implemented using the format for feature location specified by Solar Orbiter SOC (Solar Orbiter Feature Triplet).

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

This shall be tackled in WP20150.

5.  **(SWHV-CCN2-20200-05)** Specific event filtering, now that the filtering is not done anymore server side.

This is implemented on HEK derived events for NOAA SWPC flares on the GOES intensity values and for CACTus CME detections on the radial linear velocity.

6.  **(SWHV-CCN2-20200-06)** Proxy support.

This is implemented using the Java network proxy facilities.

## WP20300 -- Support Solar Orbiter Operations

The following tasks were identified:

1.  **(SWHV-CCN2-20300-01)** Consolidate user requirements, taking inputs from instrument teams such as EUI and STIX and the Solar Orbiter Science Operations Working Group (SOWG).

This is implemented by the draft Solar Orbiter User Requirements Document (ROB-SWHV-URD2) to be updated for the end of the project.

2.  **(SWHV-CCN2-20300-02)** Publish ROB ephemeris server on Helioviewer GitHub and collaborate with Solar Orbiter MADAWG for maintaining a coherent data (kernel) tree.

This is implemented by:

<https://github.com/Helioviewer-Project/GeometryService>

3.  **(SWHV-CCN2-20300-03)** Build propagation server incorporating several propagation models, enabling correlation of in-situ data with remote sensing data.

This will be implemented with an RPC server built on the Spyne toolkit (<http://spyne.io>, the same as the `GeometryService` service). The SWHV client will query a timestamp and a propagation model, the service will return a modified timestamp.

4.  **(SWHV-CCN2-20300-04)** Test with and integrate ACE, DSCVR or other current in-situ datasets identified as relevant today for space weather forecasters.

Once the propagation server is implemented, the specified datasets will be integrated.

5.  **(SWHV-CCN2-20300-05)** Plot the sub-spacecraft point on the solar surface (both radially and w.r.t. magnetic connectivity, i.e., Parker spiral).

This is implemented using existing functionality. The magnetic connectivity aspect will need to be tackled together with the implementation of the Solar Orbiter requirements, therefore is out of scope.

6.  **(SWHV-CCN2-20300-06)** Build on annotations to indicate instruments fields of view. Add ability to drag and show their centers. Optionally distort according to differential rotation. Distortion of solar images is outside of the resources for this CCN.

This will be implemented once the annotation functionality has full location awareness.

## WP20400 -- Enable Server Interoperability

The following tasks were identified:

1.  **(SWHV-CCN2-20400-01)** Improve the functionality of JPX manipulation to add movie merging.

This will be implemented by combining the `hvJP2K` package functionality splitting of JPX files into JP2 files and merging of JP2 files into JPX files.

2.  **(SWHV-CCN2-20400-02)** Packaging of software tools developed for the production and manipulation of JPEG2000 data.

This is implemented by modifying the validation functionality of the `hvJP2K` package to use a current `jpylyzer` package release.

3.  **(SWHV-CCN2-20400-03)** Cache JP2 headers in database at insertion time.

The purpose of this task is to reduce the server latency between the user selecting a dataset and the server making available the prepared JPX file to the `esajpip` server. While merge functionality of `hvJP2K` can be 10× faster than the Kakadu similar functionality, it is limited by I/O operations necessary to parse the JP2 files. Parsing the JP2
ahead of the time and storing the necessary information in the database together with the record of each file can possibly reduce this latency.

Other means to achieve a reduction in this latency is by changing the way SWHV requests the datasets to always request entire days.

4.  **(SWHV-CCN2-20400-04)** Build JPX from database headers.

The JPX merging of the `hvJP2K` package will be updated to accept externally produced JP2 headers.

5.  **(SWHV-CCN2-20400-05)** Support GSFC for the adoption of the `hvJP2K` tools, especially in the areas of transcoding and verification of the JP2 files.

This is an ongoing activity.

6.  **(SWHV-CCN2-20400-06)** Maintenance for `esajpip` server (not including implementation of feature requests).

This is an ongoing activity.

## WP20500 -- Release, Testing, Documentation

**(SWHV-CCN2-20500-01)** The tasks of this WP are an ongoing activity.

Currently the support for 32bit operating systems was removed in order to avoid the incidence of out-of-memory crashes and virtual address exhaustion, especially during movie creation. The software is supported under Windows, macOS and Linux for Oracle Java 8 64bit.

The software is also tested under later versions of OpenJDK. There are several illegal reflective accesses from JOGL into the now modularized Java frameworks, some of them OS-specific. Currently manifesting as warnings, it will be very difficult to fix those for future Java versions where they will become errors.

The software is under continuous refactoring and re-architecting as new features become available. Simplification and reduction of the number of lines of code are top priorities. Besides continuous testing, the software is regularly submitted to static code analysis using IDEA IntelliJ, Google ErrorProne, SpotBugs, PMD, and Synopsys Coverity.

## WP21200 -- Improve Client Interoperability

The following tasks were identified:

1.  **(SWHV-CCN2-21200-01)** Study and design a SAMP plugin; implement a prototype version based on a use case that will allow to estimate the time needed to produce an operational version.

The SAMP plugin is implemented and integrated in SWHV. It allows sending the loaded image layers to a SunPy script in order to load the original FITS files via VSO onto the user's computer. It also allows receiving compatible data from any external SAMP-aware program or web page (<http://swhv.oma.be/test/samp/>).

2.  **(SWHV-CCN2-21200-02)** Explore how to implement a new VSO plugin, including use case and time estimates.

In view of the above SAMP capabilities, direct VSO capabilities are better handled by a community wide effort such as SunPy which incorporates solar physics data web services such as VSO, JSOC and others.

However, the IDL-to-Java bridge will be investigated in order to enable the interoperability between IDL and SWHV via the SAMP protocol.

3.  **(SWHV-CCN2-21200-03)** Explore ways to improve and/or integrate FITS, NetCDF, VOTable formats.

SWHV has improved support FITS files and can support CDF (not NetCDF) format using the <https://github.com/mbtaylor/jcdf> library and VOTable format using the <https://github.com/aschaaff/savot> library.

This will be limited to known use cases exhibiting data calibration and known standard metadata.

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
