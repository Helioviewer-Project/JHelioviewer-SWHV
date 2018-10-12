@idljava.pro
@utils.pro

H = start_idljava()
jSampHub = H['hub']
jMsgHandler = H['msgHandler']

; be aware that as of the current version, index "layers" is an array with one element
; direct access requires brackets, e.g.: (n['layers'])[0]
print, "waiting for notification from SAMP-Hub..."
n = wait_for_notification(jMsgHandler)
print, "done."

img_data = get_vso_img((n['layers'])[0])  ; get image(s) for layer0

if typename(img_data) ne 'STRING' then begin
  I = mrdfits(img_data.FILEID)  ; mreadfits for multiple files
  window, 1  ; create plot window
  plot_image, I
  
  M = make_map(I)
  ; plot_map, M
  
  if UINT(n['cutout.set']) then begin
    x0=DOUBLE(n['cutout.x0'])
    y0=DOUBLE(n['cutout.y0'])
    x1=DOUBLE(n['cutout.w'])
    y1=DOUBLE(n['cutout.h'])
    sub_map, M, subM, xrange=[x0, x1], yrange=[y0, y1]
    window, 2
    plot_map, subM
  endif
endif

END