KPL/FK

Frame (FK) SPICE kernel file for Solar Orbiter Science Operations frames
===============================================================================

   This frames kernel defines a number of frames used by the Solar Orbiter
   science operations centre to perform mission analysis and attitude
   dependent science opportunity identification.
   
   These frames can be used stand-alone, i.e. referring directly to them and
   assuming they correspond to the SOLO spacecraft reference frame, or
   in combination with the SOLO spacecraft frames. The latter will allow the
   user to use the existing alignments and instrument frame definitions to
   perform instrument specific mission analysis and attitude dependent
   science opportunity identification. Please refer to the section ``Using
   these frames'' for further details.


Version and Date
-------------------------------------------------------------------------------

   Version 0.2 -- July 20, 2017 -- Marc Costa Sitja (ESAC/ESA)
   
      Corrected bug in SOLO_ECLIP_NORM and SOLO_EQUAT_NORM.

   Version 0.1 -- June 19, 2017 -- Marc Costa Sitja (ESAC/ESA)
   
      Reviewed by SOLO SOC (Andrew Walsh). Updated frame names according to
      latest agreements, corrected some typos and updated frame definition of 
      SOLO_EQUAT_NORM Secondary axis: 'ELCLIPJ200' instead of 'J2000'.

   Version 0.0 -- May 15, 2017 -- Marc Costa Sitja (ESAC/ESA)
   
      Initial version.


References
-------------------------------------------------------------------------------

   1.   ``Frames Required Reading''
 
   2.   ``Kernel Pool Required Reading''
 
   3.   ``C-Kernel Required Reading''

   4.   ``MoM meeting on SPICE-SW. 2016-08-12'',
        https://issues.cosmos.esa.int/solarorbiterwiki/display/SOL/
        MoM+meeting+on+SPICE-SW.+2016-08-12
   

Contact Information
-------------------------------------------------------------------------------

   If you have any questions regarding this file contact SPICE support at
   ESAC:

           Marc Costa Sitja
           (+34) 91-8131-457
           mcosta@sciops.esa.int, esa_spice@sciops.esa.int

   or the Solar Orbiter Science Operations Center at ESAC:

           sol_soc@esa.int
           

Implementation Notes
-------------------------------------------------------------------------------

   This file is used by the SPICE system as follows: programs that make use
   of this frame kernel must "load" the kernel normally during program
   initialization. Loading the kernel associates the data items with
   their names in a data structure called the "kernel pool".  The SPICELIB
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
 
   * SPICEPY is a non-official, community developed Python wrapper for the
     NAIF SPICE toolkit. Its development is managed on Github.
     It is available at: https://github.com/AndrewAnnex/SpiceyPy


Solar Orbiter Science Operations frame names and NAIF ID Codes
-------------------------------------------------------------------------------
 
   The following frames are defined in this kernel file:

      SPICE Frame Name       Long-name
      ---------------------  -------------------------------------------------
      SOLO_ORBIT_NORM         SOLO Orbit-NORM pointing @ Sun
      SOLO_EQUAT_NORM         SOLO Equatorial-NORM pointing @ Sun
      SOLO_ECLIP_NORM         SOLO Ecliptic-NORM pointing @ Sun
      SOLO_ECLIP_NORM_J2000   SOLO Ecliptic-NORM pointing @ Sun frozen @ J2000


   These frames have the following centers, frame class and NAIF
   IDs:
   
      SPICE Frame Name       Center                 Class    NAIF ID
      ---------------------  ---------------------  -------  ----------
      SOLO_ORBIT_NORM        SOLO                   DYNAMIC  -144900
      SOLO_EQUAT_NORM        SOLO                   DYNAMIC  -144901   
      SOLO_ECLIP_NORM        SOLO                   DYNAMIC  -144902
      SOLO_ECLIP_NORM_J2000  SOLO                   DYNAMIC  -144903


   The keywords implementing these frame definitions are located in the 
   "SOLO Science Operations Frame Definitions" section.
               

General Notes About This File
-------------------------------------------------------------------------------

   About Required Data:
   --------------------
   All the dynamic frames defined in this file require at least one
   of the following kernel types to be loaded prior to their evaluation, 
   normally during program initialization:

     - Planetary and Satellite ephemeris data (SPK), i.e. de432, jup300, etc;
     - Spacecraft ephemeris data (SPK);

   Note that loading different kernels will lead to different
   orientations of the same frame at a given epoch, providing different
   results from each other, in terms of state vectors referred to these 
   frames.


   Using these frames
   ------------------
   These frames have been implemented to define the different pointing
   profiles for the SOLO spacecraft. These pointing profiles can be
   used in two different ways:

      [1] ``As is'' for analysis of offsets between the spacecraft
          attitude defined in the corresponding CK and a given pointing
          profile. Loading this kernel in combination with any SOLO CK
          will allow the user to perform this comparison between the
          SOLO_PRF frame and any of the different frames defined within this 
          kernel.
   
      [2] In combination with the SOLO Frames kernel, to define
          a default pointing profile for the whole duration of the mission
          together with the spacecraft and instrument frames defined in the
          SOLO FK. In this way, instrument-specific mission analysis
          activities, for which a particular pointing profile and knowledge
          of the instruments is required, can be conducted without the need
          for a spacecraft CK.
      
          In order to define such default pointing profile, the latest
          SOLO frames kernel and this file shall be loaded before the
          selected ``SOLO spacecraft frame overwrite'' frame kernel. As
          an example, imagine that the desired default pointing profile is
          "Orbit-NORM", then the furnish (metakernel) file should contain the 
          following sequence of frames kernels, in the following order:
      
              ...
         
              $DATA/fk/solo_ANC_soc-sc-fk_V00.tf
              $DATA/fk/solo_ANC_soc-ops-fk_V00.tf
              $DATA/fk/solo_ANC_soc-orb-norm-fk_V00.tf
         
              ...
         
            (*) the example presents version 0.0 of the SOLO frames and
            SOLO Science Operations frames kernels. Newer versions of
            these files will produce the same results. 
   
          By loading the ``solo_sc_orb_NORM.tf'' frames kernel last, the
          spacecraft frame SOLO_PRF, which is defined as a CK-based
          frame in the ``SOLO frames kernel'', will be overwritten as a
          type-4 fixed offset frame, mapping the SOLO_PRF frame to
          the SOLO_ORBIT_NORM frame defined in the ``SOLO Science
          Operations Frames Kernel'' (this) file.
      

SOLO Science Operations Frame Definitions
-------------------------------------------------------------------------------

   This section contains the definition of the SOLO science operations
   frames.
         
   
SOLO NORM Baselines Frames
------------------------------------------------------------------------

   The frames defined hereafter represent the three possible frames, one of 
   which the S/C frame will be aligned to be default (when not actually 
   off-pointing, and in predictive kernels). 

   In all of them the +X axis aligns with the S/C-Sun direction in every case. 
   In principal this ought to be the apparent sun direction (i.e. LT+S). 
   Although this needs to be checked with FD. The secondary axis for each is
   defined as followed:

      - Equatorial-NORM: +Z axis projected to Solar North. 
        This secondary direction should not be aberration corrected.

      - Ecliptic-NORM: +Z axis projected to Ecliptic North. 
        This secondary direction should not be aberration corrected.

      - Orbit-NORM: -Y axis projected to velocity direction (in any inertial, 
        Sun-centered frame). This secondary direction should not be aberration 
        corrected.


SOLO Orbit-NORM pointing frame (SOLO_ORBIT_NORM):
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

   According to [4] the SOLO Spacecraft Orbit-NORM reference frame 
   -- SOLO_ORBIT_NORM -- is defined as follows:

      -  +X axis is parallel to the S/C-Sun apparent direction.
      
      -  +Y axis is anti-parallel to the projected velocity direction of the 
         S/C.

      -  +Z axis completes the right-handed system;

      -  the origin of this frame is is the point of intersection of the 
         launcher longitudinal axis with the separation plane between the 
         launcher and the composite.

   The S/C-Sun vector is not geometric and aberration corrections apply to the 
   state of the Sun to account for one-way light time and stellar aberration.

   
   Required Data:
   --------------
 
   This frame is defined as a two-vector frame using two different
   types of specifications for the primary and secondary vectors.
   
   The primary vector is defined as an 'observer-target position' vector. 
   Therefore, the ephemeris data required to compute the SOLO-Sun position 
   vector in the J2000 reference frame must be loaded before using this frame.

   The secondary vector is defined as an 'observer-target velocity' vector.
   Therefore, the ephemeris data required to compute the SOLO-Sun velocity 
   vector in the J2000 reference frame must be loaded before using this frame.
   
   Remarks:
   --------
 
   This frame is defined based on SPK data: different planetary ephemerides 
   for SOLO, the Sun and the Solar System Barycenter will lead to a different 
   frame orientation at a given time.
      
   It is strongly recommended to indicate what data have been used in the 
   evaluation of this frame when referring to it, e.g. SOLO_RARF using the IAU 
   2009 constants and the DE405 ephemeris.
   
   \begindata

      FRAME_SOLO_ORBIT_NORM          = -144900
      FRAME_-144900_NAME             = 'SOLO_ORBIT_NORM'
      FRAME_-144900_CLASS            =  5
      FRAME_-144900_CLASS_ID         = -144900
      FRAME_-144900_CENTER           =  -144
      FRAME_-144900_RELATIVE         = 'J2000'
      FRAME_-144900_DEF_STYLE        = 'PARAMETERIZED'
      FRAME_-144900_FAMILY           = 'TWO-VECTOR'
      FRAME_-144900_PRI_AXIS         = 'X'
      FRAME_-144900_PRI_VECTOR_DEF   = 'OBSERVER_TARGET_POSITION'
      FRAME_-144900_PRI_OBSERVER     = 'SOLO'
      FRAME_-144900_PRI_TARGET       = 'SUN'
      FRAME_-144900_PRI_ABCORR       = 'LT+S'
      FRAME_-144900_SEC_AXIS         = 'Y'
      FRAME_-144900_SEC_VECTOR_DEF   = 'OBSERVER_TARGET_VELOCITY'
      FRAME_-144900_SEC_OBSERVER     = 'SOLO'
      FRAME_-144900_SEC_TARGET       = 'SUN'
      FRAME_-144900_SEC_ABCORR       = 'NONE'
      FRAME_-144900_SEC_FRAME        = 'J2000'

   \begintext


SOLO Equatorial-NORM pointing frame (SOLO_EQUAT_NORM):
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

   According to [4] the SOLO Spacecraft Equatorial-NORM reference frame 
   -- SOLO_EQUAT_NORM -- is defined as follows:

      -  +X axis is parallel to the S/C-Sun apparent direction.
      
      -  +Z axis is parallel to the Solar North,

      -  +Y axis completes the right-handed system;

      -  the origin of this frame is is the point of intersection of the 
         launcher longitudinal axis with the separation plane between the 
         launcher and the composite.

   The S/C-Sun vector is not geometric and aberration corrections apply to the 
   state of the Sun to account for one-way light time and stellar aberration.

   
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
   evaluation of this frame when referring to it, e.g. SOLO_RARF using the IAU 
   2009 constants and the DE405 ephemeris.

   \begindata

      FRAME_SOLO_EQUAT_NORM          = -144901
      FRAME_-144901_NAME             = 'SOLO_EQUAT_NORM'
      FRAME_-144901_CLASS            =  5
      FRAME_-144901_CLASS_ID         = -144901
      FRAME_-144901_CENTER           =  -144
      FRAME_-144901_RELATIVE         = 'J2000'
      FRAME_-144901_DEF_STYLE        = 'PARAMETERIZED'
      FRAME_-144901_FAMILY           = 'TWO-VECTOR'
      FRAME_-144901_PRI_AXIS         = 'X'
      FRAME_-144901_PRI_VECTOR_DEF   = 'OBSERVER_TARGET_POSITION'
      FRAME_-144901_PRI_OBSERVER     = 'SOLO'
      FRAME_-144901_PRI_TARGET       = 'SUN'
      FRAME_-144901_PRI_ABCORR       = 'LT+S'
      FRAME_-144901_SEC_AXIS         = 'Z'
      FRAME_-144901_SEC_VECTOR_DEF   = 'CONSTANT'
      FRAME_-144901_SEC_FRAME        = 'IAU_SUN'
      FRAME_-144901_SEC_SPEC         = 'RECTANGULAR'
      FRAME_-144901_SEC_VECTOR       = ( 0, 0, 1 )
      FRAME_-144901_SEC_ABCORR       = 'NONE'

   \begintext


SOLO Ecliptic-NORM pointing frame (SOLO_ECLIP_NORM):
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

   According to [4] the SOLO Spacecraft Ecliptic-NORM reference frame 
   -- SOLO_ECLIP_NORM -- is defined as follows:

      -  +X axis is parallel to the S/C-Sun apparent direction.
      
      -  +Z axis is parallel to the Ecliptic North,

      -  +Z axis completes the right-handed system;

      -  the origin of this frame is is the point of intersection of the 
         launcher longitudinal axis with the separation plane between the 
         launcher and the composite.

   The S/C-Sun vector is not geometric and aberration corrections apply to the 
   state of the Sun to account for one-way light time and stellar aberration.

   
   Required Data:
   --------------
 
   This frame is defined as a two-vector frame using two different
   types of specifications for the primary and secondary vectors.
   
   The primary vector is defined as an 'observer-target position' vector. 
   Therefore, the ephemeris data required to compute the SOLO-Sun position 
   vector in the J2000 reference frame must be loaded before using this frame.

   The secondary vector is defined as a constant vector in the ECLIPJ2000 
   frame, which provides the Ecliptic North.

   
   Remarks:
   --------
 
   This frame is defined based on SPK data: different planetary ephemerides 
   for SOLO, the Sun and the Solar System Barycenter will lead to a different 
   frame orientation at a given time.
      
   It is strongly recommended to indicate what data have been used in the 
   evaluation of this frame when referring to it, e.g. SOLO_RARF using the IAU 
   2009 constants and the DE405 ephemeris.

   \begindata

      FRAME_SOLO_ECLIP_NORM          = -144902
      FRAME_-144902_NAME             = 'SOLO_ECLIP_NORM'
      FRAME_-144902_CLASS            =  5
      FRAME_-144902_CLASS_ID         = -144902
      FRAME_-144902_CENTER           =  -144
      FRAME_-144902_RELATIVE         = 'J2000'
      FRAME_-144902_DEF_STYLE        = 'PARAMETERIZED'
      FRAME_-144902_FAMILY           = 'TWO-VECTOR'
      FRAME_-144902_PRI_AXIS         = 'X'
      FRAME_-144902_PRI_VECTOR_DEF   = 'OBSERVER_TARGET_POSITION'
      FRAME_-144902_PRI_OBSERVER     = 'SOLO'
      FRAME_-144902_PRI_TARGET       = 'SUN'
      FRAME_-144902_PRI_ABCORR       = 'LT+S'
      FRAME_-144902_SEC_AXIS         = 'Z'
      FRAME_-144902_SEC_VECTOR_DEF   = 'CONSTANT'
      FRAME_-144902_SEC_FRAME        = 'ECLIPJ2000'
      FRAME_-144902_SEC_SPEC         = 'RECTANGULAR'
      FRAME_-144902_SEC_VECTOR       = ( 0, 0, 1 )
      FRAME_-144902_SEC_ABCORR       = 'NONE'

   \begintext


End of FK file.