; Function used to fit a gimp gradient with 5 segments
; x - array with 3*256 entries [red green blue]
; P are the paramter as
; P[blendingType
;   until0, mid0, redEnd0, greenEnd0, blueEnd0,
;   until1, mid1, redEnd1, greenEnd1, blueEnd1,
;   until2, mid2, redEnd2, greenEnd2, blueEnd2,
;   .....
;   untiln, midn, redEndn, greenEndn, blueEndn]
; is must
; - until0 = 0
; - untilN = 1
; - and mid0 is ignored
; See README for overview -- Helge Dietert
function g_segment_fit, X, P
  ; go over every value
  for i=0, 255 do begin
    pos = i/255.0
    segment = 1;
    ; Go to the right segment
    while not (pos le P[1+segment*5]) do segment = segment +1
    ; Now calc the value
    len = P[1+segment*5] - P[1+(segment-1)*5]
    rPos = (pos - P[1+(segment-1)*5]) / len
    rMid = (P[2+segment*5] - P[1+(segment-1)*5]) / len
    f = g_segment(rMid, fix(P[0]), rPos)
    X[i] = P[3+(segment-1)*5] + (P[3+segment*5] - P[3+(segment-1)*5]) * f
    X[i+256] = P[4+(segment-1)*5] + (P[4+segment*5] - P[4+(segment-1)*5]) * f
    X[i+512] = P[5+(segment-1)*5] + (P[5+segment*5] - P[5+(segment-1)*5]) * f
  end
  return, X*255.0
end