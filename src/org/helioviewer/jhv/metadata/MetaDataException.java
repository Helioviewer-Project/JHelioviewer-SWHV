package org.helioviewer.jhv.metadata;

@SuppressWarnings("serial")
class MetaDataException extends RuntimeException {

    // public MetaDataException() { super(); }

    MetaDataException(String s) { super(s + " not found in metadata"); }

}
