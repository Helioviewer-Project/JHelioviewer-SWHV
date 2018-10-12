; add datetime to value
; date_src: source date (any format readable by date_conv supported)
; v_delta: delta-scalar [years, days, hours, minutes, seconds] to be added
; format: output format (see date_conv)
function date_add_v, date_src, v_delta, format=format
  if ~keyword_set(format) then format='F'
  
  v_src = date_conv(date_src, 'V')
  v_dst = v_src + v_delta
  return, date_conv(v_dst, format)
end

function date_add, date_src, y=y, d=d, h=h, m=m, s=s, format=format
  if ~keyword_set(y) then y=0
  if ~keyword_set(d) then d=0
  if ~keyword_set(h) then h=0
  if ~keyword_set(m) then m=0
  if ~keyword_set(s) then s=0
  if ~keyword_set(format) then format='F'

  return, date_add_v(date_src, [y, d, h, m, s], format=format)
end