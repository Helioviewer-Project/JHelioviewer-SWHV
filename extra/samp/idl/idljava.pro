; how to set up IDL-Java Bridge (https://www.harrisgeospatial.com/docs/initializingtheidl-javabridge.html):
; 
; 1. Download JSAMP from http://www.star.bristol.ac.uk/~mbt/jsamp (or Github: https://github.com/mbtaylor/jsamp)
; 2. Adjust jar-file paths for JSAMP and IDL_JSAMP_Bridge (in initialize_IDL_Java_Bridge)
;    Remember that this routine needs to be called before anything else is done regarding java
; 
; Alternatively to step 2:
; - Go to <IDL_DEFAULT>/resource/bridges/import/java
; - Open your config file (.idljavabrc | idljavabrc | idljavabrc.32), and
;   Overwrite the example .jar in the JVM Classpath with the JSAMP .jar
; - (re) start IDL
;
; Attention: if you build IDL_SAMP_Bridge.jar by yourself, make sure your IDL uses the JRE that corresponds to your JDK!
; If you use a newer JDK, change your IDL configuration file (see above) accordingly!
; see also: https://www.harrisgeospatial.com/Support/Self-Help-Tools/Help-Articles/Help-Articles-Detail/ArtMID/10220/ArticleID/16290/Changing-the-Java-version-used-with-the-IDL-8-Workbench


@utils.pro


; IMPORTANT: this needs to be executed before the IDL-JAVA Bridge is loaded, hence before any java class is invoked!
pro initialize_IDL_Java_Bridge
  path_to_jsamp = 'C:\Projects\JHV\JHelioviewer\resources\SAMP-IDL\jsamp-1.3.5_signed.jar
  path_to_jsamp = path_to_jsamp + ';C:\Projects\JHV\JHelioviewer\resources\SAMP-IDL\IDL_JSAMP_Bridge.jar'
  setenv, 'CLASSPATH=' + getenv('CLASSPATH') + path_to_jsamp
end

; create hub client and connect
function start_idljava
  initialize_IDL_JAVA_Bridge

  jDCP = OBJ_NEW('IDLjavaObject$Static$DEFAULTCLIENTPROFILE', 'org.astrogrid.samp.client.DefaultClientProfile')
  jSampHub = OBJ_NEW('IDLjavaObject$JAVA_OASC_HUBCONNECTOR', 'org.astrogrid.samp.client.HubConnector', jDCP->getProfile())
  obj_destroy, jDCP
  
  jSampHub->setActive, 1 ; connect to hub
  
  jMsgHandler = OBJ_NEW('IDLjavaObject$MessageHandler', 'ch.fhnw.jsamp.IDL_MessageHandler', ["jhv.vso.load"])
  jSampHub->addMessageHandler, jMsgHandler
  jSampHub->declareSubscriptions, jSampHub->computeSubscriptions()
  
  return, Hash('hub', jSampHub, 'msgHandler', jMsgHandler)
end

; convert java wrapper object to IDL object
function convert_java_idl, obj
  case typename(obj) of  ; contrary to switch, case does not need break statements
    'IDLJAVAOBJECT$JAVA_LANG_STRING': return, obj->toString()
    'IDLJAVAOBJECT$JAVA_UTIL_ARRAYLIST': begin
      a = []
      foreach elem, obj->toArray() do begin
        a = [a, convert_java_idl(elem)]
      endforeach
      return, a
    end
    'IDLJAVAOBJECT$JAVA_UTIL_HASHMAP': begin
      h = hash()
      foreach node, (obj->entrySet())->toArray() do begin
        h[(node->getKey())->toString()] = convert_java_idl(node->getValue())
      endforeach
      return, h
    end
  endcase

  print, "cannot convert object of type '", obj, "': ", obj->toString()
  return, !null
end

; check for notifications
function check_notifications, msgHandler
  result = !null
  
  jCall = msgHandler->getLatestCall()
  if jCall ne !null then begin
    msg = jCall->getMessage()
    if msg->getMType() eq 'jhv.vso.load' then result = convert_java_idl(msg->getParams())
    obj_destroy, msg
  endif
  obj_destroy, jCall
  
  return, result
end

; blocking, waits until SAMP-HUB sends a notification and returns it
function wait_for_notification, msgHandler
  while 1 do begin
    n = check_notifications(msgHandler)
    if n ne !null then return, n
    wait, 0.1
  endwhile
  return, !null
end

; download images given the timestamp from the layer HASH-object in the time range +- 2 minutes
; usually there is an image every 3 minutes
function get_vso_img, layer
  range = date_add(layer['timestamp'], m=-2) + ' - ' + date_add(layer['timestamp'], m=2)
  meta = vso_search(date=range, inst=layer['instrument'], wave=layer['measurement']) ; provider=layer['observatory']
  img = vso_get(meta[0])  ; downloads first image into cwd
  AIA_LCT, wavelnth=LONG(layer['measurement']), /LOAD
  
  return, img
end

; destroy IDL Java-Wrapper objects
; also keep in mind you might want to manually remove the images that were downloaded to the current directory of your IDL Console 
pro cleanup_idljava, jSampHub, jMsgHandler
  jSampHub->setActive, 0 ; disconnect from hub
  obj_destroy, jSampHub, jMsgHandler
end
