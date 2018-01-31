package org.helioviewer.jhv.base;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class XMLUtils {

    public static Document parse(String xml) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

}
