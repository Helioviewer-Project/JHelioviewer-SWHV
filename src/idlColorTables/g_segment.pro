;+
; calculates a gimp gradient from 0 to 1
; INPUT m - relative midpoint
;       type - type of interpolation
;       pos - position
; See README for overview -- Helge Dietert
function g_segment,m,type,pos
  if pos le m then begin
    f = pos/(2.0*m)
  endif else begin
    f = (pos-m)/(2.0*(1-m)) + 0.5
  end
  case type of
  0:  begin
        return, f
      end
  1:  begin
        return, pos^(alog(0.5)/alog(m))
      end
  2:  begin
        return, (sin(-!pi/2.0 + !pi*f) + 1.0) / 2.0
      end
  3:  begin
        f = f - 1.0
        return, sqrt(1-f*f)
      end
  4:  begin
        return, 1.0 - sqrt(1.0 - f*f)
      end
  endcase
end