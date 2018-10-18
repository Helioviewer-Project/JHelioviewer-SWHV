; add datetime to value
; date_src: source date (any format readable by date_conv supported)
; y, d, h, m, s: years, days, hours, minutes and seconds to be added
; format: output format (see date_conv)
function date_add, date_src, y=y, d=d, h=h, m=m, s=s, format=format
  if ~keyword_set(y) then y=0
  if ~keyword_set(d) then d=0
  if ~keyword_set(h) then h=0
  if ~keyword_set(m) then m=0
  if ~keyword_set(s) then s=0
  if ~keyword_set(format) then format='F'
  
  d1 = date_conv(date_src, 'J')
  d2 = d1 + d + double(h)/24 + double(m)/24/60 + double(s)/24/60/60  ; add days, hours, minutes and seconds
  d2 = date_conv(d2, 'V') + [y, 0, 0, 0, 0]
  return, date_conv(d2, format)
end
