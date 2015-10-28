package org.helioviewer.jhv.viewmodel.view.jp2view.kakadu;

import java.awt.Rectangle;
import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import kdu_jni.Jp2_input_box;
import kdu_jni.Jp2_family_src;
import kdu_jni.Jp2_locator;
import kdu_jni.KduException;
import kdu_jni.Kdu_coords;
import kdu_jni.Kdu_dims;
import kdu_jni.Kdu_global;

import org.helioviewer.jhv.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.SubImage;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPConstants;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPDatabinClass;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPQuery;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPRequest;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPResponse;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPSocket;

/**
 * A collection of useful static methods.
 *
 * @author caplins
 * @author Benjamin Wamsler
 * @author Juan Pablo
 */
public class KakaduUtils {

    /**
     * Converts a Kdu_dims object to its Java equivalent (Rectangle).
     *
     * @param _dims
     *            Kdu_dims to convert
     * @return Rectangle equivalent to the given Kdu_dims
     */
    public static Rectangle kdu_dimsToRect(Kdu_dims dims) {
        Rectangle rect = new Rectangle();
        try {
            Kdu_coords pos = dims.Access_pos();
            Kdu_coords siz = dims.Access_size();
            rect.setBounds(pos.Get_x(), pos.Get_y(), siz.Get_x(), siz.Get_y());
        } catch (KduException ex) {
            ex.printStackTrace();
        }
        return rect;
    }

    /**
     * Converts a Rectangle object to a Kdu_dims object
     *
     * @param _rect
     *            Rectangle to convert
     * @return Kdu_dims equivalent to the given Rectangle
     */
    public static Kdu_dims rectangleToKdu_dims(Rectangle rect) {
        Kdu_dims dims = null;
        try {
            dims = new Kdu_dims();
            Kdu_coords pos = dims.Access_pos();
            Kdu_coords siz = dims.Access_size();
            pos.Set_x(rect.x);
            pos.Set_y(rect.y);
            siz.Set_x(rect.width);
            siz.Set_y(rect.height);
        } catch (KduException ex) {
            ex.printStackTrace();
        }
        return dims;
    }

    /**
     * Converts a SubImage object to a Kdu_dims object
     *
     * @param _roi
     *            SubImage to convert
     * @return Kdu_dims equivalent to the given SubImage
     */
    public static Kdu_dims roiToKdu_dims(SubImage roi) {
        Kdu_dims dims = null;
        try {
            dims = new Kdu_dims();
            Kdu_coords pos = dims.Access_pos();
            Kdu_coords siz = dims.Access_size();
            pos.Set_x(roi.x);
            pos.Set_y(roi.y);
            siz.Set_x(roi.width);
            siz.Set_y(roi.height);
        } catch (KduException ex) {
            ex.printStackTrace();
        }
        return dims;
    }

    /**
     * Downloads all the necessary initial data of an image. In the case of this
     * application, it includes the main header as well as the metadata.
     * JPIPSocket object should already be connected.
     *
     * @param _socket
     * @param _cache
     * @throws IOException
     * @throws JHV_KduException
     */
    public static void downloadInitialData(JPIPSocket socket, JHV_Kdu_cache cache) throws IOException, JHV_KduException {
        JPIPResponse res = null;
        JPIPRequest req = new JPIPRequest(JPIPRequest.Method.GET, new JPIPQuery("stream", "0", "metareq", "[*]!!", "len", Integer.toString(JPIPConstants.MAX_REQUEST_LEN)));

        try {
            do {
                socket.send(req);
                if ((res = socket.receive()) == null)
                    break;
            } while (!cache.addJPIPResponseData(res, null));

            if (!cache.isDataBinCompleted(JPIPDatabinClass.MAIN_HEADER_DATABIN, 0, 0)) {
                req.setQuery(new JPIPQuery("stream", "0"));
                do {
                    socket.send(req);
                    if ((res = socket.receive()) == null)
                        break;
                } while (!cache.addJPIPResponseData(res, null) && !cache.isDataBinCompleted(JPIPDatabinClass.MAIN_HEADER_DATABIN, 0, 0));
            }
        } catch (EOFException e) {
            e.printStackTrace();
        }

        if (!cache.isDataBinCompleted(JPIPDatabinClass.MAIN_HEADER_DATABIN, 0, 0)) {
            throw new IOException("Unable to read all data, data bin is not complete");
        }
    }

    /**
     * Searches the _familySrc for a box of type _boxType (the box types are
     * defined in the Kdu_global class). The method returns the in _boxNumber
     * specified matching box found and its superbox (if any) or null if none
     * were found. The superbox is returned so it can be closed properly after
     * the matching box has been closed.
     *
     * @param _familySrc
     * @param _boxType
     * @param _boxNumber
     * @return Box found and its superbox if one was opened
     * @throws JHV_KduException
     */
    private static Jp2_input_box[] findBox(Jp2_family_src src, long boxType, int boxNumber) throws JHV_KduException {
        Jp2_locator jp2Locator = null;
        Jp2_input_box box = null, box_final = null;
        Jp2_input_box result[] = { null, null };

        try {
            box = new Jp2_input_box();
            box_final = new Jp2_input_box();
            jp2Locator = new Jp2_locator();

            if (!box.Open(src, jp2Locator)) {
                throw new JHV_KduException("Box not open: " + boxNumber);
            } else {
                if (boxType == Kdu_global.jp2_association_4cc) {
                    while (box.Get_box_type() != boxType && box.Exists()) {
                        box.Close();
                        if (!box.Open_next()) {
                            return result;
                        }
                    }

                    if (box.Exists()) {
                        if (!box_final.Open(box)) {
                            return result;
                        }

                        int i = 1;
                        while ((box_final.Get_box_type() != boxType || i < boxNumber) && box_final.Exists()) {
                            if (box_final.Get_box_type() == boxType)
                                i++;
                            box_final.Close();
                            if (!box_final.Open_next()) {
                                return result;
                            }
                        }
                        result[1] = box;
                        box = box_final;
                        box_final = null;
                    }

                    if (!box.Exists() || box.Get_box_type() != boxType) {
                        if (result[1] != null)
                            result[1].Native_destroy();
                        result[1] = null;
                        return result;
                    }
                } else {
                    int i = 1;
                    while ((box.Get_box_type() != boxType || i < boxNumber) && box.Exists()) {
                        if (box.Get_box_type() == boxType)
                            i++;
                        box.Close();
                        if (!box.Open_next()) {
                            return result;
                        }
                    }

                    if (!box.Exists() || box.Get_box_type() != boxType) {
                        return result;
                    }
                }
            }
            result[0] = box;
            return result;
        } catch (KduException ex) {
            throw new JHV_KduException("Internal Kakadu Error (findBox " + boxNumber + "): " + ex.getMessage(), ex);
        } finally {
            if (box_final != null) {
                box_final.Native_destroy();
            }
            if (result[0] != box && box != null) {
                box.Native_destroy();
            }
        }
    }

    /**
     * Searches for a box of type _boxType, but within a superbox, instead of a
     * Jp2_family_src like the previous method. And in this case the search
     * process is quite simpler.
     *
     * @param _supBox
     * @param _boxType
     * @param _boxNumber
     * @return Box found
     * @throws JHV_KduException
     */
    private static Jp2_input_box findBox2(Jp2_input_box supBox, long boxType, int boxNumber) throws JHV_KduException {
        Jp2_input_box box = null;

        try {
            box = new Jp2_input_box();
            if (!box.Open(supBox))
                throw new JHV_KduException("Box not open: " + boxNumber);
            else {
                int i = 1;
                while ((box.Get_box_type() != boxType || i < boxNumber) && box.Exists()) {
                    if (box.Get_box_type() == boxType)
                        i++;
                    box.Close();
                    box.Open_next();
                }

                if (!box.Exists() || box.Get_box_type() != boxType) {
                    box.Native_destroy();
                    box = null;
                }
            }
        } catch (KduException ex) {
            throw new JHV_KduException("Internal Kakadu Error(findBox2 " + boxNumber + "): " + ex.getMessage(), ex);
        }

        return box;
    }

    /**
     * Returns the in _boxNumber specified XML box for an image.
     *
     * @throws JHV_KduException
     */
    public static String getXml(Jp2_family_src src, int boxNumber) throws JHV_KduException {
        String xml = null;
        Jp2_input_box xmlBox = null, assocBox = null, assoc2Box = null;
        Jp2_input_box findBoxResult[];

        findBoxResult = KakaduUtils.findBox(src, Kdu_global.jp2_xml_4cc, boxNumber);
        xmlBox = findBoxResult[0];

        if (xmlBox == null) {
            findBoxResult = KakaduUtils.findBox(src, Kdu_global.jp2_association_4cc, boxNumber);
            assocBox = findBoxResult[0];
            if (assocBox != null) {
                xmlBox = KakaduUtils.findBox2(assocBox, Kdu_global.jp2_xml_4cc, 1);
                if (xmlBox == null) {
                    assoc2Box = KakaduUtils.findBox2(assocBox, Kdu_global.jp2_association_4cc, boxNumber);
                    if (assoc2Box != null)
                        xmlBox = KakaduUtils.findBox2(assoc2Box, Kdu_global.jp2_xml_4cc, 1);
                }
            }
        }

        if (xmlBox != null) {
            try {
                // Grab the xml data if available
                if (xmlBox.Get_remaining_bytes() > 0) {
                    int len = (int) xmlBox.Get_remaining_bytes();
                    byte[] buf = new byte[len];
                    xmlBox.Read(buf, len);
                    xml = new String(buf, "UTF-8");
                }
                xmlBox.Native_destroy();
            } catch (KduException ex) {
                throw new JHV_KduException("Kakadu core error: " + ex.getMessage(), ex);
            } catch (UnsupportedEncodingException ex) {
                ex.printStackTrace();
                xml = null;
            }
        }

        if (assocBox != null) {
            assocBox.Native_destroy();
            assocBox = null;
        }
        if (assoc2Box != null) {
            assoc2Box.Native_destroy();
            assoc2Box = null;
        }
        if (findBoxResult[1] != null) {
            findBoxResult[1].Native_destroy();
            findBoxResult[1] = null;
        }
        if (findBoxResult[0] != null) {
            findBoxResult[0].Native_destroy();
            findBoxResult[0] = null;
        }
        if (xml != null) {
            try {
                if (xml.indexOf("<?xml version=\"1.0\" encoding=\"UTF-8\"?>") != 0)
                    xml = xml.substring(xml.indexOf("<meta>"));
                if (xml.indexOf("<?xml version=\"1.0\" encoding=\"UTF-8\"?>") != 0)
                    xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + xml;
            } catch (Exception ex) {
                throw new JHV_KduException("Failed parsing XML data", ex);
            }
        }

        return xml;
    }

    private static String xmlBox2xml(Jp2_input_box xmlBox) throws JHV_KduException {
        String xml = null;

        try {
            // Grab the xml data if available
            if (xmlBox.Get_remaining_bytes() > 0) {
                int len = (int) xmlBox.Get_remaining_bytes();
                byte[] buf = new byte[len];
                xmlBox.Read(buf, len);
                xml = new String(buf, "UTF-8");
            }
        } catch (KduException ex) {
            throw new JHV_KduException("Kakadu core error: " + ex.getMessage(), ex);
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
            xml = null;
        }

        if (xml != null) {
            try {
                if (xml.indexOf("<?xml version=\"1.0\" encoding=\"UTF-8\"?>") != 0)
                    xml = xml.substring(xml.indexOf("<meta>"));
                if (xml.indexOf("<?xml version=\"1.0\" encoding=\"UTF-8\"?>") != 0)
                    xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + xml;
            } catch (Exception ex) {
                throw new JHV_KduException("Failed parsing XML data", ex);
            }
        }

        return xml;
    }

    private static boolean myFindBox2(Jp2_input_box box, Jp2_input_box supBox, long boxType, int boxNumber) throws JHV_KduException {
        try {
            if (!box.Open(supBox))
                throw new JHV_KduException("Box not open: " + boxNumber);
            else {
                int i = 1;
                while ((box.Get_box_type() != boxType || i < boxNumber) && box.Exists()) {
                    if (box.Get_box_type() == boxType)
                        i++;
                    box.Close();
                    box.Open_next();
                }

                if (!box.Exists() || box.Get_box_type() != boxType) {
                    return false;
                }
            }
        } catch (KduException ex) {
            throw new JHV_KduException("Internal Kakadu Error(myFindBox2 " + boxNumber + "): " + ex.getMessage(), ex);
        }
        return true;
    }

    public static void cacheMetaData(Jp2_family_src src, MetaData[] metaDataList) throws Exception {
        XMLMetaDataContainer hvMetaData = new XMLMetaDataContainer();
        int num = metaDataList.length;

        Jp2_input_box findBoxResult[], assocBox;
        Jp2_input_box xmlBox = new Jp2_input_box();

        findBoxResult = findBox(src, Kdu_global.jp2_association_4cc, 1);
        assocBox = findBoxResult[0];
        if (assocBox != null) {
            for (int i = 0; i < num; i++) {
                try {
                    if (myFindBox2(xmlBox, assocBox, Kdu_global.jp2_xml_4cc, 1) == true) {
                        hvMetaData.parseXML(xmlBox2xml(xmlBox));
                        metaDataList[i] = new HelioviewerMetaData(hvMetaData);
                        hvMetaData.destroyXML();
                    }

                    xmlBox.Close();
                    assocBox.Close();
                    assocBox.Open_next();
                } catch (KduException ex) {
                    throw new JHV_KduException("Kakadu core error: " + ex.getMessage(), ex);
                }
            }
        } else { // JP2
            findBoxResult = KakaduUtils.findBox(src, Kdu_global.jp2_xml_4cc, 1);
            xmlBox = findBoxResult[0];
            if (xmlBox != null) {
                hvMetaData.parseXML(xmlBox2xml(xmlBox));
                metaDataList[0] = new HelioviewerMetaData(hvMetaData);
                hvMetaData.destroyXML();
            }
        }

        if (xmlBox != null) {
            xmlBox.Native_destroy();
        }
        if (findBoxResult[1] != null) {
            findBoxResult[1].Native_destroy();
        }
        if (findBoxResult[0] != null) {
            findBoxResult[0].Native_destroy();
        }
    }

}
