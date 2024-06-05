KPL/FK

Frame (FK) SPICE kernel file for Solar Orbiter-specific science frames
==============================================================================

   This frames kernel defines a number of generic frames used by SOLO
   mission for Science opportunities identification, data analysis and
   scientific research. These frames are currently not ``built'' into
   the SPICE toolkit.

   These frames are sorted in two groups: those that are SOLO mission
   specific and those that are Sun and Earth generic. The first group contains
   the frames defined by and for the Solar Orbiter mission, while the second
   provides the frames that are commonly accepted by the scientific community
   for the Sun.

   The IAU body-fixed rotational frames for the Sun is an exception to this
   grouping, as it is provided in a separate PCK kernel file.


Version and Date
------------------------------------------------------------------------------

   Version 0.8  -- February 23, 2023 -- Ricardo Valles Blanco, ESAC/ESA
                                        Alfredo Escalante Lopez, ESAC/ESA
                                        Andrew Walsh, ESAC/ESA

      Added Heliocentric orbital reference frame (SOLO_HOR).

      Implemented Geocentric Solar Magnetospheric (SOLO_GSM) dynamic frame
      based on the EARTH_NORTH_POLE position from SPK.

      Corrected several typos.

   Version 0.7  -- January 21, 2021 -- Alfredo Escalante Lopez, ESAC/ESA
                                       Andrew Walsh, ESAC/ESA

      Corrected EARTH_MECL_MEQX_J2000 reference frame family.
      Added Venus-centric Solar Orbital frame (VSO).

   Version 0.6  -- February 13, 2020 -- Marc Costa Sitja, ESAC/ESA
                                        Andrew Walsh, ESAC/ESA

      Corrected SOLO_GSE and SOLO_HEE definitions to implement definition
      of secondary vector w.r.t. EARTH_MECL_MEQX_J2000 that has also been
      defined (EARTH_MECL_MEQX frozen at J2000 epoch).

   Version 0.5  -- January 27, 2020 -- Marc Costa Sitja, ESAC/ESA
                                       Andrew Walsh, ESAC/ESA

      Removed duplicated definition of SOLO_GSE. The duplicated one was
      the same as EARTH_SUN_ECL: we use ecliptic of J2000 in all mission
      specific frames (SOLO_*). Corrected SOLO_SOLAR_MHP, SOLO_GSE and
      SOLO_HEE descriptions.

   Version 0.4  -- October 1, 2019 -- Marc Costa Sitja, ESAC/ESA

      Corrected SOLO_SUN_RTN frame definition.

   Version 0.3  -- July 3, 2019 -- Marc Costa Sitja, ESAC/ESA
                                   Bill Thomson, JPL/NASA
                                   Andrew Walsh, ESAC/ESA

      Corrected SOLO_SUN_RTN frame definition and updates names and IDs
      for the frames from [17]. Corrected SOLO_GAE for it cannot be a
      rotating frame if the epoch is frozen at date J2000.

   Version 0.2  -- March 28, 2019 -- Marc Costa Sitja, ESAC/ESA
                                     Bill Thomson, JPL/NASA

      Corrected SOLO_SUN_RTN frame definition and updates names and IDs
      for the frames from [17]. Corrected SOLO_GAE for it cannot be a
      rotating frame if the epoch is frozen at date J2000.

   Version 0.1  -- October 18, 2017 -- Marc Costa Sitja, ESAC/ESA

      Redefined SOLO_GAE, SOLO_GSE, SOLO_HEE to be of date J2000. Added
      all the frames defined in [17].

   Version 0.0 -- June 20, 2017 -- Marc Costa Sitja, ESAC/ESA

      Initial version.


References
------------------------------------------------------------------------------

   1.  ``Frames Required Reading''

   2.  ``Kernel Pool Required Reading''

   3.  ``Report of the IAU/IAG/COSPAR Working Group on Cartographic
       Coordinates and Rotational Elements: 2003.''

   4.  ``Report of the IAU/IAG/COSPAR Working Group on Cartographic
       Coordinates and Rotational Elements: 2009.''

   5.  ``Dynamic Heliospheric Coordinate Frames developed for the
        NASA STEREO mission'', heliospheric_v004u.tf from the NEW HORIZONS
        NH-J/P/SS-SPICE-6-V1.0 dataset.

   6.  ``Solar Orbiter Science Operations Frames'',
       solo_ANC_soc-sci-fk_V00.tf.

   7.  ``List of needed frames for Solar Orbiter'', Solar Orbiter
       Confluence, https://issues.cosmos.esa.int/solarorbiterwiki/display/
       SOL/List+of+needed+frames+for+Solar+Orbiter

   8.  ``Generic Frame Definitions Kernel for Heliocentric frames'',
       sun_v02.tf, Draft Generic Frame Kernel from NAIF.

   9.  http://stereo.sr.unh.edu/data/PLASTIC_Resources/
       stereo_coordinates.pdf

   10. Weiduo Hu, "Fundamental Spacecraft Dynamics and Control,"
       Wiley 2015

   11. Li, H., "Geostationary Satellites Collocation," Springer, 2014

   12. ``The SunSPICE Ephemeris Package for Solar Missions'',
        Thompson, W., August 4, 2016.

   13. Hapgood,M. (1992). "Space physics coordinate transformations:
       A user guide," Planetary and Space Science, 40, 711-717

   14. OMNIWeb - Description of Heliospheric Coordinate Systems:
       http://omniweb.gsfc.nasa.gov/coho/helios/plan_des.html

   15. Franz and Harper. (2002) "Heliospheric Coordinate Systems,"
       Space Science, 50, 217ff.

   16. Seidelmann, P.K., "Explanatory Supplement to the Astronomical
       Almanac," (1992), University Science Books.

   17. ``Dynamic Heliospheric Coordinate Frames developed for the NASA
       STEREO mission'', heliospheric__v004u.tf, William Thompson,
       Brian T. Carcich, April 19, 2016.

   18. "International Geomagnetic Reference Field 13th generation (1900-2025)
       from the Working Group V-MOD of IAGA."
       https://www.ngdc.noaa.gov/IAGA/vmod/igrf.html

   19. ``Solar Orbiter SGS-FD ICD'', SOL-ESC-IF-50005 Issue2 Rev 3,
       Michael Mueller(OPS-GFS), December 3, 2020.


Contact Information
------------------------------------------------------------------------------

   If you have any questions regarding this file contact the
   ESA SPICE Service (ESS) at ESAC:

           Alfredo Escalante Lopez
           (+34) 91-8131-429
           alfredo.escalante@esa.int

   or the Solar Orbiter Science Operations Center at ESAC:

           sol_soc@esa.int


Implementation Notes
------------------------------------------------------------------------------

   This file is used by the SPICE system as follows: programs that make use
   of this frame kernel must "load" the kernel normally during program
   initialization. Loading the kernel associates the data items with
   their names in a data structure called the "kernel pool". The SPICELIB
   routine FURNSH loads a kernel into the pool as shown below:

     FORTRAN: (SPICELIB)

       CALL FURNSH ( frame_kernel_name )

     C: (CSPICE)

       furnsh_c ( frame_kernel_name );

     IDL: (ICY)

       cspice_furnsh, frame_kernel_name

     MATLAB: (MICE)

          cspice_furnsh ( 'frame_kernel_name' )

     PYTHON: (SPICEYPY)*

          furnsh( frame_kernel_name )

   In order for a program or routine to extract data from the pool, the
   SPICELIB routines GDPOOL, GIPOOL, and GCPOOL are used.  See [2] for
   more details.

   This file was created and may be updated with a text editor or word
   processor.

   * SPICEYPY is a non-official, community developed Python wrapper for the
     NAIF SPICE toolkit. Its development is managed on Github.
     It is available at: https://github.com/AndrewAnnex/SpiceyPy


SPICE Frame names and NAIF ID Codes
------------------------------------------------------------------------------

   The following names and NAIF ID codes are defined in this kernel file:

       Name                   ID
       ---------------------  --------
       EARTH_NORTH_POLE       399901


   The following generic frames are defined in this kernel file:

      SPICE Frame Name            Long-name
      -------------------------   --------------------------------------------

   SOLO mission specific generic frames:

      SOLO_SUN_RTN                Sun Solar Orbiter Radial-Tangential-Normal
      SOLO_SOLAR_MHP              S/C-centred mirror helioprojective
      SOLO_IAU_SUN_2009           Sun Body-Fixed based on IAU 2009 report
      SOLO_IAU_SUN_2003           Sun Body-Fixed based on IAU 2003 report
      SOLO_GAE                    Geocentric Aries Ecliptic at J2000 (GAE)
      SOLO_GSE                    Geocentric Solar Ecliptic at J2000 (GSE)
      SOLO_HEE                    Heliocentric Earth Ecliptic at J2000 (HEE)
      SOLO_HOR                    Heliocentric orbital reference frame (HOR)
      SOLO_VSO                    Venus-centric Solar Orbital (VSO)

   Heliospheric Coordinate Frames developed for the NASA STEREO mission:

      SOLO_ECLIPDATE              Mean Ecliptic of Date Frame
      SOLO_HCI                    Heliocentric Inertial Frame
      SOLO_HEE_NASA               Heliocentric Earth Ecliptic Frame
      SOLO_HEEQ                   Heliocentric Earth Equatorial Frame
      SOLO_GEORTN                 Geocentric Radial Tangential Normal Frame

   Heliocentric Generic Frames(*):

      SUN_ARIES_ECL               Heliocentric Aries Ecliptic   (HAE)
      SUN_EARTH_CEQU              Heliocentric Earth Equatorial (HEEQ)
      SUN_EARTH_ECL               Heliocentric Earth Ecliptic   (HEE)
      SUN_INERTIAL                Heliocentric Inertial         (HCI)

   Geocentric Generic Frames:

      EARTH_SUN_ECL   (*)         Geocentric Solar Ecliptic     (GSE)
      EARTH_MECL_MEQX (*)         Earth Mean Ecliptic and Equinox of date
                                  frame (Auxiliary frame for EARTH_SUN_ECL)
      EARTH_MECL_MEQX_J2000       Earth Mean Ecliptic and Equinox at J2000
                                  frame (Auxiliary frame for SOLO_GSE and
                                  SOLO_HEE)


   (*) These frames are commonly used by other missions for data analysis
       and scientific research. In the future NAIF may include them
       in their official generic frames kernel for the Sun and Earth systems.
       When this happens the frames will be removed from this kernel.


   These frames have the following centers, frame class and NAIF
   IDs:

      SPICE Frame Name              Center      Class    NAIF ID
      --------------------------    ----------  -------  ----------
      SOLO_SUN_RTN                  SOLO        DYNAMIC     -144991
      SOLO_SOLAR_MHP                SOLO        DYNAMIC     -144992
      SOLO_IAU_SUN_2009             SUN         FIXED       -144993
      SOLO_IAU_SUN_2003             SUN         FIXED       -144994

      SOLO_GAE                      EARTH       DYNAMIC     -144995
      SOLO_GSE                      EARTH       DYNAMIC     -144996
      SOLO_HEE                      SUN         DYNAMIC     -144997
      SOLO_HOR                      SUN         DYNAMIC     -144985
      SOLO_VSO                      VENUS       DYNAMIC     -144999
      SOLO_GSM                      EARTH       DYNAMIC     -144962

      SOLO_ECLIPDATE                EARTH       PARAM       -144980
      SOLO_HCI                      SUN         DYNAMIC     -144981
      SOLO_HEE_NASA                 SUN         DYNAMIC     -144982
      SOLO_HEEQ                     SUN         DYNAMIC     -144983
      SOLO_GEORTN                   SUN         DYNAMIC     -144984

      SUN_ARIES_ECL                 SUN         DYNAMIC  1000010000
      SUN_EARTH_CEQU                SUN         DYNAMIC  1000010001
      SUN_EARTH_ECL                 SUN         DYNAMIC  1000010002
      SUN_INERTIAL                  SUN         FIXED    1000010004

      EARTH_SUN_ECL                 EARTH       DYNAMIC   300399005
      EARTH_MECL_MEQX               EARTH       PARAM     300399000
      EARTH_MECL_MEQX_J2000         EARTH       DYNAMIC     -144998


   These frames have the following common names and other designators
   in literature:

      SPICE Frame Name            Common names and other designators
      --------------------        --------------------------------------
      SUN_ARIES_ECL               HAE, Solar Ecliptic (SE)
      SUN_EARTH_CEQU              HEEQ, Stonyhurst Heliographic
      SUN_INERTIAL                HCI, Heliographic Inertial (HGI)
      EARTH_SUN_ECL               GSE of Date, Hapgood
      SUN_EARTH_ECL               HEE of Date
      EARTH_MECL_MEQX             Mean Ecliptic of Date (ECLIPDATE)


   The keywords implementing these frame definitions are located in the
   section "Generic Dynamic Frames" and "Generic Inertial Frames".


General Notes About This File
------------------------------------------------------------------------------

   About Required Data:
   --------------------

   Most of the dynamic frames defined in this file require at least one
   of the following kernels to be loaded prior to their evaluation,
   normally during program initialization:

     - Planetary ephemeris data (SPK), e.g. DE403, DE405, etc;
     - Planetary constants data (PCK);
     - Earth generic frames definitions (FK).

   Note that loading different kernels will lead to different orientations of
   the same frame at a given epoch, providing different results from each
   other, in terms of state vectors referred to these frames.


   About Implementation:
   ---------------------

   The SPICE frames defined within this file and their corresponding
   references in literature might not be equivalent, both due to
   variations in the SPICE kernels on which the SPICE frame depends,
   and due to possible differences in both the frame's definition and
   implementation (e.g. GSE can be defined using the instantaneous
   orbital plane or mean ecliptic; the mean ecliptic is a function of
   the ecliptic model). Please refer to each applicable frame
   description section for particular details on the current SPICE
   kernel implementation.


SOLO Mission Specific Scientific Frame Definitions
------------------------------------------------------------------------------

   This section contains the definition of the SOLO mission specific
   scientific frames.


Sun Solar Orbiter Radial-Tangential-Normal (SOLO_SUN_RTN)
------------------------------------------------------------------------

   SPICE frame name, common names and other designators:
   -----------------------------------------------------

   Within the SPICE system, the Sun SOLO Radial-Tangential-Normal
   frame is referred as SOLO_SUN_RTN. In literature, this frame is
   referred as RTN (from [9]), Orbit RTN Coordinate System (from
   [11]), or radial-transverse-normal (from [10]).


   Definition:
   -----------
   The Sun SOLO Radial-Tangential-Normal frame is defined as
   follows (from [10]):

      -  the position of SOLO relative to the Sun is the
         primary vector: +X axis points from the Sun to
         SOLO (antisunward direction);

      -  +Z axis is parallel to the Solar North;

      -  +Y axis completes the right-handed system;

      -  the origin of this frame is the center of mass of SOLO.

   All vectors are geometric: no aberration corrections are used.


   Required Data:
   --------------

   This frame is defined as a two-vector frame using two different
   types of specifications for the primary and secondary vectors.

   The primary vector is defined as an 'observer-target position'
   therefore, the ephemeris data required to compute the SOLO-Sun state
   vector in the J2000 reference frame must be loaded before using this
   frame.


   Remarks:
   --------

   This frame is defined based on SPK data: different planetary
   ephemerides for the Sun, the Solar System Barycenter and SOLO spacecraft
   will lead to different frame orientation at a given time.

   It is strongly recommended to indicate what data have been used
   in the evaluation of this frame when referring to it, e.g.
   SOLO_SUN_RTN using the DE405 ephemeris and the SOLO ephemeris
   version N.

   \begindata

      FRAME_SOLO_SUN_RTN             = -144991
      FRAME_-144991_NAME             = 'SOLO_SUN_RTN'
      FRAME_-144991_CLASS            =  5
      FRAME_-144991_CLASS_ID         = -144991
      FRAME_-144991_CENTER           = -144
      FRAME_-144991_RELATIVE         = 'J2000'
      FRAME_-144991_DEF_STYLE        = 'PARAMETERIZED'
      FRAME_-144991_FAMILY           = 'TWO-VECTOR'
      FRAME_-144991_PRI_AXIS         = 'X'
      FRAME_-144991_PRI_VECTOR_DEF   = 'OBSERVER_TARGET_POSITION'
      FRAME_-144991_PRI_OBSERVER     = 'SUN'
      FRAME_-144991_PRI_TARGET       = 'SOLO'
      FRAME_-144991_PRI_ABCORR       = 'NONE'
      FRAME_-144991_SEC_AXIS         = 'Z'
      FRAME_-144991_SEC_VECTOR_DEF   = 'CONSTANT'
      FRAME_-144991_SEC_FRAME        = 'IAU_SUN'
      FRAME_-144991_SEC_SPEC         = 'RECTANGULAR'
      FRAME_-144991_SEC_VECTOR       = ( 0, 0, 1 )

  \begintext


Solar Orbiter-centred mirror helioprojective (SOLO_SOLAR_MHP):
------------------------------------------------------------------------

   This is a frame to be used together with Solar Orbiter FITS files.
   According to [7] the Solar Orbiter-centred mirror helioprojective
   reference frame -- SOLO_SOLAR_MHP -- is defined as follows:

      -  +Z axis is parallel to the S/C-Sun apparent direction;

      -  +Y axis is parallel to the Solar North;

      -  +X axis completes the right-handed system;

      -  the origin of this frame is is the point of intersection of the
         launcher longitudinal axis with the separation plane between the
         launcher and the composite.

   The S/C-Sun vector is not geometric and aberration corrections apply to the
   state of the Sun to account for one-way light time and stellar aberration.

   This is almost the same as the SOLO_SUN_NORM frame [6], except for
   different axis labelling. It is almost ``helioprojective'' except that the
   +X axis has been mirrored to make the system Right-Handed for SPICE. In
   addition the +Z axis approach is not identical (actually helioprojective is
   implicitly stellar aberration corrected, since it defines the apparent disk
   centre on the axis. Thus z-axis approach is the same).


   Required Data:
   --------------

   This frame is defined as a two-vector frame using two different
   types of specifications for the primary and secondary vectors.

   The primary vector is defined as an 'observer-target position' vector.
   Therefore, the ephemeris data required to compute the SOLO-Sun position
   vector in the J2000 reference frame must be loaded before using this frame.

   The secondary vector is defined as a constant vector in the IAU_SUN frame,
   which provides the Solar North.


   Remarks:
   --------

   This frame is defined based on SPK data: different planetary ephemerides
   for SOLO, the Sun and the Solar System Barycenter will lead to a different
   frame orientation at a given time.

   It is strongly recommended to indicate what data have been used in the
   evaluation of this frame when referring to it, e.g. SOLO_SOLAR_MHP using
   the IAU 2009 constants and the DE405 ephemeris.

   \begindata

      FRAME_SOLO_SOLAR_MHP           = -144992
      FRAME_-144992_NAME             = 'SOLO_SOLAR_MHP'
      FRAME_-144992_CLASS            =  5
      FRAME_-144992_CLASS_ID         = -144992
      FRAME_-144992_CENTER           = -144
      FRAME_-144992_RELATIVE         = 'J2000'
      FRAME_-144992_DEF_STYLE        = 'PARAMETERIZED'
      FRAME_-144992_FAMILY           = 'TWO-VECTOR'
      FRAME_-144992_PRI_AXIS         = 'Z'
      FRAME_-144992_PRI_VECTOR_DEF   = 'OBSERVER_TARGET_POSITION'
      FRAME_-144992_PRI_OBSERVER     = 'SOLO'
      FRAME_-144992_PRI_TARGET       = 'SUN'
      FRAME_-144992_PRI_ABCORR       = 'LT+S'
      FRAME_-144992_SEC_AXIS         = 'Y'
      FRAME_-144992_SEC_VECTOR_DEF   = 'CONSTANT'
      FRAME_-144992_SEC_FRAME        = 'IAU_SUN'
      FRAME_-144992_SEC_SPEC         = 'RECTANGULAR'
      FRAME_-144992_SEC_VECTOR       = ( 0, 0, 1 )

   \begintext


Sun Body-Fixed Frame - IAU 2009 (SOLO_IAU_SUN_2009)
------------------------------------------------------------------------

   Definition:
   -----------

   The IAU frame is defined as follows:

      -  +Z axis is parallel to the Sun rotation axis, pointing
         toward the North side of the invariable plane;

      -  +X axis is aligned with the ascending node of the Sun
         orbital plane with the Sun equator plane;

      -  +Y axis completes the right-handed system;

      -  the origin of this frame is the center of mass of the Sun.


   Remarks:
   --------

   This frame is defined as a PCK-based frame, using the Sun's
   orientation data provided in the IAU 2009 report (see [3]). This
   frame is equivalent to the IAU_SUN body-fixed frame, when
   using pck00009.tpc and pck00010.tpc.

   The orientation of this frame with respect to the J2000 inertial
   frame is provided using three Euler angles which describe the pole
   and prime meridian location: the first two angles, in order, are
   the right ascension and declination (RA and DEC) of the north pole
   of the Sun as a function of time. The third angle is the prime
   meridian location (represented by 'PM'), which is expressed as a
   rotation about the north pole, also a function of time.

   The time arguments of functions that define orientation always
   refer to Barycentric Dynamical Time (TDB), measured in centuries
   or days past J2000 epoch, which is Julian ephemeris date 2451545.0.
   The time units are ephemeris days for prime meridian motion and
   ephemeris centuries for motion of the pole.

   \begindata

      FRAME_SOLO_IAU_SUN_2009     = -144993
      FRAME_-144993_NAME          = 'SOLO_IAU_SUN_2009'
      FRAME_-144993_CLASS         =  2
      FRAME_-144993_CLASS_ID      = -144993
      FRAME_-144993_CENTER        =  10

      BODY-144993_POLE_RA         = (  286.13       0.          0. )
      BODY-144993_POLE_DEC        = (   63.87       0.          0. )
      BODY-144993_PM              = (   84.176     14.18440     0. )
      BODY-144993_LONG_AXIS       = (    0.                        )

   \begintext


Sun Body-Fixed Frame - IAU 2003 (SOLO_IAU_SUN_2003)
------------------------------------------------------------------------

   Definition:
   -----------

   The IAU frame is defined as follows:

      -  +Z axis is parallel to the Sun rotation axis, pointing
         toward the North side of the invariable plane;

      -  +X axis is aligned with the ascending node of the Sun
         orbital plane with the Sun equator plane;

      -  +Y axis completes the right-handed system;

      -  the origin of this frame is the center of mass of the Sun.


   Remarks:
   --------

   This frame is defined as a PCK-based frame, using the Sun's
   orientation data provided in the IAU 2003 report (see [4]). This
   frame is equivalent to the IAU_SUN body-fixed frame, when
   using pck00008.tpc.

   The orientation of this frame with respect to the J2000 inertial
   frame is provided using three Euler angles which describe the pole
   and prime meridian location: the first two angles, in order, are
   the right ascension and declination (RA and DEC) of the north pole
   of the Sun as a function of time. The third angle is the prime
   meridian location (represented by 'PM'), which is expressed as a
   rotation about the north pole, also a function of time.

   The time arguments of functions that define orientation always
   refer to Barycentric Dynamical Time (TDB), measured in centuries
   or days past J2000 epoch, which is Julian ephemeris date 2451545.0.
   The time units are ephemeris days for prime meridian motion and
   ephemeris centuries for motion of the pole.

   \begindata

      FRAME_SOLO_IAU_SUN_2003     = -144994
      FRAME_-144994_NAME          = 'SOLO_IAU_SUN_2003'
      FRAME_-144994_CLASS         =  2
      FRAME_-144994_CLASS_ID      = -144994
      FRAME_-144994_CENTER        =  10

      BODY-144994_POLE_RA         = (  286.13       0.          0. )
      BODY-144994_POLE_DEC        = (   63.87       0.          0. )
      BODY-144994_PM              = (   84.10      14.18440     0. )
      BODY-144994_LONG_AXIS       = (    0.                        )

   \begintext


Geocentric Aries Ecliptic of J2000 frame (SOLO_GAE)
------------------------------------------------------------------------

   Definition:
   -----------
   The Heliocentric Aries Ecliptic of date frame is defined as follows
   (from [12]):

      -  +Z axis is aligned with the north-pointing vector normal to the
         mean orbital plane of the Earth (Ecliptic North Pole);

      -  +X axis points toward the first point of Aries, i.e. along the
         "mean equinox", which is defined as the intersection of the
         Earth's mean orbital plane with the Earth's mean equatorial
         plane. It is aligned with the cross product of the north-pointing
         vectors normal to the Earth's mean equator and mean orbit plane
         of date;

      -  +Y axis is the cross product of the Z and X axes and completes
         the right-handed frame;

      -  the origin of this frame is the Earth's center of mass.


   The mathematical model used to obtain the orientation of the Earth's
   mean equator and equinox of date frame is the 1976 IAU precession model,
   built into SPICE.

   The mathematical model used to obtain the mean orbital plane of the
   Earth is the 1980 IAU obliquity model, also built into SPICE.

   The base frame for the 1976 IAU precession model is J2000.


   Required Data:
   --------------
   The usage of this frame does not require additional data since both
   the precession and the obliquity models used to define this frame are
   already built into SPICE.


   Remarks:
   --------
   This frame is (to first order) fixed with respect to the distant stars,
   and therefore inertial, nevertheless it is subject to slow change owing
   to the various slow motions of the Earth's rotation axis with respect
   to the fixed stars, and as such it is defined as 'ROTATING.' For details
   about implications of the rotation state definition, please refer to
   reference [1].

   \begindata

      FRAME_SOLO_GAE                 = -144995
      FRAME_-144995_NAME             = 'SOLO_GAE'
      FRAME_-144995_CLASS            =  5
      FRAME_-144995_CLASS_ID         = -144995
      FRAME_-144995_CENTER           =  399
      FRAME_-144995_RELATIVE         = 'J2000'
      FRAME_-144995_DEF_STYLE        = 'PARAMETERIZED'
      FRAME_-144995_FAMILY           = 'MEAN_ECLIPTIC_AND_EQUINOX_OF_DATE'
      FRAME_-144995_PREC_MODEL       = 'EARTH_IAU_1976'
      FRAME_-144995_OBLIQ_MODEL      = 'EARTH_IAU_1980'
      FRAME_-144995_FREEZE_EPOCH     = @2000-JAN-1/12:00:00.000

   \begintext


Geocentric Solar Ecliptic (SOLO_GSE) Frame
------------------------------------------------------------------------

   Definition:
   -----------
   The Geocentric Solar Ecliptic frame is defined as follows (from [5]):

      Definition of the Geocentric Solar Ecliptic frame:

      -  The position of the sun relative to the earth is the primary
         vector: the X axis points from the earth to the sun;

      -  The northern surface normal to the mean ecliptic of J2000 is the
         secondary vector: the Z axis is the component of this vector
         orthogonal to the X axis;

      -  The Y axis is Z cross X, completing the right-handed
         reference frame;

      -  the origin of this frame is the Earth's center of mass.

   All vectors are geometric: no aberration corrections are used.

   \begindata

      FRAME_SOLO_GSE               = -144996
      FRAME_-144996_NAME           = 'SOLO_GSE'
      FRAME_-144996_CLASS          =  5
      FRAME_-144996_CLASS_ID       = -144996
      FRAME_-144996_CENTER         =  399
      FRAME_-144996_RELATIVE       = 'J2000'
      FRAME_-144996_DEF_STYLE      = 'PARAMETERIZED'
      FRAME_-144996_FAMILY         = 'TWO-VECTOR'
      FRAME_-144996_PRI_AXIS       = 'X'
      FRAME_-144996_PRI_VECTOR_DEF = 'OBSERVER_TARGET_POSITION'
      FRAME_-144996_PRI_OBSERVER   = 'EARTH'
      FRAME_-144996_PRI_TARGET     = 'SUN'
      FRAME_-144996_PRI_ABCORR     = 'NONE'
      FRAME_-144996_SEC_AXIS       = 'Z'
      FRAME_-144996_SEC_VECTOR_DEF = 'CONSTANT'
      FRAME_-144996_SEC_FRAME      = 'EARTH_MECL_MEQX_J2000'
      FRAME_-144996_SEC_SPEC       = 'RECTANGULAR'
      FRAME_-144996_SEC_VECTOR     = ( 0, 0, 1 )

   \begintext


Heliocentric Earth Ecliptic (SOLO_HEE) Frame
------------------------------------------------------------------------

   Definition:
   -----------
   The Heliocentric Earth Ecliptic frame is defined as follows (from [5]):

      Definition of the Heliocentric Earth Ecliptic frame:

      -  The position of the earth relative to the sun is the primary
         vector: the X axis points from the sun to the earth;

      -  The northern surface normal to the mean ecliptic of J2000 is the
         secondary vector: the Z axis is the component of this vector
         orthogonal to the X axis;

      -  The Y axis is Z cross X, completing the right-handed reference
         frame;

      -  the origin of this frame is the Sun's center of mass.

   All vectors are geometric: no aberration corrections are used.

   \begindata

      FRAME_SOLO_HEE               = -144997
      FRAME_-144997_NAME           = 'SOLO_HEE'
      FRAME_-144997_CLASS          =  5
      FRAME_-144997_CLASS_ID       = -144997
      FRAME_-144997_CENTER         =  10
      FRAME_-144997_RELATIVE       = 'J2000'
      FRAME_-144997_DEF_STYLE      = 'PARAMETERIZED'
      FRAME_-144997_FAMILY         = 'TWO-VECTOR'
      FRAME_-144997_PRI_AXIS       = 'X'
      FRAME_-144997_PRI_VECTOR_DEF = 'OBSERVER_TARGET_POSITION'
      FRAME_-144997_PRI_OBSERVER   = 'SUN'
      FRAME_-144997_PRI_TARGET     = 'EARTH'
      FRAME_-144997_PRI_ABCORR     = 'NONE'
      FRAME_-144997_SEC_AXIS       = 'Z'
      FRAME_-144997_SEC_VECTOR_DEF = 'CONSTANT'
      FRAME_-144997_SEC_FRAME      = 'EARTH_MECL_MEQX_J2000'
      FRAME_-144997_SEC_SPEC       = 'RECTANGULAR'
      FRAME_-144997_SEC_VECTOR     = ( 0, 0, 1 )

   \begintext


Heliocentric orbital (SOLO_HOR) Frame
------------------------------------------------------------------------

   Definition:
   -----------
   The Heliocentric orbital frame is defined as follows (from [19]):

      Definition of the Heliocentric orbital frame:

      -  +Z axis is in the direction from the Sun centre of mass to the SC
         centre of mass;

      -  +Y axis is in the  direction  of  the  projection  of  the  solar
         north  pole  on  the  plane perpendicular to Z-axis;

      -  +X axis is the cross product of the Z and Y axes and completes
         the right-handed frame;

      -  the origin of this frame is the Sun's center of mass.

   All vectors are geometric: no aberration corrections are used.


   Required Data:
   --------------

   This frame is defined as a two-vector frame using two different
   types of specifications for the primary and secondary vectors.

   The primary vector is defined as an 'observer-target position'
   therefore, the ephemeris data required to compute the SOLO-Sun state
   vector in the J2000 reference frame must be loaded before using this
   frame.


   Remarks:
   --------

   This frame is defined based on SPK data: different planetary
   ephemerides for the Sun, the Solar System Barycenter and SOLO spacecraft
   will lead to different frame orientation at a given time.

   It is strongly recommended to indicate what data have been used
   in the evaluation of this frame when referring to it, e.g.
   SOLO_HOR using the DE405 ephemeris and the SOLO ephemeris version N.

   Because the SC elevation over the solar equator will not exceed 35 degrees
   by mission design [CREMA], this definition is valid within the entire
   mission [19].

   \begindata

      FRAME_SOLO_HOR               = -144985
      FRAME_-144985_NAME           = 'SOLO_HOR'
      FRAME_-144985_CLASS          =  5
      FRAME_-144985_CLASS_ID       = -144985
      FRAME_-144985_CENTER         =  10
      FRAME_-144985_RELATIVE       = 'J2000'
      FRAME_-144985_DEF_STYLE      = 'PARAMETERIZED'
      FRAME_-144985_FAMILY         = 'TWO-VECTOR'
      FRAME_-144985_PRI_AXIS       = 'Z'
      FRAME_-144985_PRI_VECTOR_DEF = 'OBSERVER_TARGET_POSITION'
      FRAME_-144985_PRI_OBSERVER   = 'SUN'
      FRAME_-144985_PRI_TARGET     = 'SOLO'
      FRAME_-144985_PRI_ABCORR     = 'NONE'
      FRAME_-144985_SEC_AXIS       = 'Y'
      FRAME_-144985_SEC_VECTOR_DEF = 'CONSTANT'
      FRAME_-144985_SEC_FRAME      = 'IAU_SUN'
      FRAME_-144985_SEC_SPEC       = 'RECTANGULAR'
      FRAME_-144985_SEC_VECTOR     = ( 0, 0, 1 )

   \begintext


Mean Ecliptic of Date Frame (SOLO_ECLIPDATE)
------------------------------------------------------------------------

   Definition:
   -----------
   The Mean Ecliptic of Date frame is defined as follows (from[17]):

      -  +X axis is the first point in Aries for the mean ecliptic of
         date;

      -  +Z axis points along the ecliptic north pole;

      -  +Y axis completes the right-handed system;

      -  the origin of this frame is the Earth's center of mass.

   All vectors are geometric: no aberration corrections are used.
   This reference frame can be used to realize the HAE coordinate system by
   using the sun as the observing body.

   \begindata

      FRAME_SOLO_ECLIPDATE         = -144980
      FRAME_-144980_NAME           = 'SOLO_ECLIPDATE'
      FRAME_-144980_CLASS          =  5
      FRAME_-144980_CLASS_ID       = -144980
      FRAME_-144980_CENTER         =  399
      FRAME_-144980_RELATIVE       = 'J2000'
      FRAME_-144980_DEF_STYLE      = 'PARAMETERIZED'
      FRAME_-144980_FAMILY         = 'MEAN_ECLIPTIC_AND_EQUINOX_OF_DATE'
      FRAME_-144980_PREC_MODEL     = 'EARTH_IAU_1976'
      FRAME_-144980_OBLIQ_MODEL    = 'EARTH_IAU_1980'
      FRAME_-144980_ROTATION_STATE = 'ROTATING'

   \begintext


Heliocentric Inertial frame (SOLO_HCI)
------------------------------------------------------------------------

   Definition:
   -----------
   The Heliocentric Inertial frame is defined as follows (from[17]):

      - +Z points in the solar north direction: the solar rotation axis
        (IAU_SUN frozen at J2000 epoch);

      - +X axis is the the ascending node on the ecliptic of J2000 of the
        IAU_SUN equator. This is accomplished by using the +Z axis of the
        ecliptic of J2000 as the secondary vector and HCI +Y as the secondary
        axis;

      - +Y axis completes the right-handed system;

      - the origin of this frame is the Sun's center of mass.

   All vectors are geometric: no aberration corrections are used.

   \begindata

      FRAME_SOLO_HCI               = -144981
      FRAME_-144981_NAME           = 'SOLO_HCI'
      FRAME_-144981_CLASS          =  5
      FRAME_-144981_CLASS_ID       = -144981
      FRAME_-144981_CENTER         =  10
      FRAME_-144981_RELATIVE       = 'J2000'
      FRAME_-144981_DEF_STYLE      = 'PARAMETERIZED'
      FRAME_-144981_FAMILY         = 'TWO-VECTOR'
      FRAME_-144981_FREEZE_EPOCH   = @2000-JAN-01/12:00:00
      FRAME_-144981_PRI_AXIS       = 'Z'
      FRAME_-144981_PRI_VECTOR_DEF = 'CONSTANT'
      FRAME_-144981_PRI_FRAME      = 'IAU_SUN'
      FRAME_-144981_PRI_SPEC       = 'RECTANGULAR'
      FRAME_-144981_PRI_VECTOR     = ( 0, 0, 1 )
      FRAME_-144981_SEC_AXIS       = 'Y'
      FRAME_-144981_SEC_VECTOR_DEF = 'CONSTANT'
      FRAME_-144981_SEC_FRAME      = 'ECLIPJ2000'
      FRAME_-144981_SEC_SPEC       = 'RECTANGULAR'
      FRAME_-144981_SEC_VECTOR     = ( 0, 0, 1 )

   \begintext


Heliocentric Earth Ecliptic frame (SOLO_HEE_NASA)
------------------------------------------------------------------------

   Definition:
   -----------
   The Heliocentric Earth Ecliptic frame is defined as follows (from[17]):

      - +X axis points from the Earth to the Sun;

      - +Z axis is the component orthogonal to the +X axis of the northern
        surface normal to the mean ecliptic of date;

      - +Y axis completes the right-handed system;

      - the origin of this frame is the Sun's center of mass.

   All vectors are geometric: no aberration corrections are used.

   \begindata

      FRAME_SOLO_HEE_NASA          = -144982
      FRAME_-144982_NAME           = 'SOLO_HEE_NASA'
      FRAME_-144982_CLASS          =  5
      FRAME_-144982_CLASS_ID       = -144982
      FRAME_-144982_CENTER         =  10
      FRAME_-144982_RELATIVE       = 'J2000'
      FRAME_-144982_DEF_STYLE      = 'PARAMETERIZED'
      FRAME_-144982_FAMILY         = 'TWO-VECTOR'
      FRAME_-144982_PRI_AXIS       = 'X'
      FRAME_-144982_PRI_VECTOR_DEF = 'OBSERVER_TARGET_POSITION'
      FRAME_-144982_PRI_OBSERVER   = 'SUN'
      FRAME_-144982_PRI_TARGET     = 'EARTH'
      FRAME_-144982_PRI_ABCORR     = 'NONE'
      FRAME_-144982_SEC_AXIS       = 'Z'
      FRAME_-144982_SEC_VECTOR_DEF = 'CONSTANT'
      FRAME_-144982_SEC_FRAME      = 'SOLO_ECLIPDATE'
      FRAME_-144982_SEC_SPEC       = 'RECTANGULAR'
      FRAME_-144982_SEC_VECTOR     = ( 0, 0, 1 )

   \begintext


Heliocentric Earth Equatorial frame (SOLO_HEEQ)
------------------------------------------------------------------------

   Definition:
   -----------
   The Heliocentric Earth Equatorial frame is defined as follows (from[17]):

      - +Z points in the solar north direction: the solar rotation axis;

      - +X is the orthogonal to the +Z axis component of the position of the
        Earth relative to the Sun;

      - +Y axis completes the right-handed system;

      - the origin of this frame is the Sun's center of mass.

   All vectors are geometric: no aberration corrections are used.

   \begindata

      FRAME_SOLO_HEEQ              = -144983
      FRAME_-144983_NAME           = 'SOLO_HEEQ'
      FRAME_-144983_CLASS          =  5
      FRAME_-144983_CLASS_ID       = -144983
      FRAME_-144983_CENTER         =  10
      FRAME_-144983_RELATIVE       = 'J2000'
      FRAME_-144983_DEF_STYLE      = 'PARAMETERIZED'
      FRAME_-144983_FAMILY         = 'TWO-VECTOR'
      FRAME_-144983_PRI_AXIS       = 'Z'
      FRAME_-144983_PRI_VECTOR_DEF = 'CONSTANT'
      FRAME_-144983_PRI_FRAME      = 'IAU_SUN'
      FRAME_-144983_PRI_SPEC       = 'RECTANGULAR'
      FRAME_-144983_PRI_VECTOR     = ( 0, 0, 1 )
      FRAME_-144983_SEC_AXIS       = 'X'
      FRAME_-144983_SEC_VECTOR_DEF = 'OBSERVER_TARGET_POSITION'
      FRAME_-144983_SEC_OBSERVER   = 'SUN'
      FRAME_-144983_SEC_TARGET     = 'EARTH'
      FRAME_-144983_SEC_ABCORR     = 'NONE'
      FRAME_-144983_SEC_FRAME      = 'IAU_SUN'

   \begintext


Geocentric Radial Tangential Normal Frame (SOLO_GEORTN)
------------------------------------------------------------------------

   Definition:
   -----------
   The Geocentric RTN frame is defined as follows (from[17]):

      - +Z the component of the solar north direction perpendicular to the
        +X axis (+Z is the secondary axis and is the solar rotation axis);

      - +X points from the Sun center to Earth (primary axis);

      - +Y axis completes the right-handed system;

      -  the origin of this frame is the Sun's center of mass.

   All vectors are geometric: no aberration corrections are used.

   \begindata

      FRAME_SOLO_GEORTN            = -144984
      FRAME_-144984_NAME           = 'SOLO_GEORTN'
      FRAME_-144984_CLASS          =  5
      FRAME_-144984_CLASS_ID       = -144984
      FRAME_-144984_CENTER         =  10
      FRAME_-144984_RELATIVE       = 'J2000'
      FRAME_-144984_DEF_STYLE      = 'PARAMETERIZED'
      FRAME_-144984_FAMILY         = 'TWO-VECTOR'
      FRAME_-144984_PRI_AXIS       = 'X'
      FRAME_-144984_PRI_VECTOR_DEF = 'OBSERVER_TARGET_POSITION'
      FRAME_-144984_PRI_OBSERVER   = 'SUN'
      FRAME_-144984_PRI_TARGET     = 'EARTH'
      FRAME_-144984_PRI_ABCORR     = 'NONE'
      FRAME_-144984_PRI_FRAME      = 'IAU_SUN'
      FRAME_-144984_SEC_AXIS       = 'Z'
      FRAME_-144984_SEC_VECTOR_DEF = 'CONSTANT'
      FRAME_-144984_SEC_FRAME      = 'IAU_SUN'
      FRAME_-144984_SEC_SPEC       = 'RECTANGULAR'
      FRAME_-144984_SEC_VECTOR     = ( 0, 0, 1 )

   \begintext


Sun Generic Frame Definitions
------------------------------------------------------------------------------

   This section contains the definition of the Sun generic frames.


Heliocentric Aries Ecliptic of Date frame (SUN_ARIES_ECL)
------------------------------------------------------------------------

   SPICE frame name, common names and other designators:
   -----------------------------------------------------
   Within the SPICE system, the Heliocentric Aries Ecliptic of Date
   frame is referred as SUN_ARIES_ECL. In literature, this frame is
   referred as HAE (from [13]), or Solar Ecliptic Coordinate System,
   SE (from [14]).


   Definition:
   -----------
   The Heliocentric Aries Ecliptic frame is defined as follows
   (from [4]):

      -  +Z axis is aligned with the north-pointing vector normal to the
         mean orbital plane of the Earth;

      -  +X axis points toward the first point of Aries, i.e. along the
         "mean equinox", which is defined as the intersection of the
         Earth's mean orbital plane with the Earth's mean equatorial
         plane. It is aligned with the cross product of the north-pointing
         vectors normal to the Earth's mean equator and mean orbit plane
         of date;

      -  +Y axis is the cross product of the Z and X axes and completes
         the right-handed frame;

      -  the origin of this frame is the Sun's center of mass.

   In [8] this frame is defined equivalent to SOLO_GAE where the center of the
   frame is the Earth instead of the Sun as the definition suggests. Whilst
   waiting for final confirmation from NAIF, the frame has been re-defined to
   be equivalent to SOLO_GAE but with its origin on the Sun. SUN_ARIES_ECL
   cannot be defined directly as SOLO_GAE with a different CENTER for the
   following SPICE Error is generated:

      SPICE(INVALIDSELECTION)

         Definition of frame SUN_ARIES_ECL specifies frame center SUN and
         precession model EARTH_IAU_1976. This precession model is not
         applicable to body SUN. This situation is usually caused by an error
         in a frame kernel in which the frame is defined.

      pxform_c --> PXFORM --> REFCHG --> ROTGET --> ZZDYNROT


   Please note that is also true that the origin of frames in SPICE is not
   relevant for any computation since they are independent of the vector's
   origins.s

   Therefore the frames is defined as a TK frame as follows:

   \begindata

      FRAME_SUN_ARIES_ECL         =  1000010000
      FRAME_1000010000_NAME       = 'SUN_ARIES_ECL'
      FRAME_1000010000_CLASS      =  4
      FRAME_1000010000_CLASS_ID   =  1000010000
      FRAME_1000010000_CENTER     =  10

      TKFRAME_1000010000_RELATIVE = 'SOLO_GAE'
      TKFRAME_1000010000_SPEC     = 'ANGLES'
      TKFRAME_1000010000_UNITS    = 'DEGREES'
      TKFRAME_1000010000_AXES     = ( 3, 2, 1 )
      TKFRAME_1000010000_ANGLES   = ( 0, 0, 0 )

  \begintext


Heliocentric Earth Ecliptic frame (SUN_EARTH_ECL)
------------------------------------------------------------------------

   SPICE frame name, common names and other designators:
   -----------------------------------------------------
   Within the SPICE system, the Heliocentric Earth Ecliptic frame is
   referred as SUN_EARTH_ECL. In literature, this frame is referred as
   HEE (from [15]).


   Definition:
   -----------
   The Heliocentric Earth Ecliptic frame is defined as follows
   (from [15]):

      -  X-Y plane is defined by the Earth Mean Ecliptic plane of date,
         therefore, the +Z axis is the primary vector, and it defined as
         the normal vector to the Ecliptic plane that points toward the
         north pole of date;

      -  +X axis is the component of the Sun-Earth vector that is
         orthogonal to the +Z axis;

      -  +Y axis completes the right-handed system;

      -  the origin of this frame is the Sun's center of mass.

   All vectors are geometric: no aberration corrections are used.


   Required Data:
   --------------
   This frame is defined as a two-vector frame using two different types
   of specifications for the primary and secondary vectors.

   The primary vector is defined as a constant vector in the Earth mean
   ecliptic and equinox of date (EARTH_MECL_MEQX) frame and therefore
   the definition of this frame have to be loaded before using this
   frame.

   The secondary vector is defined as an 'observer-target position'
   vector, therefore, the ephemeris data required to compute the
   Sun-Earth vector in J2000 frame have to be loaded prior to using
   this frame.


   Remarks:
   --------
   SPICE imposes a constraint in the definition of dynamic frames
   (see [1]):

      When the definition of a parameterized dynamic frame F1 refers to
      a second frame F2 the referenced frame F2 may be dynamic, but F2
      must not make reference to any dynamic frame.

      If F2 is not dynamic but its evaluation requires evaluation of a
      dynamic frame F3, the same restrictions apply to F3.

   Therefore, no other dynamic frame should make reference to this
   frame.

   Since the secondary vector of this frame is defined as an
   'observer-target position' vector, the usage of different planetary
   ephemerides leads to different implementations of this frame,
   but only when these data lead to different projections of the
   Sun-Earth vector on the Earth Ecliptic plane of date.

   It is strongly recommended to indicate what data have been used in the
   evaluation of this frame when referring to it, e.g. SUN_EARTH_ECL using
   de405 ephemerides.

   As an example, note that the average difference in position of the
   +X axis of this frame, when using de405 vs. de403 ephemerides, is
   about 14.3 micro-radians, with a maximum of 15.0 micro-radians.


   \begindata

      FRAME_SUN_EARTH_ECL              =  1000010002
      FRAME_1000010002_NAME            = 'SUN_EARTH_ECL'
      FRAME_1000010002_CLASS           =  5
      FRAME_1000010002_CLASS_ID        =  1000010002
      FRAME_1000010002_CENTER          =  10
      FRAME_1000010002_RELATIVE        = 'J2000'
      FRAME_1000010002_DEF_STYLE       = 'PARAMETERIZED'
      FRAME_1000010002_FAMILY          = 'TWO-VECTOR'
      FRAME_1000010002_PRI_AXIS        = 'Z'
      FRAME_1000010002_PRI_VECTOR_DEF  = 'CONSTANT'
      FRAME_1000010002_PRI_FRAME       = 'EARTH_MECL_MEQX'
      FRAME_1000010002_PRI_SPEC        = 'RECTANGULAR'
      FRAME_1000010002_PRI_VECTOR      = ( 0, 0, 1 )
      FRAME_1000010002_SEC_AXIS        = 'X'
      FRAME_1000010002_SEC_VECTOR_DEF  = 'OBSERVER_TARGET_POSITION'
      FRAME_1000010002_SEC_OBSERVER    = 'SUN'
      FRAME_1000010002_SEC_TARGET      = 'EARTH'
      FRAME_1000010002_SEC_ABCORR      = 'NONE'

   \begintext


Heliocentric Inertial frame (SUN_INERTIAL)
------------------------------------------------------------------------

   SPICE frame name, common names and other designators:
   -----------------------------------------------------
   Within the SPICE system, the Heliocentric Inertial frame is
   referred as SUN_INERTIAL. In literature, this frame is referred as
   HCI (from [15]), or Heliographic Inertial, HGI (from [14]).


   Definition:
   -----------
   The Heliocentric Inertial Frame is defined as follows (from [4]):

    -  X-Y plane is defined by the Sun's equator of epoch J2000: the +Z
       axis, primary vector, is parallel to the Sun's rotation axis of
       epoch J2000, pointing toward the Sun's north pole;

    -  +X axis is defined by the ascending node of the Sun's equatorial
       plane on the ecliptic plane of J2000;

    -  +Y completes the right-handed frame;

    -  the origin of this frame is the Sun's center of mass.


   Remarks:
   --------
   Note that even when the original frame defined in [15] is referenced
   to the orientation of the Solar equator in J1900, the SUN_INERTIAL
   frame is based on J2000 instead.

   \begindata

      FRAME_SUN_INERTIAL              =  1000010004
      FRAME_1000010004_NAME           = 'SUN_INERTIAL'
      FRAME_1000010004_CLASS          =  5
      FRAME_1000010004_CLASS_ID       =  1000010004
      FRAME_1000010004_CENTER         =  10
      FRAME_1000010004_RELATIVE       = 'J2000'
      FRAME_1000010004_DEF_STYLE      = 'PARAMETERIZED'
      FRAME_1000010004_FAMILY         = 'TWO-VECTOR'
      FRAME_1000010004_FREEZE_EPOCH   = @2000-JAN-01/12:00:00
      FRAME_1000010004_PRI_AXIS       = 'Z'
      FRAME_1000010004_PRI_VECTOR_DEF = 'CONSTANT'
      FRAME_1000010004_PRI_FRAME      = 'IAU_SUN'
      FRAME_1000010004_PRI_SPEC       = 'RECTANGULAR'
      FRAME_1000010004_PRI_VECTOR     = ( 0, 0, 1 )
      FRAME_1000010004_SEC_AXIS       = 'Y'
      FRAME_1000010004_SEC_VECTOR_DEF = 'CONSTANT'
      FRAME_1000010004_SEC_FRAME      = 'ECLIPJ2000'
      FRAME_1000010004_SEC_SPEC       = 'RECTANGULAR'
      FRAME_1000010004_SEC_VECTOR     = ( 0, 0, 1 )

  \begintext


Heliocentric earth equatorial frame (SUN_EARTH_CEQU)
------------------------------------------------------------------------

   SPICE frame name, common names and other designators:
   -----------------------------------------------------
   Within the SPICE systems, the Heliocentric Earth Equatorial frame is
   referred as SUN_EARTH_CEQU. In literature, this frame is referred as
   HEEQ (from [15] and [13]), or Stonyhurst Heliographic Coordinates
   (from [6], chapter 2.1)


   Definition:
   -----------
   The Heliocentric Earth Equatorial frame is defined as follows (from
   [15] and [13]):

      -  X-Y plane is the solar equator of date, therefore, the +Z axis
         is the primary vector and it is aligned to the Sun's north pole
         of date;

      -  +X axis is defined by the intersection between the Sun
         equatorial plane and the solar central meridian of date as seen
         from the Earth. The solar central meridian of date is defined
         as the meridian of the Sun that is turned toward the Earth.
         Therefore, +X axis is the component of the Sun-Earth vector
         that is orthogonal to the +Z axis;

      -  +Y axis completes the right-handed system;

      -  the origin of this frame is the Sun's center of mass.

   All vectors are geometric: no aberration corrections are used.


   Required Data:
   --------------
   This frame is defined as a two-vector frame using two different types
   of specifications for the primary and secondary vectors.

   The primary vector is defined as a constant vector in the IAU_SUN
   frame, which is a PCK-based frame. Therefore a PCK file containing
   the orientation constants for the Sun has to be loaded before any
   evaluation of this frame.

   The secondary vector is defined as an 'observer-target position'
   vector. Therefore, the ephemeris data required to compute the
   Sun-Earth vector in J2000 frame have to be loaded before using this
   frame.


   Remarks:
   --------
   This frame is defined based on the IAU_SUN frame, whose evaluation is
   based on the data included in the loaded PCK file: different
   orientation constants for the Sun's spin axis will lead to a different
   frame orientation at a given time.

   Since the secondary vector of this frame is defined as an
   'observer-target position' vector, the usage of different planetary
   ephemerides conduces to different implementations of this frame,
   but only when these data lead to different solar central meridians,
   i.e. the projection of the Sun-Earth vector on the Sun equatorial
   plane obtained from the different ephemerides has a non-zero angular
   separation.

   It is strongly recommended to indicate what data have been used in the
   evaluation of this frame when referring to it, e.g. SUN_EARTH_CEQU using
   IAU 2009 constants and de405 ephemerides.

   Note that the effect of using different SPK files is smaller, in
   general, that using different Sun's spin axis constants. As an
   example, the average difference in the position of the +X axis of
   the frame, when using DE405 or DE403 ephemerides is about 14.3
   micro-radians, with a maximum of 15.3 micro-radians.


   \begindata

      FRAME_SUN_EARTH_CEQU             =  1000010001
      FRAME_1000010001_NAME            = 'SUN_EARTH_CEQU'
      FRAME_1000010001_CLASS           =  5
      FRAME_1000010001_CLASS_ID        =  1000010001
      FRAME_1000010001_CENTER          =  10
      FRAME_1000010001_RELATIVE        = 'J2000'
      FRAME_1000010001_DEF_STYLE       = 'PARAMETERIZED'
      FRAME_1000010001_FAMILY          = 'TWO-VECTOR'
      FRAME_1000010001_PRI_AXIS        = 'Z'
      FRAME_1000010001_PRI_VECTOR_DEF  = 'CONSTANT'
      FRAME_1000010001_PRI_FRAME       = 'IAU_SUN'
      FRAME_1000010001_PRI_SPEC        = 'RECTANGULAR'
      FRAME_1000010001_PRI_VECTOR      = ( 0, 0, 1 )
      FRAME_1000010001_SEC_AXIS        = 'X'
      FRAME_1000010001_SEC_VECTOR_DEF  = 'OBSERVER_TARGET_POSITION'
      FRAME_1000010001_SEC_OBSERVER    = 'SUN'
      FRAME_1000010001_SEC_TARGET      = 'EARTH'
      FRAME_1000010001_SEC_ABCORR      = 'NONE'

   \begintext


Earth Generic Frame Definitions
------------------------------------------------------------------------------

   This section contains the definition of the Earth generic frames.


Geocentric solar ecliptic of date frame (EARTH_SUN_ECL)
------------------------------------------------------------------------

   SPICE frame name, common names and other designators:
   -----------------------------------------------------
   Within the SPICE system, the geocentric solar ecliptic of date frame
   is referred as EARTH_SUN_ECL. In literature, this frame is referred
   as GSE (from [15]).


   Definition:
   -----------
   The Geocentric Solar Ecliptic frame of date is defined as follows
   (from [15]):

      -  X-Y plane is defined by the Earth Mean Ecliptic plane of date:
         the +Z axis, primary vector, is the normal vector to this plane,
         always pointing toward the North side of the invariable plane;

      -  +X axis is the component of the Earth-Sun vector that is
         orthogonal to the +Z axis;

      -  +Y axis completes the right-handed system;

      -  the origin of this frame is the Earth's center of mass.

   All the vectors are geometric: no aberration corrections are used.


   Required Data:
   --------------
   This frame is defined as a two-vector frame using two different types
   of specifications for the primary and secondary vectors.

   The primary vector is defined as a constant vector in the Earth mean
   ecliptic and mean equinox of date frame, and therefore the definition
   of EARTH_MECL_MEQX has to be loaded before using this frame.

   The secondary vector is defined as an 'observer-target position'
   vector, therefore, the ephemeris data required to compute the
   Earth-Sun vector in J2000 frame have to be loaded prior to using this
   frame.


   Remarks:
   --------
   SPICE imposes a constraint in the definition of dynamic frames
   (see [1]):

      When the definition of a parameterized dynamic frame F1 refers to
      a second frame F2 the referenced frame F2 may be dynamic, but F2
      must not make reference to any dynamic frame.

   Therefore, no other dynamic frame should make reference to this
   frame.

   Since the secondary vector of this frame is defined as an
   'observer-target position' vector, the usage of different planetary
   ephemerides may lead to a different frame orientation at a given time,
   but only when these data lead to different projections of the
   Earth-Sun vector on the Earth mean ecliptic plane of date.

   It is strongly recommended to indicate what data have been used
   in the evaluation of this frame when referring to it, e.g.
   EARTH_SUN_ECL using de405 ephemerides.

   As an example, note that the average difference in position of the +X
   axis of this frame, when using de405 vs. de403 ephemerides, is about
   14.3 micro-radians, with a maximum of 15.0 micro-radians.


   \begindata

      FRAME_EARTH_SUN_ECL             =  300399005
      FRAME_300399005_NAME            = 'EARTH_SUN_ECL'
      FRAME_300399005_CLASS           =  5
      FRAME_300399005_CLASS_ID        =  300399005
      FRAME_300399005_CENTER          =  399
      FRAME_300399005_RELATIVE        = 'J2000'
      FRAME_300399005_DEF_STYLE       = 'PARAMETERIZED'
      FRAME_300399005_FAMILY          = 'TWO-VECTOR'
      FRAME_300399005_PRI_AXIS        = 'Z'
      FRAME_300399005_PRI_VECTOR_DEF  = 'CONSTANT'
      FRAME_300399005_PRI_FRAME       = 'EARTH_MECL_MEQX'
      FRAME_300399005_PRI_SPEC        = 'RECTANGULAR'
      FRAME_300399005_PRI_VECTOR      = ( 0, 0, 1 )
      FRAME_300399005_SEC_AXIS        = 'X'
      FRAME_300399005_SEC_VECTOR_DEF  = 'OBSERVER_TARGET_POSITION'
      FRAME_300399005_SEC_OBSERVER    = 'EARTH'
      FRAME_300399005_SEC_TARGET      = 'SUN'
      FRAME_300399005_SEC_ABCORR      = 'NONE'

   \begintext


Earth Mean Ecliptic and Equinox of date frame (EARTH_MECL_MEQX)
------------------------------------------------------------------------

   SPICE frame name, common names and other designators:
   -----------------------------------------------------
   Within the SPICE system, the Earth mean ecliptic and equinox of
   date frame is referred as EARTH_MECL_MEQX. In literature, this frame
   is referred as mean ecliptic of date (from [16])


   Definition:
   -----------
   The Earth mean ecliptic and equinox of date frame is defined as
   follows (from [16]):

      -  +Z axis is aligned with the north-pointing vector normal to the
         mean orbital plane of the Earth;

      -  +X axis points along the ``mean equinox'', which is defined as
         the intersection of the Earth's mean orbital plane with the
         Earth's mean equatorial plane. It is aligned with the cross
         product of the north-pointing vectors normal to the Earth's
         mean equator and mean orbit plane of date;

      -  +Y axis is the cross product of the Z and X axes and completes
         the right-handed frame;

      -  the origin of this frame is the Earth's center of mass.

   The mathematical model used to obtain the orientation of the Earth's
   mean equator and equinox of date frame is the 1976 IAU precession model,
   built into SPICE.

   The mathematical model used to obtain the mean orbital plane of the
   Earth is the 1980 IAU obliquity model, also built into SPICE.

   The base frame for the 1976 IAU precession model is J2000.


   Required Data:
   --------------
   The usage of this frame does not require additional data since both
   the precession and the obliquity models used to define this frame are
   already built into SPICE.


   Remarks:
   --------
   None.


   \begindata

      FRAME_EARTH_MECL_MEQX            =  300399000
      FRAME_300399000_NAME             = 'EARTH_MECL_MEQX'
      FRAME_300399000_CLASS            =  5
      FRAME_300399000_CLASS_ID         =  300399000
      FRAME_300399000_CENTER           =  399
      FRAME_300399000_RELATIVE         = 'J2000'
      FRAME_300399000_DEF_STYLE        = 'PARAMETERIZED'
      FRAME_300399000_FAMILY           = 'MEAN_ECLIPTIC_AND_EQUINOX_OF_DATE'
      FRAME_300399000_PREC_MODEL       = 'EARTH_IAU_1976'
      FRAME_300399000_OBLIQ_MODEL      = 'EARTH_IAU_1980'
      FRAME_300399000_ROTATION_STATE   = 'ROTATING'

   \begintext


Earth Mean Ecliptic and Equinox frame frozen at J2000 (EARTH_MECL_MEQX_J2000)
-----------------------------------------------------------------------------

   SPICE frame name, common names and other designators:
   -----------------------------------------------------
   Within the SPICE system, the Earth mean ecliptic and equinox frame
   frozen at J2000 is referred as EARTH_MECL_MEQX. In literature,
   this frame is referred as mean ecliptic of date (from [16])


   Definition:
   -----------
   The Earth mean ecliptic and equinox of date frame is defined as
   follows (from [16]):

      -  +Z axis is aligned with the north-pointing vector normal to the
         mean orbital plane of the Earth at 2000 JAN 1 12:00:00.000;

      -  +X axis points along the ``mean equinox'', which is defined as
         the intersection of the Earth's mean orbital plane with the
         Earth's mean equatorial plane at at 2000 JAN 1 12:00:00.000.
         It is aligned with the cross product of the north-pointing vectors
         normal to the Earth's mean equator and mean orbit plane at that date;

      -  +Y axis is the cross product of the Z and X axes and completes
         the right-handed frame;

      -  the origin of this frame is the Earth's center of mass.

   The mathematical model used to obtain the orientation of the Earth's
   mean equator and equinox of date frame is the 1976 IAU precession model,
   built into SPICE.

   The mathematical model used to obtain the mean orbital plane of the
   Earth is the 1980 IAU obliquity model, also built into SPICE.

   The base frame for the 1976 IAU precession model is J2000.


   Required Data:
   --------------
   The usage of this frame does not require additional data since both
   the precession and the obliquity models used to define this frame are
   already built into SPICE.


   Remarks:
   --------
   This frame has a Solar Orbiter specific ID because it is not envisaged
   to be included as an Earth Generic Dynamic Frame.


   \begindata

      FRAME_EARTH_MECL_MEQX_J2000      = -144998
      FRAME_-144998_NAME               = 'EARTH_MECL_MEQX_J2000'
      FRAME_-144998_CLASS              =  5
      FRAME_-144998_CLASS_ID           = -144998
      FRAME_-144998_CENTER             =  399
      FRAME_-144998_RELATIVE           = 'J2000'
      FRAME_-144998_DEF_STYLE          = 'PARAMETERIZED'
      FRAME_-144998_FAMILY             = 'MEAN_ECLIPTIC_AND_EQUINOX_OF_DATE'
      FRAME_-144998_PREC_MODEL         = 'EARTH_IAU_1976'
      FRAME_-144998_OBLIQ_MODEL        = 'EARTH_IAU_1980'
      FRAME_-144998_FREEZE_EPOCH       = @2000-JAN-1/12:00:00.000

   \begintext


Venus based Frames
-------------------------------------------------------------------------------

   This section contains the definition of the Venus generic frames.


Venus-centric Solar Orbital frame (VSO)
----------------------------------------

   Definition:
   -----------

   The Venus-centric solar orbital frame is defined as follows:

      -  +X axis is the position of the Sun relative to Venus;
         it's the primary vector and points from Venus to Sun;

      -  +Y axis is the component of the inertially referenced
         velocity of Sun relative to Venus orthogonal to the +X axis;

      -  +Z axis completes the right-handed system;

      -  the origin of this frame is the center of mass of Venus.

   All vectors are geometric: no corrections are used.


   Required Data:
   --------------

   This frame is defined as a two-vector frame using two different
   types of specifications for the primary and secondary vectors.

   The primary vector is defined as an 'observer-target position'
   vector. Therefore, the ephemeris data required to compute the
   Venus-Sun vector in J2000 reference frame have
   to be loaded before using this frame.

   The secondary vector is defined as an 'observer-target velocity'
   vector. Therefore, the ephemeris data required to compute the
   Venus-Sun velocity vector in the J2000 reference frame
   have to be loaded before using this frame.


   Remarks:
   --------

   This frame is defined based on SPK data: different planetary
   ephemerides for Venus, Sun and the Sun Barycenter
   will lead to a different frame orientation at a given time.

   It is strongly recommended to indicate what data have been used
   in the evaluation of this frame when referring to it, e.g.
   SOLO_VSO using de405 ephemerides.


   \begindata

      FRAME_SOLO_VSO                = -144999
      FRAME_-144999_NAME            = 'SOLO_VSO'
      FRAME_-144999_CLASS           =  5
      FRAME_-144999_CLASS_ID        = -144999
      FRAME_-144999_CENTER          =  299
      FRAME_-144999_RELATIVE        = 'J2000'
      FRAME_-144999_DEF_STYLE       = 'PARAMETERIZED'
      FRAME_-144999_FAMILY          = 'TWO-VECTOR'
      FRAME_-144999_PRI_AXIS        = 'X'
      FRAME_-144999_PRI_VECTOR_DEF  = 'OBSERVER_TARGET_POSITION'
      FRAME_-144999_PRI_OBSERVER    = 'VENUS'
      FRAME_-144999_PRI_TARGET      = 'SUN'
      FRAME_-144999_PRI_ABCORR      = 'NONE'
      FRAME_-144999_SEC_AXIS        = 'Y'
      FRAME_-144999_SEC_VECTOR_DEF  = 'OBSERVER_TARGET_VELOCITY'
      FRAME_-144999_SEC_OBSERVER    = 'VENUS'
      FRAME_-144999_SEC_TARGET      = 'SUN'
      FRAME_-144999_SEC_ABCORR      = 'NONE'
      FRAME_-144999_SEC_FRAME       = 'J2000'

   \begintext


Geocentric Solar Magnetospheric (SOLO_GSM)
------------------------------------------------------------------------

   Definition:
   -----------

   The Solar-Orbiter Geocentric Solar Magnetospheric frame is defined
   as follows:

      -  +X axis is the position of the Sun relative to the Earth;
         it's the primary vector and points from Earth to Sun;

      -  +Z axis is the projection of the Earth's magnetic dipole axis
         (positive North) onto the plane perpendicular to the X axis;

      -  +Y axis completes the right-handed system;

      -  the origin of this frame is the center of mass of the Earth.

   All vectors are geometric: no corrections are used.

   Remarks:
   --------

   The location of Earth's north geomagnetic pole has been calculated from
   the latest IGRF 13 model (from [18]). This data has been used to generate
   and SPK file defining the position of a point "EARTH_NORTH_POLE" along
   the Earth's magnetic dipole axis from 1950-01-01 to 2025-12-31. Note that
   this SPK file needs to be loaded in order to compute the SOLO_GSM frame
   orientation during this period.

   Please note that when we refer to the ``north geomagnetic pole'' we refer
   to the location of the magnetic dipole axis axis penetrating through the
   surface of Earth on the northern hemisphere (from [7]).

   This frame is defined based on SPK data: different planetary ephemerides
   for Earth, Sun and the Sun Barycenter will lead to a different frame
   orientation at a given time.

   It is strongly recommended to indicate what data have been used in the
   evaluation of this frame when referring to it, e.g. SOLO_GSM using de421
   ephemerides.

   The definition of the Solar-Orbiter Geocentric Solar Magnetospheric
   frame is as follows:

   \begindata

      FRAME_SOLO_GSM                = -144962
      FRAME_-144962_NAME            = 'SOLO_GSM'
      FRAME_-144962_CLASS           =  5
      FRAME_-144962_CLASS_ID        = -144962
      FRAME_-144962_CENTER          =  399
      FRAME_-144962_RELATIVE        = 'J2000'
      FRAME_-144962_DEF_STYLE       = 'PARAMETERIZED'
      FRAME_-144962_FAMILY          = 'TWO-VECTOR'
      FRAME_-144962_PRI_AXIS        = 'X'
      FRAME_-144962_PRI_VECTOR_DEF  = 'OBSERVER_TARGET_POSITION'
      FRAME_-144962_PRI_OBSERVER    = 'EARTH'
      FRAME_-144962_PRI_TARGET      = 'SUN'
      FRAME_-144962_PRI_ABCORR      = 'NONE'
      FRAME_-144962_SEC_AXIS        = 'Z'
      FRAME_-144962_SEC_VECTOR_DEF  = 'OBSERVER_TARGET_POSITION'
      FRAME_-144962_SEC_OBSERVER    = 'EARTH'
      FRAME_-144962_SEC_TARGET      = 'EARTH_NORTH_POLE'
      FRAME_-144962_SEC_ABCORR      = 'NONE'

   \begintext


SOLO SCI NAIF ID Codes to Name Mapping
------------------------------------------------------------------------------

   This section contains name to NAIF ID mappings for the Solar-Orbiter.
   Once the contents of this file are loaded into the KERNEL POOL,
   these mappings become available within SPICE, making it possible to use
   names instead of ID code in the high level SPICE routine calls.

      ----------------------  --------
       Name                    ID
      ----------------------  --------
       EARTH_NORTH_POLE       399901
      ----------------------  --------

    Name-ID Mapping keywords:

   \begindata

      NAIF_BODY_NAME += ( 'EARTH_NORTH_POLE'           )
      NAIF_BODY_CODE += ( 399901                       )

   \begintext

End of FK file.