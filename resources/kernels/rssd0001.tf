KPL/FK

Generic Frame Definition Kernel File for ESA Planetary Missions
===========================================================================

   This frame kernel defines a number of mission independent frames that
   could be used by any of the users of any of the ESA planetary missions,
   and that are not ``built'' in the SPICE toolkit.


Version and Date
========================================================================

   Version 1.0 -- May 03, 2006 -- Jorge Diaz del Rio, RSSD/ESA

      Complete new kernel.

   Version 0.0 -- July 19, 2005 -- Jorge Diaz del Rio, RSSD/ESA

      Initial version.


References
========================================================================

   1. ``Frames Required Reading''

   2. ``Kernel Pool Required Reading''

   3. Franz and Harper. (2002) ``Heliospheric Coordinate Systems''
      Space Science, 50, 217ff.

   4. Hapgood,M. (1992). ``Space physics coordinate transformations: A user
      guide'' Planetary and Space Science, 40, 711-717

   5. ``STK: Technical Notes - Coordinate System Computations''

   6. Seidelmann, P.K., Abalakin, V.K., Bursa, M., Davies, M.E., Bergh, C
      de, Lieske, J.H., Oberst, J., Simon, J.L., Standish, E.M., Stooke,
      and Thomas, P.C. (2002). ``Report of the IAU/IAG Working Group on
      Cartographic Coordinates and Rotational Elements of the Planets and
      Satellites: 2000'' Celestial Mechanics and Dynamical Astronomy, v.8
      Issue 1, pp. 83-111.

   7. Russell, C.T. (1971). ``Geophysical Coordinate Transformations''
      Cosmic Electrodynamics, v.2, 184-196


Contact Information
========================================================================

   Jorge Diaz del Rio, RSSD/ESA, (31) 71-565-5175, jdiaz@rssd.esa.int


Implementation Notes
========================================================================

   This file is used by the SPICE system as follows: programs that make
   use of this frame kernel must 'load' the kernel, normally during
   program initialization. The SPICELIB routine FURNSH, the CSPICE
   function furnsh_c and the ICY function cspice_furnsh load a kernel
   file into the kernel pool as shown below.

      CALL FURNSH   ( 'frame_kernel_name' )
      furnsh_c      ( "frame_kernel_name" );
      cspice_furnsh ( 'frame_kernel_name' )

   This file was created and may be updated with a text editor or word
   processor.


ESA/RSSD Generic Frame Names and NAIF ID Codes
========================================================================
 
   The following names and NAIF ID codes are assigned to the generic
   frames defined in this kernel file:

      Frame Name     NAIF ID    Center   Description
      ------------   -------    -------  -------------------------------

   Generic Dynamic Frames names/IDs:

      HEE            1500010    SUN      Heliocentric Earth Ecliptic
      HEEQ           1501010    SUN      Heliocentric Earth Equatorial
      ------------------------------------------------------------------
      VSO            1500299    VENUS    Venus-centric Solar Orbital
      VME            1501299    VENUS    Venus Mean Equator of date
      ------------------------------------------------------------------
      LSE            1500301    MOON     Moon-centric Solar Ecliptic
      LME            1501301    MOON     Moon Mean Equator of date
      ------------------------------------------------------------------
      GSE            1500399    EARTH    Geocentric Solar Ecliptic
      EME            1501399    EARTH    Earth Mean Equator and Equinox
      GSEQ           1502399    EARTH    Geocentric Solar Equatorial
      ECLIPDATE      1503399    EARTH    Earth Mean Ecliptic and Equinox
      ------------------------------------------------------------------
      MME            1500499    MARS     Mars Mean Equator of date
      MME_IAU2000    1501499    MARS     Mars Mean Equator of date
                                         using IAU 2000 report constants.
      MSO            1502499    MARS     Mars-centric Solar Orbital

   Generic Inertial Frames names/IDs:

      HCI            1502010    SUN      Heliocentric Inertial
      ------------------------------------------------------------------
      VME2000        1503299    VENUS    VME of date J2000 (1)
      ------------------------------------------------------------------
      LME2000        1502301    MOON     LME of date J2000 (1)
      ------------------------------------------------------------------
      MME2000        1503499    MARS     MME_IAU2000 of date J2000 (1)


   (1) These frames are defined using the IAU 2000 Report Constants
       described at [6].


General Notes About This File
========================================================================

   About Required Data:
   --------------------

   Most of the dynamic frames defined in this file require at least one
   of the following kernels to be loaded prior to their evaluation, 
   normally during program initialization:

     - Planetary ephemeris data (SPK), i.e. DE403, DE405, etc.
     - Planetary Constants data (PCK), i.e. PCK00007.TPC, PCK00008.TPC.

   Note that loading different kernels will lead to different
   implementations of the same frame, providing different results from
   each other, in terms of state vectors referred to these frames.

   About 'of Date' Frames:
   -----------------------

   This file contains two or more implementations for the 'of Date' 
   frame, i.e. Mars Mean Equator of date (MME).

   Usually, one of these implementations is a PCK-based frame, which
   gives the user the ability of selecting the planetary constants
   used in the evaluation of the frame by changing the PCK file.

   In addition to this PCK-based frame, and whenever feasible, one more
   frame is implemented using the latest IAU report constants, included
   directly into the frame definition. In this case, the frame name is
   made up by adding the '_IAUxxxx' suffix to the PCK-based frame name,
   where xxxx is the IAU report date, i.e. MME_IAU2000. It is
   recommended to use this implementation of the frame instead of the
   PCK-based whenever possible since these frames do not depend on the
   loaded PCK data.

   In many cases, an instance of an 'of Date' frame frozen at J2000
   epoch is desired. For this reason, an to improve computing 
   efficiency, another implementation of this frame is provided. For 
   such frozen 'of Date' frame, its name is made up by appending the
   character string '2000' to the PCK-based frame name, i.e. MME2000.

       
Generic Dynamic Frames
========================================================================

   This section contains the definition of the Generic Dynamic Frames.


Heliocentric Earth Ecliptic frame (HEE)
---------------------------------------

   Definition:
   -----------
   The Heliocentric Earth Ecliptic frame is defined as follows (from [3]):

      -  X-Y plane is defined by the Earth Mean Ecliptic plane of date,
         therefore, the +Z axis is the primary vector,and it defined as
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

   The primary vector is defined as a constant vector in the ECLIPDATE
   frame and therefore no additional data is required to compute this
   vector.

   The secondary vector is defined as an 'observer-target position' vector,
   therefore, the ephemeris data required to compute the Sun-Earth vector
   in J2000 frame have to be loaded prior to using this frame.


   Remarks:
   --------
   SPICE imposes a constraint in the definition of dynamic frames:

   When the definition of a parameterized dynamic frame F1 refers to a
   second frame F2 the referenced frame F2 may be dynamic, but F2 must not
   make reference to any dynamic frame. For further information on this
   topic, please refer to [1].

   Therefore, no other dynamic frame should make reference to this frame.

   Since the secondary vector of this frame is defined as an
   'observer-target position' vector, the usage of different planetary
   ephemerides conduces to different implementations of this frame,
   but only when these data lead to different projections of the
   Sun-Earth vector on the Earth Ecliptic plane of date.

   As an example, note that the average difference in position of the +X
   axis of this frame, when using DE405 vs. DE403 ephemerides, is about
   14.3 micro-radians, with a maximum of 15.0 micro-radians.


  \begindata

      FRAME_HEE                     =  1500010
      FRAME_1500010_NAME            = 'HEE' 
      FRAME_1500010_CLASS           =  5
      FRAME_1500010_CLASS_ID        =  1500010
      FRAME_1500010_CENTER          =  10
      FRAME_1500010_RELATIVE        = 'J2000'
      FRAME_1500010_DEF_STYLE       = 'PARAMETERIZED'
      FRAME_1500010_FAMILY          = 'TWO-VECTOR'
      FRAME_1500010_PRI_AXIS        = 'Z'
      FRAME_1500010_PRI_VECTOR_DEF  = 'CONSTANT'
      FRAME_1500010_PRI_FRAME       = 'ECLIPDATE'
      FRAME_1500010_PRI_SPEC        = 'RECTANGULAR'
      FRAME_1500010_PRI_VECTOR      = ( 0, 0, 1 )
      FRAME_1500010_SEC_AXIS        = 'X'
      FRAME_1500010_SEC_VECTOR_DEF  = 'OBSERVER_TARGET_POSITION'
      FRAME_1500010_SEC_OBSERVER    = 'SUN'
      FRAME_1500010_SEC_TARGET      = 'EARTH'
      FRAME_1500010_SEC_ABCORR      = 'NONE'

  \begintext



Heliocentric Earth Equatorial frame (HEEQ)
------------------------------------------

   Definition:
   -----------
   The Heliocentric Earth Equatorial frame is defined as follows (from [3]
   and [4]):

      -  X-Y plane is the solar equator of date, therefore, the +Z axis 
         is the primary vector and it is aligned to the Sun's north pole
         of date;

      -  +X axis is defined by the intersection between the Sun equatorial
         plane and the solar central meridian of date as seen from the Earth.
         The solar central meridian of date is defined as the meridian of the
         Sun that is turned toward the Earth. Therefore, +X axis is the
         component of the Sun-Earth vector that is orthogonal to the +Z axis;

      -  +Y axis completes the right-handed system;

      -  the origin of this frame is the Sun's center of mass.

   All vectors are geometric: no aberration corrections are used.


   Required Data:
   --------------
   This frame is defined as a two-vector frame using two different types
   of specifications for the primary and secondary vectors.

   The primary vector is defined as a constant vector in the IAU_SUN
   frame, which is a PCK-based frame, therefore a PCK file containing
   the orientation constants for the Sun has to be loaded before any
   evaluation of this frame.

   The secondary vector is defined as an 'observer-target position' vector,
   therefore, the ephemeris data required to compute the Sun-Earth vector
   in J2000 frame have to be loaded before using this frame.


   Remarks:
   --------
   This frame is defined based on the IAU_SUN frame, whose evaluation is
   based on the data included in the loaded PCK file: different
   orientation constants for the Sun's spin axis will lead to different
   frames. It is strongly recommended to indicate what data have been
   used in the evaluation of this frame when referring to it, i.e. HEEQ
   using IAU 2000 constants.

   Since the secondary vector of this frame is defined as an
   'observer-target position' vector, the usage of different planetary
   ephemerides conduces to different implementations of this frame,
   but only when these data lead to different solar central meridians,
   i.e. the projection of the Sun-Earth vector on the Sun equatorial
   plane obtained from the different ephemerides has a non-zero angular
   separation.

   Note that the effect of using different SPK files is smaller, in general,
   that using different Sun's spin axis constants. As an example, the
   average difference in the position of the +X axis of the frame, when
   using DE405 or DE403 ephemerides is about 14.3 micro-radians, with a
   maximum of 15.3 micro-radians.


  \begindata

      FRAME_HEEQ                    =  1501010
      FRAME_1501010_NAME            = 'HEEQ'
      FRAME_1501010_CLASS           =  5
      FRAME_1501010_CLASS_ID        =  1501010
      FRAME_1501010_CENTER          =  10
      FRAME_1501010_RELATIVE        = 'J2000'
      FRAME_1501010_DEF_STYLE       = 'PARAMETERIZED'
      FRAME_1501010_FAMILY          = 'TWO-VECTOR'
      FRAME_1501010_PRI_AXIS        = 'Z'
      FRAME_1501010_PRI_VECTOR_DEF  = 'CONSTANT'
      FRAME_1501010_PRI_FRAME       = 'IAU_SUN'
      FRAME_1501010_PRI_SPEC        = 'RECTANGULAR'
      FRAME_1501010_PRI_VECTOR      = ( 0, 0, 1 )
      FRAME_1501010_SEC_AXIS        = 'X'
      FRAME_1501010_SEC_VECTOR_DEF  = 'OBSERVER_TARGET_POSITION'
      FRAME_1501010_SEC_OBSERVER    = 'SUN'
      FRAME_1501010_SEC_TARGET      = 'EARTH'
      FRAME_1501010_SEC_ABCORR      = 'NONE'

  \begintext


Venus-centric Solar Orbital frame (VSO)
----------------------------------------
   
   Definition:
   -----------
   The Venus-centric Solar Orbital frame is defined as follows:

      -  The position of the Sun relative to Venus is the primary vector:
         +X axis points from Venus to the Sun;

      -  The inertially referenced velocity of the Sun relative to Venus
         is the secondary vector: +Y axis is the component of this
         velocity vector orthogonal to the +X axis;

      -  +Z axis completes the right-handed system;

      -  the origin of this frame is Venus' center of mass.

   All vectors are geometric: no corrections are used.


   Required Data:
   --------------
   This frame is defined as a two-vector frame using two different types
   of specifications for the primary and secondary vectors.

   The primary vector is defined as an 'observer-target position' vector,
   therefore, the ephemeris data required to compute the Venus-Sun vector
   in J2000 frame have to be loaded before using this frame.

   The secondary vector is defined as an 'observer-target velocity' vector,
   therefore, the ephemeris data required to compute the Venus-Sun velocity
   vector in the J2000 frame have to be loaded before using this frame.


   Remarks:
   --------
   This frame is defined based on SPK data: different planetary 
   ephemerides (DE families) for Venus, the Sun and the Solar System
   Barycenter will lead to different frames. 


  \begindata

      FRAME_VSO                     =  1500299
      FRAME_1500299_NAME            = 'VSO' 
      FRAME_1500299_CLASS           =  5
      FRAME_1500299_CLASS_ID        =  1500299
      FRAME_1500299_CENTER          =  299
      FRAME_1500299_RELATIVE        = 'J2000'
      FRAME_1500299_DEF_STYLE       = 'PARAMETERIZED'
      FRAME_1500299_FAMILY          = 'TWO-VECTOR'
      FRAME_1500299_PRI_AXIS        = 'X'
      FRAME_1500299_PRI_VECTOR_DEF  = 'OBSERVER_TARGET_POSITION'
      FRAME_1500299_PRI_OBSERVER    = 'VENUS'
      FRAME_1500299_PRI_TARGET      = 'SUN'
      FRAME_1500299_PRI_ABCORR      = 'NONE'
      FRAME_1500299_SEC_AXIS        = 'Y'
      FRAME_1500299_SEC_VECTOR_DEF  = 'OBSERVER_TARGET_VELOCITY'
      FRAME_1500299_SEC_OBSERVER    = 'VENUS'
      FRAME_1500299_SEC_TARGET      = 'SUN'
      FRAME_1500299_SEC_ABCORR      = 'NONE'
      FRAME_1500299_SEC_FRAME       = 'J2000'

  \begintext


Venus Mean Equator of Date frame (VME)
--------------------------------------

   Definition:
   -----------   
   The Venus Mean Equatorial of Date frame (also known as Venus Mean
   Equator and IAU vector of Date frame) is defined as follows (from [5]):

      -  X-Y plane is defined by the Venus equator of date, and
         the +Z axis is parallel to the Venus' rotation axis of date,
         pointing toward the North side of the invariant plane;

      -  +X axis is defined by the intersection of the Venus' equator
         of date with the Earth Mean Equator of J2000;

      -  +Y axis completes the right-handed system;

      -  the origin of this frame is Venus' center of mass.

   All vectors are geometric: no corrections are used.


   Required Data:
   --------------
   This frame is defined as a two-vector frame using constant vectors as
   the specification method. The secondary vector is defined in the J2000
   frame and therefore it does not require to load any additional data.

   The primary vector is defined as a constant vector in the IAU_VENUS
   frame, which is a PCK-based frame, therefore a PCK file containing
   the orientation constants for Venus has to be loaded before using this
   frame.


   Remarks:
   --------
   This frame is defined based on the IAU_VENUS frame, whose evaluation is
   based on the data included in the loaded PCK file: different orientation
   constants for Venus' spin axis will lead to different frames. It is
   strongly recommended to indicate what data have been used in the
   evaluation of this frame when referring to it, i.e. VME using IAU 2000
   constants.

   This frame is provided as the ``most generic'' Venus Mean Equator of
   date frame since the user has the possibility of loading different Venus
   orientation constants that would help him/her to define different
   implementations of this frame.


  \begindata

      FRAME_VME                     =  1501299
      FRAME_1501299_NAME            = 'VME' 
      FRAME_1501299_CLASS           =  5
      FRAME_1501299_CLASS_ID        =  1501299
      FRAME_1501299_CENTER          =  299
      FRAME_1501299_RELATIVE        = 'J2000'
      FRAME_1501299_DEF_STYLE       = 'PARAMETERIZED'
      FRAME_1501299_FAMILY          = 'TWO-VECTOR'
      FRAME_1501299_PRI_AXIS        = 'Z'
      FRAME_1501299_PRI_VECTOR_DEF  = 'CONSTANT'
      FRAME_1501299_PRI_FRAME       = 'IAU_VENUS' 
      FRAME_1501299_PRI_SPEC        = 'RECTANGULAR'
      FRAME_1501299_PRI_VECTOR      = ( 0, 0, 1 )
      FRAME_1501299_SEC_AXIS        = 'Y'
      FRAME_1501299_SEC_VECTOR_DEF  = 'CONSTANT'                
      FRAME_1501299_SEC_FRAME       = 'J2000'
      FRAME_1501299_SEC_SPEC        = 'RECTANGULAR'
      FRAME_1501299_SEC_VECTOR      = ( 0, 0, 1 )

  \begintext


Moon-centric Solar Ecliptic frame (LSE)
---------------------------------------

   Definition:
   -----------     
   The Moon-centric Solar Ecliptic frame is defined as follows:

      -  The position of the Sun relative to Moon is the primary vector:
         +X axis points from Moon to the Sun;
 
      -  The inertially referenced velocity of the Sun relative to Moon
         is the secondary vector: +Y axis is the component of this
         velocity vector orthogonal to the +X axis;

      -  +Z axis completes the right-handed system;

      -  the origin of this frame is Moon's center of mass.

   All vectors are geometric: no corrections are used.


   Required Data:
   --------------
   This frame is defined as a two-vector frame using two different types
   of specifications for the primary and secondary vectors.

   The primary vector is defined as an 'observer-target position' vector,
   therefore, the ephemeris data required to compute the Moon-Sun vector
   in J2000 frame have to be loaded before using this frame.

   The secondary vector is defined as an 'observer-target velocity' vector,
   therefore, the ephemeris data required to compute the Moon-Sun velocity
   vector in the J2000 frame have to be loaded before using this frame.


   Remarks:
   --------
   This frame is defined based on SPK data: different planetary 
   ephemerides (DE families) for the Moon, the Sun, the Solar System
   Barycenter and the Earth-Moon Barycenter will lead to different frames. 


  \begindata

      FRAME_LSE                     =  1500301
      FRAME_1500301_NAME            = 'LSE'
      FRAME_1500301_CLASS           =  5
      FRAME_1500301_CLASS_ID        =  1500301
      FRAME_1500301_CENTER          =  301
      FRAME_1500301_RELATIVE        = 'J2000'
      FRAME_1500301_DEF_STYLE       = 'PARAMETERIZED'
      FRAME_1500301_FAMILY          = 'TWO-VECTOR'
      FRAME_1500301_PRI_AXIS        = 'X'
      FRAME_1500301_PRI_VECTOR_DEF  = 'OBSERVER_TARGET_POSITION'
      FRAME_1500301_PRI_OBSERVER    = 'MOON' 
      FRAME_1500301_PRI_TARGET      = 'SUN'
      FRAME_1500301_PRI_ABCORR      = 'NONE'
      FRAME_1500301_SEC_AXIS        = 'Y'
      FRAME_1500301_SEC_VECTOR_DEF  = 'OBSERVER_TARGET_VELOCITY'
      FRAME_1500301_SEC_OBSERVER    = 'MOON' 
      FRAME_1500301_SEC_TARGET      = 'SUN'
      FRAME_1500301_SEC_ABCORR      = 'NONE'
      FRAME_1500301_SEC_FRAME       = 'J2000'

  \begintext


Moon Mean Equator of Date frame (LME)
-------------------------------------

   Definition:
   -----------   
   The Moon Mean Equator of Date frame (also known as Moon Mean Equator
   and IAU vector of Date frame) is defined as follows (from [5]):

      -  X-Y plane is defined by the Moon equator of date, and the
         +Z axis, primary vector of this frame, is parallel to the
         Moon's rotation axis of date, pointing toward the North side
         of the invariant plane;

      -  +X axis is defined by the intersection of the Moon's equator
         of date with the Earth Mean Equator of J2000;

      -  +Y axis completes the right-handed system;

      -  the origin of this frame is Moon's center of mass.

   All vectors are geometric: no corrections are used.


   Required Data:
   --------------
   This frame is defined as a two-vector frame using constant vectors as
   the specification method. The secondary vector is defined in the J2000
   frame and it therefore does not require to load any additional data.

   The primary vector is defined as a constant vector in the IAU_MOON
   frame, which is a PCK-based frame, therefore a PCK file containing
   the orientation constants for the Moon has to be loaded before using
   this frame.


   Remarks:
   --------
   This frame is defined based on the IAU_MOON frame, whose evaluation is
   based on the data included in the loaded PCK file: different orientation
   constants for the Moon's spin axis will lead to different frames. It is
   strongly recommended to indicate what data have been used in the
   evaluation of this frame when referring to it, i.e. LME using IAU 2000
   constants.


  \begindata

      FRAME_LME                     =  1501301
      FRAME_1501301_NAME            = 'LME' 
      FRAME_1501301_CLASS           =  5
      FRAME_1501301_CLASS_ID        =  1501301
      FRAME_1501301_CENTER          =  301
      FRAME_1501301_RELATIVE        = 'J2000'
      FRAME_1501301_DEF_STYLE       = 'PARAMETERIZED'
      FRAME_1501301_FAMILY          = 'TWO-VECTOR'
      FRAME_1501301_PRI_AXIS        = 'Z'
      FRAME_1501301_PRI_VECTOR_DEF  = 'CONSTANT'
      FRAME_1501301_PRI_FRAME       = 'IAU_MOON'  
      FRAME_1501301_PRI_SPEC        = 'RECTANGULAR'
      FRAME_1501301_PRI_VECTOR      = ( 0, 0, 1 )
      FRAME_1501301_SEC_AXIS        = 'Y'
      FRAME_1501301_SEC_VECTOR_DEF  = 'CONSTANT'                
      FRAME_1501301_SEC_FRAME       = 'J2000'
      FRAME_1501301_SEC_SPEC        = 'RECTANGULAR'
      FRAME_1501301_SEC_VECTOR      = ( 0, 0, 1 )

  \begintext

Geocentric Solar Ecliptic frame (GSE)
---------------------------------------

   Definition:
   -----------
   The Geocentric Solar Ecliptic frame is defined as follows (from [3]):

      -  X-Y plane is defined by the Earth Mean Ecliptic plane of date:
         the +Z axis, primary vector, is the normal vector to this plane,
         always pointing toward the North side of the invariant plane;

      -  +X axis is the component of the Earth-Sun vector that is orthogonal
         to the +Z axis;

      -  +Y axis completes the right-handed system;

      -  the origin of this frame is the Sun's center of mass.

   All the vectors are geometric: no aberration corrections are used.


   Required Data:
   --------------
   This frame is defined as a two-vector frame using two different types
   of specifications for the primary and secondary vectors.

   The primary vector is defined as a constant vector in the ECLIPDATE
   frame and therefore, no additional data is required to compute this
   vector.

   The secondary vector is defined as an 'observer-target position' vector,
   therefore, the ephemeris data required to compute the Earth-Sun vector
   in J2000 frame have to be loaded prior to using this frame.


   Remarks:
   --------
   SPICE imposes a constraint in the definition of dynamic frames:

   When the definition of a parameterized dynamic frame F1 refers to a
   second frame F2 the referenced frame F2 may be dynamic, but F2 must not
   make reference to any dynamic frame. For further information on this
   topic, please refer to [1].

   Therefore, no other dynamic frame should make reference to this frame.

   Since the secondary vector of this frame is defined as an
   'observer-target position' vector, the usage of different planetary
   ephemerides conduces to different implementations of this frame,
   but only when these data lead to different projections of the
   Earth-Sun vector on the Earth Ecliptic plane of date.

   As an example, note that the average difference in position of the +X
   axis of this frame, when using DE405 vs. DE403 ephemerides, is about
   14.3 micro-radians, with a maximum of 15.0 micro-radians.


  \begindata

      FRAME_GSE                     =  1500399
      FRAME_1500399_NAME            = 'GSE' 
      FRAME_1500399_CLASS           =  5
      FRAME_1500399_CLASS_ID        =  1500399
      FRAME_1500399_CENTER          =  399
      FRAME_1500399_RELATIVE        = 'J2000'
      FRAME_1500399_DEF_STYLE       = 'PARAMETERIZED'
      FRAME_1500399_FAMILY          = 'TWO-VECTOR'
      FRAME_1500399_PRI_AXIS        = 'Z'
      FRAME_1500399_PRI_VECTOR_DEF  = 'CONSTANT'
      FRAME_1500399_PRI_FRAME       = 'ECLIPDATE'
      FRAME_1500399_PRI_SPEC        = 'RECTANGULAR'
      FRAME_1500399_PRI_VECTOR      = ( 0, 0, 1 )
      FRAME_1500399_SEC_AXIS        = 'X'
      FRAME_1500399_SEC_VECTOR_DEF  = 'OBSERVER_TARGET_POSITION'
      FRAME_1500399_SEC_OBSERVER    = 'EARTH'
      FRAME_1500399_SEC_TARGET      = 'SUN'  
      FRAME_1500399_SEC_ABCORR      = 'NONE'

  \begintext

Earth Mean Equator and Equinox of Date frame (EME)
--------------------------------------------------

   Definition:
   -----------
   The Earth Mean Equator and Equinox of Date frame is defined as follows:

      -  +Z axis is aligned with the north-pointing vector normal to the
         mean equatorial plane of the Earth;

      -  +X axis points along the ``mean equinox'', which is defined as the
         intersection of the Earth's mean orbital plane with the Earth's mean
         equatorial plane. It is aligned with the cross product of the
         north-pointing vectors normal to the Earth's mean equator and mean
         orbit plane of date;

      -  +Y axis is the cross product of the Z and X axes and completes the
         right-handed frame;

      -  the origin of this frame is the Earth's center of mass.

   The mathematical model used to obtain the orientation of the Earth's mean
   equator and equinox of date frame is the 1976 IAU precession model, built
   into SPICE.

   The base frame for the 1976 IAU precession model is J2000.


   Required Data:
   --------------
   The usage of this frame does not require additional data since the
   precession model used to define this frame is already built into
   SPICE.


   Remarks:
   --------
   None.


  \begindata

      FRAME_EME                     =  1501399
      FRAME_1501399_NAME            =  'EME'        
      FRAME_1501399_CLASS           =  5
      FRAME_1501399_CLASS_ID        =  1501399
      FRAME_1501399_CENTER          =  399
      FRAME_1501399_RELATIVE        = 'J2000'
      FRAME_1501399_DEF_STYLE       = 'PARAMETERIZED'
      FRAME_1501399_FAMILY          = 'MEAN_EQUATOR_AND_EQUINOX_OF_DATE'
      FRAME_1501399_PREC_MODEL      = 'EARTH_IAU_1976'
      FRAME_1501399_ROTATION_STATE  = 'ROTATING'        
 
  \begintext


Geocentric Solar Equatorial frame (GSEQ)
----------------------------------------

   Definition:
   -----------
   The Geocentric Solar Equatorial frame is defined as follows (from [7]):

      -  +X axis is the position of the Sun relative to the Earth; it's
         the primary vector and points from the Earth to the Sun;

      -  +Z axis is the component of the Sun's north pole of date orthogonal
         to the +X axis;

      -  +Y axis completes the right-handed reference frame;

      -  the origin of this frame is the Earth's center of mass.

   All the vectors are geometric: no aberration corrections are used.

   Required Data:
   --------------
   This frame is defined as a two-vector frame using two different types
   of specifications for the primary and secondary vectors.

   The primary vector is defined as an 'observer-target position' vector,
   therefore, the ephemeris data required to compute the Earth-Sun vector
   in J2000 frame have to be loaded before using this frame.

   The secondary vector is defined as a constant vector in the IAU_SUN
   frame, which is a PCK-based frame, therefore a PCK file containing
   the orientation constants for the Sun has to be loaded before using
   this frame.


   Remarks:
   --------
   This frame is defined based on the IAU_SUN frame, whose evaluation is
   based on the data included in the loaded PCK file: different orientation
   constants for the Sun's spin axis will lead to different frames. It is
   strongly recommended to indicate what data have been used in the
   evaluation of this frame when referring to it, i.e.GSEQ using IAU 2000
   constants.

   Since the primary vector of this frame is defined as an 'observer-target
   position' vector, the usage of different planetary ephemerides
   conduces to different implementations of this frame. As an example,
   the difference between using DE405 or DE403 ephemerides is, in average,
   approximately 10.9 micro-radians, with a maximum of 21.6 micro-radians.


  \begindata

      FRAME_GSEQ                    =  1502399
      FRAME_1502399_NAME            = 'GSEQ'
      FRAME_1502399_CLASS           =  5
      FRAME_1502399_CLASS_ID        =  1502399
      FRAME_1502399_CENTER          =  399
      FRAME_1502399_RELATIVE        = 'J2000'
      FRAME_1502399_DEF_STYLE       = 'PARAMETERIZED'
      FRAME_1502399_FAMILY          = 'TWO-VECTOR'
      FRAME_1502399_PRI_AXIS        = 'X'
      FRAME_1502399_PRI_VECTOR_DEF  = 'OBSERVER_TARGET_POSITION'
      FRAME_1502399_PRI_OBSERVER    = 'EARTH'
      FRAME_1502399_PRI_TARGET      = 'SUN'
      FRAME_1502399_PRI_ABCORR      = 'NONE'
      FRAME_1502399_SEC_AXIS        = 'Z'
      FRAME_1502399_SEC_VECTOR_DEF  = 'CONSTANT'
      FRAME_1502399_SEC_FRAME       = 'IAU_SUN'
      FRAME_1502399_SEC_SPEC        = 'RECTANGULAR'
      FRAME_1502399_SEC_VECTOR      = ( 0, 0, 1 )

  \begintext

Earth Mean Ecliptic and Equinox of Date frame (ECLIPDATE)
---------------------------------------------------------

   Definition:
   -----------
   The Earth Mean Ecliptic and Equinox of Date frame is defined as follows:

      -  +Z axis is aligned with the north-pointing vector normal to the
         mean orbital plane of the Earth;

      -  +X axis points along the ``mean equinox'', which is defined as the
         intersection of the Earth's mean orbital plane with the Earth's mean
         equatorial plane. It is aligned with the cross product of the
         north-pointing vectors normal to the Earth's mean equator and mean
         orbit plane of date;

      -  +Y axis is the cross product of the Z and X axes and completes the
         right-handed frame;

      -  the origin of this frame is the Earth's center of mass.

   The mathematical model used to obtain the orientation of the Earth's mean
   equator and equinox of date frame is the 1976 IAU precession model, built
   into SPICE.

   The mathematical model used to obtain the mean orbital plane of the Earth
   is the 1980 IAU obliquity model, also built into SPICE.

   The base frame for the 1976 IAU precession model is J2000.

   Required Data:
   --------------
   The usage of this frame does not require additional data since both the
   precession and the obliquity models used to define this frame are already
   built into SPICE.


   Remarks:
   --------
   None.


  \begindata

      FRAME_ECLIPDATE                =  1503399   
      FRAME_1503399_NAME             = 'ECLIPDATE'
      FRAME_1503399_CLASS            =  5
      FRAME_1503399_CLASS_ID         =  1503399
      FRAME_1503399_CENTER           =  399
      FRAME_1503399_RELATIVE         = 'J2000'
      FRAME_1503399_DEF_STYLE        = 'PARAMETERIZED'
      FRAME_1503399_FAMILY           = 'MEAN_ECLIPTIC_AND_EQUINOX_OF_DATE
      FRAME_1503399_PREC_MODEL       = 'EARTH_IAU_1976'
      FRAME_1503399_OBLIQ_MODEL      = 'EARTH_IAU_1980'
      FRAME_1503399_ROTATION_STATE   = 'ROTATING'
 
  \begintext



Mars Mean Equator of Date frame (MME)
-------------------------------------

   Definition:
   -----------   
   The Mars Mean Equator of Date frame (also known as Mars Mean Equator
   and IAU vector of Date frame) is defined as follows (from [5]):

      -  X-Y plane is defined by the Mars equator of date: the
         +Z axis, primary vector, is parallel to the Mars' rotation
         axis of date, pointing toward the North side of the invariant
         plane;

      -  +X axis is defined by the intersection of the Mars' equator of
         date with the J2000 equator;

      -  +Y axis completes the right-handed system;

      -  the origin of this frame is Mars' center of mass.


   All vectors are geometric: no corrections are used.


   Required Data:
   --------------
   This frame is defined as a two-vector frame using constant vectors as
   the specification method. The secondary vector is defined in the J2000
   frame and therefore it does not require to load any additional data.

   The primary vector is defined as a constant vector in the IAU_MARS
   frame, which is a PCK-based frame, therefore a PCK file containing
   the orientation constants for Mars has to be loaded before using this
   frame.


   Remarks:
   --------
   This frame is defined based on the IAU_MARS frame, whose evaluation is
   based on the data included in the loaded PCK file: different orientation
   constants for Mars' spin axis will lead to different frames. It is
   strongly recommended to indicate which data have been used in the
   evaluation of this frame when referring to it, i.e. MME using IAU 2000
   constants.

   This frame is provided as the ``most generic'' Mars Mean Equator of
   Date frame since the user has the possibility of loading different Mars
   orientation constants that would help him/her to define different
   implementations of this frame.


  \begindata

      FRAME_MME                     =  1500499
      FRAME_1500499_NAME            = 'MME' 
      FRAME_1500499_CLASS           =  5
      FRAME_1500499_CLASS_ID        =  1500499
      FRAME_1500499_CENTER          =  499
      FRAME_1500499_RELATIVE        = 'J2000'
      FRAME_1500499_DEF_STYLE       = 'PARAMETERIZED'
      FRAME_1500499_FAMILY          = 'TWO-VECTOR'
      FRAME_1500499_PRI_AXIS        = 'Z'
      FRAME_1500499_PRI_VECTOR_DEF  = 'CONSTANT'
      FRAME_1500499_PRI_FRAME       = 'IAU_MARS' 
      FRAME_1500499_PRI_SPEC        = 'RECTANGULAR'
      FRAME_1500499_PRI_VECTOR      = ( 0, 0, 1 )
      FRAME_1500499_SEC_AXIS        = 'Y'
      FRAME_1500499_SEC_VECTOR_DEF  = 'CONSTANT'                
      FRAME_1500499_SEC_FRAME       = 'J2000'
      FRAME_1500499_SEC_SPEC        = 'RECTANGULAR'
      FRAME_1500499_SEC_VECTOR      = ( 0, 0, 1 )

  \begintext


Mars Mean Equator of Date frame based on IAU 2000 Mars Constants (MME_IAU2000)
------------------------------------------------------------------------------

   Definition:
   -----------   
   The MME_IAU2000 frame is based on Mean Mars Equator and IAU
   vector of date evaluated using IAU 2000 Mars rotation constants.

   This frame is implemented as as Euler frame, mathematically identical
   to the PCK frame IAU_MARS based on IAU 2000 Mars rotation constants
   but without prime meridian rotation terms.

   The PCK data defining the IAU_MARS frame are:

      BODY499_POLE_RA          = (  317.68143   -0.1061      0.  )
      BODY499_POLE_DEC         = (   52.88650   -0.0609      0.  )
      BODY499_PM               = (  176.630    350.89198226  0.  )

   These values are from [6].

   Here pole RA/Dec terms in the PCK are in degrees and degrees/century;
   the rates have been converted to degrees/sec. Prime meridian terms
   from the PCK are disregarded.

   The 3x3 transformation matrix M defined by the angles is

      M = [    0.0]   [angle_2]   [angle_3]
                   3           1           3

   Vectors are mapped from the J2000 base frame to the MME_IAU2000
   frame via left multiplication by M.

   The relationship of these Euler angles to RA/Dec for the
   J2000-to-IAU Mars Mean Equator of Date transformation is as follows:

      angle_1 is        0.0
      angle_2 is pi/2 - Dec * (radians/degree)
      angle_3 is pi/2 + RA  * (radians/degree), mapped into the
                                                range 0 < angle_3 < 2*pi
                                                        -

   Since when we define the MME_IAU2000 frame we're defining the
   *inverse* of the above transformation, the angles for our Euler frame
   definition are reversed and the signs negated:

      angle_1 is -pi/2 - RA  * (radians/degree), mapped into the
                                                 range 0 < angle_3 < 2*pi
                                                         -
      angle_2 is -pi/2 + Dec * (radians/degree)
      angle_3 is         0.0

   The resulting values for the coefficients are (in degrees):

      ANGLE_1 = -47.68143   0.33621061170684714E-10
      ANGLE_2 = -37.11350  -0.19298045478743630E-10
      ANGLE_3 =   0.00000   0.0000


   Required Data:
   --------------
   Since the frame definition incorporates all the required data for
   evaluating this frame's orientation, the usage of this frame does not
   require additional data


   Remarks:
   --------
   None.


  \begindata

      FRAME_MME_IAU2000             =  1501499
      FRAME_1501499_NAME            = 'MME_IAU2000'    
      FRAME_1501499_CLASS           =  5
      FRAME_1501499_CLASS_ID        =  1501499
      FRAME_1501499_CENTER          =  499
      FRAME_1501499_RELATIVE        = 'J2000'
      FRAME_1501499_DEF_STYLE       = 'PARAMETERIZED'
      FRAME_1501499_FAMILY          = 'EULER'
      FRAME_1501499_EPOCH           =  @2000-JAN-1/12:00:00
      FRAME_1501499_AXES            =  ( 3  1  3 )
      FRAME_1501499_UNITS           = 'DEGREES'
      FRAME_1501499_ANGLE_1_COEFFS  = (  -47.68143
                                          0.33621061170684714E-10  )
      FRAME_1501499_ANGLE_2_COEFFS  = (  -37.1135
                                         -0.19298045478743630E-10  )
      FRAME_1501499_ANGLE_3_COEFFS  = (    0.0                     )

  \begintext


Mars-centric Solar Orbital frame (MSO)
--------------------------------------------------------

   Definition:
   -----------      
   The Mars-centric Solar Orbital frame is defined as follows:

      -  The position of the Sun relative to Mars is the primary vector:
         +X axis points from Mars to the Sun;

      -  The inertially referenced velocity of the Sun relative to Mars
         is the secondary vector: +Y axis is the component of this
         velocity vector orthogonal to the +X axis;

      -  +Z axis completes the right-handed system;

      -  the origin of this frame is Mars' center of mass.

   All vectors are geometric: no corrections are used.


   Required Data:
   --------------
   This frame is defined as a two-vector frame using two different types
   of specifications for the primary and secondary vectors.

   The primary vector is defined as an 'observer-target position' vector,
   therefore, the ephemeris data required to compute the Mars-Sun vector
   in J2000 frame have to be loaded before using this frame.

   The secondary vector is defined as an 'observer-target velocity' vector,
   therefore, the ephemeris data required to compute the Mars-Sun velocity
   vector in the J2000 frame have to be loaded before using this frame.


   Remarks:
   --------
   This frame is defined based on SPK data: different planetary 
   ephemerides (DE families) for Mars, the Sun and the Solar System
   Barycenter will lead to different implementations of the frame. As an
   example, the difference between using DE405 and DE403 ephemerides is,
   on average, approximately 11.1 micro-radians, with a maximum of 13.4
   micro-radians.
   

  \begindata

      FRAME_MSO                     =  1502499
      FRAME_1502499_NAME            = 'MSO'
      FRAME_1502499_CLASS           =  5
      FRAME_1502499_CLASS_ID        =  1502499
      FRAME_1502499_CENTER          =  499
      FRAME_1502499_RELATIVE        = 'J2000'
      FRAME_1502499_DEF_STYLE       = 'PARAMETERIZED'
      FRAME_1502499_FAMILY          = 'TWO-VECTOR'
      FRAME_1502499_PRI_AXIS        = 'X'
      FRAME_1502499_PRI_VECTOR_DEF  = 'OBSERVER_TARGET_POSITION'
      FRAME_1502499_PRI_OBSERVER    = 'MARS' 
      FRAME_1502499_PRI_TARGET      = 'SUN'
      FRAME_1502499_PRI_ABCORR      = 'NONE'
      FRAME_1502499_SEC_AXIS        = 'Y'
      FRAME_1502499_SEC_VECTOR_DEF  = 'OBSERVER_TARGET_VELOCITY'
      FRAME_1502499_SEC_OBSERVER    = 'MARS' 
      FRAME_1502499_SEC_TARGET      = 'SUN'
      FRAME_1502499_SEC_ABCORR      = 'NONE'
      FRAME_1502499_SEC_FRAME       = 'J2000'

  \begintext



Generic Inertial Frames
========================================================================

   This section contains the definitions of the Generic Inertial Frames
   used within the ESA Planetary Missions, which are not 'built-in' into
   the SPICE toolkit.


Heliocentric Inertial frame (HCI)
------------------------------------------------------

   The Heliocentric Inertial Frame is defined as follows (from [3]):

    -  X-Y plane is defined by the Sun's equator of epoch J2000: the +Z
       axis, primary vector, is parallel to the Sun's rotation axis of
       epoch J2000, pointing toward the Sun's north pole;

    -  +X axis is defined by the ascending node on ecliptic of J2000;

    -  +Y completes the right-handed frame;

    -  the origin of this frame is the Sun's center of mass.

   It is possible to define this frame as a dynamic frame frozen at
   J2000 epoch, using the following set of keywords:

      FRAME_HCI                     =  1502010
      FRAME_1502010_NAME            = 'HCI' 
      FRAME_1502010_CLASS           =  5
      FRAME_1502010_CLASS_ID        =  1502010
      FRAME_1502010_CENTER          =  10
      FRAME_1502010_RELATIVE        = 'J2000'
      FRAME_1502010_DEF_STYLE       = 'PARAMETERIZED'
      FRAME_1502010_FAMILY          = 'TWO-VECTOR'
      FRAME_1502010_PRI_AXIS        = 'Z'
      FRAME_1502010_PRI_VECTOR_DEF  = 'CONSTANT'
      FRAME_1502010_PRI_FRAME       = 'IAU_SUN'
      FRAME_1502010_PRI_SPEC        = 'RECTANGULAR'
      FRAME_1502010_PRI_VECTOR      = ( 0, 0, 1 )
      FRAME_1502010_SEC_AXIS        = 'X'
      FRAME_1502010_SEC_VECTOR_DEF  = 'CONSTANT'
      FRAME_1502010_SEC_FRAME       = 'ECLIPJ2000'
      FRAME_1502010_SEC_SPEC        = 'RECTANGULAR'
      FRAME_1502010_SEC_VECTOR      = ( 1, 0, 0 )
      FRAME_1502010_FREEZE_EPOCH    = @2000-JAN-01/12:00:00

   In the above implementation of this frame, the primary vector is
   defined as a constant vector in the IAU_SUN frame, which is a
   PCK-based frame, therefore a PCK file containing the orientation
   constants for the Sun has to be loaded before using this frame.

   Due to the fact that the transformation between the HCI frame and J2000
   frame is fixed and time independent, the HCI frame can be implemented
   as a fixed offset frame relative to the J2000 frame. The rotation matrix
   provided in the definition was computed using the following PXFORM call:

      CALL PXFORM( 'HCI', 'J2000', 0.D0, MATRIX )

   using the implementation of the frame given above, and the following PCK:

      PCK00008.TPC

   which contains the following constants for the SUN (from [5]):

      BODY10_POLE_RA         = (  286.13       0.          0. )
      BODY10_POLE_DEC        = (   63.87       0.          0. )

   This new implementation of the frame is preferred for computing efficiency
   reasons.

  \begindata

      FRAME_HCI                     = 1502010
      FRAME_1502010_NAME            = 'HCI'         
      FRAME_1502010_CLASS           = 4
      FRAME_1502010_CLASS_ID        = 1502010
      FRAME_1502010_CENTER          = 10 
      TKFRAME_1502010_SPEC          = 'MATRIX'
      TKFRAME_1502010_RELATIVE      = 'J2000'
      TKFRAME_1502010_MATRIX        = (

         0.9924865856197338       0.0521562187085713      -0.1106801979348909
         0.0000000000000000       0.9045936882866612       0.4262748633357763
         0.1223534934723278      -0.4230720836476433       0.8977971010607901

                                     )

  \begintext

   
Venus Mean Equator of Date J2000 frame (VME2000)
------------------------------------------------

   The Venus Mean Equator of Date J2000 is defined as follows:

    -  +Z axis points toward Venus North Pole of date J2000;

    -  +X axis points toward the Venus IAU vector of date J2000. Venus
       IAU vector of date is defined as the intersection between the
       Venus equator of date and the J2000 equator;

    -  +Y axis completes the right-hand frame;

    -  the origin of this frame is Venus center of mass.

   The VME2000 frame is the VME frame frozen at J2000 (using the IAU
   2000 constants for Venus' North Pole and prime meridian). For
   computing efficiency reasons this frame is defined as a fixed offset
   frame relative to the J2000 frame. The rotation matrix provided in
   the definition was computed using the following PXFORM call:

      CALL PXFORM( 'VME', 'J2000', 0.D0, MATRIX )

   using the following kernel file:

      PCK00008.TPC

   which implements the following IAU constants for Venus (from [5]): 

      BODY299_POLE_RA          = (  272.76       0.          0. )
      BODY299_POLE_DEC         = (   67.16       0.          0. )
      BODY299_PM               = (  160.20      -1.4813688   0. )

   Note that the prime meridian terms are not used on the evaluation of
   the VME frame.


  \begindata

      FRAME_VME2000                 = 1503299
      FRAME_1503299_NAME            = 'VME2000'     
      FRAME_1503299_CLASS           = 4
      FRAME_1503299_CLASS_ID        = 1503299
      FRAME_1503299_CENTER          = 299
      TKFRAME_1503299_SPEC          = 'MATRIX'
      TKFRAME_1503299_RELATIVE      = 'J2000'
      TKFRAME_1503299_MATRIX        = (

         0.9988399975085458       0.0481524597204341       0.0000000000000000
        -0.0443769404401835       0.9205233405740161       0.3881590738545506
         0.0186908141689023      -0.3877088083617988       0.9215923900425704
 
                                     )

  \begintext


Moon Mean Equator of Date J2000 frame (LME2000)
-----------------------------------------------

   The Moon Mean Equator of Date J2000 is defined as follows:

    -  +Z axis points toward Moon's North Pole of date J2000;

    -  +X axis points toward the Moon's equinox of date J2000. Moon's
       equinox of date J2000 is defined as the intersection between the
       Moon's equator of date and the J2000 equator;

    -  +Y axis completes the right-hand frame;

    -  the origin of this frame is Moon's center of mass.

  The LME2000 frame is the LME frame frozen at J2000 (using the IAU 2000
  constants for the Moon's North Pole, prime meridian and nutation model).
  For computing efficiency reasons this frame is defined as a fixed offset
  frame relative to the J2000 frame. The rotation matrix provided in the
  definition was computed using the following PXFORM call:

        CALL PXFORM( 'LME', 'J2000', 0.D0, MATRIX )

  using the following kernel files:

        PCK00008.TPC

  which implements the following IAU constants for the Moon (from [5]):
   
    - Moon's J2000 right ascension and declination (RA and DEC) of the
      north pole. 

        BODY301_POLE_RA      = (  269.9949        0.0031        0.      )
        BODY301_POLE_DEC     = (   66.5392        0.0130        0.      )


    - Coefficients of the trigonometric terms for the computation of the
      nutation and precession of the Moon:

        BODY301_NUT_PREC_RA  = (   -3.8787   -0.1204   0.0700   -0.0172
                                    0.0       0.0072   0.0       0.0
                                    0.0      -0.0052   0.0       0.0
                                    0.0043                              )
        
        BODY301_NUT_PREC_DEC = (   1.5419     0.0239  -0.0278    0.0068
                                   0.0       -0.0029   0.0009    0.0
                                   0.0        0.0008   0.0       0.0     
                                  -0.0009                               )
        
     The effective RA/DEC of the Moon's North pole is computed, for a given
     time as:

        alpha   =  269.9949 +  0.0031 T  -  3.8787 sin(E1)  - 0.1204 sin(E2)
             0
                                         +  0.0700 sin(E3)  - 0.0172 sin(E4) 
                                        
                                         +  0.0072 sin(E6)  - 0.0052 sin(E10)
 
                                         +  0.0043 sin(E13)           
 
 
        delta   =  66.5392  +  0.013 T   +  1.5419 cos(E1)  + 0.0239 cos(E2)
             0
                                         -  0.0278 cos(E3)  + 0.0068 cos(E4)
                                      
                                         -  0.0029 cos(E6)  + 0.0009 cos(E7)
   
                                         +  0.0008 cos(E10) - 0.0009 cos(E13)

      where T represents centuries past J2000 ( TDB ), and the nutation 
      precession angles for the Earth-Moon system are:

                          E1  =   125.045 -  0.0529921 d
                          E2  =   250.089 -  0.1059842 d
                          E3  =   260.008 + 13.0120009 d
                          E4  =   176.625 + 13.3407154 d
                          E5  =   357.529 +  0.9856003 d
                          E6  =   311.589 + 26.4057084 d
                          E7  =   134.963 + 13.0649930 d
                          E8  =   276.617 -  0.3287146 d
                          E9  =    34.226 -  1.7484877 d
                          E10 =    15.134 -  0.1589763 d
                          E11 =   119.743 +  0.0036096 d
                          E12 =   239.961 +  0.1643573 d
                          E13 =    25.053 + 12.9590088 d
       
      where d represents days past J2000 ( TDB )           


  \begindata

      FRAME_LME2000                 = 1502301
      FRAME_1502301_NAME            = 'LME2000'     
      FRAME_1502301_CLASS           = 4
      FRAME_1502301_CLASS_ID        = 1502301
      FRAME_1502301_CENTER          = 301
      TKFRAME_1502301_SPEC          = 'MATRIX'
      TKFRAME_1502301_RELATIVE      = 'J2000'
      TKFRAME_1502301_MATRIX        = (

         0.9984965052050879      -0.0548154092680678       0.0000000000000000
         0.0499357293985326       0.9096101252380440       0.4124510189026893
        -0.0226086714041825      -0.4118309009426129       0.9109797785934293
 
                                     )

  \begintext


Mars Mean Equator of Date J2000 frame (MME2000)
-----------------------------------------------

   The Mars Mean Equator of Date J2000 is defined as follows:

    -  +Z axis points toward Mars North Pole of date J2000;

    -  +X axis points toward the Mars IAU vector of date J2000. Mars
       IAU vector of date is defined as the intersection between the
       Mars equator of date and the J2000 equator;

    -  +Y axis completes the right-hand frame;

    -  the origin of this frame is Mars center of mass.

  The MME2000 frame is the MME_IAU2000 frame frozen at J2000. For computing
  efficiency reasons this frame is defined as a fixed offset frame relative
  to the J2000 frame. The rotation matrix provided in the definition was
  computed using the following PXFORM call:

      CALL PXFORM( 'MME_IAU2000', 'J2000', 0.D0, MATRIX )

  \begindata

      FRAME_MME2000                 = 1503499
      FRAME_1503499_NAME            = 'MME2000'     
      FRAME_1503499_CLASS           = 4
      FRAME_1503499_CLASS_ID        = 1503499
      FRAME_1503499_CENTER          = 499
      TKFRAME_1503499_SPEC          = 'MATRIX'
      TKFRAME_1503499_RELATIVE      = 'J2000'
      TKFRAME_1503499_MATRIX        = (

         0.6732521982472339       0.7394129276360180       0.0000000000000000
        -0.5896387605430040       0.5368794307891331       0.6033958972853946
         0.4461587269353556      -0.4062376142607541       0.7974417791532832

                                     )

  \begintext

