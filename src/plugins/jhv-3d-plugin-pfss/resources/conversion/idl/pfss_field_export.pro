; This script creates a file from a traced PFSS Model and in a format
; that is readable by ESA's JHelioviewer 3D Edition.
; The implementation is based upon the pfss_sample1.pro sample file provided
; in the pfss package written by Marc de Rosa and uses the theory documented
; at http://www.lmsal.com/~derosa/pfsspack/#sect1.2.
; 
; To use, do a  .r pfss_field_export  (this file) at the IDL> prompt.

;  Simon Spörri (simon.spoerri@fhnw.ch) -  18 Sep 2012 - created
; --------------------------------------------------------------------------

; usage: pfss_field_export
; exampel pfss_field_export
; include common block (not necessary but useful for looking at things...)
pro pfss_field_export,p_time=p_time,p_distribution_type=p_distribution_type,p_distribution_spacing=p_distribution_spacing
  
  ; Set Default values for keywords if none set
  if(n_elements(p_time) eq 0) then $
    p_time = '2011-03-04'
  if(n_elements(p_distribution_type) eq 0) then $
    p_distribution_type = 3
  if(n_elements(p_distribution_spacing) eq 0) then $
    p_distribution_spacing = 1200
  

  
  
  
  @pfss_data_block
  
  
  ; define the timestamp of the model
  
  PRINT, 'Executing with the following Arguments'
  print, 'Timestamp:     ',p_time
  print, 'Dist.-Type:    ',p_distribution_type
  print, 'Dist.-Spacing: ',p_distribution_spacing
  
  ; 1. Load the coronal file model
  PRINT, '--------------------------------------------------'
  PRINT, 'Step 121: Loading model for ', p_time
  PRINT, '--------------------------------------------------'
  pfss_restore,pfss_time2file(p_time,/ssw_cat,/url)  ;  for all users
  ;pfss_restore,pfss_time2file('2003-04-05')   ;  for users at LMSAL
  
  o_date = NOW
  o_b0 = B0
  o_l0 = L0
  PRINT, '-- Done.'
  PRINT
  
  
  ; 2. Create staring points for line tracing
  PRINT, '--------------------------------------------------'
  PRINT, 'Step 2: Placing start points for line tracing '
  PRINT, '--------------------------------------------------'
  start_radius = 1.0159 ; in Rsun
  ; distribution_fieldtype = 5 ; how the start points should be distributed
  distribution_fieldtype = p_distribution_type ; randomly distributed within box
  
  ; distribution_spacing   = 8 ; Density parameter for start point positioning, depending on fieldtype 
  distribution_spacing   = p_distribution_spacing ; for field type 3, number of points
  
  ; pfss_field_start_coord,distribution_fieldtype,distribution_spacing,radstart=start_radius
  pfss_field_start_coord,distribution_fieldtype,distribution_spacing,radstart=start_radius,bbox=[0,-90,359,90]
  ; pfss_field_start_coord,distribution_fieldtype,distribution_spacing,radstart=start_radius,bbox=[o_l0,o_b0,o_l0,o_b0]
  PRINT, '-- Done.'
  PRINT
  
  ; 3. Trace the field lines
  PRINT, '--------------------------------------------------'
  PRINT, 'Step 3: Tracing the field lines'
  PRINT, '--------------------------------------------------'
  pfss_trace_field
  
  pfss_to_spherical,pfss_data
  
  num_lines = n_elements(NSTEP)
  
  rmax=max(RIX,min=rmin)
  
  open=intarr(num_lines)
  bla=byte([  0,  0,  0])
  yel=byte([255,255,  0])
  gre=byte([  0,255,  0])
  red=byte([255,  0,255])
  whi=byte([255,255,255])
  
  
  PRINT, '-- Determining Field type (Open/Closed)'
  for i=0l,num_lines-1 do begin
  
    ;  only draw lines that have line data
    ns=NSTEP[i]
    if ns gt 0 then begin
  
      ;  determine whether field lines are open or closed
      if (max((PTR)(0:ns-1,i))-rmin)/(rmax-rmin) gt 0.99 then begin
        irc=get_interpolation_index(RIX,(PTR)(0,i))
        ithc=get_interpolation_index( $
          LAT,90-(PTTH)(0,i)*!radeg)
        iphc=get_interpolation_index( $
          LON,((PTPH)(0,i)*!radeg+360) mod 360)
        brc=interpolate(BR,iphc,ithc,irc)
        if brc gt 0 then open(i)=1 else open(i)=-1
      endif  ;  else open(i)=0, which has already been done
  
      ;  flag those lines that go higher than the first radial gridpoint
      heightflag=max((PTR)(0:ns-1,i)) gt (RIX)(1)
  
      ;  create an object for this line and add it to the model
      if heightflag then begin
  
        ;  set appropriate color
         case open(i) of
          -1: col=red
           0: col=whi
           1: col=gre
        endcase
        ; print, 'Color is ', col
      endif
    endif
  endfor
  PRINT, '-- Done.'
  PRINT
  
  ; 4. Extract the coordinates
  PRINT, '--------------------------------------------------'
  PRINT, 'Step 4: Extracting coordinates and writing to file'
  PRINT, '--------------------------------------------------'
  s_date = str_replace(str_replace(o_date,'/','-'),' ','_')
  outfile = '/tmp/pfss_field_'+ s_date +'.dat'
  
  ; open output file
  OPENW,lun,outfile,/get_lun
  
  PRINT, '-- Extracting ',num_lines,' Field Lines'
  
  ; write meta-data to file 
  printf, lun, 'timestamp:'+o_date
  printf, lun, 'B0:',o_b0
  printf, lun, 'L0:',o_l0
  printf, lun, '---'
  
  ; iterate over all traced lines
  for i_line = 0L, num_lines-1 do begin
    num_points = NSTEP[i_line]
    l_open = open(i_line)
    printf, lun, l_open
    printf, lun, '-'
    ; iterate over all points of the i_line'th line
    for i_point = 0L, num_points-1 do begin
      o_r = PTR[i_point, i_line]
      o_theta = PTTH[i_point, i_line]
      o_phi = PTPH[i_point, i_line]
  
      ; linecoords = cv_coord(/to_rect,from_sph=[o_phi, o_theta, o_r])
      ; print, linecoords; , FORMAT='(D,D,D)'
      ; 
      ; write point coordinates to file
      printf, lun, [o_r,o_theta,o_phi]
      ;printf, lun, linecoords; , FORMAT='(D,D,D)'
    endfor
    
    ; write marker for line end to file 
    printf, lun, '--'
  endfor
  
  FREE_LUN, lun
  PRINT, '-- Done.'
  PRINT
  
  PRINT, '--------------------------------------------------'
  PRINT, ' Success!'
  PRINT, '--------------------------------------------------'
  PRINT, '-- Data exported to '+outfile
  PRINT, '-- Num Lines ',num_lines
  PRINT, '-- B0 ',o_b0
  PRINT, '-- L0 ',o_l0
  PRINT, '--------------------------------------------------'


end
