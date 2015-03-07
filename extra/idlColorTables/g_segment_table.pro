; calculates a gimp gradient table for one segment from 0 to 1
; It allows easily to understand the kind of the different
; blending functions
; INPUT m - relative midpoint
;       type - type of interpolation
; OUTPUT table - created table
; EXAMPLE
; > g_segment_table, out, 0.3, 1
; > plot, out
; See README for overview -- Helge Dietert
@g_segment
pro g_segment_table,table,m,type
table = findgen(256)
; Go over each segment
for i=0, 255 do begin
  pos = i/255.0
  table[i] = g_segment(m,type,pos)
end
return
end