; procedure to convert aia color tables into the gimp gradient file
; 
; To do this, it tries to fit a gimp gradient file format with 4
; segments.
; INPUT wavelength: wavelength to fit (it will use aia_lct to load the
;                   color table)
;       type: blending type used for interpolating
; EXAMPLE
; In most cases the followin will give a break down for a gimp gradient
; file
; fit_gimp, 94, 0 -- will try to match the colortable for 94A with lines
; If necessary specify the start break and midpoints
; fit_gimp, 171, 0, div_points=[0.25,0.7,0.75], mid_points=[0.125,0.5,0.725,0.9]
; See README for overview -- Helge Dietert
pro fit_gimp, wavelength, type, div_points=div_points, mid_points=mid_points
; If not set assume standard break points
if not(keyword_set(div_points)) then begin
   div_points = [0.25, 0.5, 0.75]
   mid_points = [0.125, 0.375, 0.625, 0.875]
endif
; load the aia color table
aia_lct,r_aia,g_aia,b_aia,wavelnth=wavelength
; Try to fit gimp gradient accordingly
; we work with one common vector
; [red...green..blue]
aia = [r_aia, g_aia, b_aia]
error = findgen(3*256)
error[*] = 0.005
; parameter are
;  r_m, r_t, g_m, g_t, b_m, b_t
pi = replicate({fixed:0, limited:[0,0], limits:[0.D,0.D]},26)
pi[0].fixed = 1
; first row
pi[1].fixed = 1
pi[2].fixed = 1
pi[3].fixed = 1
pi[4].fixed = 1
pi[5].fixed = 1
; second row
pi[6].limited(0) = 1
pi[6].limited(1) = 1
pi[6].limits[0] = 0.1
pi[6].limits[1] = 0.9
pi[7].limited(0) = 1
pi[7].limited(1) = 1
pi[7].limits[0] = 0.0
pi[7].limits[1] = 1.0
pi[8].limited(0) = 1
pi[8].limited(1) = 1
pi[8].limits[0] = 0.0
pi[8].limits[1] = 1.0
pi[9].limited(0) = 1
pi[9].limited(1) = 1
pi[9].limits[0] = 0.0
pi[9].limits[1] = 1.0
pi[10].limited(0) = 1
pi[10].limited(1) = 1
pi[10].limits[0] = 0.0
pi[10].limits[1] = 1.0
; third row
pi[11].limited(0) = 1
pi[11].limited(1) = 1
pi[11].limits[0] = 0.1
pi[11].limits[1] = 0.9
pi[12].limited(0) = 1
pi[12].limited(1) = 1
pi[12].limits[0] = 0.0
pi[12].limits[1] = 1.0
pi[13].limited(0) = 1
pi[13].limited(1) = 1
pi[13].limits[0] = 0.0
pi[13].limits[1] = 1.0
pi[14].limited(0) = 1
pi[14].limited(1) = 1
pi[14].limits[0] = 0.0
pi[14].limits[1] = 1.0
pi[15].limited(0) = 1
pi[15].limited(1) = 1
pi[15].limits[0] = 0.0
pi[15].limits[1] = 1.0
; forth row
pi[16].limited(0) = 1
pi[16].limited(1) = 1
pi[16].limits[0] = 0.1
pi[16].limits[1] = 0.9
pi[17].limited(0) = 1
pi[17].limited(1) = 1
pi[17].limits[0] = 0.0
pi[17].limits[1] = 1.0
pi[18].limited(0) = 1
pi[18].limited(1) = 1
pi[18].limits[0] = 0.0
pi[18].limits[1] = 1.0
pi[19].limited(0) = 1
pi[19].limited(1) = 1
pi[19].limits[0] = 0.0
pi[19].limits[1] = 1.0
pi[20].limited(0) = 1
pi[20].limited(1) = 1
pi[20].limits[0] = 0.0
pi[20].limits[1] = 1.0
; fifth row
pi[21].fixed = 1
pi[22].limited(0) = 1
pi[22].limited(1) = 1
pi[22].limits[0] = 0.0
pi[22].limits[1] = 0.95
pi[23].fixed = 1
pi[24].fixed = 1
pi[25].fixed = 1
start = [type,$
  0.00,          0.000, r_aia[0]/255.0, g_aia[0]/255.0, b_aia[0]/255.0,$
  div_points[0], mid_points[0], 0.25, 0.25, 0.25,$
  div_points[1], mid_points[1], 0.50, 0.50, 0.50,$
  div_points[2], mid_points[2], 0.75, 0.75, 0.75,$
  1.00,          mid_points[3], r_aia[255]/255.0, g_aia[255]/255.0, b_aia[255]/255.0]
expr = "g_segment_fit(X,P)"
result = MPFITEXPR(expr, findgen(3*256), aia, error, start, PARINFO=pi)
gimp = g_segment_fit(findgen(3*256), result)
; show result
print, result
for i=0,3 do begin
  print, result[1+5*i], result[2+5*(i+1)], result[1+5*(i+1)], result[3+5*i], result[4+5*i], result[5+5*i], 1.0, result[3+5*(i+1)], result[4+5*(i+1)], result[5+5*(i+1)], 1.0, type, 0, 0, 0
end
; Plotting the result
waitString = '' ; string to read
plot, r_aia, title="Red component", linestyle=1
oplot, gimp[0:255], linestyle=2
read, waitString, prompt="Showing red, next?"
plot, g_aia, title="Green component", linestyle=1
oplot, gimp[256:511], linestyle=2
read, waitString, prompt="Showing green, next?"
plot, b_aia, title="Blue component", linestyle=1
oplot, gimp[512:767], linestyle=2
read, waitString, prompt="Showing blue, next?"
return
end