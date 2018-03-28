---
title: SWHV CCN2 Design Document
subtitle: ROB-SWHV(7186)_DDF2
author: SWHV Team
subject: Space Weather HelioViewer
date: 2018-04-01
geometry: margin=1in
papersize: A4
lot: true
lof: true
toc: true
colorlinks: true
fontsize: 10pt
titlepage: true
titlepage-color: "0088cc"
titlepage-text-color: "ffffff"
titlepage-rule-color: "ffffff"
titlepage-rule-height: 5
logo: hvLogo.png
logowidth: 0.1
---
\frenchspacing

`id: \exec{git hash-object \file}`

[traceability]: wp_traceability.pdf


**Royal Observatory of Belgium**

**University of Applied Sciences North-Western Switzerland**

  ---------------------- ------------------------------------------------------------------
  Contributing authors   Roman Bolzern, Bram Bourgoignie, Bogdan Nicula, Freek Verstringe
  Approved by            Bogdan Nicula
  ---------------------- ------------------------------------------------------------------


# Introduction

+------------+----------------------------------------------+
| Date       | Notes                                        |
+:===========+:=============================================+
| 2018-01-15 | Version 1.00 (Initial release)               |
+------------+----------------------------------------------+
| 2018-03-12 | Version 1.01 (Update following CDR2)         |
+------------+----------------------------------------------+
| 2018-04-01 | Version 1.1 (Update following remarks):\     |
|            |                                              |
|            | * translated to Markdown                     |
|            | * added design                               |
+------------+----------------------------------------------+

Table: Document history

## Purpose & Scope

This document (SWHV-DDF2) is part of the report of the design study of the work performed during the CCN2 phase of "Space Weather Helioviewer" project (Contract No. 4000107325/12/NL/AK, "High Performance Distributed Solar Imaging and Processing System" ESTEC/ITT AO/1-7186/12/NL/GLC). It focuses on the detailed explanation of the changes for several software components that need to be implemented or were already implemented by the time this document was made available for review.

## Applicable Documents

\[1\] Contract Change Notice No. 2: SWHV\_CCN2\_Proposal3\_BN2.pdf

## Reference Documents

\[2\] TR Architectural outline of JHV3 by Simon Felix (FHNW)

# Work Logic

The identified tasks are presented together with a proposed implementation. In many cases, by the time of writing of this document (MS6), the work was already performed, therefore the present tense is used. For the work to be performed for the MS7, the future tense is used.

Chapter 4 presents a traceability matrix for the tasks, as well as the assigned priority and the milestone for delivery. Features already delivered will be subjected to refinement and refactoring as new functionality becomes available in the client-server system.

# DDF

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

Remote streams are cached by SWHV in Kakadu KduCache objects in memory. This is a concern for memory usage, therefore saving the codestream data to disk and limiting the KduCache size seems beneficial. However, SWHV was demonstrated to be capable of playing back streams of 10000 frames. This is the limit accepted by the ROB server, GSFC and IAS servers are
limited to 1000 frames per stream.

Another concern is that the data of already downloaded frames cannot be reused between streams, for example when extending or shifting the temporal navigation of datasets. Having the capability of reusing already downloaded data is of high importance.

The approach taken by JHV3D is described in \[2\]. Note the limitations in the Kakadu API and documentation. It is possible that there are alternatives to the JHV3D approach.

**T4. Plugin interface (SWHV-CCN2-20100-04)**

Historically, plugins were implemented as standalone libraries to be loaded by the program to augment functionality. They necessitated a relatively limited support from the main program. As the functionality of the program becomes more complex and the expectations of integration between the parts higher, it is unclear if this approach is sustainable
and if keeping programming interfaces too stable impedes progress.

As part of implementing the SAMP plugin functionality, SWHV currently has simple interfaces for adding data layers and for loading state.

**T5. Playback engine: Extract portable ideas from JHV3D (SWHV-CCN2-20100-05)**

SWHV operates in a fully asynchronous manner and can achieve high frame rates with low CPU utilization. This topic deals on how the time and data flows are distributed within system and is closely linked to how Kakadu is used for decoding the JPEG2000 data. It is unclear whether there are advantages to the JHV3D approach. On a superficial level, SWHV appears to reach about 50% the CPU utilization of JHV3D with uncached decoded data and as little as 5% with cached decoded data.

**T6. Handling of transparency (SWHV-CCN2-20100-06)**

SWHV currently uses alpha-premultiplied colour blending. The default compositing of the layers is at the middle between the ADD and OVER operators and an extra setting named "Blend" was added to control the variable level of additivity and opacity.

**T7. Installation (SWHV-CCN2-20100-07)**

The installation procedures of SWHV are derived from the previous versions. The install4j procedure of JHV3D is currently ported, but lightly tested and not deployed.

**T8. Telemetry (SWHV-CCN2-20100-08)**

SWHV communicates back only crash reports to a Sentry server deployed at ROB. This server is connected to private email, Slack and GitLab services.

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

This is implemented using JSON documents specifying requests to the default server in a simple manner as in the example below. Natural language specification of time is supported (as is the case for the state file) and, besides the "dataset" field, all fields are optional with sensible defaults.

Example:

`
{
  "observatory":"SDO",
  "startTime":"yesterday",
  "endTime":"today",
  "cadence":1800,
  "dataset":"AIA 304"
}
`

4.  **(SWHV-CCN2-20200-04)** On disk caching of JP2 code-stream data.

This shall be tackled in WP20150.

5.  **(SWHV-CCN2-20200-05)** Specific event filtering, now that the filtering is not done anymore server side.

This is implemented on HEK derived events for NOAA SWPC flares on the GOES intensity values and for CACTus CME detections on the radial linear velocity.

6.  **(SWHV-CCN2-20200-06)** Proxy support.

This is implemented using the Java network proxy facilities.

## WP20300 -- Support Solar Orbiter Operations

The following tasks were identified:

1.  **(SWHV-CCN2-20300-01)** Consolidate user requirements, taking inputs from instrument teams such as EUI and STIX and the Solar Orbiter Science Operations Working Group (SOWG).

This is implemented by the draft Solar Orbiter User Requirements Document (ROB-SWHV\_URD2) to be updated for the end of the project.

2.  **(SWHV-CCN2-20300-02)** Publish ROB ephemeris server on Helioviewer GitHub and collaborate with Solar Orbiter MADAWG for maintaining a coherent data (kernel) tree.

This is implemented by:

<https://github.com/Helioviewer-Project/GeometryService>

3.  **(SWHV-CCN2-20300-03)** Build propagation server incorporating several propagation models, enabling correlation of in-situ data with remote sensing data.

This will be implemented with a RPC server built on the Spyne toolkit (<http://spyne.io>, the same as the `GeometryService` service). The SWHV client will query a timestamp and a propagation model, the service will return a modified timestamp.

4.  **(SWHV-CCN2-20300-04)** Test with and integrate ACE, DSCVR or other current in-situ datasets identified as relevant today for space weather forecasters.

Once the propagation server is implemented, the specified datasets will be integrated.

5.  **(SWHV-CCN2-20300-05)** Plot the sub-spacecraft point on the solar surface (both radially and w.r.t. magnetic connectivity, i.e. Parker spiral).

This is implemented using existing functionality. The magnetic connectivity aspect will need to be tackled together with the implementation of the Solar Orbiter requirements, therefore is out of scope.

6.  **(SWHV-CCN2-20300-06)** Build on annotations to indicate instruments fields of view. Add ability to drag and show their centres. Optionally distort according to differential rotation. Distortion of solar images is outside of the resources for this CCN.

This will be implemented once the annotation functionality has full location awareness.

## WP20400 -- Enable Server Interoperability

The following tasks were identified:

1.  **(SWHV-CCN2-20400-01)** Improve the functionality of JPX manipulation to add movie merging.

This will be implemented by combining the `hvJP2K` package functionality splitting of JPX files into JP2 files and merging of JP2 files into JPX files.

2.  **(SWHV-CCN2-20400-02)** Packaging of software tools developed for the production and manipulation of JPEG2000 data.

This is implemented by modifying the validation functionality of the `hvJP2K` package (<https://github.com/Helioviewer-Project/hvJP2K>) to use a current `jpylyzer` package (<https://github.com/openpreserve/jpylyzer>) release.

3.  **(SWHV-CCN2-20400-03)** Cache JP2 headers in database at insertion time.

The purpose of this task is to reduce the server latency between the user selecting a dataset and the server making available the prepared JPX file to the `esajpip` server. While merge functionality of `hvJP2K` can be 10x faster than the Kakadu similar functionality, it is limited by I/O operations necessary to parse the JP2 files. Parsing the JP2
ahead of the time and storing the necessary information in the database together with the record of each file can possibly reduce this latency.

Other means to achieve a reduction in this latency is by changing the way SWHV requests the datasets to always request entire days.

4.  **(SWHV-CCN2-20400-04)** Build JPX from database headers.

The JPX merging of the `hvJP2K` package will be updated to accept externally produced JP2 headers.

5.  **(SWHV-CCN2-20400-05)** Support GSFC for the adoption of the `hvJP2K` tools, especially in the areas of transcoding and verification of the JP2 files.

This is an ongoing activity.

6.  **(SWHV-CCN2-20400-06)** Maintenance for `esajpip` server (not including implementation of feature requests).

This is an ongoing activity.

## WP20500 -- Release, Testing, Documentation

**(SWHV-CCN2-20500-01)** The tasks of this WP are an ongoing activity. Currently the support for 32bit operating systems was removed in order to avoid the incidence of out-of-memory crashes and virtual address exhaustion, especially during movie creation.

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

+-----------------------------------+-----------------------------------+
| Video support                     | The most supported video format   |
|                                   | by current web browsers is MP4    |
|                                   | H264                              |
|                                   | (<https://caniuse.com/#feat=mpeg4 |
|                                   | >,                                |
|                                   | including «fragmented» versions   |
|                                   | for streaming). All tested        |
|                                   | platforms support HTML5 video     |
|                                   | playback of grayscale 8bit        |
|                                   | interlaced mp4 videos.            |
+===================================+===================================+
| Storage                           | For the proof of concept, a video |
|                                   | tree was built from AIA images,   |
|                                   | covering one day, with 4          |
|                                   | different pixel resolutions (full |
|                                   | res 4k down to 512) and 6         |
|                                   | different time resolutions i.e.   |
|                                   | playback speeds (1x, 2x, 4x, 8x,  |
|                                   | 16x, 32x).                        |
|                                   |                                   |
|                                   | Helioviewer.org provided 2379     |
|                                   | JPEG2000 images, from which       |
|                                   | 6\'035 videos were built. With    |
|                                   | the current video format, we      |
|                                   | expect **50TB** of videos,        |
|                                   | compared to the **57TB** of       |
|                                   | JPEG2000 images they cover.       |
+-----------------------------------+-----------------------------------+
| Bandwidth                         | In full resolution streaming      |
|                                   | mode, i.e. streaming the full 4k  |
|                                   | images, only 7MB/sec are          |
|                                   | required. Yet the tool is built   |
|                                   | to download with region of        |
|                                   | interests, with a realistic worst |
|                                   | case of **2.5MB/sec** bandwidth   |
|                                   | required. For mobile devices,     |
|                                   | bandwidth usage can be throttled  |
|                                   | down to **0.15MB/sec**.           |
+-----------------------------------+-----------------------------------+
| WebGL                             | The proof of concept projects 64  |
|                                   | videos (8 512px videos in width   |
|                                   | and height) onto a 3D sphere.     |
|                                   | While the GPU has no issues, this |
|                                   | worst-case performance test shows |
|                                   | significant CPU consumption, with |
|                                   | the naive implementation slowing  |
|                                   | down the playback to 10 FPS       |
|                                   | (frames per second). We are       |
|                                   | confident though that this can be |
|                                   | addressed with improved GPU       |
|                                   | communication.                    |
|                                   |                                   |
|                                   | Practically all devices support   |
|                                   | WebGL and 4k textures             |
|                                   | (<https://webglstats.com/webgl/pa |
|                                   | rameter/MAX_TEXTURE_SIZE>)        |
+-----------------------------------+-----------------------------------+
| Decoding                          | The decoding speed of MP4 videos  |
|                                   | proved to be no performance       |
|                                   | bottleneck at all. Even more,     |
|                                   | this work can be offloaded to a   |
|                                   | worker thread, and would          |
|                                   | therefore be non-blocking.        |
|                                   |                                   |
|                                   | All browsers support web workers  |
|                                   | (<https://caniuse.com/#feat=webwo |
|                                   | rkers>)                           |
+-----------------------------------+-----------------------------------+

The proof of concept showed that no criterion from this first evaluation set hinders further investigation into video streaming of AIA images.

Further, with the growing support for the Media Source Extensions API (<https://caniuse.com/#feat=mediasource>), close control can be exercised over video playback, chunking and streaming. For caching, IndexedDB seems suitable (<https://caniuse.com/#search=indexeddb>).

# Traceability Matrix

![][traceability]

\include{"acronyms.md"}
