KPL/FK

Solar Orbiter (SOLO) Frames Kernel
===============================================================================

   This frame kernel contains complete set of frame definitions for
   the Solar Orbiter Spacecraft (SOLO) including definitions for
   the SOLO structures and SOLO science instrument frames.
   This kernel also contains NAIF ID/name mapping for the SOLO instruments.


Version and Date
------------------------------------------------------------------------

   Version 0.9 -- February 23, 2023 -- Alfredo Escalante Lopez, ESAC/ESA
                                       Ricardo Valles Blanco, ESAC/ESA

      Corrected EPD SIS rotation angles and added body/ID definitions
      for SOLO_EPD_HET_MY-ASW and SOLO_EPD_HET_PY-S.

      Corrected several typos.

   Version 0.8 -- January 21, 2020 -- Alfredo Escalante Lopez, ESAC/ESA

      Added Solar Arrays reference frames.

   Version 0.7 -- May 4, 2020 -- Marc Costa Sitja, ESAC/ESA
                                 Matthieu Kretzschmar, LPC2E

      Corrected SOLO_SOLOHI_ILS diagram definition and default matrix
      transformation. Updated SOLO_RPW_SCM frame definition to align
      the frame axis with the orthogonal magnetic coils.

   Version 0.6 -- March 13, 2020 -- Marc Costa Sitja, ESAC/ESA
                                    Andrew Walsh, ESAC/ESA

      Minor PHI description correction.

   Version 0.5 -- January 31, 2020 -- Marc Costa Sitja, ESAC/ESA

      Fixed bug in SOLO_RPW_ANT_1, SOLO_RPW_ANT_2 and SOLO_RPW_ANT_3
      frame definitions. Updated HGA and MGA descriptions. Minor edits.

   Version 0.4 -- July 4, 2019 -- Marc Costa Sitja, ESAC/ESA

      Corrected typo in SOLO_METIS_IEO-M0 descriptions.
      Added SOLAR-ORBITER and SOL acronyms.
      Corrected several typos and IDs.

   Version 0.3 -- March 29, 2019 -- Marc Costa Sitja, ESAC/ESA

      Added SWA EAS Science frames "SOLO_SWA_EAS1-SCI" and
      "SOLO_SWA_EAS2-SCI".

   Version 0.2 -- October 11, 2017 -- Marc Costa Sitja, ESAC/ESA

      Reviewed by SOLO SOC (Christopher Watson). Updated references and
      descriptions.

      Corrected SOLO_SOLOHI_ILS and SOLO_EPD_EPT definitions and
      descriptions.

      Renamed the Spacecraft Physical reference frame "SOLO_PRF"
      (the Airbus definition, which becomes unknowable in-flight) to
      Spacecraft Reference Frame "SOLO_SRF" (what is in the FITS
      definitions).

      Added inboard and outboard segments of the instrument boom.

   Version 0.1 -- July 6, 2017 -- Marc Costa Sitja, ESAC/ESA

      Reviewed by SOLO SOC (Andrew Walsh). Updated references and
      descriptions. Added Star Trackers reference frames.

      Corrected typo in SOLO_MGA_EL and SOLO_MGA_MRF definitions.

   Version 0.0 -- May 18, 2017 -- Marc Costa Sitja, ESAC/ESA

      Preliminary Version.


References
------------------------------------------------------------------------

   1. ``Frames Required Reading'', NAIF.

   2. ``Kernel Pool Required Reading'', NAIF.

   3. ``C-Kernel Required Reading'', NAIF.

   4. ``Solar Orbiter Coordinate System Document EN-15'',
      SOL.S.STR.TN.00099, Issue 5, Airbus Defence and Space,
      26th February 2016.

   5. ``Solar Orbiter Interface Control Document for Low Latency Data
      CDF Files'', SOL-SGS- ICD-0004, Andrew Walsh, Issue 1, Revision 2,
      20th January 2017.

   6. ``Metis Instrument for the Solar Orbiter Mission Experiment
      Interface Document - Part B'', METIS-OATO-ICD-001,
      G. Nicolini and the Metis team, Issue 5, Revision 0,
      18th March 2017.

   7. ``Experiment Interface Document Part B for RWP'',
      SOLO-RPWSY-IF-55-CNES, RPW Team, Issue 5, Revision 4,
      28th November 2016.

   8. ``Experiment Interface Document Part B for Solar Wind Analyser
      Suite'', MSSL-SO-SWA-EID-B, Barry Hancock and the SWA team,
      Issue 7, Revision 0, 21st October 2015.

   9. ``Solar Orbiter Energic Particle Detector EPD Instrument User
      Manual'', SO-EPD-PO-MA-0002, Issue 2, Revision 7,
      9th September 2016.

   10. ``SWA EAS - science frames and rotation matrices'',
       SO-SWA-MSSL-TN, A. Varsani, Mullard Space Science Laboratory,
       Issue 1, January 2019.


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
------------------------------------------------------------------------

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


SOLO NAIF ID Codes -- Summary Section
------------------------------------------------------------------------

   The following names and NAIF ID codes are assigned to the SOLO spacecraft,
   its structures and science instruments (the keywords implementing these
   definitions are located in the section "SOL NAIF ID Codes -- Definition
   Section" at the end of this file):

      SOLO Spacecraft and Spacecraft Structures names/IDs:

            SOLO                      -144     (synonyms: SOLAR ORBITER,
                                                SOLAR-ORBITER, SOL)

            SOLO_SRF                  -144000  (synonyms: SOLO_SPACECRAFT,
                                                SOLO_SC)
            SOLO_HGA                  -144013

            SOLO_SA+Y_ZERO            -144014
            SOLO_SA+Y                 -144015
            SOLO_SA-Y_ZERO            -144016
            SOLO_SA-Y                 -144017

            SOLO_LGA_PZ               -144020
            SOLO_LGA_MZ               -144021

            SOLO_MGA                  -144032

            SOLO_INS_BOOM             -144040
            SOLO_INS_BOOM_IB          -144041
            SOLO_INS_BOOM_OB          -144042

            SOLO_STR-1                -144051
            SOLO_STR-2                -144052

      EPD names/IDs:

            SOLO_EPD_STEP             -144100
            SOLO_EPD_SIS_ASW          -144111
            SOLO_EPD_SIS_SW           -144112
            SOLO_EPD_EPT_MY           -144123
            SOLO_EPD_EPT_PY           -144124
            SOLO_EPD_HET_MY           -144125
            SOLO_EPD_HET_PY           -144126

      EUI names/IDs:

            SOLO_EUI                  -144200
            SOLO_EUI_FSI              -144210
            SOLO_EUI_HRI_LYA          -144220
            SOLO_EUI_HRI_EUV          -144230

      MAG names/IDs:

            SOLO_MAG                  -144300
            SOLO_MAG_IBS              -144301
            SOLO_MAG_OBS              -144302

      Metis names/IDs:

            SOLO_METIS                -144400
            SOLO_METIS_EUV            -144410
            SOLO_METIS_EUV_MIN        -144413
            SOLO_METIS_EUV_MAX        -144414
            SOLO_METIS_VIS            -144420
            SOLO_METIS_VIS_MIN        -144423
            SOLO_METIS_VIS_MAX        -144424
            SOLO_METIS_IEO-M0         -144430

      PHI names/IDs:

            SOLO_PHI                  -144500
            SOLO_PHI_FDT              -144510
            SOLO_PHI_HRT              -144520

      RPW names/IDs:

            SOLO_RPW                  -144600
            SOLO_RPW_ANT_1            -144610
            SOLO_RPW_ANT_2            -144620
            SOLO_RPW_ANT_3            -144630
            SOLO_RPW_SCM              -144640

      SOLOHI names/IDs:

            SOLO_SOLOHI               -144700

      SPICE names/IDs:

            SOLO_SPICE                -144800
            SOLO_SPICE_SW             -144810
            SOLO_SPICE_LW             -144820

      STIX names/IDs:

            SOLO_STIX                 -144850

      SWA names/IDs:

            SOLO_SWA                  -144870
            SOLO_SWA_HIS              -144871
            SOLO_SWA_PAS              -144872
            SOLO_SWA_EAS              -144873


SOLO Frames
------------------------------------------------------------------------

   The following SOLO frames are defined in this kernel file:

           Name                  Relative to            Type        NAIF ID
      ======================  ===================   ============    =======

   SOLO Spacecraft and Spacecraft Structures frames:
   -------------------------------------------------
      SOLO_SRF                J2000                 CK              -144000
      SOLO_FOF                SOLO_SRF              CK              -144001

      SOLO_HGA_URF            SOLO_SRF              FIXED           -144010
      SOLO_HGA_EL             SOLO_HGA_URF          CK              -144011
      SOLO_HGA_AZ             SOLO_HGA_EL           CK              -144012
      SOLO_HGA_MRF            SOLO_HGA_AZ           FIXED           -144013

      SOLO_LGA_PZ             SOLO_SRF              FIXED           -144020
      SOLO_LGA_MZ             SOLO_SRF              FIXED           -144021

      SOLO_MGA_URF            SOLO_SRF              FIXED           -144030
      SOLO_MGA_EL             SOLO_MGA_URF          CK              -144031
      SOLO_MGA_MRF            SOLO_MGA_AZ           FIXED           -144032

      SOLO_INS_BOOM_IB        SOLO_SRF              CK              -144041
      SOLO_INS_BOOM_OB        SOLO_SRF              CK              -144042

      SOLO_STR-1              SOLO_SRF              FIXED           -144051
      SOLO_STR-2              SOLO_SRF              FIXED           -144052

   EPD frames:
   -----------
      SOLO_EPD_STEP           SOLO_SRF              FIXED           -144100
      SOLO_EPD_SIS            SOLO_SRF              FIXED           -144110
      SOLO_EPD_SIS_ASW        SOLO_EPD_SIS          FIXED           -144111
      SOLO_EPD_SIS_SW         SOLO_EPD_SIS          FIXED           -144112
      SOLO_EPD_EPT-HET_MY     SOLO_SRF              FIXED           -144121
      SOLO_EPD_EPT-HET_PY     SOLO_SRF              FIXED           -144122

   EUI frames:
   -----------
      SOLO_EUI_FSI_ILS        SOLO_FOF              CK              -144211
      SOLO_EUI_FSI_OPT        SOLO_EUI_FSI_ILS      FIXED           -144212
      SOLO_EUI_HRI_LYA_ILS    SOLO_FOF              CK              -144221
      SOLO_EUI_HRI_LYA_OPT    SOLO_EUI_HRI_LYA_ILS  FIXED           -144222
      SOLO_EUI_HRI_EUV_ILS    SOLO_FOF              CK              -144231
      SOLO_EUI_HRI_EUV_OPT    SOLO_EUI_HRI_EUV_ILS  FIXED           -144232

   MAG frames:
   -----------
      SOLO_MAG_IBS            SOLO_INS_BOOM         FIXED           -144301
      SOLO_MAG_OBS            SOLO_INS_BOOM         FIXED           -144302

   Metis frames:
   -------------
      SOLO_METIS_EUV_ILS      SOLO_FOF              CK              -144411
      SOLO_METIS_EUV_OPT      SOLO_METIS_EUV_ILS    FIXED           -144412
      SOLO_METIS_VIS_ILS      SOLO_FOF              CK              -144421
      SOLO_METIS_VIS_OPT      SOLO_METIS_VIS_ILS    FIXED           -144422
      SOLO_METIS_IEO-M0       SOLO_FOF              CK              -144430

   PHI frames:
   -----------
      SOLO_PHI_FDT_ILS        SOLO_FOF              CK              -144511
      SOLO_PHI_FDT_OPT        SOLO_PHI_FDT_ILS      FIXED           -144512
      SOLO_PHI_HRT_ILS        SOLO_FOF              CK              -144521
      SOLO_PHI_HRT_OPT        SOLO_PHI_HRT_ILS      FIXED           -144522

   RPW frames:
   -----------
      SOLO_RPW_ANT_1          SOLO_SRF              FIXED           -144610
      SOLO_RPW_ANT_2          SOLO_SRF              FIXED           -144620
      SOLO_RPW_ANT_3          SOLO_SRF              FIXED           -144630
      SOLO_RPW_SCM            SOLO_INS_BOOM         FIXED           -144640

   SOLOHI frames:
   --------------
      SOLO_SOLOHI_ILS         SOLO_FOF              CK              -144701
      SOLO_SOLOHI_OPT         SOLO_SOLHI_ILS        FIXED           -144702

   SPICE frames:
   -------------
      SOLO_SPICE_SW_ILS       SOLO_FOF              CK              -144811
      SOLO_SPICE_SW_OPT       SOLO_SPICE_SW_ILS     FIXED           -144812
      SOLO_SPICE_LW_ILS       SOLO_FOF              CK              -144821
      SOLO_SPICE_LW_OPT       SOLO_SPICE_LW_ILS     FIXED           -144822

   STIX frames:
   ------------
      SOLO_STIX_ILS           SOLO_FOF              CK              -144851
      SOLO_STIX_OPT           SOLO_STIX_ILS         FIXED           -144852

   SWA frames:
   -----------
      SOLO_SWA_HIS            SOLO_SRF              FIXED           -144871
      SOLO_SWA_PAS            SOLO_SRF              FIXED           -144872
      SOLO_SWA_EAS            SOLO_SRF              FIXED           -144873
      SOLO_SWA_EAS1           SOLO_SWA_EAS          FIXED           -144874
      SOLO_SWA_EAS2           SOLO_SWA_EAS          FIXED           -144875
      SOLO_SWA_EAS1-SCI       SOLO_SWA_EAS          FIXED           -144875
      SOLO_SWA_EAS2-SCI       SOLO_SWA_EAS          FIXED           -144876


Solar Orbiter Frames Hierarchy
------------------------------------------------------------------------

   The diagram below shows the Solar Orbiter frames hierarchy (except
   for science operations frames):


                               "J2000" INERTIAL
           +---------------------------------------------------------+
           |                          |             |      |         |
           |<-pck                     |             |<-dyn |<-dyn    |<-dyn
           |                          |             |      |         |
           v                          |             |      |         v
       "IAU_SUN"                      |             |      | "SOLO_ORBIT_NORM"
       ---------                      |             |      | -----------------
                                      |             |      v
                                      |             | "SOLO_ECLIP_NORM"
                                      |             | -----------------
        SOLO_STR-1                    |             v
        ----------                    |         "SOLO_EQUAT_NORM"
            ^                         |         -----------------
            |                         |       `- - - - - - - - - - - - - - - -'
            |<-fixed                  |                       '
            |                         |                       '
            |  SOLO_STR-2             |                       '
            |  ----------             |   . - - - - - - - - - '
            |     ^                   |   '
            |     |               ck->|   '<-fixed(*)
            |     |<-fixed            V   V
            |     |              "SOLO_SRF"(1)
         +----- ---------------------------------------------------+
         |     |   |         |     |      .         |              |
         |     |   |<-fixed  |     |      .         |<-fixed       |<-fixed
         |     |   |         |     |      .         |              |
         |     |   |  fixed->|     |      .         v              v
         |     |   |         v     |      .   "SOLO_MGA_URF"  "SOLO_HGA_URF"
         |     |   | "SOLO_LGA_MZ" |      .   -------------   -------------
         |     |   | ------------- |      .         |              |
         |     |   v               |      .         |<-ck          |<-ck
         |     |  "SOLO_LGA_PZ"    |      .         |              |
         |     |  -------------    |      .         v              v
         |     |                   |      .   "SOLO_MGA_EL"   "SOLO_HGA_EL"
         |     |                   |      .   ------------    ------------
         |     |                   |      .         |              |
         |     |                   |      .         |<-fixed       |<-ck
         |     |                   |      .         |              |
         |     |                   |      .         v              v
         |     |                   |      .   "SOLO_MGA_MRF" "SOLO_HGA_AZ"
         |     |                   |      .   -------------- -------------
         |     |                   |      .                        |
         |     |                   |      .                        |<-fixed
         |     |                   |      .                        |
         |     |<-ck(4)     ck(2)->|      .                        v
         |     |                   |      .                  "SOLO_HGA_MRF"
         |     v                   |      .                  --------------
         |   "SOLO_INS_BOOM_OB"    |      .<-fixed
         |   ------------------    |      .
         |                 .       |      .
         |<-ck(3)          .       |      .
         |                 .       |      .
         v                 .       v      .
        "SOLO_INS_BOOM_IB" .  "SOLO_FOF"  .
        ------------------ .  ----------  .
               .           .       .      .
               .           .       .      .
               .           .       .      .
               .           .       .      .
               V           V       V      V
           Individual instrument frame trees are
         provided in the other sections of this file


   (1) The SOLO_SRF frame can be a CK based frame, as it is actually defined
       in this file and use one or a combination of the following CK files for
       its orientation w.r.t J2000:

          solo_ANC_soc-default-att_YYYYMMDD-YYYYMMDD_VOEM_VNN.bc
          solo_ANC_soc-pred-roll-att_YYYYMMDD-YYYYMMDD_VOEM_VFECS_VNN.bc
          solo_ANC_soc-flown-att_YYYYMMDD[-YYYYMMDD_]VNN.bc

       Alternatively, the SOLO_SRF frame can be "mapped" to one of the SOLO
       Science Operations frames defined in:

          solo_ANC_soc-ops-fk_VNN.tf

       More details are provided in the SOLO Science Operations frame kernel.


   (2) The CK file used for this transformation is:

          solo_ANC_soc-sc-fof-ck_YYYYMMDD-YYYYMMDD_VNN.bc


   (3) The CK file used for this transformation is:

          solo_ANC_soc-sc-iboom-ck_YYYYMMDD-YYYYMMDD_VNN.bc


   (4) The CK file used for this transformation is:

          solo_ANC_soc-sc-oboom-ck_YYYYMMDD-YYYYMMDD_VNN.bc


   (*) The SOLO_SRF frame can be mapped into one of the SOLO_*_NORM dynamic
       frames. More information is available on the Solar Orbiter Science
       Operations frame kernel (solo_ANC_soc-ops-fk_VNN.tf). This can be used
       for early analysis where no detailed orientation information is
       available.


SOLO Spacecraft and Spacecraft Structures Frames
------------------------------------------------------------------------

   This section of the file contains the definitions of the spacecraft
   and spacecraft structures frames.

   DISCLAIMER: The origin of the frames specified in the following
   definitions are not implemented. The ``true'' origin of all frames
   is in the center of the SOLO_SPACECRAFT frame, the center of which
   is defined by the position given by the SPK (ephemeris) kernel in
   use.


SOLO Spacecraft Reference Frame:
--------------------------------

   The Solar Orbiter Spacecraft  Reference Frame is the principal
   mechanical reference frame. According to [4] the SOLO spacecraft reference
   frame -- SOLO_SRF is defined as follows:

      -  +X axis is the longitudinal axis of Solar Orbiter, pointing from the
         Origin towards Solar Orbiter, positive towards the heatshield.

      -  -Y axis is the Transverse axis, pointing towards the Service Module
         panel of the S/C.

      -  +Z axis completes the right-handed frame and is pointing towards the
         MGA.

      -  the origin of this frame is is the point of intersection of the
         launcher longitudinal axis with the separation plane between the
         launcher and the composite.


   These diagrams illustrate the SOLO_SRF frame:

   -Y S/C side (Science deck side) view:
   -------------------------------------

                                                               |
                                                               |
                                                               |
                                                               |
                                                               |
                                                               |
                                                               |
                                                               H
                                                               |
                                      __--o.      _____..|     |
                                __--''     \'.  .o.----..|     |      | |
                            __''==-   +Zsrf \.'---------------------. |=|
                        __--             ^  >|                      |=| |
                  __--'' ()              |   |                      | |=|
            __--''==-                    |.  |                      | | |
       __--''                            | \ |                      | | |
     >|__|                               |  '|                      | | |
                                         |   |                      | | |
                                  +Ysrf  x-----------> +Xsrf        | | |
                                         |   |                      | | |
                                         |  .|                      | | |
                                         | / |                      | | |
                                         .'  |                      | | |
                                             |                      | |=|
                                            >|                      |=| |
                                            o:______________________: |=|
                                           //      /        | \       | |
                                          //      /_________|__\
                                         ||                 |
                                         ||                 |
               +Ysrf is into          '. ||                 |
                the page.            .| |||                 |
                                   -: | |'/                 H
                                     '| |                   |
                                      .'                    |
                                                            |


   +Z S/C side view:
   -----------------
                                            /\/\/
                                              H
                                              H
                                              H
                                              |             |
                                         +Ysrf|             |
                                           ^  |             |
                                           |  |             |
                                           |  0             |         | |
                                         '.| .----------------------. |=|
                                      '. | | |                      |=| |
                                      | || |'|                      | | |
     >___________________________________ _|__________..|           | |=|
     >-------------------------------------o--------------->        | | |
                                      | ||+Zsrf           +Xsrf     |=| |
                                      .'|| / |                      | |=|
                                         .'  '----------------------' | |
                                              0             |
                                              |             |
                                              |             |
               +Zsrf is out of                |             |
                the page.                     |             |
                                              H
                                              H
                                              H
                                            /\/\/


   +X S/C side (Heat Shield side) view:
   ------------------------------------

                                        |  ANT 1
                                        |
                                        |
                                        |
                                        |
                                        |
                                        |
                                        H
                                        |
                                        |
                                        |
                                       .O.
                             .--------------------.
                             |                    |
                             |         . __       |
   .__  _________.           |    oO    |__|      |           ,_________  __.
   |  \ \        |\          |         ^ +Zsrf    |          /|        / /  |
   |  / /        |\\         |         |          |         //|        \ \  |
   |  \ \        | \\________|         |          |________// |        / /  |
   |  / /        | /.--------|         |          |--------.\ |        \ \  |
   |  \ \        |//         |         o--------> |         \\|        / /  |
   |  / /        |/          |      +Xsrf     +Ysrf          \|        \ \  |
   '-'  '--------'           |                    |           '---------' '-'
                             |                    |
                             |                    |
                             |                    |
                             |                    |           +Xsrf is out
                             .____________________.            of the page.
                           .'    /____________\    '.
                         .'            ||            '.
                       .'              ||              '.
                     .'              .-''-.              '.
                   .'               /      \               '.
                 .'                |        |                '.
               .'                   \      /                   '.
     ANT 2   .'                      `-..-'                      '.  ANT 3
           .'                                                      '.
         .'                                                          '.


   Since the S/C bus attitude with respect to an inertial frame is provided
   by a C-kernel (see [3] for more information), this frame is defined as
   a CK-based frame.

   \begindata

      FRAME_SOLO_SRF                   = -144000
      FRAME_-144000_NAME               = 'SOLO_SRF'
      FRAME_-144000_CLASS              =  3
      FRAME_-144000_CLASS_ID           = -144000
      FRAME_-144000_CENTER             = -144
      CK_-144000_SCLK                  = -144
      CK_-144000_SPK                   = -144
      OBJECT_-144_FRAME                = 'SOLO_SRF'

   \begintext


SOLO Flight Optical Frame:
--------------------------

   The Flight Optical Frame (FOF) is an intermediate frame which is a
   theoretical construct, and it allows for global misalignments to be
   inserted if it is determined in-flight that the remote sensing instrument
   frames are tending to move together. This frame provides a useful
   operational option to allow a centralised update of remote sensing
   instrument alignments based on a single ILS-SRF calibration.

   Nominally the FOF frame is equivalent to the PRF frame. The transformation
   from the SRF to the FOF is represented by a rotation matrix I. This is
   chosen because this starts as the identity matrix, and only changes if a
   clear case for its use emerges.

   Since the FOF orientation with respect to the PRF frame is provided
   by a C-kernel (see [3] for more information), this frame is defined as
   a CK-based frame. The SOLO_FOF has a rotation matrix  w.r.t. the SOLO_SRF
   specified in the FOF CK file:

      solo_ANC_soc-sc-fof-ck_YYYYMMDD-YYYYMMDD_VNN.bc

         where

            YYYYMMDD   start and finish dates of the CK coverage;

            sYYYYMMDD  the SCLK reference with which the CK was generated if
                       the kernel is to be used in an as-flown scenario
                       (optional);

            NN         version of the kernel


   This set of keywords defines the FOF frame as a CK frame:

   \begindata

      FRAME_SOLO_FOF                   = -144001
      FRAME_-144001_NAME               = 'SOLO_FOF'
      FRAME_-144001_CLASS              =  3
      FRAME_-144001_CLASS_ID           = -144001
      FRAME_-144001_CENTER             = -144
      CK_-144001_SCLK                  = -144
      CK_-144001_SPK                   = -144
      OBJECT_-144_FRAME                = 'SOLO_FOF'

   \begintext


SOLO High Gain Antenna Frames:
------------------------------

   The HGA Unit Reference Frame is used to define the position and orientation
   of the HGA relative to the SOLO_SRF frame or HGA APM, used to provide a
   reference for the definition of the HGA inertial properties, and used to
   define the deformation of the HGA from its nominal geometry.

   The SOLO High Gain Antenna is attached to the -X panel of the S/C bus
   in the corner with the -Z panel by a gimbal providing two degrees of
   freedom and it articulates during flight to track Earth.

   According to [4] the SOLO HGA Unit reference frame -- SOLO_HGA_URF -- is
   defined as follows:

      -  +X axis is aligned with the S/C +X axis,

      -  +Y axis is antiparallel to the S/C +Y axis,

      -  +Y completes the right-handed frame and is antiparallel to the
         S/C +Z axis,

      -  the origin of this frame is on the HGA major assembly interface plane
         with the spacecraft lower floor. The coordinates in meters
         of the origin w.r.t SOLO_SRF frame are:

            ( x, y, z ) =  ( 0.4002, 0.1459, -0.8782 )

   The SOLO_HGA_URF frame is defined as a fixed offset frame relative to the
   SOLO_MRF frame and is rotated by 180 degrees about the +X axis from it.

   \begindata

      FRAME_SOLO_HGA_URF              = -144010
      FRAME_-144010_NAME              = 'SOLO_HGA_URF'
      FRAME_-144010_CLASS             =  4
      FRAME_-144010_CLASS_ID          = -144010
      FRAME_-144010_CENTER            = -144
      TKFRAME_-144010_RELATIVE        = 'SOLO_SRF'
      TKFRAME_-144010_SPEC            = 'ANGLES'
      TKFRAME_-144010_UNITS           = 'DEGREES'
      TKFRAME_-144010_AXES            = (     3,      2,      1 )
      TKFRAME_-144010_ANGLES          = (   0.0,    0.0,  180.0 )

   \begintext


HGA Moveable Reference Frames:
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

   DISCLAIMER: The HGA Moveable Referenfe frames although defined are not
   yet implemented in the Solar Orbiter SPICE Kernel Dataset (CK files
   are not being generated).

   To incorporate rotations in the gimbal the HGA frame chain includes
   three frames: SOLO_HGA_EL, SOLO_HGA_AZ, and SOLO_HGA_MRF.

   The first two frames are defined as CK-based frames and are
   co-aligned with the SOLO_HGA_MRF frame in the zero gimbal position. In
   a non-zero position the SOLO_HGA_EL is rotated from the HGA MRF
   frame by an elevation angle about +Y and the SOLO_HGA_AZ frame is
   rotated from the SOLO_HGA_EL frame by an azimuth angle about +Z. These
   rotations are stored in separated segments in CK files.

   The canonical upright position of the HGA is defined by azimuth = 0 deg,
   elevation = -90 deg. Note that the HGA safe position is identical to the
   canonical upright position. When in stowed position, the SOLO_HGA_URF axes
   are aligned with the axes of the SOLO_HGA_MRF reference frame.

   According to [4] the SOLO optical reference frame -- SOLO_HGA_MRF -- is
   defined as follows:

      -  +Z axis is aligned with the boresight of the undeformed HGA.

      -  +X axis is aligned with the Along the undeformed boom, from the
         HGA APM to the HGA ARA,

      -  +Y completes the right-handed frame,

      -  the origin of this frame is at the intersection of the axis of
         rotation of the azimuth and elevation mechanisms. This point is
         therefore fixed in the SOLO_SRF frame. The coordinates in meters
         of the origin w.r.t SOLO_SRF frame are:

            ( x, y, z ) = ( 0.125,  0.1459, -1.1193 )


   This set of keywords defines the HGA frame as a CK frame:

   \begindata

      FRAME_SOLO_HGA_EL                = -144011
      FRAME_-144011_NAME               = 'SOLO_HGA_EL'
      FRAME_-144011_CLASS              =  3
      FRAME_-144011_CLASS_ID           = -144011
      FRAME_-144011_CENTER             = -144
      CK_-144011_SCLK                  = -144
      CK_-144011_SPK                   = -144

      FRAME_SOLO_HGA_AZ                = -144012
      FRAME_-144012_NAME               = 'SOLO_HGA_AZ'
      FRAME_-144012_CLASS              =  3
      FRAME_-144012_CLASS_ID           = -143012
      FRAME_-144012_CENTER             = -144
      CK_-144012_SCLK                  = -144
      CK_-144012_SPK                   = -144

      FRAME_SOLO_HGA_MRF               = -144013
      FRAME_-144013_NAME               = 'SOLO_HGA_MRF'
      FRAME_-144013_CLASS              =  4
      FRAME_-144013_CLASS_ID           = -144013
      FRAME_-144013_CENTER             = -144
      TKFRAME_-144013_RELATIVE         = 'SOLO_HGA_AZ'
      TKFRAME_-144013_SPEC             = 'ANGLES'
      TKFRAME_-144013_UNITS            = 'DEGREES'
      TKFRAME_-144013_AXES             = (   2,       1,       3     )
      TKFRAME_-144013_ANGLES           = (   0.000,   0.000,   0.000 )

   \begintext


SOLO Low Gain Antenna Frames:
-----------------------------

   Solar Orbiter is equipped with two Low Gain Antennas in order to have a
   backup for the HGA for Earth Communication. One of the LGA antennas, the
   LGA PZ is placed in the +Z panel of the S/C whilst the other one, LGA MZ
   is placed in the -Z panel of the S/C in order to have a better coverage.

   According to [4] the SOLO LGA PZ Unit reference frame -- SOLO_LGA_PZ -- is
   defined as follows:

      -  +X axis is aligned with the boresight of the LGA,

      -  +Y axis is perpendicular to the waveguide interface plane, orientated
         from the waveguide interface plane towards the LGA.

      -  +Y completes the right-handed frame,

      -  the origin of this frame is on the centre of the Unit Reference Hole
         (URH) of the LGA PZ, at the interface plane with the S/C bracket.
         The coordinates in meters of the origin w.r.t SOLO_SRF frame are:

            ( x, y, z ) =  ( 0.85351, -0.64433, 1.18275 )

   The SOLO_HGA_URF frame is defined as a fixed offset frame relative to the
   SOLO_MRF frame. The following rotation matrix from [4] is used to define
   the fixed offset =

                         | -0.707107  -0.328179  0.626337 |
      M               =  |  0.663060   0         0.748566 |
       PRF -> LGA_PZ     | -0.245666   0.944615  0.217600 |


   According to [4] the SOLO LGA MZ Unit reference frame -- SOLO_LGA_MZ -- is
   defined as follows:

      -  +X axis is aligned with the boresight of the LGA,

      -  +Y axis is perpendicular to the waveguide interface plane, orientated
         from the waveguide interface plane towards the LGA.

      -  +Y completes the right-handed frame,

      -  the origin of this frame is on the centre of the Unit Reference Hole
         (URH) of the LGA MZ, at the interface plane with the S/C bracket.
         The coordinates in meters of the origin w.r.t SOLO_SRF frame are:

            ( x, y, z ) =  ( 1.94725, 0.5581, -1.46296 )

   The SOLO_HGA_URF frame is defined as a fixed offset frame relative to the
   SOLO_MRF frame. The following rotation matrix from [4] is used to define
   the fixed offset =

                         |  0.906308    0.171894  -0.386081 |
      M               =  |  0.186340   -0.982485   0        |
       PRF -> LGA_MZ     | -0.379320   -0.071936  -0.922465 |


   Here, we need the rotation matrices from the SOLO_LGA_PZ and SOLO_LGA_MZ
   frames to the S/C frame, and hence the inverse of the above matrices, which
   is the same as the transpose for rotation matrices. This is incorporated
   by the frame definitions below.

   \begindata

      FRAME_SOLO_LGA_PZ            = -144020
      FRAME_-144020_NAME           = 'SOLO_LGA_PZ'
      FRAME_-144020_CLASS          =  4
      FRAME_-144020_CLASS_ID       = -144020
      FRAME_-144020_CENTER         = -144
      TKFRAME_-144020_RELATIVE     = 'SOLO_SRF'
      TKFRAME_-144020_SPEC         = 'MATRIX'
      TKFRAME_-144020_MATRIX       = ( -0.707107,  -0.328179,  0.626337,
                                       0.663060,   0       ,  0.748566,
                                       -0.245666,   0.944615,  0.217600 )

      FRAME_SOLO_LGA_MZ            = -144021
      FRAME_-144021_NAME           = 'SOLO_LGA_MZ'
      FRAME_-144021_CLASS          =  4
      FRAME_-144021_CLASS_ID       = -144021
      FRAME_-144021_CENTER         = -144
      TKFRAME_-144021_RELATIVE     = 'SOLO_SRF'
      TKFRAME_-144021_SPEC         = 'MATRIX'
      TKFRAME_-144021_MATRIX       = (  0.906308,    0.171894,  -0.386081,
                                        0.186340,   -0.982485,   0       ,
                                       -0.379320,   -0.071936,  -0.922465 )

   \begintext


SOLO Medium Gain Antenna Frames:
--------------------------------

   SOLO is equipped with an articulated Medium Gain Antenna used as a backup
   of the HGA.

   The MGA_URF frame is used to define the position and orientation of the
   MGA-root relative to the SOLO PRF frame, used to provide a reference for
   the definition of the MGA inertial properties, and used to define the
   deformation of the MGA from its nominal geometry.

   The SOLO High Gain Antenna is attached to the +Z panel of the S/C bus
   in the corner with the -X panel by a gimbal providing one degree of
   freedom and it articulates during flight to track Earth.

   According to [4] the SOLO HGA Unit reference frame -- SOLO_MGA_URF -- is
   defined as follows:

      -  +X axis is aligned with the S/C +X axis,

      -  +Y axis is aligned with the S/C +Y axis, and is also aligned with
         axis of rotation of the MGA hinge.

      -  +Z completes the right-handed frame and is aligned with the
         S/C +Z axis,

      -  the origin of this frame is on the following coordinates in meters
         w.r.t SOLO_SRF frame are:

            ( x, y, z ) =  ( 0.5405, -0.3569, 1.3337 )

   The SOLO_MGA_URF frame is defined as a fixed offset frame relative to the
   SOLO_MRF frame.

   \begindata

      FRAME_SOLO_MGA_URF              = -144030
      FRAME_-144030_NAME              = 'SOLO_MGA_URF'
      FRAME_-144030_CLASS             =  4
      FRAME_-144030_CLASS_ID          = -144030
      FRAME_-144030_CENTER            = -144
      TKFRAME_-144030_RELATIVE        = 'SOLO_SRF'
      TKFRAME_-144030_SPEC            = 'ANGLES'
      TKFRAME_-144030_UNITS           = 'DEGREES'
      TKFRAME_-144030_AXES            = (     3,      2,      1 )
      TKFRAME_-144030_ANGLES          = (   0.0,    0.0,    0.0 )

   \begintext


MGA Moveable Reference Frames:
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

   DISCLAIMER: The MGA Moveable Referenfe frames although defined are not
   yet implemented in the Solar Orbiter SPICE Kernel Dataset (CK files
   are not being generated).

   To incorporate rotations in the gimbal the MGA frame chain includes
   three frames: SOLO_MGA_EL and SOLO_MGA_MRF.

   The first frame is defined as CK-based frame and is co-aligned with the
   SOLO_MGA_MRF frame in the zero gimbal position. In a non-zero position the
   SOLO_MGA_EL is rotated from the MGA MRF frame by an elevation angle
   about -Y.

    The rotation is defined by the elevation of the MGA. The elevation is the
   angle of positive rotation of the MGA around the -Y axis of the
   SOLO_MGA_MRF frame hence the elevation angle is the angle in between the
   +X axis of the SOLO_MGA_MRF frame and the +X axis of the SOLO_SRF frame.

   The range of elevation is 0 to +210 degrees. The value of that provides the
   nominal boresight of the MGA is 22 degrees. Note that the MGA safe position
   corresponds to an elevation of 0.5 degree.

   According to [4] the SOLO optical reference frame -- SOLO_HGA_MRF -- is
   defined as follows:

      -  +X axis is aligned with the boresight of the undeformed HGA.

      -  +Y axis is aligned with the S/C +Y axis,

      -  +Z completes the right-handed frame,

      -  the origin of this frame is defined as the orthogonal projection of
         the boresight axis onto the rotation axis of the elevation mechanism.


   This set of keywords defines the HGA frame as a CK frame:

   \begindata

      FRAME_SOLO_MGA_EL                = -144031
      FRAME_-144031_NAME               = 'SOLO_MGA_EL'
      FRAME_-144031_CLASS              =  3
      FRAME_-144031_CLASS_ID           = -144031
      FRAME_-144031_CENTER             = -144
      CK_-144031_SCLK                  = -144
      CK_-144031_SPK                   = -144

      FRAME_SOLO_MGA_MRF               = -144032
      FRAME_-144032_NAME               = 'SOLO_MGA_MRF'
      FRAME_-144032_CLASS              =  4
      FRAME_-144032_CLASS_ID           = -144032
      FRAME_-144032_CENTER             = -144
      TKFRAME_-144032_RELATIVE         = 'SOLO_MGA_EL'
      TKFRAME_-144032_SPEC             = 'ANGLES'
      TKFRAME_-144032_UNITS            = 'DEGREES'
      TKFRAME_-144032_AXES             = (   2,       1,       3     )
      TKFRAME_-144032_ANGLES           = (   0.000,   0.000,   0.000 )

   \begintext


Instrument Boom Frames:
-----------------------

   The Solar Orbiter deployable boom function is to support and deploy four
   instruments which, due to their sensitivity to magnetic fields, need to be
   placed far from the electromagnetic disturbances generated by the satellite.
   The total length of the deployed boom is 4.4 meters. The boon consists on
   two segments: an inboard segment and an outboard segment joint by a hinge.
   In a nominal scenario the inboard and the outboard segments are co-aligned.

   We only consider the Instrument Boom on its fully deployed configuration
   and we define a reference frame for the inboard segment of the boom and
   another one for the outboard segment of the boom. Nominally both are
   co-aligned.

   According to [4] the SOLO Instrument Boom Inboard reference frame
   -- SOLO_INS_BOOM_IB -- is defined as follows:

      -  +Z axis is perpendicular to the rotation axis of the instrument boom,
         pointing away from the boom axis;

      -  +X axis points towards the the 1st rigid element of the boom
         deployment mechanism between the S/C body and the 1st rigid
         element of the boom;

      -  +Y axis completes the right-handed frame;

      -  the origin of this frame is at the centre of the Unit Reference Hole
         (URH) of the +Y-Z tripod, at the interface plane with the spacecraft
         structure.


    According to [4] the SOLO Instrument Boom Outboard reference frame
    -- SOLO_INS_BOOM_OB -- is defined as follows:

      -  +Z axis is perpendicular to the rotation axis of the instrument boom,
         pointing away from the boom axis;

      -  +X axis points towards the the 1st rigid element of the outboard boom
         deployment mechanism between the boom hinge and the 1st rigid
         element of the outboard boom;

      -  +Y axis completes the right-handed frame;

      -  the origin of this frame is at the centre of the instrument boom
         hinge.


   These diagrams illustrate the SOLO_INS_BOOM frame:

   -Y S/C side (Science deck side) view:
   -------------------------------------

                                                               |
                                                               |
                                                               |
                                                               |
                                                               |
                                                               |
                                                               |
                                                               |
                                     +Yinsb                    |
                                      __--x.         __..|     |
                    +Xinsb      __--''     \'.  .o.----..|     |      | |
                          <__''==-   +Zsrf \.'---------------------. |=|
                        __--             ^  >\                      |=| |
                  __--'' ()              |    \                     | |=|
            __--''==-                    |.  | V                    | | |
       __--''                            | \+Zinsb                  | | |
     >|__|                               |  '|                      | | |
                                         |   |                      | | |
                                  +Ysrf  x-----------> +Xsrf        | | |
                                         |   |                      | | |
                                         |  .|                      | | |
                                         | / |                      | | |
                                         .'  |                      | | |
                                             |                      | |=|
                                            >|                      |=| |
                                            o:______________________: |=|
                                           //      /        | \       | |
                                          //      /_________|__\
                                         ||                 |
                                         ||                 |
               +Ysrf and +Yinsb       '. ||                 |
                are into the         .| |||                 |
                page.              -: | |'/                 H
                                     '| |                   |
                                      .'                    |
                                                            |



   -Z Instrument Boom side view:
   -----------------------------

                     / \.---./ \
                     \.'     './     SWA Electrons
                      |       |     Analysers Systems
                      '-------'          (EAS)
                         | |
                         | |
                         | |
                         | |
                        .---.
                        |   |     MAG Outboard Sensor
                        |   |            (OBS)
                        '---'
                         | |
                         | |
                         | |
                         |^| +Xinsbob
                         |||
                         |||
                        / ||
                +Zinsbob\_o-------->
                         | |      +Yinsbob
                         | |
                         |_|
                        |___|    RPW Magnetic Search Coil
                         | |             (SCM)
                         | |
                         ~~~
                         ~~~
                         | |
                         | |
                        .---.
                        |   |      MAG Inboard Sensor
                        |   |             (IBS)
                        '---'
                         | |
                         | |
                         |^| +Xinsbib
                         |||
                         |||
                         |||
                          o-------->            +Zinsbib and +Zinsbob
                     +Zinsbib     +Yinsbib       are out of the page.


   The SOLO_BOOM frames are defined as CK-based frame and the SOLO_INS_BOOM_IB
   and SOLO_INS_BOOM_OB have a rotation matrix w.r.t. the SOLO_SRF specified
   in the BOOM inboard and outboard CK files:

      solo_ANC_soc-sc-iboom-ck_YYYYMMDD-YYYYMMDD_VNN.bc
      solo_ANC_soc-sc-oboom-ck_YYYYMMDD-YYYYMMDD_VNN.bc

         respectively, where

            YYYYMMDD   start and finish dates of the CK coverage;

            NN         version of the kernel


   The Nominal Rotation Matrix for both segments of the boom specified in [4]
   is:

                   |  cos(T0+T)*cos(P)  cos(T0+T)*sin(P) -sin(T0+T) |
      M          = | -sin(P)            cos(P)            0.0       |
       PRF -> BOOM |  sin(T0+T)*cos(P)  sin(T0+T)*sin(P)  cos(T0+T) |

   where the T (theta), T0 (theta zero) and P (Phi) angles are defined as
   follows:


    -Y S/C side (Stowed INS BOOM):        +Z S/C side (Stowed INS BOOM):
    ------------------------------        ------------------------------

        T0                                         P
      .      |                                   '  .-|
         .-''                                      '
       .'    |                                    '   |
             .--------------.                      --------------.
        .    |     +Xsrf    |                      '  |          |
             |       ^      |                     |              |
         .   |       |      |                     | ' |          |
             |       |      |                     |              |
          .  |   <---x      |                   ins  '|          | +Xsrf
     ins     | +Zsrf        |                  boom              |  ^
    boom   . |              |                 stowed  '          |  |
   stowed    '--------------'                     '-- -----------.  |
            .|                                       \'/        <---o
             O                                        O       +Ysrf


    -Y S/C side (Deployed INS BOOM):
    --------------------------------

                                T0
                              .      |
                                 .-''
                               .'    |
                            .        .--------------.
                           '    .    |     +Xsrf    |
                        .            |       ^      |
                   T             .   |       |      |
                       '             |       |      |
                     .            .  |   <---x      |
                                     | +Zsrf        |
                    '              . |              |
                   .                 '--------------'
                                    .|
                   '            .. --O
                    .  .. -- ''
              .. -- ''
                    ins boom deployed

   for

      T0 =    -1 degree;
      T  =  -195 degrees;
      P  =     4 degrees;

   the resulting Nominal Rotation Matrix specified in [4] is:

                         | -0.958920 -0.067054 -0.275637 |
      M               =  | -0.069756  0.997564  0.0      |
       PRF -> BOOM       |  0.274965  0.019227 -0.961261 |


   This Rotation Matrix is described here only as reference. Please note that
   the implemented one is described in the INS_BOOM CK files. More details can
   be found in the CK files comment area.

   This set of keywords defines the SOLO_INS_BOOM_IB and SOLO_INS_BOOM_OB
   frames as CK frames:

   \begindata

      FRAME_SOLO_INS_BOOM_IB           = -144041
      FRAME_-144041_NAME               = 'SOLO_INS_BOOM_IB'
      FRAME_-144041_CLASS              =  3
      FRAME_-144041_CLASS_ID           = -144041
      FRAME_-144041_CENTER             = -144
      CK_-144041_SCLK                  = -144
      CK_-144041_SPK                   = -144

      FRAME_SOLO_INS_BOOM_OB           = -144042
      FRAME_-144042_NAME               = 'SOLO_INS_BOOM_OB'
      FRAME_-144042_CLASS              =  3
      FRAME_-144042_CLASS_ID           = -144042
      FRAME_-144042_CENTER             = -144
      CK_-144042_SCLK                  = -144
      CK_-144042_SPK                   = -144

   \begintext


Star Trackers Frames:
---------------------

   Solar Orbiter has two Star Trackers -- STR-1 and STR-2 -- with the same
   orientation and the same specifications the only difference being their
   origin in the S/C interface plane.

   The Star Trackers (STR) frames -- SOLO_STR-1 and SOLO_STR-2 --, are defined
   as follows (from [4]):

      -  +Z axis is perpendicular to the mechanical interface with the S/C
         and pointing towards the STR baffle; it is normal to the +Z SOLO_SRF
         axis;

      -  +X axis is in the plane of the mechanical interface with the S/C;

      -  +Y completes the right-handed frame;

      -  the origin of this frame is at the centre of the Unit Reference
         Hole (URH) of the Star Trackers; STR-1 and STR-2.


   This diagram illustrates the SOLO_STR +Z axis:

   -Z S/C side view:
   -----------------

                                            /\/\/
                                              H
                                              H
                                              H
                                              H
                                              |
                                              |
                                              |
                                              0
                                         '.  .----------------------. |=|
                                +Zstr  . | . |                      |=| |
                                    <--------|                      | | |
     >________________________________| ||   |                      | |=|
     >--------------------------------| || x------------->          | | |
                                      | || | +Zsrf      +Xsrf       |=| |
                                      .'|| | |                      | |=|
                                         .'| '----------------------' | |
                                           |  0             |
                                           |  |             |
                                           |  |             |
               +Zsrf is out of             V  |             |
                  the page                +Ysrf             |
                                              H
                                              H
                                              H
                                            /\/\/


   The SOLO_STR-1 and SOLO_STR-2 frames are defined as a fixed offset frame
   relative to the SOLO_SRF frame. The following rotation matrix from [4] is
   used to define the fixed offset:

                    |  -0.3100  0.6356 -0.7071 |
      M        =    |   0.3100 -0.6355 -0.7071 |
       STR -> PRF   |  -0.8988 -0.4384  0.0    |

   \begindata

      FRAME_SOLO_STR-1             = -144051
      FRAME_-144051_NAME           = 'SOLO_STR-1'
      FRAME_-144051_CLASS          =  4
      FRAME_-144051_CLASS_ID       = -144051
      FRAME_-144051_CENTER         = -144
      TKFRAME_-144051_RELATIVE     = 'SOLO_SRF'
      TKFRAME_-144051_SPEC         = 'MATRIX'
      TKFRAME_-144051_MATRIX       = ( -0.3100,  0.6356, -0.7071,
                                        0.3100, -0.6355, -0.7071,
                                       -0.8988, -0.4384,  0.0    )

      FRAME_SOLO_STR-2             = -144052
      FRAME_-144052_NAME           = 'SOLO_STR-2'
      FRAME_-144052_CLASS          =  4
      FRAME_-144052_CLASS_ID       = -144052
      FRAME_-144052_CENTER         = -144
      TKFRAME_-144052_RELATIVE     = 'SOLO_SRF'
      TKFRAME_-144052_SPEC         = 'MATRIX'
      TKFRAME_-144052_MATRIX       = ( -0.3100,  0.6356, -0.7071,
                                        0.3100, -0.6355, -0.7071,
                                       -0.8988, -0.4384,  0.0    )

   \begintext


SOLO Solar Arrays Frames:
--------------------

   SOLO solar arrays are articulated (having one degree of freedom),
   therefore the Solar Array frames, SOLO_SA+Y and SOLO_SA-Y, are
   defined as CK frames with their orientation given relative to
   SOLO_SA+Y_ZERO and SOLO_SA-Y_ZERO respectively.

   SOLO_SA+Y_ZERO and SOLO_SA-Y_ZERO are two ``fixed-offset'' frames,
   defined with respect to SOLO_SRF, as follows:

      -  +Y is parallel to the longest side of the array, positively
         oriented from the yoke to the end of the wing;

      -  +Z is aligned to the spacecraft bus +Z;

      -  +X completes the right-handed frame.

      -  the origin of the frame is located at the yoke geometric
         center.


   Both Solar Array frames (SOLO_SA+Y and SOLO_SA-Y) are defined as
   follows:

      -  +Y is parallel to the longest side of the array, positively oriented
         from the yoke to the end of the wing;

      -  +Z is normal to the solar array plane, the solar cells facing +Z;

      -  +X completes the right-handed frame;

      -  the origin of the frame is located at the yoke geometric center.


   The axis of rotation is parallel to the Y axis of the spacecraft and the
   solar array frames. Please note that in all the diagrams of this file the
   Solar Arrays are rotated 90 degrees.

   This diagram illustrates the JUICE_SA+Y and JUICE_SA-Y frames:


   +X S/C side (Heat Shield side) view:
   ------------------------------------

                                        |  ANT 1
                                        |
                                        |
                                        |
                                        |
                                        |
                                        |
                                        H
                                        |
                                        |
                                        |
                                       .O.
                             .--------------------.
                             |                    |
                  +Zsa+y_zero|         . __       |+Zsa-y_zero
   .__  _________.          ^|    oO    |__|      |^          ,_________  __.
   |  \ \        |\         ||         ^ +Zsrf    ||         /|        / /  |
   |  / /        |\\        ||         |          ||        //|        \ \  |
   |  \ \        | \\_______||         |          ||______//  |        / /  |
   |  +Ysa+y_zero<----------x|         |          |o---------->+Ysa-y_zero  |
   |  \ \        |//         |         o--------> |         \\|        / /  |
   |  / /        |/          |      +Xsrf     +Ysrf          \|        \ \  |
   '-'  '--------'           |                    |           '---------' '-'
                             |                    |
                             |                    |
                             |                    |
                             |                    |           +Xsrf is out
                             .____________________.            of the page.
                           .'    /____________\    '.
                         .'            ||            '.
                       .'              ||              '.
                     .'              .-''-.              '.
                   .'               /      \               '.
                 .'                |        |                '.
               .'                   \      /                   '.
     ANT 2   .'                      `-..-'                      '.  ANT 3
           .'                                                      '.
         .'                                                          '.


   These sets of keywords define solar array frames:

   \begindata

      FRAME_SOLO_SA+Y_ZERO             = -144014
      FRAME_-144014_NAME               = 'SOLO_SA+Y_ZERO'
      FRAME_-144014_CLASS              =  4
      FRAME_-144014_CLASS_ID           = -144014
      FRAME_-144014_CENTER             = -144015
      TKFRAME_-144014_RELATIVE         = 'SOLO_SRF'
      TKFRAME_-144014_SPEC             = 'ANGLES'
      TKFRAME_-144014_UNITS            = 'DEGREES'
      TKFRAME_-144014_AXES             = (      3,      2,     1 )
      TKFRAME_-144014_ANGLES           = (    0.0,    0.0,   0.0 )

      FRAME_SOLO_SA+Y                  = -144015
      FRAME_-144015_NAME               = 'SOLO_SA+Y'
      FRAME_-144015_CLASS              =  3
      FRAME_-144015_CLASS_ID           = -144015
      FRAME_-144015_CENTER             = -144015
      CK_-144015_SCLK                  = -144
      CK_-144015_SPK                   = -144015

      FRAME_SOLO_SA-Y_ZERO             = -144016
      FRAME_-144016_NAME               = 'SOLO_SA-Y_ZERO'
      FRAME_-144016_CLASS              =  4
      FRAME_-144016_CLASS_ID           = -144016
      FRAME_-144016_CENTER             = -144017
      TKFRAME_-144016_RELATIVE         = 'SOLO_SRF'
      TKFRAME_-144016_SPEC             = 'ANGLES'
      TKFRAME_-144016_UNITS            = 'DEGREES'
      TKFRAME_-144016_AXES             = (     3,      2,     1   )
      TKFRAME_-144016_ANGLES           = (   0.0,  180.0,   180.0 )

      FRAME_SOLO_SA-Y                  = -144017
      FRAME_-144017_NAME               = 'SOLO_SA-Y'
      FRAME_-144017_CLASS              =  3
      FRAME_-144017_CLASS_ID           = -144017
      FRAME_-144017_CENTER             = -144017
      CK_-144017_SCLK                  = -144
      CK_-144017_SPK                   = -144017

   \begintext



SOLO Instrument Frames
------------------------------------------------------------------------

   In order to incorporate a mechanism to account for the calibration history
   of a given remote sensing instrument boresight a CK-based frame is
   incorporated, the Instrument Detector Centre frame, and a fixed-offset
   frame relative to the Detector Centre frame, the Instrument Optical Axis is
   defined.

   The Instrument Detector Centre frame (ILS) is defined equivalent to the
   center of the FoV for a non-subfielded image (with the center of the
   detector projected onto the sky) whereas the Optical Axis frame is defined
   by the pixel position that projects onto the sky from the flat detector
   with least distortion (using a TAN projection, this corresponds to the
   reference pixel in FITS). The following scheme depicts the usage of these
   frames:

      FOF ----------------> Detector Centre -------------------> Ref-pixel
            calibrated/    (Instrument Line  fixed at launch, (Optical Axis)
            update-able        of sight)    typically identity
     Usage:
          needed for "pointing the FoV"
      |<---------------------------------->|

          needed for correcting "raw" image FITS into FITS with proper
          world coordinates
      |<---------------------------------------------------------------->|


   Nominally both frames are co-aligned and the FITS reference pixel
   corresponds exactly to the middle of the detector. The _OPT frames
   provide the possibility to define the centre of the (e.g.) TAN projection
   for those Remote Sensing instruments that can calibrate this. For those
   instruments where this cannot be calibrated the _OPT access will be defined
   co-aligned with the detector centre (and then the reference pixel used in
   FITS should also be detector-centred). Current assumption is that _OPT
   frame alignment is only  determined pre-launch (where it is determined at
   all).

   Please note that the ILS frames follow the "LIF" frames convention as
   defined in [5]. The idea of the LIF frames is to have a common convention
   on the detector frames in terms of which direction the frame axis are in.
   Because of this the ILS frames are defined according to the LIF convention,
   and ignoring whatever XYZ convention that each individual instrument may
   use at unit level. The LIF frames have a +X that points away from the Sun*.
   This is a consequence of defining a right handed frame consistent with the
   normal conventions of FITS axes for Solar Physics.

   The Solar Orbiter SOC needs this for the production of Low Latency Data
   because they will use SPICE as part of the process of converting FITS
   output in the instrument frame into properly WCS referenced FITS, and since
   the instrument-delivered pipelines will output the initial FITS according
   to LIF convention, it is needed that s what SPICE uses it this way as well.

   The strategy followed in this FK is to incorporate a CK frame per each
   sensor of every instrument.


   (*) Except for SOLOHI; +X axis is rotated 25 degrees around the +Z axis
       from the anti-parallel Sun direction with nominal attitude


EPD Frames:
------------------------------------------------------------------------

   This section of the file contains the definitions of the Energetic
   Particle Detector (EPD) instrument frames.


EPD Frame Tree:
~~~~~~~~~~~~~~~

   The diagram below shows the EPD frame hierarchy.


                           "J2000" INERTIAL
                           ----------------
                                  |
                                  |<-ck
                                  |
                                  V
                              "SOLO_SRF"
           +---------------------------------------------+
           |          |           |                      |
           |<-fixed   |<-fixed    |<-fixed               |<-fixed
           |          |           |                      |
           V          |           |                      V
    "SOLO_EPD_STEP"   |           |                 "SOLO_EPD_SIS"
   ----------------   |           |         +--------------------------+
                      V           |         |                          |
         "SOLO_EPD_EPT-HET_MY"    |         |<-fixed            fixed->|
         ---------------------    |         |                          |
                                  |         V                          V
                                  |   "SOLO_EPD_SIS_SW"      "SOLO_EPD_SIS_ASW"
                                  |   -----------------      ------------------
                                  V
                        "SOLO_EPD_EPT-HET_PY"
                        ---------------------


EPD SupraThermal Electron and Proton (STEP) Frame:
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

   The EPD SupraThermal Electron and Proton (STEP) frame -- SOLO_EPD_STEP --,
   is defined as follows:

      -  +Z axis is perpendicular to the interface plane with the S/C,
         pointing away from the S/C;

      -  +X axis is in the interface plane with the S/C, aligned with the
         boresight of the STEP sensor;

      -  +Y completes the right-handed frame;

      -  the origin of this frame is at the centre of the Unit Reference
         Hole (URH) of STEP, at the interface plane with the S/C structure.


   This diagram illustrates the SOLO_EPD_STEP frame:

   -Z S/C side view:
   -----------------

                                            /\/\/
                                              H
                                              H
                                              H
                                            +Zstep          |
                                              ^             |
                                               \           > +Xstep
                                              | \      . '
                                              0  \ . '   ) 35 deg     | |
                                         '.  .----x-----------------. |=|
                                      '. | . |                      |=| |
                                      | ||  '|                      | | |
     >________________________________| ||   |                      | |=|
     >--------------------------------| || x------------->          | | |
                                      | || | +Zsrf      +Xsrf       |=| |
                                      .'|| | |                      | |=|
                                         .'| '----------------------' | |
                                           |  0             |
                                           |  |             |
                                           |  |             |
               +Zsrf is out of             V  |             |
                the page and              +Ysrf             |
               +Ystep is into                 H
                the page.                     H
                                              H
                                            /\/\/


   The SOLO_EPD_STEP frame is defined as a fixed offset frame relative to the
   SOLO_SRF frame. The following rotation matrix from [4] is used to define
   the fixed offset =

                    |  cos(35)  -sin(35)   0.0 |    |  0.8192 -0.5736 0.0 |
      M         =   |      0.0       0.0   1.0 | =  |  0.0     0.0    1.0 |
       PRF -> STEP  | -sin(35)  -cos(35)   0.0 |    | -0.5736 -0.8192 0.0 |


   \begindata

      FRAME_SOLO_EPD_STEP          = -144100
      FRAME_-144100_NAME           = 'SOLO_EPD_STEP'
      FRAME_-144100_CLASS          =  4
      FRAME_-144100_CLASS_ID       = -144100
      FRAME_-144100_CENTER         = -144
      TKFRAME_-144100_RELATIVE     = 'SOLO_SRF'
      TKFRAME_-144100_SPEC         = 'MATRIX'
      TKFRAME_-144100_MATRIX       = (  0.819152, -0.573576,       0.0,
                                        0.0,            0.0,       1.0,
                                       -0.573576, -0.819152,       0.0 )

   \begintext


EPD Suprathermal Ion Spectrograph (SIS) Frames:
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

   The EPD Suprathermal Ion Spectrograph (SIS) Unit Reference frame
   -- SOLO_EPD_SIS --, is on the -Y side panel of the S/C is defined
   as follows:

      -  +X axis is anti-parallel to the SOLO_SRF +X axis,

      -  +Y axis is anti-parallel with the SOLO_SRF +Z axis

      -  +Z axis completes the right-handed frame and is nominally
         anti-parallel to the SOLO_SRF +Y axis

      -  the origin of this frame is at At the centre of the Unit
         Reference Hole (URH) of the SIS instrument, at the interface
         plane with the S/C structure.


   The EPD SIS consists of two telescopes. The sunward telescope points
   30 degrees off the deck, looking over the heatshield. The antisunward
   telescope points 20 degrees of the deck looking out into deep space [9].

   According to [4] the SOLO EPD SIS Sunward (SW) and the SOLO EPD SIS
   antisunward (ASW) -- SOLO_EPD_SIS_SW and SOLO_EPD_SIS_ASW -- are
   defined as follows:

      -  +X axis is aligned with the boresight of the telescope;

      -  +Y axis is perpendicular to the waveguide interface plane, orientated
         from the waveguide interface plane towards the LGA for SW and
         opposite for ASW;

      -  +Z completes the right-handed frame;

      -  the origin of this frame is on the centre of the Unit Reference Hole
         (URH) of the LGA MZ, at the interface plane with the S/C bracket.


   These diagram illustrate the SOLO EPD SIS frames:

   -Y S/C side (Science deck side) view:
   -------------------------------------

                                                             ~~~~~
                                                               |
                                      __--o.      _____..|     |
                                __--''     \'.  .o.----..|     |      | |
                            __''==-   +Zsrf \.'---------------------. |=|
                        __--             ^  >| <------o  +Zsis      |=| |
                  __--'' ()              | +Xsis      |             | |=|
            __--''==-                    |.  |        |             | | |
       __--''                            | \ |        |             | | |
     >|__|                               |  '|        V +Ysis       | | |
                                         |   |                      | | |
                                  +Ysrf  x-----------> +Xsrf        | | |
                                         |   |                      | | |
                                         |  .|                      | | |
                                         | / |                      | | |
                                         .'  |                      | | |
                                             |                      | |=|
                                            >|                      |=| |
                                            o:______________________: |=|
                                           //      /        | \       | |
                                          //      /_________|__\
                                         ||                 |
                                         ||                 |
               +Ysrf is into          '. ||                 |
                the page and         .| |||                 |
                                   -: | |'/                 H
                                     '| |                   |
                                      .'                    |
                                                            |


   +Z S/C side view:
   -----------------

                      +Zsw                +Zasw
                         ^                    ^
   .                    /                      \                    .  '
   / '  .  +Xsw        /                        \       +Xasw .  '
  30 deg   < .        /                          \        . >
  |            ' .   .                            .   . '  ) 20 deg
 - - - - - - - -  o/  ' .                    . '  \x   - - - - - - - - -
             +Ysw /     +Zsis            . '       \ +Yasw
                  ' .     ^   ' .    . '         . '         <--------o +Zsrf
                      ' . |     /    \       . '           +Xsrf      |
                        | |    /______\      |                        |
                        | |                  |                        |
              ----------- x---------> -------------------             V
                        +Ysis      +Xsis                            +Ysrf


   Since the SPICE frames subsystem calls for specifying the reverse
   transformation--going from the instrument or structure frame to the
   base frame--as compared to the description given above, the order of
   rotations assigned to the TKFRAME_*_AXES keyword is also reversed
   compared to the above text, and the signs associated with the
   rotation angles assigned to the TKFRAME_*_ANGLES keyword are the
   opposite from what is written in the above text.

   \begindata

      FRAME_SOLO_EPD_SIS               = -144110
      FRAME_-144110_NAME               = 'SOLO_EPD_SIS'
      FRAME_-144110_CLASS              =  4
      FRAME_-144110_CLASS_ID           = -144110
      FRAME_-144110_CENTER             = -144
      TKFRAME_-144110_RELATIVE         = 'SOLO_SRF'
      TKFRAME_-144110_SPEC             = 'MATRIX'
      TKFRAME_-144110_MATRIX           = ( -1.0,  0.0,  0.0,
                                            0.0,  0.0, -1.0,
                                            0.0, -1.0,  0.0 )

      FRAME_SOLO_EPD_SIS_ASW           = -144111
      FRAME_-144111_NAME               = 'SOLO_EPD_SIS_ASW'
      FRAME_-144111_CLASS              =  4
      FRAME_-144111_CLASS_ID           = -144111
      FRAME_-144111_CENTER             = -144
      TKFRAME_-144111_RELATIVE         = 'SOLO_EPD_SIS'
      TKFRAME_-144111_SPEC             = 'ANGLES'
      TKFRAME_-144111_UNITS            = 'DEGREES'
      TKFRAME_-144111_AXES             = (    2,       1,       3  )
      TKFRAME_-144111_ANGLES           = (   20,   0.000,   0.000  )

      FRAME_SOLO_EPD_SIS_SW            = -144112
      FRAME_-144112_NAME               = 'SOLO_EPD_SIS_SW'
      FRAME_-144112_CLASS              =  4
      FRAME_-144112_CLASS_ID           = -144112
      FRAME_-144112_CENTER             = -144
      TKFRAME_-144112_RELATIVE         = 'SOLO_EPD_SIS'
      TKFRAME_-144112_SPEC             = 'ANGLES'
      TKFRAME_-144112_UNITS            = 'DEGREES'
      TKFRAME_-144112_AXES             = (    2,       1,       3  )
      TKFRAME_-144112_ANGLES           = (  -30,   0.000,   180.0  )

   \begintext


EPD High Energy Telescope and Electron Proton Telescope (EPT-HET) Frames:
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

   EPT-HET has multiple view cones sharing a common electronics box and there
   are two identical EPT-HET units.

   EPD Electron Proton Telescope and High Energy Telescope (EPT-HET) consists
   of two sensor double-ended sensor heads, one pointing sun/anti-sunward
   (EPT-HET_MY), the other out of the ecliptic (EPT-HET_PY). Thus, EPT-HET
   has a total of four viewing directions.

   The EPT-HET frames -- SOLO_EPD_EPT-HET_MY and SOLO_EPD_EPT-HET_PY --, are
   defined as follows:

      -  +Z axis is perpendicular to the interface plane with the S/C,
         pointing away from the S/C;

      -  +X axis is in the interface plane with the S/C, aligned with the
         boresight of the given EPT-HET sensor;

      -  +Y completes the right-handed frame;

      -  the origin of this frame is at the centre of the Unit Reference
         Hole (URH) of EPT-HET, at the interface plane with the S/C structure.

   This diagram illustrates the SOLO_EPD_EPT-HET_MY frame:

   +Z S/C side view:
   -----------------

                                            /\/\/
                                              H
                                              H
                                              H
                                              |             |
                                         +Ysrf|             |
                                           ^  |             |
                                           |  |             |
                                           |  0             |         | |
                                         '.| .----------------------. |=|
                                      '. | | |                      |=| |
                                      | || |'|                      | | |
     >___________________________________ _|__________..|           | |=|
     >-------------------------------------o--------------->        | | |
                                      | ||+Zsrf           +Xsrf     |=| |
                                      .'|| / |                      | |=|
                                         .'  '-o--------------------' | |
                                             .' '. ) alp    |
                                           .' |   '.        |
                                         .'   |     '.      |
               +Zsrf is out of          V     |       ' > +Xmy
                                    +Zmy      H
                                              H
                                              H
                                            /\/\/

   Reference [4] provides the following nominal angles to define the
   SOLO_EPD_EPT-HET_MY and SOLO_EPD_EPT-HET_PY frames:

      alp = 35      degrees
      the = 30      degrees
      phi = 17.6388 degrees


   The SOLO_EPD_HET_MY and SOLO_EPD_HET_PY frames are defined as a fixed
   offset frames relative to the SOLO_SRF frame. The following rotation
   matrices from [4] are used to define the fixed offset:


                     |  cos(alp)  -sin(alp)   0.0 |    |  0.8192 -0.5736 0.0 |
      M           =  |      0.0       0.0   1.0   | =  |  0.0     0.0    1.0 |
       PRF -> E-H_MY | -sin(alp)  -cos(alp)   0.0 |    | -0.5736 -0.8192 0.0 |


                      |   sin(phi) sin(the)*cos(phi) -cos(the)*cos(phi) |
      M            =  |  -cos(phi) sin(the)*sin(phi) -cos(the)*sin(phi) | =
       PRF -> E-H_PY  |        0.0          cos(the)           sin(the) |

                      |   0.303015   0.476493  -0.825310 |
                   =  |  -0.952986   0.151508  -0.262419 |
                      |   0.0        0.886025  0.5       |


   \begindata

      FRAME_SOLO_EPD_EPT-HET_MY    = -144121
      FRAME_-144121_NAME           = 'SOLO_EPD_EPT-HET_MY'
      FRAME_-144121_CLASS          =  4
      FRAME_-144121_CLASS_ID       = -144121
      FRAME_-144121_CENTER         = -144
      TKFRAME_-144121_RELATIVE     = 'SOLO_SRF'
      TKFRAME_-144121_SPEC         = 'MATRIX'
      TKFRAME_-144121_MATRIX       = (  0.819152, -0.573576,       0.0,
                                        0.0,            0.0,       1.0,
                                       -0.573576, -0.819152,       0.0 )


      FRAME_SOLO_EPD_EPT-HET_PY    = -144122
      FRAME_-144122_NAME           = 'SOLO_EPD_EPT-HET_PY'
      FRAME_-144122_CLASS          =  4
      FRAME_-144122_CLASS_ID       = -144122
      FRAME_-144122_CENTER         = -144
      TKFRAME_-144122_RELATIVE     = 'SOLO_SRF'
      TKFRAME_-144122_SPEC         = 'MATRIX'
      TKFRAME_-144122_MATRIX       = (   0.303015,   0.476493,  -0.825310,
                                        -0.952986,   0.151508,  -0.262419,
                                         0.0     ,   0.886025,  0.5       )

   \begintext


EUI Frames:
------------------------------------------------------------------------

   This section of the file contains the definitions of the Extreme
   Ultraviolet Imager (EUI) instrument frames.


EUI Frame Tree:
~~~~~~~~~~~~~~~

   The diagram below shows the EUI frame hierarchy.


                           "J2000" INERTIAL
                           ----------------
                                  |
                                  |<-ck
                                  |
                                  V
                              "SOLO_SRF"
                              ----------
                                  |
                                  |<-ck
                                  |
                                  V
                              "SOLO_FOF"
           +----------------------------------------------+
           |                      |                       |
           |<-ck                  |<-ck                   |<-ck
           |                      |                       |
           V                      V                       V
   "SOLO_EUI_FSI_ILS"   "SOLO_EUI_HRI_LYA_ILS"   "SOLO_EUI_HRI_EUV_ILS"
   ------------------   ----------------------   ----------------------
           |                      |                       |
           |<-fixed               |<-fixed                |<-fixed
           |                      |                       |
           V                      V                       V
   "SOLO_EUI_FSI_OPT"   "SOLO_EUI_HRI_LYA_OPT"   "SOLO_EUI_HRI_EUV_OPT"
   ------------------   ----------------------   ----------------------


EUI Sensors Line of Sight Frames:
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

   The EUI Sensors Line of Sight (ILS) frames -- SOLO_EUI_FSI_ILS,
   SOLO_EUI_HRI_LYA_ILS and SOLO_EUI_HRI_EUV_ILS -- are defined as follows:

      -  +X axis is anti-parallel with the Sun direction with nominal
         attitude; anti-parallel with the sensor boresight,

      -  +Y axis is nominally anti-parallel to the SOLO_SRF +Y axis and is
         aligned with the detector rows,

      -  +Z axis completes the right-handed frame and is aligned with the
         detector rows,

      -  the origin of this frame is the geometrical center of the EUI
         Detector.

   This diagram illustrates the SOLO_EUI_*_ILS frames in nominal
   position:

   -Y S/C side (Science deck side) view:
   -------------------------------------

                                                               |
                                                               |
                                                               |
                                                               |
                                                               |
                                                               |
                                                               |
                                                               H
                                                               |
                                      __--o.      _____..|    +Zeui
                                __--''     \'.  .o.----..|     |^     | |
                            __''==-   +Zsrf \.'-----------------|---. |=|
                        __--             ^  >|                  |   |=| |
                  __--'' ()              |   |                  |   | |=|
            __--''==-                    |.  |                  |   | | |
       __--''                            | \ |    +Xeui<--------o   | | |
     >|__|                               |  '|                +Yeui | | |
                                         |   |                      | | |
                                  +Ysrf  x-----------> +Xsrf        | | |
                                         |   |                      | | |
                                         |  .|                      | | |
                                         | / |                      | | |
                                         .'  |                      | | |
                                             |                      | |=|
                                            >|                      |=| |
                                            o:______________________: |=|
                                           //      /        | \       | |
                                          //      /_________|__\
                                         ||                 |
                                         ||                 |
           +Ysrf is out of the        '. ||                 |
           page and +ZYeui is into   .| |||                 |
           the page.               -: | |'/                 H
                                     '| |                   |
                                      .'                    |
                                                            |


   The SOLO_EUI_*_ILS frames are defined as CK-based frames and
   SOLO_EUI_*_ILS have a rotation matrix  w.r.t. the SOLO_FOF specified
   in the EUI Instrument CK files:

      solo_ANC_soc-eui-fsi-ck_YYYYMMDD-YYYYMMDD_VNN.bc
      solo_ANC_soc-eui-hri-lya-ck_YYYYMMDD-YYYYMMDD_VNN.bc
      solo_ANC_soc-eui-hri-euv-ck_YYYYMMDD-YYYYMMDD_VNN.bc

         where

            YYYYMMDD   start and finish dates of the CK coverage;

            NN         version of the kernel


   The Nominal Rotation Matrix specified in [4] for the three sensors is:

                      | -1.0000000  0.0         0.0       |
      M            =  |  0.0       -1.0000000   0.0       |
       FOF -> EUI     |  0.0        0.0         1.0000000 |


   This Rotation Matrix is described here only as reference. Please note that
   the implemented one is described in the EUI CK files. More details can be
   found in the CK file comment area.

   This set of keywords defines the SOLO_EUI_*_ILS frames as a CK frame:

   \begindata

      FRAME_SOLO_EUI_FSI_ILS           = -144211
      FRAME_-144211_NAME               = 'SOLO_EUI_FSI_ILS'
      FRAME_-144211_CLASS              =  3
      FRAME_-144211_CLASS_ID           = -144211
      FRAME_-144211_CENTER             = -144
      CK_-144211_SCLK                  = -144
      CK_-144211_SPK                   = -144

      FRAME_SOLO_EUI_HRI_LYA_ILS       = -144221
      FRAME_-144221_NAME               = 'SOLO_EUI_HRI_LYA_ILS'
      FRAME_-144221_CLASS              =  3
      FRAME_-144221_CLASS_ID           = -144221
      FRAME_-144221_CENTER             = -144
      CK_-144221_SCLK                  = -144
      CK_-144221_SPK                   = -144

      FRAME_SOLO_EUI_HRI_EUV_ILS       = -144231
      FRAME_-144231_NAME               = 'SOLO_EUI_HRI_EUV_ILS'
      FRAME_-144231_CLASS              =  3
      FRAME_-144231_CLASS_ID           = -144231
      FRAME_-144231_CENTER             = -144
      CK_-144231_SCLK                  = -144
      CK_-144231_SPK                   = -144

   \begintext


EUI Sensors Optical Axis Frames:
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

   The EUI Instrument Sensors Optical Axis frames are nominally equivalent
   to the EUI Instrument Sensors Line of Sight frames.

   \begindata

      FRAME_SOLO_EUI_FSI_OPT           = -144212
      FRAME_-144212_NAME               = 'SOLO_EUI_FSI_OPT'
      FRAME_-144212_CLASS              =  4
      FRAME_-144212_CLASS_ID           = -144212
      FRAME_-144212_CENTER             = -144
      TKFRAME_-144212_RELATIVE         = 'SOLO_EUI_FSI_ILS'
      TKFRAME_-144212_SPEC             = 'MATRIX'
      TKFRAME_-144212_MATRIX           = ( 1.0, 0.0, 0.0,
                                           0.0, 1.0, 0.0,
                                           0.0, 0.0, 1.0 )

      FRAME_SOLO_EUI_HRI_LYA_OPT       = -144222
      FRAME_-144222_NAME               = 'SOLO_EUI_HRI_LYA_OPT'
      FRAME_-144222_CLASS              =  4
      FRAME_-144222_CLASS_ID           = -144222
      FRAME_-144222_CENTER             = -144
      TKFRAME_-144222_RELATIVE         = 'SOLO_EUI_HRI_LYA_ILS'
      TKFRAME_-144222_SPEC             = 'MATRIX'
      TKFRAME_-144222_MATRIX           = ( 1.0, 0.0, 0.0,
                                           0.0, 1.0, 0.0,
                                           0.0, 0.0, 1.0 )

      FRAME_SOLO_EUI_HRI_EUV_OPT       = -144232
      FRAME_-144232_NAME               = 'SOLO_EUI_HRI_EUV_OPT'
      FRAME_-144232_CLASS              =  4
      FRAME_-144232_CLASS_ID           = -144232
      FRAME_-144232_CENTER             = -144
      TKFRAME_-144232_RELATIVE         = 'SOLO_EUI_HRI_EUV_ILS'
      TKFRAME_-144232_SPEC             = 'MATRIX'
      TKFRAME_-144232_MATRIX           = ( 1.0, 0.0, 0.0,
                                           0.0, 1.0, 0.0,
                                           0.0, 0.0, 1.0 )

   \begintext


MAG Frames:
------------------------------------------------------------------------

   This section of the file contains the definitions of the Magnetometer
   (MAG) instrument frames.

   This section of the file contains the definitions of the Solar Orbiter
   Magnetometer (MAG) instrument frames. The Magnetometer will measure the DC
   magnetic field (in the bandwidth DC to 64Hz) in the S/C vicinity.


MAG Frame Tree:
~~~~~~~~~~~~~~~

   The diagram below shows the MAG frame hierarchy.


                           "J2000" INERTIAL
                            ---------------
                                  |
                                  |<-ck
                                  |
                                  V
                              "SOLO_SRF"
                              ----------
                                  |
                                  |<-ck
                                  |
                                  V
                            "SOLO_INS_BOOM"
                      +-----------------------+
                      |                       |
                      |<-fixed                |<-fixed
                      |                       |
                      V                       V
               "SOLO_MAG_IBS"          "SOLO_MAG_OBS"
               --------------          --------------


MAG Frames:
~~~~~~~~~~~

   The MAG experiment comprises two sensors mounted in the Solar Orbiter
   Magnetometer Boom at different distances from the boom's hinge. The
   innermost is called the in-board sensor (MAG IBS) and it is a fluxgate
   magnetometer. A second fluxgate magnetometer, called the out-board
   sensor (MAG OBS), is located approximately 3 meters away toward the end
   of the boom.

   Each of the sensor's frames -- SOLO_MAG_IBS and SOLO_MAG_OBS -- are
   nominally co-aligned with the instrument boom frames -- SOLO_INS_BOOM_IB
   and SOLO_INS_BOOM_OB -- respectively and are defined as follows:

      -  +Z axis is perpendicular to the rotation axis of the instrument boom,
         pointing away from the boom axis;

      -  +X axis points towards the deployment mechanism between the S/C body
         and the 1st rigid element of the boom;

      - +Y axis completes the right-handed frame;

      -  the origin of this frame is the centre of the Unit Reference Hole
         of the MAGOBS/MAGIBS instrument, but in the interface plane with the
         Instrument Boom.


   This diagram illustrates the MAG sensors' frames:

   -Z Instrument Boom side view:
   -----------------------------

                     / \.---./ \
                     \.'     './     SWA Electrons
                      |       |     Analysers Systems
                      '-------'          (EAS)
                         | |
                         | |
                         | |
                         | |
                        .---.
          +Yobs <---------x |    MAG Outboard Sensor
                        | | |            (OBS)
                        '-|-'
                         |||
                         |V| +Xobs
                         | |
                         | |
                         | |
                         |_|
                        /   \
                        \___/
                         | |
                         | |
                         |_|
                        |___|    RPW Magnetic Search Coil
                         | |             (SCM)
                         | |
                         ~~~
                         ~~~
                         | |
                         | |
                        .---.
          +Yibs <---------x |    MAG Inboard Sensor
                        | | |           (IBS)
                        '-|-'
                         |||
                         |V| +Xibs
                         |^| +Xinsb
                         |||                   +Zibs and +Zobs
                         |||                    are into the page.
                         |||                   +Zinsb is out
                          o-------->            of the page.
                     +Zinsb      +Yinsb

   \begindata

      FRAME_SOLO_MAG_IBS               = -144301
      FRAME_-144301_NAME               = 'SOLO_MAG_IBS'
      FRAME_-144301_CLASS              =  4
      FRAME_-144301_CLASS_ID           = -144301
      FRAME_-144301_CENTER             = -144
      TKFRAME_-144301_RELATIVE         = 'SOLO_INS_BOOM_IB'
      TKFRAME_-144301_SPEC             = 'MATRIX'
      TKFRAME_-144301_MATRIX           = ( -1.0,  0.0,  0.0,
                                            0.0, -1.0,  0.0,
                                            0.0,  0.0,  1.0 )

      FRAME_SOLO_MAG_OBS               = -144302
      FRAME_-144302_NAME               = 'SOLO_MAG_OBS'
      FRAME_-144302_CLASS              =  4
      FRAME_-144302_CLASS_ID           = -144302
      FRAME_-144302_CENTER             = -144
      TKFRAME_-144302_RELATIVE         = 'SOLO_INS_BOOM_OB'
      TKFRAME_-144302_SPEC             = 'MATRIX'
      TKFRAME_-144302_MATRIX           = ( -1.0,  0.0,  0.0,
                                            0.0, -1.0,  0.0,
                                            0.0,  0.0,  1.0 )

   \begintext


Metis Frames:
------------------------------------------------------------------------

   This section of the file contains the definitions of the Multi Element
   Telescope for Imaging and Spectroscopy (Metis) instrument frames.


Metis Frame Tree:
~~~~~~~~~~~~~~~~~

   The diagram below shows the PHI frame hierarchy.


                              "J2000" INERTIAL
                              ----------------
                                     |
                                     |<-ck
                                     |
                                     V
                                 "SOLO_SRF"
                                 ----------
                                     |
                                     |<-ck
                                     |
                                     V
                                 "SOLO_FOF"
            +------------------------------------------------+
            |                        |                       |
            |<-ck                    |<-ck                   |<-ck
            |                        |                       |
            V                        V                       V
   "SOLO_METIS_M0_TEL"      "SOLO_METIS_EUV_ILS"      "SOLO_METIS_VIS_ILS"
   -------------------      --------------------      --------------------
                                     |                       |
                                     |<-fixed                |<-fixed
                                     |                       |
                                     V                       V
                            "SOLO_METIS_EUV_OPT"      "SOLO_METIS_VIS_OPT"
                            --------------------      --------------------


Metis Sensors Line of Sight Frames:
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

   The Metis Sensors Line of Sight (ILS) frames -- SOLO_METIS_EUV_ILS,
   and SOLO_METIS_VIS_ILS -- definition differs from the one specified in [6]
   and is according to the indications provided in the ``SOLO Instrument
   Frames'' section. They are defined as follows:

      -  +X axis is anti-parallel with the Sun direction with nominal
         attitude; anti-parallel with the sensor boresight,

      -  +Y axis is nominally anti-parallel to the SOLO_SRF +Y axis and is
         aligned with the detector rows,

      -  +Z axis completes the right-handed frame and is aligned with the
         detector rows,

      -  the instrument Vertex is the centre of the M0 (occulting) mirror.

   This diagram illustrates the SOLO_METIS_*_ILS frames in nominal
   position:

   -Y S/C side (Science deck side) view:
   -------------------------------------

                                                               |
                                                               |
                                                               |
                                                               |
                                                               |
                                                               |
                                                               |
                                                               H
                                                               |
                                      __--o.      _____..|    +Zmetis
                                __--''     \'.  .o.----..|     |^     | |
                            __''==-   +Zsrf \.'-----------------|---. |=|
                        __--             ^  >|                  |   |=| |
                  __--'' ()              |   |                  |   | |=|
            __--''==-                    |.  |                  |   | | |
       __--''                            | \ |  +Xmetis<--------o   | | |
     >|__|                               |  '|                +Ymetis | |
                                         |   |                      | | |
                                  +Ysrf  x-----------> +Xsrf        | | |
                                         |   |                      | | |
                                         |  .|                      | | |
                                         | / |                      | | |
                                         .'  |                      | | |
                                             |                      | |=|
                                            >|                      |=| |
                                            o:______________________: |=|
                                           //      /        | \       | |
                                          //      /_________|__\
                                         ||                 |
                                         ||                 |
           +Ysrf is out of the        '. ||                 |
           page and +Ymetis is       .| |||                 |
           into the page.          -: | |'/                 H
                                     '| |                   |
                                      .'                    |
                                                            |


   The SOLO_METIS_*_ILS frames are defined as CK-based frames and
   SOLO_METIS_*_ILS have a rotation matrix  w.r.t. the SOLO_FOF specified
   in the EUI Sensor's CK files:

      solo_ANC_soc-metis-euv-ck_YYYYMMDD-YYYYMMDD_VNN.bc
      solo_ANC_soc-metis-vis-ck_YYYYMMDD-YYYYMMDD_VNN.bc

         where

            YYYYMMDD   start and finish dates of the CK coverage;

            NN         version of the kernel


   The Nominal Rotation Matrix specified in [4] for the three sensors is:

                      | -1.0000000  0.0         0.0       |
      M            =  |  0.0        0.0         1.0000000 |
       FOF -> Metis   |  0.0        1.0000000   0.0       |


   This Rotation Matrix is described here only as reference. Please note that
   the implemented one is described in the Metis CK files. More details can be
   found in the CK file comment area.

   This set of keywords defines the SOLO_METIS_*_ILS frames as a CK frame:

   \begindata

      FRAME_SOLO_METIS_EUV_ILS         = -144411
      FRAME_-144411_NAME               = 'SOLO_METIS_EUV_ILS'
      FRAME_-144411_CLASS              =  3
      FRAME_-144411_CLASS_ID           = -144411
      FRAME_-144411_CENTER             = -144
      CK_-144411_SCLK                  = -144
      CK_-144411_SPK                   = -144

      FRAME_SOLO_METIS_VIS_ILS         = -144421
      FRAME_-144421_NAME               = 'SOLO_METIS_VIS_ILS'
      FRAME_-144421_CLASS              =  3
      FRAME_-144421_CLASS_ID           = -144421
      FRAME_-144421_CENTER             = -144
      CK_-144421_SCLK                  = -144
      CK_-144421_SPK                   = -144

      FRAME_SOLO_METIS_M0_TEL          = -144431
      FRAME_-144431_NAME               = 'SOLO_METIS_M0_TEL'
      FRAME_-144431_CLASS              =  3
      FRAME_-144431_CLASS_ID           = -144431
      FRAME_-144431_CENTER             = -144
      CK_-144431_SCLK                  = -144
      CK_-144431_SPK                   = -144

   \begintext


Metis Sensors Optical Axis Frames:
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

   The Metis Instrument Sensors Optical Axis frames are nominally equivalent
   to the Metis Instrument Sensors Line of Sight frames.

   \begindata

      FRAME_SOLO_METIS_EUV_OPT         = -144412
      FRAME_-144412_NAME               = 'SOLO_METIS_EUV_OPT'
      FRAME_-144412_CLASS              =  4
      FRAME_-144412_CLASS_ID           = -144412
      FRAME_-144412_CENTER             = -144
      TKFRAME_-144412_RELATIVE         = 'SOLO_METIS_EUV_ILS'
      TKFRAME_-144412_SPEC             = 'MATRIX'
      TKFRAME_-144412_MATRIX           = ( 1.0, 0.0, 0.0,
                                           0.0, 1.0, 0.0,
                                           0.0, 0.0, 1.0 )

      FRAME_SOLO_METIS_VIS_OPT         = -144422
      FRAME_-144422_NAME               = 'SOLO_METIS_VIS_OPT'
      FRAME_-144422_CLASS              =  4
      FRAME_-144422_CLASS_ID           = -144422
      FRAME_-144422_CENTER             = -144
      TKFRAME_-144422_RELATIVE         = 'SOLO_METIS_VIS_ILS'
      TKFRAME_-144422_SPEC             = 'MATRIX'
      TKFRAME_-144422_MATRIX           = ( 1.0, 0.0, 0.0,
                                           0.0, 1.0, 0.0,
                                           0.0, 0.0, 1.0 )

   \begintext


Metis Telescope IEO-M0 Boom Frame:
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

   The telescope contains all the opto-mechanical elements and the detectors
   producing the Sun corona images in visible (VL) and UV light that reflects
   the occultor (technically the IEO-M0 alignment).

   The inverted external-occulter (IEO) consists of a circular aperture on the
   front face of the Solar Orbiter thermal shield. An internal spherical
   mirror (M0) rejects back the disk-light through the IEO. The part of Metis
   between the IEO and M0 is called optical boom and it is optically delimited
   by two stops: IEO, on the front face of the heat shield, and the annular
   aperture delimited internally by M0 and, externally, by the internal edge
   of the mirror M2. The M0 mirror has the task to reflect back into IEO
   aperture the light coming from the solar disk.

   The Metis Telescope IEO-M0 Boom Frame -- SOLO_METIS_IEO-M0 -- accounts for
   the alignment of the IEO-M0 Boom and is defined as follows:

      -  +X points from the Vertex outward through the centre of the IEO
         aperture (it is parallel with the instrument Line of Sight),

      -  +Y axis is aligned with the detector rows,

      -  +Y completes the right-handed frame and is aligned with the detector
         columns,

      -  the origin of this frame is the centre of the M0 (occulting) mirror.

   This diagram illustrates the SOLO_METIS_IEO-M0 frame in nominal position:

   -Y S/C side (Metis Optical Unit) view:
   --------------------------------------



                                    Telescope              ^ +Zieo-m0
      IEO                                                  |
       |                      ...-----------------------...|
       V          _____....'''                             |'--.
       .-----'''''                                         |    |
       |                                  +Xieo-m0 <-------o    | UV Camera
       '-----....._____                                +Yieo-m0 |
                       ''''---___                        __..--'
                                 """"""""""""""""""""""""            +Ymetis
                                ||                      || ^            ^
                              ------                   ----|            |
       \________________________________________________/  |            |
                                |                          |            |
                               Boom                     M0 Mirror       |
                                                                        |
                                                     +Xmetis <----------x
                                                                     +Zmetis


   The SOLO_METIS_IEO-M0 frame is defined as CK-based frame and it has a
   rotation matrix  w.r.t. the SOLO_FOF specified in the Metis IEO-MO CK file:

      solo_ANC_soc-metis-ieo-m0-ck_YYYYMMDD-YYYYMMDD_VNN.bc

         where

            YYYYMMDD   start and finish dates of the CK coverage;

            NN         version of the kernel


   The Nominal Rotation Matrix specified in [4] for the three sensors is:

                      | -1.0000000  0.0         0.0       |
      M            =  |  0.0        0.0         1.0000000 |
       FOF -> IEO-M0  |  0.0        1.0000000   0.0       |


   This Rotation Matrix is described here only as reference. Please note that
   the implemented one is described in the Metis CK files. More details can be
   found in the CK file comment area.

   This set of keywords defines the SOLO_METIS_IEO-M0 frames as a CK frame:

   \begindata

      FRAME_SOLO_METIS_IEO-M0          = -144430
      FRAME_-144430_NAME               = 'SOLO_METIS_IEO-M0'
      FRAME_-144430_CLASS              =  3
      FRAME_-144430_CLASS_ID           = -144430
      FRAME_-144430_CENTER             = -144
      CK_-144430_SCLK                  = -144
      CK_-144430_SPK                   = -144

   \begintext


PHI Frames:
------------------------------------------------------------------------

   This section of the file contains the definitions of the Polarimetric and
   Helioseismic Imager (PHI) instrument frames.


PHI Frame Tree:
~~~~~~~~~~~~~~~

   The diagram below shows the PHI frame hierarchy.


                              "J2000" INERTIAL
                               ---------------
                                     |
                                     |<-ck
                                     |
                                     V
                                 "SOLO_SRF"
                                 ----------
                                     |
                                     |<-ck
                                     |
                                     V
                                 "SOLO_FOF"
                          +--------------------+
                          |                    |
                          |<-ck                |<-ck
                          |                    |
                          V                    V
                   "SOLO_PHI_FDT_ILS"    "SOLO_PHI_HRT_ILS"
                   ------------------    -----------------
                          |                    |
                          |<-fixed             |<-fixed
                          |                    |
                          V                    V
                   "SOLO_PHI_FDT_OPT"    "SOLO_PHI_HRT_OPT"
                   ------------------    ------------------


PHI Sensors Line of Sight Frames:
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

   The PHI Sensors Line of Sight (ILS) frames -- SOLO_PHI_FDT_ILS and
   SOLO_PHI_HRT_ILS -- are
   defined as follows:

      -  +X axis is anti-parallel with the Sun direction with nominal
         attitude; anti-parallel with the sensor boresight,

      -  +Y axis is nominally anti-parallel to the SOLO_SRF +Y axis and is
         aligned with the detector rows,

      -  +Z axis completes the right-handed frame and is aligned with the
         detector rows,

      -  the origin of this frame is the geometrical center of the PHI
         Detector.

   This diagram illustrates the SOLO_PHI_*_ILS frames in nominal
   position:

   -Y S/C side (Science deck side) view:
   -------------------------------------

                                                               |
                                                               |
                                                               |
                                                               |
                                                               |
                                                               |
                                                               |
                                                               H
                                                               |
                                      __--o.      _____..|    +Zphi
                                __--''     \'.  .o.----..|     |^     | |
                            __''==-   +Zsrf \.'-----------------|---. |=|
                        __--             ^  >|                  |   |=| |
                  __--'' ()              |   |                  |   | |=|
            __--''==-                    |.  |                  |   | | |
       __--''                            | \ |   +Xphi <--------o   | | |
     >|__|                               |  '|                +Yphi | | |
                                         |   |                      | | |
                                  +Ysrf  x-----------> +Xsrf        | | |
                                         |   |                      | | |
                                         |  .|                      | | |
                                         | / |                      | | |
                                         .'  |                      | | |
                                             |                      | |=|
                                            >|                      |=| |
                                            o:______________________: |=|
                                           //      /        | \       | |
                                          //      /_________|__\
                                         ||                 |
                                         ||                 |
           +Ysrf is out of the        '. ||                 |
           page and +Yphi is         .| |||                 |
           into the page.          -: | |'/                 H
                                     '| |                   |
                                      .'                    |
                                                            |


   The SOLO_PHI_*_ILS frames are defined as CK-based frames and
   SOLO_PHI_*_ILS have a rotation matrix  w.r.t. the SOLO_FOF specified
   in the PHI instrument CK files:

      solo_ANC_soc-phi-fdt-ck_YYYYMMDD-YYYYMMDD_VNN.bc
      solo_ANC_soc-phi-hrt-ck_YYYYMMDD-YYYYMMDD_VNN.bc

         where

            YYYYMMDD   start and finish dates of the CK coverage;

            NN         version of the kernel


   The Nominal Rotation Matrix specified in [4] for the three sensors is:

                      | -1.0000000  0.0         0.0       |
      M            =  |  0.0        0.0         1.0000000 |
       FOF -> EUI     |  0.0        1.0000000   0.0       |


   This Rotation Matrix is described here only as reference. Please note that
   the implemented one is described in the PHI CK files. More details can be
   found in the CK file comment area.

   This set of keywords defines the SOLO_PHI_*_ILS frames as a CK frame:

   \begindata

      FRAME_SOLO_PHI_FDT_ILS           = -144511
      FRAME_-144511_NAME               = 'SOLO_PHI_FDT_ILS'
      FRAME_-144511_CLASS              =  3
      FRAME_-144511_CLASS_ID           = -144511
      FRAME_-144511_CENTER             = -144
      CK_-144511_SCLK                  = -144
      CK_-144511_SPK                   = -144

      FRAME_SOLO_PHI_HRT_ILS           = -144521
      FRAME_-144521_NAME               = 'SOLO_PHI_HRT_ILS'
      FRAME_-144521_CLASS              =  3
      FRAME_-144521_CLASS_ID           = -144521
      FRAME_-144521_CENTER             = -144
      CK_-144521_SCLK                  = -144
      CK_-144521_SPK                   = -144

   \begintext


PHI Sensors Optical Axis Frames:
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

   The PHI Instrument Sensors Optical Axis frames are nominally equivalent
   to the PHI Instrument Sensors Line of Sight frames.

   \begindata

      FRAME_SOLO_PHI_FDT_OPT           = -144512
      FRAME_-144512_NAME               = 'SOLO_PHI_FDT_OPT'
      FRAME_-144512_CLASS              =  4
      FRAME_-144512_CLASS_ID           = -144512
      FRAME_-144512_CENTER             = -144
      TKFRAME_-144512_RELATIVE         = 'SOLO_PHI_FDT_ILS'
      TKFRAME_-144512_SPEC             = 'MATRIX'
      TKFRAME_-144512_MATRIX           = ( 1.0, 0.0, 0.0,
                                           0.0, 1.0, 0.0,
                                           0.0, 0.0, 1.0 )

      FRAME_SOLO_PHI_HRT_OPT           = -144522
      FRAME_-144522_NAME               = 'SOLO_PHI_HRT_OPT'
      FRAME_-144522_CLASS              =  4
      FRAME_-144522_CLASS_ID           = -144522
      FRAME_-144522_CENTER             = -144
      TKFRAME_-144522_RELATIVE         = 'SOLO_PHI_HRT_ILS'
      TKFRAME_-144522_SPEC             = 'MATRIX'
      TKFRAME_-144522_MATRIX           = ( 1.0, 0.0, 0.0,
                                           0.0, 1.0, 0.0,
                                           0.0, 0.0, 1.0 )

   \begintext


RPW frames:
------------------------------------------------------------------------

   This section of the file contains the definitions of the Radio and Plasma
   Waves (RPW) instrument frames.


RPW Frame Tree:
~~~~~~~~~~~~~~~

   The diagram below shows the RPW frame hierarchy.


                           "J2000" INERTIAL
                            ---------------
                                  |
                                  |<-ck
                                  |
                                  V
                              "SOLO_SRF"
           +----------------------------------------------------+
           |                |                 |                 |
           |<-fixed         |<-fixed          |<-fixed          |<-ck
           |                |                 |                 |
           V                V                 V                 |
   "SOLO_RPW_ANT_1"  "SOLO_RPW_ANT_2"  "SOLO_RPW_ANT_3"         |
   ----------------  ----------------  ----------------         |
                                                                V
                                                         "SOLO_INS_BOOM"
                                                         ---------------
                                                                |
                                                                |<-fixed
                                                                |
                                                                V
                                                          "SOLO_RPW_SCM"
                                                          --------------


RPW Antennas Frames:
~~~~~~~~~~~~~~~~~~~~

   The Electric antennas (ANT), consist on a set of three monopoles mounted on
   the +Z, +Y and -Y panels of the S/C.

   The Antenna's frames are referred as  ANT 1, ANT 2 and ANT 3 in [7] or ANT
   +Z, ANT +Y, ANT -Y in [4]. The RPW Antenna's frames -- SOLO_RPW_ANT_1,
   SOLO_RPW_ANT_2 and SOLO_RPW_ANT_3 -- are defined as follows:

      -  +Z axis is aligned with the undeformed deployed boom of the antenna,
         pointing away from the S/C body;

      -  +X axis is co-aligned with S/C -X axis (-Xsrf);

      -  +Y axis completes the right-handed frame;

      -  the origin of these frames are at the point of intersection of flange
         axis with the interface plane between the monopole and the
         corresponding panel of the S/C structure.


   The aligment of the undeformed deployed boom of the antenna is defined as
   follows for the different Antennas. For the +Z RPW antenna (ANT 1), it is
   aligned with the S/C +Z axis (+Zsrf). For the RPW +Y antenna (ANT 2), it
   corresponds to the rotation of the S/C +Z axis (+Zsrf) by -Theta around
   the S/C +X axis (+Xsrf). For the RPW -Y antenna (ANT 3), it corresponds to
   the rotation of the S/C +Z axis (+Zsrf) by +theta around the S/C +X axis
   (+Xsrf).

   This diagram illustrates the SOLO_RPW_ANT_* frames in their nominal
   position:

   -X S/C side (S/C-launcher separation plane) view:
   -------------------------------------------------

                                        |  ANT 1
                                    .   |
                            .           |
                                        |
                     .                  |
       theta                            |
     (~125 deg) .                       |
                                        ^ +Zant1
                                        |
           .                            |
                                        |
                                       .|.
         .                   .--------/|||\--------.
                             | .-------|o---------> +Yant1
        .                    | |       | |      | |
   .__  _________.           | |       |^+Zsrf  | |            ,_________  __.
   |  \ \        |\          | |     __|||_     | |           /|        / /  |
   |  / /        |\\         | |   .'  ||| `.   | |          //|        \ \  |
   |  \ \        | \\_________ |  /   ()|()  \  | __________// |        / /  |
   |  / /        | /.--------  <--------x     | |'----------.\ |        \ \  |
   |  \ \        |//       Ysrf| |      +Xsrf | | |          \\|        / /  |
   |  / /        |/          | |  \          /  | |           \|        \ \  |
   '-'  '------ +Yant2 <.    | |   `.______.'   | |            '---------' '-'
                         '.  | |                | |
        .                  '.| |                | |          +Xsrf is into
                             '.|                | |           the page. +Xant1
                             | o_______()_______o |          +Xant2 and +Xant3
          .                  .'___/    ||    \_.  .          are out of the
                           .'    /_____||____.'    '.        page.
                         .'            ||  .'        '.
             .         .'              || V            '.
              +Zant2 <'              .-'+Yant3           '> +Zant3
               .    .'              / \  / \               '.
                 .'                |   ()   |                '.
               .'                   \  ||  /                   '.
             .'                      `-..-'                      '.
    ANT 2  .'                                                      '.   ANT 3
         .'                                                          '.


   The SOLO_RPW_ANT_* frames are defined as a fixed offset frame relative to the
   SOLO_SRF frame. The following rotation matrices from [4] are used to define
   the fixed offset.


                      | -1.0000000  0.0         0.0        |
      M            =  |  0.0       -1.0000000   0.0        |
       PRF -> ANT 1   |  0.0        0.0         1.00000000 |


                      | -1.0000000  0.0         0.0        |
      M            =  |  0.0       -cos(theta)  sin(theta) |
       PRF -> ANT 2   |  0.0        sin(theta)  cos(theta) |


                      | -1.0000000  0.0         0.0        |
      M            =  |  0.0       -cos(theta) -sin(theta) |
       PRF -> ANT 3   |  0.0       -sin(theta)  cos(theta) |


   As indicated in [7] with the nominal value for theta = 125 degrees, we
   obtain:

                      | -1.0000000  0.0         0.0        |
      M            =  |  0.0        0.57357643  0.81915204 |
       PRF -> ANT 2   |  0.0        0.81915204 -0.57357643 |


                      | -1.0000000  0.0         0.0        |
      M            =  |  0.0        0.57357643 -0.81915204 |
       PRF -> ANT 3   |  0.0       -0.81915204 -0.57357643 |


   \begindata

      FRAME_SOLO_RPW_ANT_1             = -144610
      FRAME_-144610_NAME               = 'SOLO_RPW_ANT_1'
      FRAME_-144610_CLASS              =  4
      FRAME_-144610_CLASS_ID           = -144610
      FRAME_-144610_CENTER             = -144
      TKFRAME_-144610_RELATIVE         = 'SOLO_SRF'
      TKFRAME_-144610_SPEC             = 'MATRIX'
      TKFRAME_-144610_MATRIX           = ( -1.0,   0.0,   0.0,
                                            0.0,  -1.0,   0.0,
                                            0.0,   0.0,   1.0 )


      FRAME_SOLO_RPW_ANT_2             = -144620
      FRAME_-144620_NAME               = 'SOLO_RPW_ANT_2'
      FRAME_-144620_CLASS              =  4
      FRAME_-144620_CLASS_ID           = -144620
      FRAME_-144620_CENTER             = -144
      TKFRAME_-144620_RELATIVE         = 'SOLO_SRF'
      TKFRAME_-144620_SPEC             = 'MATRIX'
      TKFRAME_-144620_MATRIX           = (  -1.0000000,  0.0,          0.0,
                                             0.0,  0.57357643,  0.81915204,
                                             0.0,  0.81915204, -0.57357643  )

      FRAME_SOLO_RPW_ANT_3             = -144630
      FRAME_-144630_NAME               = 'SOLO_RPW_ANT_3'
      FRAME_-144630_CLASS              =  4
      FRAME_-144630_CLASS_ID           = -144630
      FRAME_-144630_CENTER             = -144
      TKFRAME_-144630_RELATIVE         = 'SOLO_SRF'
      TKFRAME_-144630_SPEC             = 'MATRIX'
      TKFRAME_-144630_MATRIX           = (  -1.0000000,  0.0,          0.0,
                                             0.0,   0.57357643, -0.81915204,
                                             0.0,  -0.81915204, -0.57357643 )

   \begintext


RPW Search Coil Unit Frame:
~~~~~~~~~~~~~~~~~~~~~~~~~~~

   The Search Coil unit SCM is a magnetic sensor of inductive type. It is the
   sensor intended to measure the three components of the magnetic field from
   near DC to about 10 KHz and to about 500 kHz for one component. The search
   coil magnetometer is composed of 3 orthogonal magnetic antennas assembled
   orthogonally in the most compact way as possible by the body of the sensor.

   The RPW Search Coil Unit (SCM) frame -- SOLO_RPW_SCM -- is co-aligned with
   the instrument boom inboard frame and is defined as follows:

      -  +X axis is in the direction of the first magnetometer coil;

      -  +Y axis is in the direction of the second magnetometer coil;

      -  +Z axis completes the right-handed frame;

      -  the origin of this frame is the centre of the Unit Reference Hole
         of the SCM.


   This diagram illustrates the SOLO_RPW_SCM frame with the instrument boom
   fully deployed:

   -Z Instrument Boom side view:
   -----------------------------

                     / \.---./ \
                     \.'     './     SWA Electrons
                      |       |     Analysers Systems
                      '-------'          (EAS)
                         | |
                         | |
                         | |
                         | |
                        .---.
                        |   |     MAG Outboard Sensor
                        |   |            (OBS)
                        '---'
                         | |
                         | |
                         | |
                         | |
                         | |
                         |_|
                        /    +xscm
                        \___/ ^
                         ||| /
                         |||/       RPW Magnetic Search Coil
                         ||/              (SCM)
                        |_o--------->
                         | ' .
                         | |   ' .
                         ~~~       '>  +Yscm
                         ~~~
                         | |
                         | |
                        .---.
                        |   |      MAG Inboard Sensor
                        |   |             (IBS)
                        '---'
                         | |
                         | |
                         |^| +Xinsb
                         |||
                         |||
                         |||
                          o-------->           +Zinsb is out
                     +Zinsb      +Yinsb         of the page.

   \begindata

      FRAME_SOLO_RPW_SCM               = -144640
      FRAME_-144640_NAME               = 'SOLO_RPW_SCM'
      FRAME_-144640_CLASS              =  4
      FRAME_-144640_CLASS_ID           = -144640
      FRAME_-144640_CENTER             = -144
      TKFRAME_-144640_RELATIVE         = 'SOLO_INS_BOOM_IB'
      TKFRAME_-144640_SPEC             = 'MATRIX'
      TKFRAME_-144640_MATRIX           = (
                                    -0.40820402,  0.70710678,  0.57738157,
                                    -0.40820402, -0.70710678,  0.57738157,
                                     0.81654085,  0.0,         0.57728766
                                         )

   \begintext


SOLOHI Frames:
------------------------------------------------------------------------

   This section of the file contains the definitions of the Solar Orbiter
   Heliospheric Imager (SOLOHI) instrument frames.

   The current SoloHI frames implementation follow these guidelines: SoloHI
   has 4 separate detectors tilted into the focal plane to effectively build
   a ``big'' composite detector. Each of the 4 will output it's own FITS file.
   However because the detectors share the same optical path (and because
   SoloHI uses a non-TAN projection because of it's very wide FoV) the
   current guess is that each FITS will actually share a common reference
   pixel. Thus for purposes of SPICE we define a single frame for the
   composite detector (and revise this decision later once we get clear
   information from the SoloHI team).


SOLOHI Frame Tree:
~~~~~~~~~~~~~~~~~~

   The diagram below shows the PHI frame hierarchy.


                           "J2000" INERTIAL
                            ---------------
                                  |
                                  |<-ck
                                  |
                                  V
                              "SOLO_SRF"
                              ----------
                                  |
                                  |<-ck
                                  |
                                  V
                              "SOLO_FOF"
                              ----------
                                  |
                                  |<-ck
                                  |
                                  V
                           "SOLO_SOLOHI_ILS"
                           -----------------
                                  |
                                  |<-fixed
                                  |
                                  V
                           "SOLO_SOLOHI_OPT"
                           -----------------


SOLOHI Sensor Line of Sight Frame:
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

   According to [4] the nominal SoloHI Sensor Line of Sight reference
   frame -- SOLO_SOLOHI_ILS -- is defined as follows:

      -  +Z axis is co-aligned with the SOLO_SRF +Z axis and is
         aligned with the detector rows,

      -  +X axis is rotated 25 degrees around the +Z axis from the
         anti-parallel Sun direction with nominal attitude (or 155
         around the +Z axis from the Sun direction);

      -  +Y axis completes the right-handed frame;

      -  the origin of this frame is the geometrical center of the SOLOHI
         Detector.

   This diagram illustrates the SOLO_SOLOHI_ILS frames in nominal
   position:

   +Z S/C side view:
   -----------------
                                            /\/\/
                                              H
                                              H
                                              H
                                              |             |
                                         +Ysrf|             |
                                           ^  |             |
                                           |  |             |
                                           |  0             |
                                         '.| .----------------------. |=|
                                      '. | | |                      |=| |
                                      | || |'|             +Zsolohi | | |
     >___________________________________ _|______ +Xsrf      . o   | |=|
     >-------------------------------------o--------->    . '    '  | | |
                                      | ||+Zsrf       . '         ' |=| |
                                      .'|| / |     <'              '| |=|
                                         .'  '-- +Xsolohi ----------' | |
                                              0             |        '
                                              |             |         '
                                              |             |          v
               +Zsrf and +Zsolohi             |             |         +Ysolohi
                are out of the page.          |             |
                                              H
                                              H
                                              H
                                            /\/\/


   The SOLO_SOLOHI_*_ILS frames are defined as CK-based frames and have the
   rotation matrices  w.r.t. the SOLO_FOF specified in the SOLOHI CK files:

      solo_ANC_soc-solohi-ck_YYYYMMDD-YYYYMMDD_VNN.bc

         where

            YYYYMMDD   start and finish dates of the CK coverage;

            NN         version of the kernel


   The Nominal Rotation Matrix specified in [4] for the sensor is:

                      |  cos(155)     -sin(155)     0.0  |
      M         =     |  sin(155)      cos(155)     1.0  |    =
       FOF -> SOLOHI  |  0.0           0.0          1.0  |

                      |  -0.90630779  -0.42261826   0.0  |
                =     |   0.42261826  -0.90630779   0.0  |
                      |   0.0          0.0          1.0  |


   This Rotation Matrix is described here only as reference. Please note that
   the implemented one is described in the SoloHI CK file. More details can be
   found in the CK files comment area.

   These set of keywords define the SOLO_SOLOHI_ILS frames as a CK frame:

   \begindata

      FRAME_SOLO_SOLOHI_ILS            = -144701
      FRAME_-144701_NAME               = 'SOLO_SOLOHI_ILS'
      FRAME_-144701_CLASS              =  3
      FRAME_-144701_CLASS_ID           = -144701
      FRAME_-144701_CENTER             = -144
      CK_-144701_SCLK                  = -144
      CK_-144701_SPK                   = -144

   \begintext


SOLOHI Sensor Optical Axis Frame:
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

   Nominally the PHI sensors optical axis are co-aligned with the SoloHI
   sensor Insrument Line of Sight frame. The SoloHI Sensor Optical Axis frame
   -- SOLO_SOLOHI_OPT -- is defined as follows:

      -  +Z axis is aligned with the sensor boresight,

      -  +X axis is aligned with the detector rows,

      -  +Y completes the right-handed frame and is aligned with the
         detector columns,

      -  the origin of this frame is the geometrical center of the SoloHI
         Detector.

   \begindata

      FRAME_SOLO_SOLOHI_OPT            = -144702
      FRAME_-144702_NAME               = 'SOLO_SOLOHI_OPT'
      FRAME_-144702_CLASS              =  4
      FRAME_-144702_CLASS_ID           = -144702
      FRAME_-144702_CENTER             = -144
      TKFRAME_-144702_RELATIVE         = 'SOLO_SOLOHI_ILS'
      TKFRAME_-144702_SPEC             = 'MATRIX'
      TKFRAME_-144702_MATRIX           = ( 1.0, 0.0, 0.0,
                                           0.0, 1.0, 0.0,
                                           0.0, 0.0, 1.0 )

   \begintext


SPICE Frames:
------------------------------------------------------------------------

   This section of the file contains the definitions of the Spectral Imaging
   of the Coronal Environment (SPICE) instrument frames. Please note that
   the SPICE Scanner is not implemented as a CK based frame instead the
   Field-of-Regard defined in the SPICE IK for a full scanner rotation.


SPICE Frame Tree:
~~~~~~~~~~~~~~~~~

   The diagram below shows the SPICE frame hierarchy.


                               "J2000" INERTIAL
                               ----------------
                                      |
                                      |<-ck
                                      |
                                      V
                                  "SOLO_SRF"
                                  ----------
                                      |
                                      |<-ck
                                      |
                                      V
                                  "SOLO_FOF"
                           +-----------------------+
                           |                       |
                           |<-ck                   |<-ck
                           |                       |
                           V                       V
                  "SOLO_SPICE_SW_ILS"     "SOLO_SPICE_LW_ILS"
                  -------------------     -------------------
                           |                       |
                           |<-fixed                |<-fixed
                           |                       |
                           V                       V
                 "SOLO_SPICE_SW_OPT"       "SOLO_SPICE_LW_OPT"
                 -------------------       -------------------


SPICE Sensors Line of Sight Frames:
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

   The SPICE Sensors Line of Sight (ILS) frames -- SOLO_SPICE_SW_ILS,
   SOLO_SPICE_LW_ILS -- are defined as follows:

      -  +Z axis is aligned with the detector rows,

      -  +X axis is anti-parallel with the Sun direction with nominal
         attitude; anti-parallel with the sensor boresight,

      -  +Y completes the right-handed frame and is aligned with the detector
         columns,

      -  the origin of this frame is the geometrical center of the PHI
         Detector.


   This diagram illustrates the SOLO_SPICE_*_ILS frames in nominal
   position:

    -Y S/C side (Science deck side) view:
    -------------------------------------

                                                               |
                                                               |
                                                               |
                                                               |
                                                               |
                                                               |
                                                               |
                                                               H
                                                               |
                                      __--o.      _____..|    +Zspice
                                __--''     \'.  .o.----..|     |^     | |
                            __''==-   +Zsrf \.'-----------------|---. |=|
                        __--             ^  >|                  |   |=| |
                  __--'' ()              |   |                  |   | |=|
            __--''==-                    |.  |                  |   | | |
       __--''                            | \ | +Xspice <--------o   | | |
     >|__|                               |  '|                +Yspice | |
                                         |   |                      | | |
                                  +Ysrf  x-----------> +Xsrf        | | |
                                         |   |                      | | |
                                         |  .|                      | | |
                                         | / |                      | | |
                                         .'  |                      | | |
                                             |                      | |=|
                                            >|                      |=| |
                                            o:______________________: |=|
                                           //      /        | \       | |
                                          //      /_________|__\
                                         ||                 |
                                         ||                 |
           +Ysrf is out of the        '. ||                 |
           page and +Yspice is       .| |||                 |
           into the page.          -: | |'/                 H
                                     '| |                   |
                                      .'                    |
                                                            |


   The SOLO_SPICE_*_ILS frames are defined as CK-based frames and
   SOLO_SPICE_*_ILS have a rotation matrix  w.r.t. the SOLO_FOF specified
   in the SPICE Sensor's CK files:

      solo_ANC_soc-spice-sw-ck_YYYYMMDD-YYYYMMDD_VNN.bc
      solo_ANC_soc-spice-lw-ck_YYYYMMDD-YYYYMMDD_VNN.bc

         where

            YYYYMMDD   start and finish dates of the CK coverage;

            NN         version of the kernel


   The Nominal Rotation Matrix specified in [4] for the three sensors is:

                      | -1.0000000  0.0         0.0       |
      M            =  |  0.0        0.0         1.0000000 |
       FOF -> SPICE   |  0.0        1.0000000   0.0       |


   This Rotation Matrix is described here only as reference. Please note that
   the implemented one is described in the SPICE CK files. More details can be
   found in the CK file comment area.

   This set of keywords defines the SOLO_SPICE_*_ILS frames as a CK frame:

   \begindata

      FRAME_SOLO_SPICE_SW_ILS          = -144811
      FRAME_-144811_NAME               = 'SOLO_SPICE_SW_ILS'
      FRAME_-144811_CLASS              =  3
      FRAME_-144811_CLASS_ID           = -144811
      FRAME_-144811_CENTER             = -144
      CK_-144811_SCLK                  = -144
      CK_-144811_SPK                   = -144

      FRAME_SOLO_SPICE_LW_ILS          = -144821
      FRAME_-144821_NAME               = 'SOLO_SPICE_LW_ILS'
      FRAME_-144821_CLASS              =  3
      FRAME_-144821_CLASS_ID           = -144821
      FRAME_-144821_CENTER             = -144
      CK_-144821_SCLK                  = -144
      CK_-144821_SPK                   = -144

   \begintext


SPICE Sensors Optical Axis Frames:
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

   The SPICE Instrument Sensors Optical Axis frames are nominally equivalent
   to the SPICE Instrument Sensors Line of Sight frames.

   \begindata

      FRAME_SOLO_SPICE_SW_OPT          = -144812
      FRAME_-144812_NAME               = 'SOLO_SPICE_SW_OPT'
      FRAME_-144812_CLASS              =  4
      FRAME_-144812_CLASS_ID           = -144812
      FRAME_-144812_CENTER             = -144
      TKFRAME_-144812_RELATIVE         = 'SOLO_SPICE_SW_ILS'
      TKFRAME_-144812_SPEC             = 'MATRIX'
      TKFRAME_-144812_MATRIX           = ( 1.0, 0.0, 0.0,
                                           0.0, 1.0, 0.0,
                                           0.0, 0.0, 1.0 )

      FRAME_SOLO_SPICE_LW_OPT          = -144822
      FRAME_-144822_NAME               = 'SOLO_SPICE_LW_OPT'
      FRAME_-144822_CLASS              =  4
      FRAME_-144822_CLASS_ID           = -144822
      FRAME_-144822_CENTER             = -144
      TKFRAME_-144822_RELATIVE         = 'SOLO_SPICE_LW_ILS'
      TKFRAME_-144822_SPEC             = 'MATRIX'
      TKFRAME_-144822_MATRIX           = ( 1.0, 0.0, 0.0,
                                           0.0, 1.0, 0.0,
                                           0.0, 0.0, 1.0 )

   \begintext


STIX Frames:
------------------------------------------------------------------------

   This section of the file contains the definitions of the Spectrometer
   Telescope for Imaging X rays (STIX) instrument frames.


STIX Frame Tree:
~~~~~~~~~~~~~~~~

   The diagram below shows the PHI frame hierarchy.


                           "J2000" INERTIAL
                            ---------------
                                  |
                                  |<-ck
                                  |
                                  V
                              "SOLO_SRF"
                              ----------
                                  |
                                  |<-ck
                                  |
                                  V
                              "SOLO_FOF"
                              ----------
                                  |
                                  |<-ck
                                  |
                                  V
                          "SOLO_PHI_STIX_ILS"
                          -------------------
                                  |
                                  |<-fixed
                                  |
                                  V
                          "SOLO_PHI_STIX_OPT"
                          -------------------


STIX Sensor Line of Sight Frame:
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

   According to [4] the nominal SOLO STIX Sensor Line of Sight reference
   frame -- SOLO_STIX_ILS -- is defined as follows:

      -  +X axis is anti-parallel with the Sun direction with nominal
         attitude; anti-parallel with the sensor boresight,

      -  +Y axis is nominally anti-parallel to the SOLO_SRF +Y axis and is
         aligned with the detector rows,

      -  +Z axis completes the right-handed frame and is aligned with the
         detector rows,

      -  the origin of this frame is the geometrical center of the PHI
         Detector.

   This diagram illustrates the SOLO_STIX_ILS frames in nominal
   position:

   -Y S/C side (Science deck side) view:
   -------------------------------------

                                                               |
                                                               |
                                                               |
                                                               |
                                                               |
                                                               |
                                                               |
                                                               H
                                                               |
                                      __--o.      _____..|    +Zstix
                                __--''     \'.  .o.----..|     |^     | |
                            __''==-   +Zsrf \.'-----------------|---. |=|
                        __--             ^  >|                  |   |=| |
                  __--'' ()              |   |                  |   | |=|
            __--''==-                    |.  |                  |   | | |
       __--''                            | \ |  +Xstix <--------o   | | |
     >|__|                               |  '|               +Ystix | | |
                                         |   |                      | | |
                                  +Ysrf  x-----------> +Xsrf        | | |
                                         |   |                      | | |
                                         |  .|                      | | |
                                         | / |                      | | |
                                         .'  |                      | | |
                                             |                      | |=|
                                            >|                      |=| |
                                            o:______________________: |=|
                                           //      /        | \       | |
                                          //      /_________|__\
                                         ||                 |
                                         ||                 |
           +Ysrf is out of the        '. ||                 |
           page and +Ystix is into   .| |||                 |
           the page.               -: | |'/                 H
                                     '| |                   |
                                      .'                    |
                                                            |


   The SOLO_STIX_*_ILS frames are defined as CK-based frames and have the
   rotation matrices  w.r.t. the SOLO_FOF specified in the STIX CK files:

      solo_ANC_soc-stix-ck_YYYYMMDD-YYYYMMDD_sYYYYMMDD_VNN.bc

         where

            YYYYMMDD   start and finish dates of the CK coverage;

            sYYYYMMDD  the SCLK reference with which the CK was generated;

            NN         version of the kernel


   The Nominal Rotation Matrix specified in [4] for the sensor is:

                      | -1.0000000  0.0         0.0       |
      M            =  |  0.0        0.0         1.0000000 |
       FOF -> STIX    |  0.0        1.0000000   0.0       |


   This Rotation Matrix is described here only as reference. Please note that
   the implemented one is described in the STIX CK file. More details can be
   found in the CK files comment area.

   These set of keywords define the SOLO_STIX_ILS frames as a CK frame:

   \begindata

      FRAME_SOLO_STIX_ILS              = -144851
      FRAME_-144851_NAME               = 'SOLO_STIX_ILS'
      FRAME_-144851_CLASS              =  3
      FRAME_-144851_CLASS_ID           = -144851
      FRAME_-144851_CENTER             = -144
      CK_-144851_SCLK                  = -144
      CK_-144851_SPK                   = -144

   \begintext


STIX Sensor Optical Axis Frame:
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

   Nominally the STIX sensors optical axis are co-aligned with the STIX sensor
   Instrument Line of Sight frame. The STIX Sensor Optical Axis frame
   -- SOLO_STIX_OPT -- is defined as follows:

      -  +Z axis is aligned with the sensor boresight,

      -  +X axis is aligned with the detector rows,

      -  +Y completes the right-handed frame and is aligned with the
         detector columns,

      -  the origin of this frame is the geometrical center of the STIX
         Detector.

   \begindata

      FRAME_SOLO_STIX_OPT              = -144852
      FRAME_-144852_NAME               = 'SOLO_STIX_OPT'
      FRAME_-144852_CLASS              =  4
      FRAME_-144852_CLASS_ID           = -144852
      FRAME_-144852_CENTER             = -144
      TKFRAME_-144852_RELATIVE         = 'SOLO_STIX_ILS'
      TKFRAME_-144852_SPEC             = 'MATRIX'
      TKFRAME_-144852_MATRIX           = ( 1.0, 0.0, 0.0,
                                           0.0, 1.0, 0.0,
                                           0.0, 0.0, 1.0 )

   \begintext


SWA Frames:
------------------------------------------------------------------------

   This section of the file contains the definitions of the Solar Wind
   Analyzer (SWA) instrument frames.


SWA Frame Tree:
~~~~~~~~~~~~~~~~

   The diagram below shows the SWA frame hierarchy.


                           "J2000" INERTIAL
                           ----------------
                                  |
                                  |<-ck
                                  |
                                  V
                              "SOLO_SRF"
               +-------------------------------------+
               |                  |                  |
               |<-fixed           |<-fixed           |<-fixed
               |                  |                  |
               V                  V                  |
         "SOLO_SWA_HIS"     "SOLO_SWA_PAS"           |
         --------------     --------------           |
                                                     V
                                             "SOLO_SWA_EAS"
                      +------------------------------------+
                      |                                    |
                      |<-fixed                             |<-fixed
                      |                                    |
                      V                                    V
             +------------------+                +------------------+
             |                  |                |                  |
             |<-fixed           |<-fixed         |<-fixed    fixed->|
             |                  |                |                  |
             V                  V                V                  V
   "SOLO_SWA_EAS1-SCI"  "SOLO_SWA_EAS2-SCI"  "SOLO_SWA_EAS1"   "SOLO_SWA_EAS2"
   -------------------  -------------------  ---------------   ---------------


SWA Heavy Ion Sensor (HIS) Frame:
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

   According to [4] the SOLO SWA Heavy Ion Sensor frame
   -- SOLO_SWA_HIS -- is defined as follows:

      -  +X axis is nominally aligned with the SOLO_SRF +X axis,

      -  +Y axis is is nominally aligned with the SOLO_SRF +Y axis

      -  +Z axis completes the right-handed frame and is nominally aligned
         with the SOLO_SRF +Z axis

      -  the origin of this frame is at the centre of the Unit Reference Hole
         (URH) of SWA HIS instrument, at the interface plane with the S/C
         structure (HIS bracket).


   This diagram illustrates the SOLO_SWA_HIS frame:

   -Y S/C side (Science deck side) view:
   -------------------------------------

                                                              ~~~
                                                               |
                                                               H +Zhis
                                                               |  ^
                                      __--o.      _____..|     |  |
                                __--''     \'.  .o.----..|     |  |   | |
                            __''==-   +Zsrf \.'-------------------|-. |=|
                        __--             ^  >|                    x-----> +Xhis
                  __--'' ()              |   |                      | |=|
            __--''==-                    |.  |                      | | |
       __--''                            | \ |                      | | |
     >|__|                               |  '|                      | | |
                                         |   |                      | | |
                                  +Ysrf  x-----------> +Xsrf        | | |
                                         |   |                      | | |
                                         |  .|                      | | |
                                         | / |                      | | |
                                         .'  |                      | | |
                                             |                      | |=|
                                            >|                      |=| |
                                            o:______________________: |=|
                                           //      /        | \       | |
                                          //      /_________|__\
                                         ||                 |
                                         ||                 |
               +Ysrf and +Yhis        '. ||                 |
                are into the         .| |||                 |
                page.              -: | |'/                 H
                                     '| |                   |
                                      .'                    |
                                                            |

   \begindata

      FRAME_SOLO_SWA_HIS               = -144871
      FRAME_-144871_NAME               = 'SOLO_SWA_HIS'
      FRAME_-144871_CLASS              =  4
      FRAME_-144871_CLASS_ID           = -144871
      FRAME_-144871_CENTER             = -144
      TKFRAME_-144871_RELATIVE         = 'SOLO_SRF'
      TKFRAME_-144871_SPEC             = 'MATRIX'
      TKFRAME_-144871_MATRIX           = ( 1.0, 0.0, 0.0,
                                           0.0, 1.0, 0.0,
                                           0.0, 0.0, 1.0 )

   \begintext


SWA Proton/Alpha Sensor (PAS) Frame:
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

   The instrument should be installed on the -Z side of the S/C. The central
   axis of its FoV should be parallel to +X of the S/C. The sensor should be
   located near the +Y edge of the -Z side.

   According to [4] the SOLO SWA Proton/Alpha Sensor frame -- SOLO_SWA_PAS --
   is defined as follows:

      -  +X axis is nominally aligned with the SOLO_SRF +X axis;

      -  +Y axis is is nominally anti-parallel to the SOLO_SRF +Y axis;

      -  +Z axis completes the right-handed frame and is nominally
         anti-parallel to the SOLO_SRF +Z axis;

      -  the origin of this frame is at At the centre of the Unit Reference
         Hole (URH) of SWA PAS instrument.


   This diagram illustrates the SOLO_SWA_HIS frame:

   -Y S/C side (Science deck side) view:
   -------------------------------------

                                                              ~~~
                                                               |
                                      __--o.      _____..|     |
                                __--''     \'.  .o.----..|     |      | |
                            __''==-   +Zsrf \.'---------------------. |=|
                        __--             ^  >|                      | |=|
                  __--'' ()              |   |                      | |=|
            __--''==-                    |.  |                      | | |
       __--''                            | \ |                      | | |
     >|__|                               |  '|                      | | |
                                         |   |                      | | |
                                  +Ysrf  x-----------> +Xsrf        | | |
                                         |   |                      | | |
                                         |  .|                      | | |
                                         | / |                      | | |
                                         .'  |                      | | |
                                             |                      | |=|
                                            >|                    o-----> +Xpas
                                            o:____________________|_: |=|
                                           //      /        | \   |   | |
                                          //      /_________|__\  |
                                         ||                 |     V
                                         ||                 |    +Zpas
            +Ysrf is into +Yhis       '. ||                 |
             the page and +Ypas      .| |||                 |
             is out of the page.   -: | |'/                 H
                                     '| |                   |
                                      .'                    |
                                                            |

   \begindata

      FRAME_SOLO_SWA_PAS               = -144872
      FRAME_-144872_NAME               = 'SOLO_SWA_PAS'
      FRAME_-144872_CLASS              =  4
      FRAME_-144872_CLASS_ID           = -144872
      FRAME_-144872_CENTER             = -144
      TKFRAME_-144872_RELATIVE         = 'SOLO_SRF'
      TKFRAME_-144872_SPEC             = 'MATRIX'
      TKFRAME_-144872_MATRIX           = ( 1.0,  0.0,  0.0,
                                           0.0, -1.0,  0.0,
                                           0.0,  0.0, -1.0 )

   \begintext


SWA Electrons Analysers Systems (EAS) Frames:
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

   For optimum FoV and the rejection of spacecraft-generated photoelectrons
   both EAS sensors are boom mounted. The boom is mounted on the -X face of
   the S/C placing EAS sensors in shadow of the S/C.

   The EAS instrument and the Solar Orbiter's boom are designed in a way that
   when boom is deployed, the +Z axis of EAS instrument should be aligned
   with the Z axis of the S/C.

   The SOLO SWA Electrons Analysers Systems (EAS) frame is connected to the
   instrument boom frame nevertheless it is nominally co-aligned with the S/C
   PRF frame. Because of this the frame definition is defined relative to the
   SOLO_SRF frame instead of the SOLO_INS_BOOM frame. According to [4] the
   SOLO Electrons Analysers Systems frame -- SOLO_SWA_EAS -- is defined as
   follows:

      -  +X axis is nominally anti-parallel to the SOLO_SRF +X axis,

      -  +Y axis is nominally anti-parallel with the SOLO_SRF +Y axis

      -  +Z axis completes the right-handed frame and is nominally aligned
         with the SOLO_SRF +Z axis

      -  the origin of this frame is at the centre of the Unit Reference Hole
         (URH) of SWA EAS instrument which is at the outermost tip of the
         instrument boom.


   EAS consists of two sensor heads mounted on the EAS boom mount: EAS1 and
   EAS2, both are rotated 45 degrees from along the +Zeas axis and their frames
   -- SOLO_SWA_EAS1 and SOLO_SWA_EAS2 -- are defined as follows:

      -  +Z axis is parallel to the EAS +Z axis;

      -  +X axis is normal to the sensor's Variable Geometric Factor
         System;

      -  +Y axis completes the right-handed frame;

      -  the origin of this frame is at the centre of the sensor.


   In addition two Science reference frames are defined (see [10]) for each
   sensor head -- SOLO_SWA_EAS1 and SOLO_SWA_EAS2 --. The +X, +Y, and +Z axes
   of the EAS science frame, are defined to be consistent with how EAS sensors
   scan the 3D sky for electron Velocity Distribution Function (VDF)
   measurements. The electron VDFs are measured in EAS Spherical coordinates,
   where the skymap is scanned by 32 azimuths and 16 elevations. To simplify
   the Spherical-to-Cartesian conversion, the X-Y plane is defined to be
   aligned with the Micro-Channel Plate (MCP) board. However, since the 32
   azimuths corresponding with the anode Pixels on MCP board increase
   clockwise, we choose the Z axis to in-plane on MCP board, so that the
   32 azimuth values could increase anti-clockwise in a right-handed
   coordinate. Accordingly each axes could defined as follows:

      - +X axis is on the X-Y plane and pointing towards azimuthal angle of
        0 degrees, which is the separator between anode Pixel 32 and anode
        Pixel 1 ( or anode ID31 and ID0 in the data);

      - +Y axis is on the X-Y plane and pointing towards azimuthal angle of
        90 degrees, which is the separator between anode Pixel 8 and anode
        Pixel 9 ( or anode ID7 and ID8 in the data);

      - +Z axis is perpendicular to the X-Y plane, and pointing in-plane
        towards the EAS sensor;

      -  the origin of this frame is at the centre of the sensor.


   These diagrams illustrate the SOLO_SWA_EAS frames:

   -Y S/C side (Science deck side) view:
   -------------------------------------

                                                                   ~~~
                                                                    |
                                           __--o.      _____..|     |
          +Zeas                      __--''     \'.  .o.----..|     |      | |
            ^                    __''==-   +Zsrf \.'---------------------. |=|
            |                __--             ^  >|                      |=| |
            |          __--'' ()              |   |                      | |=|
            |    __--''==-                    |.  |                      | | |
            |_--''                            | \ |                      | | |
    <-------o_|                               |  '|                      | | |
    +Xeas    +Yeas                            |   |                      | | |
                                       +Ysrf  x-----------> +Xsrf        | | |
                                              |   |                      | | |
                                              |  .|                      | | |
                                              | / |                      | | |
                                              .'  |                      | | |
                                                  |                      | |=|
                                                 >|                      |=| |
                                                 o:______________________: |=|
                                                //      /        | \       | |
                                               //      /_________|__\
                                              ||                 |
                                              ||                 |
                    +Ysrf is into          '. ||                 |
                     the page.            .| |||                 |
                                        -: | |'/                 H
                                          '| |                   |
                                           .'                    |
                                                                 |


   +Z EAS side view (ins boom is projected):
   -----------------------------------------


         -Zeas1sci,               -Zeas2sci
         +Xeas1                   +Xeas2         +Zsrf, +Zeas, +Zeas1
               ^        +Xeas       ^            +Zeas2 and +Xeas2sci are out
                 \        ^        /              of the page. +Xeas1sci is
                  \       |       /               into the page.
                   \      |      /
                   .o/ \.-|-./ \o.
                 .'  \.'  |  './  '.
   -Yeas1sci   .'     |   |   |     '.    -Yeas2ci
     +Yeas1  <'   <-------o---'       '>  +Yeas2
                +Yeas    | |
                         | |        SWA Electrons
                         | |       Analysers Systems
                         | |             (EAS)          +Zsrf
                        .---.                               o----------->
                        |   |     MAG Outboard Sensor       |         +Ysrf
                        |   |            (OBS)              |
                        '---'                               |
                         | |                                |
                         | |                                V
                         | |                              +Xsrf
                       ~~~~~~~


   Since the SPICE frames subsystem calls for specifying the reverse
   transformation--going from the instrument or structure frame to the
   base frame--as compared to the description given above, the order of
   rotations assigned to the TKFRAME_*_AXES keyword is also reversed
   compared to the above text, and the signs associated with the
   rotation angles assigned to the TKFRAME_*_ANGLES keyword are the
   opposite from what is written in the above text.

   \begindata

      FRAME_SOLO_SWA_EAS               = -144873
      FRAME_-144873_NAME               = 'SOLO_SWA_EAS'
      FRAME_-144873_CLASS              =  4
      FRAME_-144873_CLASS_ID           = -144873
      FRAME_-144873_CENTER             = -144
      TKFRAME_-144873_RELATIVE         = 'SOLO_SRF'
      TKFRAME_-144873_SPEC             = 'MATRIX'
      TKFRAME_-144873_MATRIX           = ( -1.0,  0.0, 0.0,
                                            0.0, -1.0, 0.0,
                                            0.0,  0.0, 1.0 )

      FRAME_SOLO_SWA_EAS1              = -144874
      FRAME_-144874_NAME               = 'SOLO_SWA_EAS1'
      FRAME_-144874_CLASS              =  4
      FRAME_-144874_CLASS_ID           = -144874
      FRAME_-144874_CENTER             = -144
      TKFRAME_-144874_RELATIVE         = 'SOLO_SWA_EAS'
      TKFRAME_-144874_SPEC             = 'ANGLES'
      TKFRAME_-144874_UNITS            = 'DEGREES'
      TKFRAME_-144874_AXES             = (   2,       1,       3     )
      TKFRAME_-144874_ANGLES           = (   0.000,   0.000, -45.000 )

      FRAME_SOLO_SWA_EAS2              = -144875
      FRAME_-144875_NAME               = 'SOLO_SWA_EAS2'
      FRAME_-144875_CLASS              =  4
      FRAME_-144875_CLASS_ID           = -144875
      FRAME_-144875_CENTER             = -144
      TKFRAME_-144875_RELATIVE         = 'SOLO_SWA_EAS'
      TKFRAME_-144875_SPEC             = 'ANGLES'
      TKFRAME_-144875_UNITS            = 'DEGREES'
      TKFRAME_-144875_AXES             = (   2,       1,       3     )
      TKFRAME_-144875_ANGLES           = (   0.000,   0.000,  45.000 )

      FRAME_SOLO_SWA_EAS1-SCI          = -144876
      FRAME_-144876_NAME               = 'SOLO_SWA_EAS1-SCI'
      FRAME_-144876_CLASS              =  4
      FRAME_-144876_CLASS_ID           = -144876
      FRAME_-144876_CENTER             = -144
      TKFRAME_-144876_RELATIVE         = 'SOLO_SWA_EAS'
      TKFRAME_-144876_SPEC             = 'MATRIX'
      TKFRAME_-144876_MATRIX           = (  0.0,        -0.0,        -1.0,
                                            0.70710678, -0.70710678, -0.0,
                                           -0.70710678, -0.70710678, -0.0  )

      FRAME_SOLO_SWA_EAS2-SCI          = -144877
      FRAME_-144877_NAME               = 'SOLO_SWA_EAS2-SCI'
      FRAME_-144877_CLASS              =  4
      FRAME_-144877_CLASS_ID           = -144877
      FRAME_-144877_CENTER             = -144
      TKFRAME_-144877_RELATIVE         = 'SOLO_SWA_EAS'
      TKFRAME_-144877_SPEC             = 'MATRIX'
      TKFRAME_-144877_MATRIX           = (  0.0,        -0.0,         1.0,
                                            0.70710678,  0.70710678, -0.0,
                                           -0.70710678,  0.70710678, -0.0  )

   \begintext


SOLO NAIF ID Codes -- Definitions
===============================================================================

   This section contains name to NAIF ID mappings for the Solar Orbiter
   mission. Once the contents of this file is loaded into the KERNEL POOL,
   these mappings become available within SPICE, making it possible to use
   names instead of ID code in the high level SPICE routine calls.


   Spacecraft:
   -----------

      This table presents the SOLO Spacecraft and its main
      structures' names and IDs

      Name                   ID       Synonyms
      ---------------------  -------  ------------------------
      SOLO                   -144     SOLAR ORBITER,
                                      SOLAR-ORBITER, SOL

      SOLO_SRF               -144000  SOLO_SPACECRAFT, SOLO_SC

      SOLO_SA+Y              -144015
      SOLO_SA-Y              -144017

      SOLO_HGA               -144013

      SOLO_LGA_PZ            -144020
      SOLO_LGA_MZ            -144021

      SOLO_MGA               -144032

      SOLO_INS_BOOM          -144040

      SOLO_STR-1             -144051
      SOLO_STR-2             -144052

      Notes:

         -- 'SOLO', 'SOL', 'SOLAR-ORBITER' and 'SOLAR ORBITER' are synonyms
            and all map to the SOLAR ORBITER spacecraft ID (-144);

         -- 'SOLO_SRF', 'SOLO_SPACECRAFT' and 'SOLO_SC' are synonyms and all
            map to the SOLAR ORBITER S/C bus structure ID (-144000);


   The mappings summarized in this table are implemented by the keywords
   below.

   \begindata

      NAIF_BODY_NAME += ( 'SOL'                         )
      NAIF_BODY_CODE += ( -144                          )

      NAIF_BODY_NAME += ( 'SOLAR ORBITER'               )
      NAIF_BODY_CODE += ( -144                          )

      NAIF_BODY_NAME += ( 'SOLO'                        )
      NAIF_BODY_CODE += ( -144                          )

      NAIF_BODY_NAME += ( 'SOLAR-ORBITER'               )
      NAIF_BODY_CODE += ( -144                          )

      NAIF_BODY_NAME += ( 'SOLO_SPACECRAFT'             )
      NAIF_BODY_CODE += ( -144000                       )

      NAIF_BODY_NAME += ( 'SOLO_SC'                     )
      NAIF_BODY_CODE += ( -144000                       )

      NAIF_BODY_NAME += ( 'SOLO_SRF'                    )
      NAIF_BODY_CODE += ( -144000                       )

      NAIF_BODY_NAME += ( 'SOLO_SA+Y'                   )
      NAIF_BODY_CODE += ( -144015                       )

      NAIF_BODY_NAME += ( 'SOLO_SA-Y'                   )
      NAIF_BODY_CODE += ( -144017                       )

      NAIF_BODY_NAME += ( 'SOLO_HGA'                    )
      NAIF_BODY_CODE += ( -144013                       )

      NAIF_BODY_NAME += ( 'SOLO_LGA_PZ'                 )
      NAIF_BODY_CODE += ( -144020                       )

      NAIF_BODY_NAME += ( 'SOLO_LGA_MZ'                 )
      NAIF_BODY_CODE += ( -144021                       )

      NAIF_BODY_NAME += ( 'SOLO_MGA'                    )
      NAIF_BODY_CODE += ( -144032                       )

      NAIF_BODY_NAME += ( 'SOLO_INS_BOOM'               )
      NAIF_BODY_CODE += ( -144040                       )

      NAIF_BODY_NAME += ( 'SOLO_STR-1'                  )
      NAIF_BODY_CODE += ( -144051                       )

      NAIF_BODY_NAME += ( 'SOLO_STR-2'                  )
      NAIF_BODY_CODE += ( -144052                       )

   \begintext


   Energetic Particle Detector (EPD):
   ----------------------------------

      This table presents the Energetic Particle Detector (EPD)
      structures, instruments and sensors names and IDs:

      Name                   ID       Synonyms
      ---------------------  -------  ------------------------
      SOLO_EPD_STEP          -144100
      SOLO_EPD_SIS_ASW       -144111
      SOLO_EPD_SIS_SW        -144112
      SOLO_EPD_EPT_MY-SW     -144123
      SOLO_EPD_EPT_MY-ASW    -144124
      SOLO_EPD_EPT_PY-N      -144125
      SOLO_EPD_EPT_PY-S      -144126
      SOLO_EPD_HET_MY-SW     -144127
      SOLO_EPD_HET_PY-N      -144128
      SOLO_EPD_HET_MY-ASW    -144129
      SOLO_EPD_HET_PY-S      -144130


      The mappings summarized in this table are implemented by the keywords
      below.

   \begindata

      NAIF_BODY_NAME += ( 'SOLO_EPD_STEP'               )
      NAIF_BODY_CODE += ( -144100                       )

      NAIF_BODY_NAME += ( 'SOLO_EPD_SIS_ASW'            )
      NAIF_BODY_CODE += ( -144111                       )

      NAIF_BODY_NAME += ( 'SOLO_EPD_SIS_SW'             )
      NAIF_BODY_CODE += ( -144112                       )

      NAIF_BODY_NAME += ( 'SOLO_EPD_EPT_MY-SW'          )
      NAIF_BODY_CODE += ( -144123                       )

      NAIF_BODY_NAME += ( 'SOLO_EPD_EPT_MY-ASW'         )
      NAIF_BODY_CODE += ( -144124                       )

      NAIF_BODY_NAME += ( 'SOLO_EPD_EPT_PY-N'           )
      NAIF_BODY_CODE += ( -144125                       )

      NAIF_BODY_NAME += ( 'SOLO_EPD_EPT_PY-S'           )
      NAIF_BODY_CODE += ( -144126                       )

      NAIF_BODY_NAME += ( 'SOLO_EPD_HET_MY-SW'          )
      NAIF_BODY_CODE += ( -144127                       )

      NAIF_BODY_NAME += ( 'SOLO_EPD_HET_PY-N'           )
      NAIF_BODY_CODE += ( -144128                       )

      NAIF_BODY_NAME += ( 'SOLO_EPD_HET_MY-ASW'         )
      NAIF_BODY_CODE += ( -144129                       )

      NAIF_BODY_NAME += ( 'SOLO_EPD_HET_PY-S'           )
      NAIF_BODY_CODE += ( -144130                       )

   \begintext


   Extreme Ultraviolet Imager (EUI):
   ---------------------------------

      This table presents the Extreme Ultraviolet Imager (EUI)
      structures, instruments and sensors names and IDs:

      Name                   ID       Synonyms
      ---------------------  -------  ------------------------
      SOLO_EUI               -144200
      SOLO_EUI_FSI           -144210
      SOLO_EUI_HRI_LYA       -144220
      SOLO_EUI_HRI_EUV       -144230


      The mappings summarized in this table are implemented by the keywords
      below.

   \begindata

      NAIF_BODY_NAME += ( 'SOLO_EUI'                    )
      NAIF_BODY_CODE += ( -144200                       )

      NAIF_BODY_NAME += ( 'SOLO_EUI_FSI'                )
      NAIF_BODY_CODE += ( -144210                       )

      NAIF_BODY_NAME += ( 'SOLO_EUI_HRI_LYA'            )
      NAIF_BODY_CODE += ( -144220                       )

      NAIF_BODY_NAME += ( 'SOLO_EUI_HRI_EUV'            )
      NAIF_BODY_CODE += ( -144230                       )

   \begintext


   Magnetometer (MAG):
   -------------------

      This table presents the magnetometer (MAG)
      structures, instruments and sensors names and IDs:

      Name                   ID       Synonyms
      ---------------------  -------  ------------------------
      SOLO_MAG               -144300
      SOLO_MAG_IBS           -144301
      SOLO_MAG_OBS           -144302


      The mappings summarized in this table are implemented by the keywords
      below.

   \begindata

      NAIF_BODY_NAME += ( 'SOLO_MAG'                    )
      NAIF_BODY_CODE += ( -144300                       )

      NAIF_BODY_NAME += ( 'SOLO_MAG_IBS'                )
      NAIF_BODY_CODE += ( -144301                       )

      NAIF_BODY_NAME += ( 'SOLO_MAG_OBS'                )
      NAIF_BODY_CODE += ( -144302                       )

   \begintext


   Multi Element Telescope for Imaging and Spectroscopy (Metis):
   -------------------------------------------------------------

      This table presents the Multi Element Telescope for Imaging and
      Spectroscopy (Metis) structures, instruments and sensors names
      and IDs:

      Name                   ID       Synonyms
      ---------------------  -------  ------------------------
      SOLO_METIS             -144400
      SOLO_METIS_EUV         -144410
      SOLO_METIS_EUV_MIN     -144413
      SOLO_METIS_EUV_MAX     -144414
      SOLO_METIS_VIS         -144420
      SOLO_METIS_VIS_MIN     -144423
      SOLO_METIS_VIS_MAX     -144424
      SOLO_METIS_IEO-M0      -144430


      The mappings summarized in this table are implemented by the keywords
      below.

   \begindata

      NAIF_BODY_NAME += ( 'SOLO_METIS'                  )
      NAIF_BODY_CODE += ( -144400                       )

      NAIF_BODY_NAME += ( 'SOLO_METIS_EUV'              )
      NAIF_BODY_CODE += ( -144410                       )

      NAIF_BODY_NAME += ( 'SOLO_METIS_EUV_MIN'          )
      NAIF_BODY_CODE += ( -144413                       )

      NAIF_BODY_NAME += ( 'SOLO_METIS_EUV_MAX'          )
      NAIF_BODY_CODE += ( -144414                       )

      NAIF_BODY_NAME += ( 'SOLO_METIS_VIS'              )
      NAIF_BODY_CODE += ( -144420                       )

      NAIF_BODY_NAME += ( 'SOLO_METIS_VIS_MIN'          )
      NAIF_BODY_CODE += ( -144423                       )

      NAIF_BODY_NAME += ( 'SOLO_METIS_VIS_MAX'          )
      NAIF_BODY_CODE += ( -144424                       )

      NAIF_BODY_NAME += ( 'SOLO_METIS_IEO-M0'           )
      NAIF_BODY_CODE += ( -144430                       )

   \begintext


   Polarimetric and Helioseismic Imager (PHI):
   -------------------------------------------

      This table presents the Polarimetric and Helioseismic Imager (PHI)
      structures, instruments and sensors names and IDs:

      Name                   ID       Synonyms
      ---------------------  -------  ------------------------
      SOLO_PHI               -144500
      SOLO_PHI_FDT           -144510
      SOLO_PHI_HRT           -144520


      The mappings summarized in this table are implemented by the keywords
      below.

   \begindata

      NAIF_BODY_NAME += ( 'SOLO_PHI'                    )
      NAIF_BODY_CODE += ( -144500                       )

      NAIF_BODY_NAME += ( 'SOLO_PHI_FDT'                )
      NAIF_BODY_CODE += ( -144510                       )

      NAIF_BODY_NAME += ( 'SOLO_PHI_HRT'                )
      NAIF_BODY_CODE += ( -144520                       )

   \begintext


   Radio and Plasma Waves (RPW):
   ------------------------------

      This table presents the Radio & Plasma Waves Investigation (RPW)
      structures, instruments and sensors names and IDs:

      Name                   ID       Synonyms
      ---------------------  -------  ------------------------
      SOLO_RPW               -144600
      SOLO_RPW_ANT_1         -144610
      SOLO_RPW_ANT_2         -144620
      SOLO_RPW_ANT_3         -144630
      SOLO_RPW_SCM           -144640


      The mappings summarized in this table are implemented by the keywords
      below.

   \begindata

      NAIF_BODY_NAME += ( 'SOLO_RPW'                    )
      NAIF_BODY_CODE += ( -144600                       )

      NAIF_BODY_NAME += ( 'SOLO_RPW_ANT_1'              )
      NAIF_BODY_CODE += ( -144610                       )

      NAIF_BODY_NAME += ( 'SOLO_RPW_ANT_2'              )
      NAIF_BODY_CODE += ( -144620                       )

      NAIF_BODY_NAME += ( 'SOLO_RPW_ANT_3'              )
      NAIF_BODY_CODE += ( -144630                       )

      NAIF_BODY_NAME += ( 'SOLO_RPW_SCM'                )
      NAIF_BODY_CODE += ( -144640                       )

   \begintext


   Solar Orbiter Heliospheric Imager (SOLOHI):
   -------------------------------------------

      This table presents the Solar Orbiter Heliospheric Imager (SOLOHI)
      structures, instruments and sensors names and IDs:

      Name                   ID       Synonyms
      ---------------------  -------  ------------------------
      SOLO_SOLOHI            -144700


      The mappings summarized in this table are implemented by the keywords
      below.

   \begindata

      NAIF_BODY_NAME += ( 'SOLO_SOLOHI'                 )
      NAIF_BODY_CODE += ( -144700                       )

   \begintext


   Spectral Imaging of the Coronal Environment (SPICE):
   ----------------------------------------------------

      This table presents the Polarimetric and Helioseismic Imager (PHI)
      structures, instruments and sensors names and IDs:

      Name                   ID       Synonyms
      ---------------------  -------  ------------------------
      SOLO_SPICE             -144800
      SOLO_SPICE_SW          -144810
      SOLO_SPICE_LW          -144820


      The mappings summarized in this table are implemented by the keywords
      below.

   \begindata

      NAIF_BODY_NAME += ( 'SOLO_SPICE'                  )
      NAIF_BODY_CODE += ( -144800                       )

      NAIF_BODY_NAME += ( 'SOLO_SPICE_SW'               )
      NAIF_BODY_CODE += ( -144810                       )

      NAIF_BODY_NAME += ( 'SOLO_SPICE_LW'               )
      NAIF_BODY_CODE += ( -144820                       )

   \begintext


   Spectrometer Telescope for Imaging X rays (STIX):
   -------------------------------------------------

      This table presents the Spectrometer Telescope for Imaging X rays
      (STIX) structures, instruments and sensors names and IDs:

      Name                   ID       Synonyms
      ---------------------  -------  ------------------------
      SOLO_STIX              -144850


      The mappings summarized in this table are implemented by the keywords
      below.

   \begindata

      NAIF_BODY_NAME += ( 'SOLO_STIX'                   )
      NAIF_BODY_CODE += ( -144850                       )

   \begintext


   Solar Wind Analyzer (SWA):
   --------------------------

      This table presents the Solar Wind Analyzer (SWA) structures,
      instruments and sensors names and IDs:

      Name                   ID       Synonyms
      ---------------------  -------  ------------------------
      SOLO_SWA               -144870
      SOLO_SWA_HIS           -144871
      SOLO_SWA_PAS           -144872
      SOLO_SWA_EAS           -144873
      SOLO_SWA_EAS1          -144874
      SOLO_SWA_EAS2          -144875
      SOLO_SWA_EAS1-1        -144881
      SOLO_SWA_EAS1-2        -144882
      SOLO_SWA_EAS1-3        -144883
      SOLO_SWA_EAS1-4        -144884
      SOLO_SWA_EAS2-1        -144885
      SOLO_SWA_EAS2-2        -144886
      SOLO_SWA_EAS2-3        -144887
      SOLO_SWA_EAS2-4        -144888

      The mappings summarized in this table are implemented by the keywords
      below.

   \begindata

      NAIF_BODY_NAME += ( 'SOLO_SWA'                    )
      NAIF_BODY_CODE += ( -144870                       )

      NAIF_BODY_NAME += ( 'SOLO_SWA_HIS'                )
      NAIF_BODY_CODE += ( -144871                       )

      NAIF_BODY_NAME += ( 'SOLO_SWA_PAS'                )
      NAIF_BODY_CODE += ( -144872                       )

      NAIF_BODY_NAME += ( 'SOLO_SWA_EAS'                )
      NAIF_BODY_CODE += ( -144873                       )

      NAIF_BODY_NAME += ( 'SOLO_SWA_EAS1'               )
      NAIF_BODY_CODE += ( -144874                       )

      NAIF_BODY_NAME += ( 'SOLO_SWA_EAS2'               )
      NAIF_BODY_CODE += ( -144875                       )

      NAIF_BODY_NAME += ( 'SOLO_SWA_EAS1-1'             )
      NAIF_BODY_CODE += ( -144881                       )

      NAIF_BODY_NAME += ( 'SOLO_SWA_EAS1-2'             )
      NAIF_BODY_CODE += ( -144882                       )

      NAIF_BODY_NAME += ( 'SOLO_SWA_EAS1-3'             )
      NAIF_BODY_CODE += ( -144883                       )

      NAIF_BODY_NAME += ( 'SOLO_SWA_EAS1-4'             )
      NAIF_BODY_CODE += ( -144884                       )

      NAIF_BODY_NAME += ( 'SOLO_SWA_EAS2-1'             )
      NAIF_BODY_CODE += ( -144885                       )

      NAIF_BODY_NAME += ( 'SOLO_SWA_EAS2-2'             )
      NAIF_BODY_CODE += ( -144886                       )

      NAIF_BODY_NAME += ( 'SOLO_SWA_EAS2-3'             )
      NAIF_BODY_CODE += ( -144887                       )

      NAIF_BODY_NAME += ( 'SOLO_SWA_EAS2-4'             )
      NAIF_BODY_CODE += ( -144888                       )

   \begintext


End of FK file.