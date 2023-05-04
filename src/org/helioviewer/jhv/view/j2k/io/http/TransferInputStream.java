package org.helioviewer.jhv.view.j2k.io.http;

import java.io.InputStream;

abstract class TransferInputStream extends InputStream {

    public abstract int getTotalLength();

}
