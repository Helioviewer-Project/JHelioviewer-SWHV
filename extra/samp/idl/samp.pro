; author: Thomas Boch
;
; synopsis:
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;; 1ere partie ;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;.r samp
;; chargement de iras60.fits et iras100.fits dans Aladin
;; recuperation de chaque image dans IDL
;get_aladin_image, 'iras100*', im100, hdr100
;print, hdr100
;help, im100
;get_aladin_image, 'iras60*', im60, hdr60
;; creation carte de temperature
;map = im100/im60
;load_image, map, hdr100, PLANENAME='TempMap'
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;; 2eme partie ;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;.r samp
;rdfloat, 'taurus.cat', glon, glat, j, h, k
;euler, glon, glat, ra, dec, 2
;; Lancer Aladin et TOPCAT
;id=send_table( ra, dec, j, h, k, COLNAMES=['ra','dec','J','H','K'])
;; faire plot dans TOPCAT : K en fonction de J-H
;t = where(j*k ne 0 AND j-k gt 1.5)
;select_obj, t, id


; TODO : objet samp et objet aladin contenant les differentes methodes

; read an ASCII file and returns its content as one string
FUNCTION read_file, file
    readcol, file,v,FORMAT='A',DELIMITER='£'
    s = ''
    for i=0,(size(v))[1]-1 do begin
        s += strtrim(v[i], 1)
    endfor
    return, s
END

; return the value (in an XML-RPC string) of a given param
FUNCTION get_in_xmlrpc, s, param_name
    idx_start = strpos(s, param_name)
    if idx_start lt 0 then return, ''

    tmp = strmid(s, idx_start)
    idx_start = strpos(tmp, '<value>')
    idx_end = strpos(tmp, '</value>')
    if idx_start lt 0 or idx_end lt 0 then return, ''

    return, strmid(tmp, idx_start+7, idx_end-idx_start-7)

    return, tmp
END


FUNCTION read_samp_key, key
    home = GETENV('HOME')

    readcol,home+'/.samp',v,FORMAT='A',DELIMITER='£'

    for i=0,(size(v))[1]-1 do begin
	idx = strpos(v[i], key)
	if idx ge 0 then begin
	    return, strmid(v[i],strlen(key)+1)
	endif
    endfor

    return, ''
END

FUNCTION read_samp_secret
    return, read_samp_key('samp.secret')
END

FUNCTION read_samp_huburl
    return, read_samp_key('samp.hub.xmlrpc.url')
END

FUNCTION build_xmlrpc, method_name, params
    xml_header  = "<?xml version='1.0'?>"

    s = '<methodCall>'
    s += '<methodName>'
    s += method_name
    s += '</methodName>'
    s += '<params>'
    s += params
    s += '</params>'
    s += '</methodCall>'

    xmlrpc = [xml_header, s]
    return, xmlrpc
END

FUNCTION register
    ourl = OBJ_NEW('IDLnetUrl')
    hub_url = read_samp_huburl()
    params  = '<param>'
    params += '<value><string>'+read_samp_secret()+'</string></value>'
    params += '</param>'
    xmlrpc_msg = build_xmlrpc('samp.hub.register', params)
    a = ourl -> put(xmlrpc_msg, url = hub_url,/buffer,/post)
    OBJ_DESTROY, ourl

    response = read_file(a)
    hub_id =  get_in_xmlrpc(response, 'samp.hub-id')
    self_id =  get_in_xmlrpc(response, 'samp.self-id')
    private_key =  get_in_xmlrpc(response, 'samp.private-key')

    return, {hubid: hub_id, selfid: self_id, privatekey:private_key}
END

PRO unregister, private_key
    ourl = OBJ_NEW('IDLnetUrl')
    hub_url = read_samp_huburl()
    params  = '<param>'
    params += '<value><string>'+private_key+'</string></value>'
    params += '</param>'
    xmlrpc_msg = build_xmlrpc('samp.hub.unregister', params)
    a = ourl -> put(xmlrpc_msg, url = hub_url,/buffer,/post)
    OBJ_DESTROY, ourl
END

; TODO : étendre pour que ça renvoie un tableau contenant toutes les applis !!, pas seulement la premiere
; find all registered applications supporting the given mtype
FUNCTION find_samp_app, mtype, private_key
    ourl = OBJ_NEW('IDLnetUrl')
    hub_url = read_samp_huburl()
    params  = '<param>'
    params += '<value><string>'+private_key+'</string></value>'
    params += '</param>'
    params += '<param>'
    params += '<value><string>'+mtype+'</string></value>'
    params += '</param>'
    xmlrpc_msg = build_xmlrpc('samp.hub.getSubscribedClients', params)
    a = ourl -> put(xmlrpc_msg, url = hub_url,/buffer,/post)
    OBJ_DESTROY, ourl
    response = read_file(a)
    idx_start = strpos(response, '<name>')
    idx_end = strpos(response, '</name>')
    if idx_start lt 0 or idx_end lt 0 then return, ''

    return, strmid(response, idx_start+6, idx_end-idx_start-6)
END

PRO send_samp_notification, mtype, msg_params, recipient, private_key
    ourl = OBJ_NEW('IDLnetUrl')
    hub_url = read_samp_huburl()

    ; private key
    params  = '<param><value><string>'+private_key+'</string></value></param>'
    ; recipient id
    params += '<param><value><string>'+recipient+'</string></value></param>'

    params += '<param><value><struct>'
    params += '<member>'
    ; mtype
    params += '<name>samp.mtype</name>'
    params += '<value>'+mtype+'</value>'
    params += '</member>'

    ; message params
    params += '<member>'
    params += '<name>samp.params</name>'
    params += msg_params
    params += '</member>'

    params += '</struct></value></param>'


    xmlrpc_msg = build_xmlrpc('samp.hub.notify', params)
    a = ourl -> put(xmlrpc_msg, url = hub_url,/buffer,/post)
    OBJ_DESTROY, ourl

END

PRO broadcast_samp_notification, mtype, msg_params, private_key
     ourl = OBJ_NEW('IDLnetUrl')
    hub_url = read_samp_huburl()

    ; private key
    params  = '<param><value><string>'+private_key+'</string></value></param>'

    params += '<param><value><struct>'
    params += '<member>'
    ; mtype
    params += '<name>samp.mtype</name>'
    params += '<value>'+mtype+'</value>'
    params += '</member>'

    ; message params
    params += '<member>'
    params += '<name>samp.params</name>'
    params += msg_params
    params += '</member>'

    params += '</struct></value></param>'


    xmlrpc_msg = build_xmlrpc('samp.hub.notifyAll', params)
    a = ourl -> put(xmlrpc_msg, url = hub_url,/buffer,/post)
    OBJ_DESTROY, ourl
END

FUNCTION send_votable, URL=url, ID=id
    if not keyword_set(id) then id = url

    a = register()

    msg_params  =  '<value><struct>'
    msg_params +=  '<member><name>table-id</name>'
    msg_params +=  '<value>'+id+'</value></member>'
    msg_params +=  '<member><name>url</name>'
    msg_params +=  '<value>'+url+'</value></member>'
    msg_params +=  '</struct></value>'

    mtype = 'table.load.votable'

    broadcast_samp_notification, mtype, msg_params, a.privatekey

    unregister, a.privatekey

    return, id
END


PRO send_aladin_script, script
    a = register()
    mtype = 'script.aladin.send'
    aladin_name = find_samp_app(mtype, a.privatekey)

    ; TODO : attention aux caracteres speciaux, il faudra les encoder
    msg_params  =  '<value><struct>'
    msg_params +=  '<member><name>script</name>'
    msg_params +=  '<value>'+script+'</value></member>'
    msg_params +=  '</struct></value>'

    send_samp_notification, mtype, msg_params, aladin_name, a.privatekey

    unregister, a.privatekey
END

PRO get_aladin_image, plane_name, im, hdr
    home = GETENV('HOME')
    ; TODO : essayer de faire qqch de plus propre
    file = home+'/.idl_temp_image'
    cmd = 'export "'+plane_name+'"'+' '+file+';sync'

    send_aladin_script, cmd

    im = readfits(file, hdr)

END

PRO load_image, im, hdr, PLANENAME=planename
    if not keyword_set(planename) then planename = 'IDL_image'


    home = GETENV('HOME')
    ; at the time, this is uglier but much simpler than passing the data array
    file = home+'/.idl/idl_image'
    writefits, file, im, hdr

    send_aladin_script, 'load '+file+';sync;rename idl_image '+planename

END

FUNCTION generate_votable, array, names
    home = GETENV('HOME')
    ; TODO : essayer de faire qqch de plus propre
    file = home+'/.idl/idl_table'
    OPENW, 1, file
    printf, 1, '<?xml version="1.0"?>'
    printf, 1, '<VOTABLE>'
    printf, 1, '<RESOURCE>'
    printf, 1, '<TABLE>'
    for i=0,(size(names))[1]-1 do begin
        printf, 1, '<FIELD ID="'+names[i]+'" name="'+names[i]+'" '
	if i eq 0 then printf, 1, 'ucd="pos.eq.ra;meta.main" '
	if i eq 1 then printf, 1, 'ucd="pos.eq.dec;meta.main" '
	printf, 1, 'datatype="double" />' 
    endfor

    printf, 1, '<DATA>'
    printf, 1, '<TABLEDATA>'
    for i=0L,(size(array[*,0]))[1]-1 do begin
        printf, 1, '<TR>'
	for j=0,(size(array[i,*]))[2]-1 do begin
	   printf, 1, '<TD>'+strtrim(string(array[i,j]), 1)+'</TD>'
	endfor
        printf, 1, '</TR>'
    endfor
    printf, 1, '</TABLEDATA>'
    printf, 1, '</DATA>'
    printf, 1, '</TABLE>'
    printf, 1, '</RESOURCE>'
    printf, 1, '</VOTABLE>'
    CLOSE, 1

    return, file
END

FUNCTION send_table, v1, v2, v3, v4, v5, v6, v7, v8, v9, COLNAMES=colnames
        if keyword_set( v9 ) then begin array = [ [v1], [v2], [v3], [v4], [v5], [v6], [v7], [v8], [v9] ]
        endif else if keyword_set( v8 ) then begin array = [ [v1], [v2], [v3], [v4], [v5], [v6], [v7], [v8] ]
        endif else if keyword_set( v7 ) then begin array = [ [v1], [v2], [v3], [v4], [v5], [v6], [v7] ]
        endif else if keyword_set( v6 ) then begin array = [ [v1], [v2], [v3], [v4], [v5], [v6] ]
        endif else if keyword_set( v5 ) then begin array = [ [v1], [v2], [v3], [v4], [v5] ]
        endif else if keyword_set( v4 ) then begin array = [ [v1], [v2], [v3], [v4] ]
        endif else if keyword_set( v3 ) then begin array = [ [v1], [v2], [v3] ]
        endif else if keyword_set( v2 ) then begin array = [ [v1], [v2] ]
	endif



	return, send_votable( URL='file://'+generate_votable(array, colnames))
END

PRO select_obj, indexes, id
    a = register()    

    indexes = strtrim(string(indexes),1)
    msg_params  =  '<value><struct>'
    msg_params +=  '<member><name>table-id</name>'
    msg_params +=  '<value>'+id+'</value></member>'
    msg_params +=  '<member><name>row-list</name>'
    msg_params +=  '<value>'
    msg_params +=  '<array><data>'
    for i=0L,(size(indexes))[1]-1 do begin
        msg_params += '<value><string>'+indexes[i]+'</string></value>'
    endfor
    msg_params +=  '</data></array>'
    msg_params +=  '</value></member>'
    msg_params +=  '</struct></value>'


    mtype = 'table.select.rowList'
    broadcast_samp_notification, mtype, msg_params, a.privatekey

    ; TODO : connexion persistente ??
    unregister, a.privatekey

END
